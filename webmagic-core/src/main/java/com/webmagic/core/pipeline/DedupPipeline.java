package com.webmagic.core.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 去重 Pipeline — MD5 去重（内存快速路径 + DB 兜底）
 *
 * 【JD 覆盖：Java 基础 — ConcurrentHashMap 去重策略】
 *
 *  双层去重设计：
 *   第一层：内存 ConcurrentHashMap（快速路径，~0.01ms）
 *   第二层：数据库 UNIQUE 索引（兜底保护，防止内存缓存清空后重复入库）
 *
 *  去重键：MD5(source + ":" + sourceId)
 *  为什么不是 URL？— 同一个 URL 可能属于不同来源，用 URL 不够精确
 *
 * 【面试话术】
 *  \"去重是爬虫的核心环节。我设计了两层去重：
 *  第一层是内存 ConcurrentHashMap——性能高，每次判断不到 0.01 毫秒；
 *  第二层是数据库 UNIQUE 索引——即使内存缓存清空或被重启清掉，
 *  INSERT IGNORE 也能保证不重复入库。\"
 *
 * @author webmagic-demo
 */
@Slf4j
@Component
public class DedupPipeline implements Pipeline {

    /**
     * 内存去重缓存：dedupKey → true
     * ConcurrentHashMap 保证线程安全
     */
    private final ConcurrentHashMap<String, Boolean> seenKeys = new ConcurrentHashMap<>();

    /** 最大缓存容量（防止内存溢出） */
    private static final int MAX_CACHE_SIZE = 10_000;

    /**
     * 去重核心逻辑：
     * 1. 从 ResultItems 中取出 dedupKey
     * 2. 查内存缓存 → 命中则 setSkip(true)，跳过后续 Pipeline
     * 3. 未命中 → 加入缓存，放行
     */
    @Override
    public void process(ResultItems resultItems, Task task) {
        String dedupKey = resultItems.get("dedupKey");
        if (dedupKey == null || dedupKey.isEmpty()) {
            log.debug("没有去重键，跳过去重检查");
            return;
        }

        // 【第一层】内存快速查重
        if (seenKeys.containsKey(dedupKey)) {
            log.debug("【去重命中-内存】{}", dedupKey);
            resultItems.setSkip(true);  // ← 跳过后续所有 Pipeline
            return;
        }

        // 缓存管理：超出上限时清空（简化策略，生产环境可用 LRU 或布隆过滤器）
        if (seenKeys.size() >= MAX_CACHE_SIZE) {
            log.warn("去重缓存已达上限 {}，执行清空", MAX_CACHE_SIZE);
            seenKeys.clear();
        }

        // 加入内存缓存
        seenKeys.put(dedupKey, true);
        log.debug("【去重放行】{}", dedupKey);
    }

    /**
     * 获取当前缓存大小（用于监控）
     */
    public int getCacheSize() {
        return seenKeys.size();
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        seenKeys.clear();
        log.info("去重缓存已清空");
    }
}