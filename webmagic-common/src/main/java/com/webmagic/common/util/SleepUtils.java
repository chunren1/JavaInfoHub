package com.webmagic.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 爬虫延迟工具类 — "礼貌爬虫"
 *
 * 【JD 覆盖：HTTP 协议 — 请求频率控制】
 *  随机延迟机制：
 *  - 默认 2-5 秒随机间隔，模拟人类浏览行为
 *  - 使用 ThreadLocalRandom（高并发下比 Math.random() 性能更好）
 *  - 可自定义最小/最大延迟范围
 *
 * 【面试话术】
 *  "爬虫不是越快越好。频繁请求会给目标服务器造成压力，也容易被封。
 *   我用了随机延迟策略——每次请求间隔 2-5 秒随机，
 *   用 ThreadLocalRandom 而不是 Math.random()，因为在高并发场景下它没有锁竞争。
 *   这就是'礼貌爬虫'的基本素养。"
 *
 * @author webmagic-demo
 */
public final class SleepUtils {

    private SleepUtils() {
        // 工具类不可实例化
    }

    /** 默认最小延迟 2 秒 */
    private static final long DEFAULT_MIN_MS = 2000L;
    /** 默认最大延迟 5 秒 */
    private static final long DEFAULT_MAX_MS = 5000L;

    /**
     * 默认随机延迟 2-5 秒
     * 如果线程被中断，恢复中断标志
     */
    public static void randomDelay() {
        randomDelay(DEFAULT_MIN_MS, DEFAULT_MAX_MS);
    }

    /**
     * 自定义范围的随机延迟
     *
     * @param minMs 最小延迟（毫秒）
     * @param maxMs 最大延迟（毫秒）
     */
    public static void randomDelay(long minMs, long maxMs) {
        long delay = ThreadLocalRandom.current().nextLong(minMs, maxMs + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // 恢复中断标志（标准做法：不吞掉中断状态）
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 自定义延迟秒数范围
     *
     * @param minSeconds 最小秒数
     * @param maxSeconds 最大秒数
     */
    public static void randomDelaySeconds(int minSeconds, int maxSeconds) {
        randomDelay(minSeconds * 1000L, maxSeconds * 1000L);
    }

    /**
     * 固定延迟（毫秒）
     *
     * @param millis 延迟毫秒数
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}