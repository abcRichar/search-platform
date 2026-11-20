package com.example.search.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 首次启动数据初始化：users / posts 表为空时插入演示数据（含一个管理员账号）。
 * 数据随后由 EsSyncJob 自动同步进 ES。
 * 默认管理员：admin / 123456
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Integer userCount = jdbc.queryForObject("select count(*) from users", Integer.class);
        if (userCount != null && userCount > 0) {
            return;
        }
        String pwd = org.springframework.util.DigestUtils
                .md5DigestAsHex(("search-platform" + "123456").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        jdbc.update("insert into users(username, password, role, avatar_url) values(?,?,?,null)",
                "admin", pwd, "admin");
        jdbc.update("insert into users(username, password, role, avatar_url) values(?,?,?,null)",
                "张三", pwd, "user");
        jdbc.update("insert into users(username, password, role, avatar_url) values(?,?,?,null)",
                "李四", pwd, "user");

        Long adminId = jdbc.queryForObject("select id from users where username = 'admin'", Long.class);
        Long zhangId = jdbc.queryForObject("select id from users where username = '张三'", Long.class);

        List<Object[]> posts = List.of(
                new Object[]{"Elasticsearch 入门指南", "Elasticsearch 是基于 Lucene 的分布式搜索引擎，核心是倒排索引，适合做全文检索和聚合分析。", "elasticsearch,搜索", adminId},
                new Object[]{"Spring Boot 整合 Elasticsearch", "引入 spring-boot-starter-data-elasticsearch，定义 @Document 实体和 Repository 即可像 JPA 一样操作 ES。", "springboot,java", zhangId},
                new Object[]{"MySQL 索引优化实战", "当查询变慢时优先检查索引：最左前缀原则、覆盖索引、避免函数失效。配合 explain 分析执行计划。", "mysql,数据库,优化", adminId},
                new Object[]{"React Hooks 最佳实践", "useEffect 依赖数组要写全，跨组件复用逻辑可以抽自定义 Hook，状态管理能用 local state 就不用全局。", "react,前端", zhangId},
                new Object[]{"JWT 登录鉴权原理", "JWT 由 Header、Payload、Signature 三段组成，服务端只需校验签名，无需存储会话状态。", "jwt,鉴权,后端", adminId},
                new Object[]{"聚合搜索平台设计思路", "把不同来源的数据（帖子、用户、图片）接入统一搜索层，前端一个搜索框同时出多类结果，这就是搜索中台的价值。", "架构,搜索", adminId},
                new Object[]{"Redis 缓存三大问题", "缓存穿透、缓存击穿、缓存雪崩的成因和应对：布隆过滤器、互斥锁重建、过期时间加随机值。", "redis,缓存,后端", zhangId},
                new Object[]{"Java 单机部署要点", "jar 包 + 外置 application.yml + systemd 守护，是单体项目最朴素也最可靠的部署姿势。", "java,部署", adminId}
        );
        jdbc.batchUpdate(
                "insert into posts(title, content, tags, user_id) values(?,?,?,?)", posts);
    }
}
