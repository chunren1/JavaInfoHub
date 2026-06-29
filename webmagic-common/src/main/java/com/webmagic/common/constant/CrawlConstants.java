package com.webmagic.common.constant;

/**
 * 爬虫常量
 *
 * 【JD 覆盖：HTTP 协议】
 *  集中展示 HTTP Header 名称、常见状态码、爬虫参数等常量
 *  体现对 HTTP 协议的系统理解
 *
 * @author webmagic-demo
 */
public final class CrawlConstants {

    private CrawlConstants() {
        // 工具类不可实例化
    }

    // ===== HTTP 状态码 =====

    /** 请求成功 */
    public static final int HTTP_OK = 200;
    /** 请求过多（限流）—— 面试重点：需要退避重试 */
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    /** 服务暂不可用 */
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;

    // ===== HTTP Header 名称 =====

    /** 用户代理 */
    public static final String HEADER_USER_AGENT = "User-Agent";
    /** 内容类型 */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    /** 接受的内容类型（内容协商） */
    public static final String HEADER_ACCEPT = "Accept";
    /** 语言偏好 */
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    /** 来源页（防盗链检查） */
    public static final String HEADER_REFERER = "Referer";
    /** 跨域来源（CORS） */
    public static final String HEADER_ORIGIN = "Origin";
    /** 会话Cookie */
    public static final String HEADER_COOKIE = "Cookie";
    /** Ajax 请求标记 */
    public static final String HEADER_X_REQUESTED_WITH = "X-Requested-With";

    // ===== 请求间隔（毫秒）—— 礼貌爬虫 =====

    /** 默认最小延迟（2秒） */
    public static final long MIN_SLEEP_MS = 2000L;
    /** 默认最大延迟（5秒） */
    public static final long MAX_SLEEP_MS = 5000L;

    // ===== 退避重试 =====

    /** 最大重试次数 */
    public static final int MAX_RETRY_TIMES = 3;
    /** 基础退避延迟（毫秒） */
    public static final long BASE_BACKOFF_MS = 2000L;

    // ===== 爬取页数限制 =====

    /** 默认最大页数 */
    public static final int DEFAULT_MAX_PAGES = 3;
    /** GitHub Trending 仅一页（Top 25 项目） */
    public static final int GITHUB_MAX_PAGES = 1;

    // ===== 去重 =====

    /** 内存去重缓存最大容量 */
    public static final int DEDUP_CACHE_MAX_SIZE = 10000;

    // ===== 分页 =====

    /** 默认每页大小 */
    public static final int DEFAULT_PAGE_SIZE = 20;
}