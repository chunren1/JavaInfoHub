package com.webmagic.common.service;

import com.webmagic.common.dto.ExtractionResult;

import java.util.List;

/**
 * AI 服务接口 — 爬虫智能增强
 *
 * 【面试重点】接口设计：与具体 LLM 实现解耦，可切换提供商（硅基流动 / DeepSeek / OpenAI / 本地 Ollama）
 *
 * @author webmagic-demo
 */
public interface AiService {

    /**
     * 从 HTML 提取文章列表 — 当手动选择器失效时的 AI fallback
     *
     * 【面试话术】"当网站改版导致 CSS/XPath 选择器失效时，
     *  不是直接报错，而是把原始 HTML 发给 LLM，
     *  让它用语义理解能力提取结构化数据。这就是 AI 让爬虫更鲁棒的核心思路。"
     *
     * @param rawHtml 原始 HTML（可能被截断以适配 LLM 上下文窗口）
     * @param source 数据源名称（JUEJIN/SEGMENTFAULT/GITHUB/OSCHINA）
     * @return 提取出的文章列表（可能为空）
     */
    List<ExtractionResult> extractFromHtml(String rawHtml, String source);

    /**
     * 为文章生成中文摘要
     *
     * @param title 文章标题
     * @param originalText 原始文本参考
     * @return AI 生成的摘要（2-3 句中文，不超过 200 字）
     */
    String generateSummary(String title, String originalText);

    /**
     * 为文章生成技术标签
     *
     * @param title 文章标题
     * @param summary 文章摘要
     * @return 2-5 个相关技术标签（如 Spring Boot、微服务、Kubernetes）
     */
    List<String> generateTags(String title, String summary);

    /**
     * 检查 AI 服务是否可用（未熔断 + 未超限）
     */
    boolean isAvailable();

    /**
     * 获取当前调用统计
     */
    CallStats getCallStats();

    /**
     * AI 调用统计
     */
    @lombok.Data
    @lombok.Builder
    class CallStats {
        private int totalCalls;
        private int errorCount;
        private boolean circuitOpen;
        private int promptTokensUsed;
        private int completionTokensUsed;
    }
}