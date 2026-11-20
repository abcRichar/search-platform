# 聚合搜索平台（search-platform）

一个用于学习的全栈 Demo：**MySQL 存主数据 + Elasticsearch 做搜索层 + 定时/实时同步 + React 前端**。
一个搜索框同时返回三类结果：**帖子（ES）、用户（ES）、图片（必应网页抓取）**。

---

## 目录

1. [项目能做什么](#1-项目能做什么)
2. [技术栈与整体架构](#2-技术栈与整体架构)
3. [目录结构](#3-目录结构)
4. [环境准备](#4-环境准备)
5. [启动项目](#5-启动项目)
6. [使用指南](#6-使用指南)
7. [API 一览](#7-api-一览)
8. [后端模块详解](#8-后端模块详解)
9. [前端模块详解](#9-前端模块详解)
10. [两条核心数据流](#10-两条核心数据流)
11. [推荐的学习/阅读顺序](#11-推荐的学习阅读顺序)
12. [常见问题排查](#12-常见问题排查)
13. [已知限制与可扩展方向](#13-已知限制与可扩展方向)

---

## 1. 项目能做什么

| 功能 | 说明 |
|---|---|
| 聚合搜索 | 输入一个关键词，同时搜「帖子」「用户」「必应图片」，前端用 Tab 分类展示 |
| 用户体系 | 注册 / 登录 / 获取当前用户，密码 MD5+盐，登录颁发 JWT |
| 帖子管理 | 管理员可新增、编辑、删除帖子；**写 MySQL 后立即同步到 ES** |
| 搜索中台思路 | 数据源可插拔：新增一种数据源只需加一个 `searchXxx()` 和聚合结果里的一个 key |

### 内置演示账号

首次启动后端时，`DataSeeder` 会在 `users` 表为空时自动写入：

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | admin（可进「帖子管理」） |
| `张三` | `123456` | user |
| `李四` | `123456` | user |

同时写入 8 篇演示帖子。**只有 users 表为空时才会初始化**，想重置数据就清空 `users`/`posts` 表再重启。

---

## 2. 技术栈与整体架构

### 技术栈

- **后端**：Java 17、Spring Boot 3.3.5、Spring Data Elasticsearch、MyBatis 3、MySQL 8、JWT（jjwt 0.12.6）、Maven
- **搜索**：Elasticsearch 8.13.4 + **IK 中文分词插件**（`analysis-ik`）
- **前端**：React 18、Vite 5、Ant Design 5、axios、react-router-dom 6
- **数据库**：MySQL 8（库名 `search_platform`）

### 架构图

```
┌────────────────────────────────────────────────────────────┐
│ 浏览器  http://localhost:5173                                │
│   React + AntD（Vite dev server，/api 代理到 8080）           │
└───────────────┬────────────────────────────────────────────┘
                │ HTTP /api/**
                ▼
┌────────────────────────────────────────────────────────────┐
│ Spring Boot  http://localhost:8080                          │
│  Controller → Service → Mapper / ES Repository              │
└──────┬───────────────────────────────┬─────────────────────┘
       │ MyBatis                       │ Spring Data ES
       ▼                               ▼
┌──────────────┐              ┌────────────────────────┐
│ MySQL 3306   │  同步任务     │ Elasticsearch 9200     │
│ users/posts  │ ───────────► │ posts / users 索引      │
│ （主数据源）   │ EsSyncJob    │ （搜索层，IK 分词）      │
└──────────────┘              └────────────────────────┘
                                   ▲
                                   │ 实时同步（增/改/删）
                                   │ 定时全量（每 60s）
```

**核心设计原则**：

- **MySQL 是唯一事实来源（source of truth）**，负责事务和持久化。
- **ES 只是搜索副本**，删了可以随时从 MySQL 重建。
- 读搜索走 ES，不压 MySQL；写数据走 MySQL，再同步到 ES。
- 用户密码**永远不进 ES**（`UserDocument` 只有 id/username/avatarUrl）。

---

## 3. 目录结构

```
search-platform/
├── README.md                      ← 本文档
├── backend/                       ← Spring Boot 后端
│   ├── pom.xml                    ← Maven 依赖
│   ├── schema.sql                 ← 建表脚本（users / posts）
│   └── src/main/
│       ├── java/com/example/search/
│       │   ├── SearchApplication.java      ← 启动类
│       │   ├── common/                     ← 通用基础设施
│       │   │   ├── Result.java             ← 统一响应体
│       │   │   ├── BusinessException.java  ← 业务异常
│       │   │   ├── GlobalExceptionHandler.java ← 全局异常处理
│       │   │   ├── JwtUtil.java            ← JWT 生成/解析
│       │   │   ├── LoginInterceptor.java   ← 登录拦截器
│       │   │   └── AdminInterceptor.java   ← 管理员拦截器
│       │   ├── config/
│       │   │   ├── WebConfig.java          ← 注册拦截器
│       │   │   └── DataSeeder.java         ← 首次启动灌演示数据
│       │   ├── controller/                 ← HTTP 接口层
│       │   │   ├── UserController.java     ← 注册/登录/当前用户
│       │   │   ├── PostController.java     ← 帖子列表（公开）
│       │   │   ├── SearchController.java   ← 聚合搜索
│       │   │   └── AdminPostController.java← 管理端增删改
│       │   ├── service/                    ← 业务逻辑层
│       │   │   ├── UserService.java
│       │   │   ├── PostService.java
│       │   │   ├── SearchService.java
│       │   │   └── ImageSearchService.java
│       │   ├── mapper/                     ← MyBatis 接口（注解 SQL）
│       │   │   ├── UserMapper.java
│       │   │   └── PostMapper.java
│       │   ├── model/                      ← MySQL 实体
│       │   │   ├── User.java
│       │   │   └── Post.java
│       │   ├── doc/                        ← ES 搜索文档实体 + Repository
│       │   │   ├── PostDocument.java
│       │   │   ├── UserDocument.java
│       │   │   ├── PostDocumentRepository.java
│       │   │   └── UserDocumentRepository.java
│       │   └── job/
│       │       └── EsSyncJob.java          ← 定时全量同步
│       └── resources/application.yml       ← 端口、MySQL、ES、JWT、同步配置
│
└── frontend/                      ← React 前端
    ├── package.json
    ├── vite.config.js             ← 端口 5173 + /api 代理
    ├── index.html
    └── src/
        ├── main.jsx               ← 入口：AntD 中文 + Router + AuthProvider
        ├── App.jsx                ← 顶栏 + 路由
        ├── api.js                 ← axios 实例 + 拦截器
        ├── context/AuthContext.jsx← 登录态（localStorage）
        └── pages/
            ├── HomePage.jsx       ← 搜索首页（三个 Tab）
            ├── LoginPage.jsx
            ├── RegisterPage.jsx
            └── AdminPage.jsx      ← 帖子管理（表格 + 弹窗表单）
```

---

## 4. 环境准备

| 依赖 | 版本要求 | 检查命令 |
|---|---|---|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -v` |
| Node.js | 18+ | `node -v` |
| MySQL | 8.x，运行在 3306 | 见下方 |
| Elasticsearch | 8.13.4，运行在 9200，**需安装 analysis-ik 插件** | `curl http://localhost:9200` |

> 本机开发环境（已验证）：
> - MySQL 客户端：`C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`
> - Elasticsearch：`D:\elasticsearch-8.13.4`（`config/elasticsearch.yml` 已设 `xpack.security.enabled: false`、`discovery.type: single-node`）

### 4.1 初始化 MySQL

```bash
# 1) 建库
mysql -uroot -p123456 -h127.0.0.1 -e \
  "create database if not exists search_platform default charset utf8mb4;"

# 2) 建表
cd backend
mysql -uroot -p123456 -h127.0.0.1 < schema.sql
```

> 如果本机 MySQL 密码不是 `123456`，改 `backend/src/main/resources/application.yml` 的 `spring.datasource.password`。

### 4.2 启动 Elasticsearch

```bash
# Windows
D:\elasticsearch-8.13.4\bin\elasticsearch.bat

# 验证
curl http://127.0.0.1:9200
```

确认 IK 插件在：`D:\elasticsearch-8.13.4\plugins\analysis-ik`。
没有的话，到 https://github.com/infinilabs/analysis-ik/releases 下载对应 8.13.4 版本，解压到 `plugins/analysis-ik` 后重启 ES。

> 如果没有 IK 插件，索引映射里的 `ik_max_word` 会导致建索引/写入失败。

---

## 5. 启动项目

按顺序启动：**MySQL → ES → 后端 → 前端**。

```bash
# 1) 后端（端口 8080）
cd backend
mvn spring-boot:run

# 2) 前端（端口 5173）
cd frontend
npm install        # 首次
npm run dev
```

打开 **http://localhost:5173**。

后端启动成功的标志（日志）：

```
Started SearchApplication in 3.098 seconds
HikariPool-1 - Start completed.
```

ES 同步在启动 **15 秒后**开始，之后每 **60 秒**全量刷一次。可验证：

```bash
curl http://127.0.0.1:9200/_cat/indices?v      # 应看到 posts / users
curl http://127.0.0.1:9200/posts/_count        # 应有 8 条
```

### 打包发布（可选）

```bash
cd backend
mvn clean package -DskipTests
java -jar target/search-platform-1.0.0.jar

cd frontend
npm run build      # 产物在 dist/，用 Nginx 托管并把 /api 反向代理到 8080
```

---

## 6. 使用指南

1. **搜索**：首页搜索框输入关键词（如 `elasticsearch`、`张三`、`猫`），回车。结果分三个 Tab：
   - **帖子**：来自 ES 的 `posts` 索引，标题/内容命中词高亮；
   - **用户**：来自 ES 的 `users` 索引；
   - **图片**：实时抓取必应图片搜索页。
2. **注册 / 登录**：右上角。登录后 token 存 `localStorage`，后续请求自动带 `Authorization: Bearer <token>`。
3. **帖子管理**：用 `admin / 123456` 登录后，顶栏出现「帖子管理」，可新增/编辑/删除帖子。
   保存成功会提示“ES 已同步”，此时去首页搜索马上能搜到。
4. **退出**：清空本地 token 与用户信息。

---

## 7. API 一览

统一响应体（HTTP 状态码基本恒为 200，业务状态看 `code`）：

```json
{ "code": 0, "data": ..., "message": "ok" }
```

`code` 约定：`0` 成功；`40000` 业务/参数错误；`40100` 未登录或 token 失效；`40300` 无管理员权限；`50000` 服务器内部错误。

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/api/user/register` | 注册，body `{username, password}` | 否 |
| POST | `/api/user/login` | 登录，返回 `{token,userId,username,role,avatarUrl}` | 否 |
| GET | `/api/user/me` | 当前用户信息 | 登录 |
| GET | `/api/post/list` | 帖子列表（带作者名） | 否 |
| GET | `/api/search/all?q=&page=0&size=20` | 聚合搜索 | 否 |
| POST | `/api/admin/post/add` | 新增帖子，body `{title, content, tags}` | admin |
| PUT | `/api/admin/post/update` | 编辑帖子，body 含 `id` | admin + 本人帖子 |
| DELETE | `/api/admin/post/delete/{id}` | 删除帖子 | admin |

### 聚合搜索响应示例

```json
{
  "code": 0,
  "data": {
    "posts": [
      { "id": 1, "title": "Elasticsearch 入门指南", "content": "...",
        "tags": "elasticsearch,搜索", "userId": 1, "createTime": "2026-11-20T16:17:19" }
    ],
    "users": [],
    "images": [
      { "url": "https://...jpg", "thumbnail": "https://ts1.mm.bing.net/...",
        "title": "猫 摄影图…", "source": "bing" }
    ],
    "total": 2, "page": 0, "size": 20
  },
  "message": "ok"
}
```

> 搜索参数名是 **`q`**，不是 `keyword`。中文关键词记得 URL 编码。

---

## 8. 后端模块详解

### 8.1 启动与配置

- **`SearchApplication`**：`@SpringBootApplication` + `@EnableScheduling`（开启定时同步）+ `@MapperScan`（扫描 MyBatis 接口）。
- **`application.yml`**：
  - 端口 8080；MySQL 连接串、账号密码；
  - `spring.elasticsearch.uris`；
  - `app.jwt.secret`（HS256 密钥，≥32 字符）、`app.jwt.expire-days`；
  - `app.sync.interval-ms`（全量同步间隔，默认 60000）。

### 8.2 `common/` —— 通用基础设施

| 类 | 作用 | 学习要点 |
|---|---|---|
| `Result` | 统一响应 `{code,data,message}` | 前后端约定 |
| `BusinessException` | 业务异常，自带 code | 与全局异常处理配合 |
| `GlobalExceptionHandler` | `@RestControllerAdvice` 统一捕获，转成 Result | 参数校验异常也在这里转 |
| `JwtUtil` | 生成/解析 JWT（subject=userId，claim role） | `Keys.hmacShaKeyFor`、jjwt 0.12 API |
| `LoginInterceptor` | 校验 `Authorization: Bearer xxx`，把 userId/role 放进 request attribute | 拦截器模式；鉴权失败**也返回 HTTP 200**，code=40100 |
| `AdminInterceptor` | 读取上一步放入的 role，非 admin 返回 40300 | 两个拦截器按顺序执行 |

拦截器注册在 **`config/WebConfig`**：

```java
loginInterceptor  → /api/user/me, /api/admin/**
adminInterceptor  → /api/admin/**
```

即：`/api/admin/**` 先过登录，再过管理员校验。

### 8.3 `model/` + `mapper/` —— MySQL 持久层

- `model/User`、`model/Post`：与表字段一一对应的 POJO。
- `UserMapper`、`PostMapper`：**注解式 MyBatis**（`@Select/@Insert/@Update/@Delete`），无 XML。
  - `PostMapper.findAllWithAuthor()` 用 `left join users` 一次性带出作者名。
  - `@Options(useGeneratedKeys=true, keyProperty="id")` 回填自增主键。

### 8.4 `doc/` —— Elasticsearch 搜索层

| 类 | 对应索引 | 说明 |
|---|---|---|
| `PostDocument` | `posts` | `title`/`content` 用 `ik_max_word` 分词；`tags` 为 keyword；`createTime` 指定日期格式 |
| `UserDocument` | `users` | 只有 id/username/avatarUrl，**不含密码** |
| `*DocumentRepository` | — | 继承 `ElasticsearchRepository`，提供 `save/saveAll/deleteById` |

`PostDocument` 的字段注解决定 ES mapping（可用 `GET /posts/_mapping` 验证）。
**`@Document(indexName = "posts")` 会自动建索引**（Spring Data ES 启动时按实体建 mapping）。

### 8.5 `service/` —— 业务逻辑

**`UserService`**
- 注册：校验用户名（字母/数字/下划线/中文，≤50）、密码（6~100），查重，`MD5(固定盐 + 密码)` 入库，默认 role=user。
- 登录：比对密码，签发 JWT，返回 token 和用户信息。
- `getById` 供 `/me` 使用；Controller 会把 password 置空再返回。

**`PostService`**（读写分离的关键）
- `listAllWithAuthor()`：查 MySQL（浏览列表）。
- `add()`：校验标题/内容长度 → 写 MySQL → 回填 id → **`syncOne()` 立即写 ES**。
- `update()`：查存在 → **校验只能改自己的帖子** → 写 MySQL → 重新查 → 同步 ES。
- `delete()`：删 MySQL + `postEs.deleteById(id)`。
- `toDocument()`：MySQL 实体 → ES 文档的转换函数（也被定时任务复用）。

**`SearchService`**（搜索中台核心）
- `searchPosts()`：ES `multiMatch`，字段权重 `title^2`、`content`、`tags^1.5`，分页。
- `searchUsers()`：`multiMatch` 查 `username`。
- `searchImages()`：委托 `ImageSearchService`，异常时返回空列表（降级）。
- 每个数据源一个独立方法 —— 新增数据源就在这里加一个 `searchXxx`。

**`ImageSearchService`**（外部数据源示例）
- 请求 `https://cn.bing.com/images/search?q=...`，从 HTML 中用正则提取 `murl`（原图）、`turl`（缩略图）、`t`（标题）。
- **注意**：必应会把 JSON 做 HTML 实体转义（`&quot;murl&quot;:&quot;...&quot;`），所以代码先做 `&quot;→"`、`&amp;→&` 等解码，再做正则匹配。
- 任何网络异常都静默降级为空列表，不影响帖子/用户结果。

### 8.6 `job/EsSyncJob` —— 定时全量同步

- `@Scheduled(fixedDelayString="${app.sync.interval-ms}", initialDelay=15000)`；
- 全量读取 MySQL 的 posts/users，转成 Document 后 `saveAll` 到 ES；
- 存在意义：**兜底直接改库/sql、ES 索引被误删**等场景；数据量大时应改为按 `update_time` 增量同步。

### 8.7 `config/DataSeeder`

- 实现 `CommandLineRunner`，启动时执行一次；
- `users` 表非空则跳过；
- 否则插入 3 个用户 + 8 篇帖子（密码同样是 MD5+盐）。

### 8.8 Controller 层

| Controller | 路径 | 说明 |
|---|---|---|
| `UserController` | `/api/user` | register / login / me |
| `PostController` | `/api/post` | list（公开） |
| `SearchController` | `/api/search` | all（聚合搜索，校验 q 非空且 ≤200 字符） |
| `AdminPostController` | `/api/admin/post` | add / update / delete，从 request attribute 取 userId |

---

## 9. 前端模块详解

### 9.1 入口与路由

- **`main.jsx`**：`ConfigProvider(zhCN)` + `BrowserRouter` + `AuthProvider` 包裹 `App`。
- **`App.jsx`**：AntD `Layout` 顶栏（Logo、「帖子管理」仅 admin 可见、登录/注册/退出）+ 四个路由：
  `/`、`/login`、`/register`、`/admin`。

### 9.2 `api.js` —— axios 封装（重点）

- `baseURL: '/api'`，由 Vite 代理到 8080，避免 CORS。
- **请求拦截器**：自动从 `localStorage` 取 token 塞进 `Authorization: Bearer`。
- **响应拦截器**：拆开 `Result`，`code !== 0` 时 reject(message)；`code === 40100` 时清掉本地 token。
  所以页面里 `await api.get(...)` 直接拿到 `data`，不用再判断 code。

### 9.3 `context/AuthContext.jsx`

- 登录态来源：`localStorage` 里的 `user`；
- `login(info)`：存 `token` 与 `user`（去掉 token 后的信息）；
- `logout()`：清空；
- 暴露 `isAdmin = user?.role === 'admin'`。

### 9.4 页面

| 页面 | 要点 |
|---|---|
| `HomePage` | 搜索框 → `GET /search/all`；三个 Tab 分别渲染帖子/用户/图片；`highlight()` 用正则高亮关键词；图片用 `Image.PreviewGroup` 支持预览 |
| `LoginPage` | 表单 → `POST /user/login` → `login(data)` → 跳首页 |
| `RegisterPage` | 表单 → `POST /user/register` → 跳登录 |
| `AdminPage` | 表格列出全部帖子；Modal + Form 新增/编辑；`Popconfirm` 删除；**前端也会挡非管理员**（后端拦截器兜底） |

---

## 10. 两条核心数据流

### 读路径（搜索）

```
HomePage 输入关键词
  → api.get('/search/all', {params:{q}})
  → Vite 代理 → SearchController
  → SearchService.searchPosts  → ES posts 索引（multiMatch）
  → SearchService.searchUsers  → ES users 索引
  → SearchService.searchImages → ImageSearchService → 必应 HTML
  → 合并 {posts, users, images} → Result
  → 前端按 Tab 渲染
```

### 写路径（管理员改帖子）

```
AdminPage 提交表单
  → POST/PUT/DELETE /api/admin/post/**
  → LoginInterceptor（登录）→ AdminInterceptor（admin）
  → AdminPostController → PostService
  → MySQL insert/update/delete
  → 立即同步 ES（save / deleteById）
  → 同时 EsSyncJob 每 60s 全量兜底
```

---

## 11. 推荐的学习/阅读顺序

按这个顺序读，能由浅入深串起整条链路：

1. **先跑起来**：按第 4~5 节启动，打开页面手动体验搜索/登录/管理。
2. **看接口**：`backend/.../controller/*.java` —— 四个 Controller，理解 URL 与参数。
3. **看统一约定**：`common/Result.java` → `GlobalExceptionHandler.java`，理解前后端如何约定错误。
4. **看鉴权**：`JwtUtil` → `LoginInterceptor` → `AdminInterceptor` → `WebConfig`，理解 token 怎么签发、怎么拦住请求。
5. **看持久层**：`model/` + `mapper/`，理解注解式 MyBatis。
6. **看搜索层**：`doc/PostDocument.java`（字段注解 → ES mapping）+ `SearchService`（multiMatch 加权）。
7. **看数据同步**：`PostService.syncOne`（实时）+ `EsSyncJob`（定时），理解“主数据 + 搜索副本”。
8. **看外部数据源**：`ImageSearchService`，理解聚合搜索如何接入第三方。
9. **看前端**：`api.js` 拦截器 → `AuthContext` → `HomePage` 的渲染与高亮 → `AdminPage` 的 CRUD。
10. **动手改造**（最好的学习方式）：
    - 给搜索加一种新数据源（比如「视频」）：新增 Document + Repository + `SearchService.searchVideos` + 前端 Tab；
    - 把全量同步改成按 `update_time` 增量同步；
    - 把 MD5+盐 换成 BCrypt。

### 关键概念对照表

| 概念 | 本项目中的位置 |
|---|---|
| 倒排索引 / 分词 | `@Field(analyzer="ik_max_word")`、ES mapping |
| 多字段加权搜索 | `multiMatch(fields("title^2","content","tags^1.5"))` |
| 主数据与搜索副本 | MySQL 表 vs ES 索引；`EsSyncJob` |
| 读写分离 | 浏览列表查 MySQL；搜索查 ES |
| JWT 无状态鉴权 | `JwtUtil` + 两个 Interceptor |
| 统一响应/全局异常 | `Result` + `GlobalExceptionHandler` |
| 前端请求封装 | `api.js` 请求/响应拦截器 |
| 外部数据源聚合 | `ImageSearchService` |

---

## 12. 常见问题排查

| 现象 | 原因 / 解决 |
|---|---|
| 后端启动报 `Communications link failure` | MySQL 没起，或账号密码不对（改 `application.yml`） |
| 启动报 `Unknown table/users` | 没建库建表，执行第 4.1 节 |
| ES 相关报错 / 索引建不出来 | ES 没启动（`curl :9200`），或缺少 **IK 插件** |
| 搜索帖子/用户一直为空 | `EsSyncJob` 启动 15s 后才首次同步；或查看 `GET /posts/_count`；也可以等 60s 下一轮 |
| 图片 Tab 为空 | 必应网络不通，或页面结构再改版（正则需适配）；属可降级功能 |
| 登录后请求仍 40100 | 本地 token 过期/被清；重新登录。JWT 默认 7 天 |
| 中文搜索接口 400 | 用 curl 测试时中文没 URL 编码；浏览器里正常 |
| 管理员编辑别人帖子报“无权修改” | 后端 `PostService.update` 只允许改自己的帖子（见下方已知限制） |
| 端口被占用 | 8080/5173/9200/3306 冲突，`netstat -ano | findstr :8080` 查 PID |
| 前端 `/api` 404 | 后端没起，或 Vite 代理目标不是 8080（见 `vite.config.js`） |

---

## 13. 已知限制与可扩展方向

### 已知限制（作为学习 Demo 可以接受）

1. **管理员编辑权限不一致**：`AdminPage` 列出所有帖子并可点编辑，但后端 `PostService.update` 校验“只能改自己的帖子”，所以 admin 编辑别人的帖子会报错；删除则没有这个校验。
2. **密码算法**：MD5 + 固定盐，仅演示用，生产必须换 BCrypt/Argon2。
3. **同步是全量**：每 60s 全表扫描，数据量大时慢；应改为基于 `update_time` 的增量同步。
4. **图片搜索靠网页正则**：必应改版即失效，生产建议换成官方/稳定 API。
5. **搜索无高亮返回**：前端自己做简单字符串高亮，未使用 ES highlight。
6. **无分页 UI**：接口支持 page/size，但前端固定取前 20 条。
7. **users 索引不随注册实时同步**：新注册用户要等下一轮 `EsSyncJob`（≤60s）才能被搜到。

### 可扩展方向

- 接入更多数据源（视频、文档、商品），验证“中台”扩展性；
- 用 ES 的 `highlight` + `suggest` 做搜索建议、关键词高亮；
- 引入 Redis 做热点搜索缓存；
- 增加帖子详情页、搜索分页、结果排序；
- 用 Docker Compose 一键起 MySQL + ES + 后端 + 前端，降低环境门槛。
