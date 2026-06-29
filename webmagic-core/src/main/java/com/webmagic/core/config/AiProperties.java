package com.webmagic.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 配置 — 硅基流动 / OpenAI 兼容
 *
 * 【面试重点】配置驱动设计：所有 AI 行为通过 application.yml 控制，
 *  无需改代码即可切换模型、调整参数、启停功能。
 *
 * @author webmagic-demo
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 总开关 — false 时零 API 调用 */
    private boolean enabled = false;

    /** LLM 提供商配置 */
    private Provider provider = new Provider();

    /** 功能开关 */
    private Features features = new Features();

    /** 速率控制 */
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Provider {
        /** API 基础地址（硅基流动: https://api.siliconflow.cn/v1） */
        private String baseUrl = "https://api.siliconflow.cn/v1";

        /** API 密钥（建议通过环境变量 AI_API_KEY 设置） */
        private String apiKey = "";

        /** 模型名称 */
        private String model = "Qwen/Qwen2.5-7B-Instruct";

        /** 温度 — 提取任务推荐 0.1（确定性），生成任务可稍高 */
        private double temperature = 0.1;

        /** 最大输出 token 数 */
        private int maxTokens = 4096;

        /** HTTP 超时秒数 */
        private int timeoutSeconds = 60;
    }

    @Data
    public static class Features {
        /** 提取兜底配置 */
        private ExtractionFallback extractionFallback = new ExtractionFallback();

        /** 内容增强配置 */
        private Enrichment enrichment = new Enrichment();

        @Data
        public static class ExtractionFallback {
            /** 是否启用 AI 提取兜底 */
            private boolean enabled = true;

            /** 手动提取低于此数量时触发 AI fallback */
            private int minThreshold = 0;
        }

        @Data
        public static class Enrichment {
            /** 是否启用 AI 内容增强 */
            private boolean enabled = true;

            /** 是否用 AI 生成摘要 */
            private boolean enrichSummary = true;

            /** 是否用 AI 生成标签 */
            private boolean enrichTags = true;

            /** 原文摘要低于此长度（字符）时触发 AI 生成 */
            private int summaryMinLength = 50;
        }
    }

    @Data
    public static class RateLimit {
        /** 单次爬取最多 AI 调用次数 */
        private int maxRequestsPerCrawl = 100;

        /** AI API 调用间隔（毫秒） */
        private long delayBetweenCallsMs = 500;
    }

    // ==================== 便捷方法 ====================

    public boolean isExtractionFallbackEnabled() {
        return enabled && features.getExtractionFallback().isEnabled();
    }

    public boolean isEnrichmentEnabled() {
        return enabled && features.getEnrichment().isEnabled();
    }

    public boolean isSummaryEnrichEnabled() {
        return isEnrichmentEnabled() && features.getEnrichment().isEnrichSummary();
    }

    public boolean isTagsEnrichEnabled() {
        return isEnrichmentEnabled() && features.getEnrichment().isEnrichTags();
    }
}