package com.webmagic.common.util;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求头工具类
 *
 * 【JD 覆盖：HTTP 协议 — Header / Cookie / Ajax 请求头】
 *
 *  集中展示对 HTTP 协议的系统理解——面试时可以逐一讲解每个 Header 的作用：
 *
 *  通用 Headers（内容协商/缓存/连接）：
 *    Accept              — 客户端支持的内容类型（MIME type）
 *    Accept-Language     — 语言偏好（zh-CN,zh;q=0.9）
 *    Accept-Encoding     — 支持的压缩格式（gzip, deflate）
 *    Cache-Control       — 缓存策略（no-cache）
 *    Connection          — 长连接复用（keep-alive）
 *    DNT                 — 请勿追踪（1）
 *
 *  Ajax Headers（模拟 AJAX 请求）：
 *    X-Requested-With    — 标记为 XMLHttpRequest（Ajax 标志）
 *    Content-Type        — 请求体格式（application/json）
 *    Referer             — 来源页地址（防盗链检查）
 *    Origin              — 跨域请求来源（CORS 策略）
 *
 *  Cookie（会话保持）：
 *    Cookie              — 登录态/会话ID，维持身份认证
 *
 *  反爬 Headers：
 *    User-Agent          — 模拟真实浏览器（由 UserAgentUtils 提供）
 *
 * 【面试话术】
 *  "HTTP Header 配置是爬虫的基本功。不同的接口需要不同的 Header 组合：
 *   AJAX 接口需要 X-Requested-With 和 Content-Type；
 *   有防盗链的页面需要 Referer；
 *   需要登录态的站点需要 Cookie。
 *   我把这些 Header 分成了通用、Ajax、Cookie 三种场景封装，按需组装。"
 *
 * @author webmagic-demo
 */
public final class HttpHeaderUtils {

    private HttpHeaderUtils() {
        // 工具类不可实例化
    }

    // ================================================================
    // 通用 Headers（适用于大多数静态页面爬取）
    // ================================================================

    /**
     * 构建通用浏览器请求头
     *
     * 用于：SegmentFault、GitHub Trending、开源中国 等普通页面
     *
     * @return Header Map
     */
    public static Map<String, String> buildCommonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Cache-Control", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("DNT", "1");  // Do Not Track
        headers.put("User-Agent", UserAgentUtils.getRandom());
        return headers;
    }

    // ================================================================
    // Ajax Headers（适用于 JSON API 接口，如掘金）
    // ================================================================

    /**
     * 构建 Ajax 请求头（JSON POST 请求）
     *
     * 【JD 覆盖：了解 Ajax 请求】
     *  关键 Header：X-Requested-With（标记 Ajax）、Content-Type（JSON 格式）、Referer
     *
     * 用于：掘金 POST JSON API
     *
     * @param referer 来源页 URL（如 "https://juejin.cn/"）
     * @param origin  跨域来源（如 "https://juejin.cn"）
     * @return Ajax Header Map
     */
    public static Map<String, String> buildAjaxHeaders(String referer, String origin) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("X-Requested-With", "XMLHttpRequest");  // ← Ajax 标记
        headers.put("Referer", referer != null ? referer : "");
        headers.put("Origin", origin != null ? origin : "");
        headers.put("Cache-Control", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("User-Agent", UserAgentUtils.getRandom());
        return headers;
    }

    // ================================================================
    // Cookie Headers（适用于需要登录态的站点，如实习僧）
    // ================================================================

    /**
     * 构建带 Cookie 的请求头
     *
     * 【JD 覆盖：了解 Cookie / 会话管理】
     *  部分站点需要 Cookie 维持登录态才能获取数据
     *
     * 用于：实习僧（如需要登录态）
     *
     * @param cookie Cookie 字符串
     * @return 带 Cookie 的 Header Map
     */
    public static Map<String, String> buildCookieHeaders(String cookie) {
        Map<String, String> headers = buildCommonHeaders();
        if (cookie != null && !cookie.isEmpty()) {
            headers.put("Cookie", cookie);
        }
        return headers;
    }
}