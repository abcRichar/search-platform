-- 聚合搜索平台建表脚本（MySQL 8.x）
-- 执行前先建库：create database search_platform default charset utf8mb4;

use search_platform;

create table if not exists users (
    id          bigint auto_increment primary key,
    username    varchar(64)  not null comment '用户名',
    password    varchar(64)  not null comment 'MD5+盐 后的密码',
    role        varchar(16)  not null default 'user' comment 'user / admin',
    avatar_url  varchar(512) null comment '头像地址',
    create_time datetime     not null default current_timestamp,
    update_time datetime     not null default current_timestamp on update current_timestamp,
    unique key uk_username (username)
) engine = innodb default charset = utf8mb4 comment '用户表';

create table if not exists posts (
    id          bigint auto_increment primary key,
    title       varchar(256) not null comment '标题',
    content     text         not null comment '内容',
    tags        varchar(256) null comment '标签，逗号分隔',
    user_id     bigint       null comment '作者 id',
    create_time datetime     not null default current_timestamp,
    update_time datetime     not null default current_timestamp on update current_timestamp,
    key idx_user_id (user_id),
    key idx_create_time (create_time)
) engine = innodb default charset = utf8mb4 comment '帖子表';
