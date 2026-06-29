-- ================================================================
-- Java 开发者信息聚合平台 — 数据库建表脚本
-- 使用方法：在 MySQL 中执行此脚本
--   mysql -u root -p < schema.sql
-- 或先建库再执行：
--   CREATE DATABASE IF NOT EXISTS java_info_hub DEFAULT CHARSET utf8mb4;
--   USE java_info_hub;
--   SOURCE schema.sql;
-- ================================================================

CREATE DATABASE IF NOT EXISTS java_info_hub
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE java_info_hub;

-- ================================================================
-- 1. 技术文章表
-- ================================================================
CREATE TABLE IF NOT EXISTS tech_article (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title         VARCHAR(500) NOT NULL                COMMENT '文章标题',
    summary       TEXT                                 COMMENT '摘要/描述',
    content_url   VARCHAR(1000) NOT NULL               COMMENT '原文链接',
    author        VARCHAR(200)                         COMMENT '作者/项目拥有者',
    tags          VARCHAR(500)                         COMMENT '标签（逗号分隔）',
    source        VARCHAR(50)  NOT NULL                COMMENT '来源：JUEJIN/SEGMENTFAULT/GITHUB/OSCHINA',
    source_id     VARCHAR(200) NOT NULL                COMMENT '源站唯一ID',
    publish_time  DATETIME                             COMMENT '发布时间',
    view_count    INT          DEFAULT 0               COMMENT '浏览数/Star数',
    star_count    INT          DEFAULT 0               COMMENT '点赞/Fork数',
    dedup_key     VARCHAR(64)  NOT NULL UNIQUE         COMMENT 'MD5去重键：MD5(source + source_id)',
    crawl_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '首次抓取时间',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_source        (source),
    INDEX idx_publish_time  (publish_time),
    INDEX idx_crawl_time    (crawl_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技术文章聚合表';

-- ================================================================
-- 2. 爬取日志表
-- ================================================================
CREATE TABLE IF NOT EXISTS crawl_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    source        VARCHAR(50)  NOT NULL COMMENT '数据源',
    start_time    DATETIME     NOT NULL COMMENT '开始时间',
    end_time      DATETIME                             COMMENT '结束时间',
    total_fetched INT          DEFAULT 0               COMMENT '抓取条数',
    new_added     INT          DEFAULT 0               COMMENT '新增条数（去重后）',
    status        VARCHAR(20)  NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING/SUCCESS/FAILED',
    error_msg     TEXT                                 COMMENT '错误信息',

    INDEX idx_source     (source),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='爬取日志表';