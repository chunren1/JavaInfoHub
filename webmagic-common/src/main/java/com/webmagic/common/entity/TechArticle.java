package com.webmagic.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 技术文章实体 — 对应 tech_article 表
 *
 * 聚合来源：掘金、SegmentFault、GitHub Trending、开源中国
 *
 * 【JD 覆盖：Java 基础（实体设计）】
 *  使用 Lombok 简化代码，展示 @Data/@Builder/@NoArgsConstructor/@AllArgsConstructor 用法
 *
 * @author webmagic-demo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Long id;

    /** 文章标题 */
    private String title;

    /** 摘要/描述 */
    private String summary;

    /** 原文链接 */
    private String contentUrl;

    /** 作者/项目拥有者 */
    private String author;

    /** 标签（逗号分隔） */
    private String tags;

    /** 来源：JUEJIN / SEGMENTFAULT / GITHUB / OSCHINA */
    private String source;

    /** 源站唯一 ID */
    private String sourceId;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 浏览数 */
    private Integer viewCount;

    /** 点赞/Star 数 */
    private Integer starCount;

    /** MD5 去重键：MD5(source + ":" + sourceId) */
    private String dedupKey;

    /** 首次抓取时间 */
    private LocalDateTime crawlTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}