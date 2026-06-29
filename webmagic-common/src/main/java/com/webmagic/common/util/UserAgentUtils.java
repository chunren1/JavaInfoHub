package com.webmagic.common.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 用户代理（User-Agent）工具类
 *
 * 【JD 覆盖：HTTP 协议 — User-Agent 头部】
 *  提供 6 个真实浏览器 UA，使用 SecureRandom 随机轮换
 *  避免被目标站点识别为爬虫而限流
 *
 * 【面试话术】
 *  "爬虫的 UA 管理是个基础但重要的环节。
 *   我准备了 6 个真实浏览器 UA，用 SecureRandom 随机选择，
 *   比 Math.random() 更安全——
 *   模拟了不同浏览器/版本的自然访问行为。"
 *
 * @author webmagic-demo
 */
public final class UserAgentUtils {

    private UserAgentUtils() {
        // 工具类不可实例化
    }

    /** 6 个真实浏览器 UA（Windows/Mac，Chrome/Firefox/Edge/Safari） */
    private static final String[] USER_AGENTS = {
            // Chrome 120 (Windows)
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            // Chrome 119 (Mac)
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            // Edge 119 (Windows)
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0",
            // Firefox 120 (Windows)
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
            // Safari 17 (Mac)
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
            // Chrome 118 (Linux) — 模拟企业内网环境
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36"
    };

    /** 移动端 UA */
    private static final String MOBILE_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    /** 加密安全的随机数生成器（比 Math.random() 更安全、更随机） */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 随机获取一个桌面端 User-Agent
     *
     * @return 随机 UA 字符串
     */
    public static String getRandom() {
        return USER_AGENTS[RANDOM.nextInt(USER_AGENTS.length)];
    }

    /**
     * 获取移动端 User-Agent
     *
     * @return 移动端 UA 字符串
     */
    public static String getMobile() {
        return MOBILE_UA;
    }

    /**
     * 获取所有 User-Agent（用于轮询或手动选择）
     *
     * @return 只读 UA 列表
     */
    public static List<String> getAll() {
        return Collections.unmodifiableList(Arrays.asList(USER_AGENTS));
    }

    /**
     * 获取 UA 总数
     *
     * @return UA 个数
     */
    public static int count() {
        return USER_AGENTS.length;
    }
}