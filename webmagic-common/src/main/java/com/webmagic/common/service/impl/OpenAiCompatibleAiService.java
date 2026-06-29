package com.webmagic.common.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webmagic.common.dto.*;
import com.webmagic.common.service.AiService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OpenAI 兼容 API 实现 — 硅基流动 / DeepSeek / Qwen 等均适用
 *
 * 【面试重点】
 *  1. 零额外 HTTP 库依赖 — 只用 JDK 内置 HttpURLConnection + Jackson
 *  2. 熔断器（Circuit Breaker）— 连续 5 次失败自动关闭，60 秒后重试
 *  3. 速率控制 — 调用间隔 + 单次爬取上限，防止刷爆 API 额度
 *  4. 幻觉检测 — 验证 AI 提取的标题是否在原始 HTML 中存在
 *
 * @author webmagic-demo
 */
@Slf4j
public class OpenAiCompatibleAiService implements AiService {

    // ==================== 配置 ====================

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final int timeoutSeconds;
    private final int maxRequestsPerSession;
    private final long delayBetweenCallsMs;

    // ==================== 状态 ====================

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 熔断器 */
    private volatile boolean circuitOpen = false;
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private volatile LocalDateTime circuitOpenTime = null;
    private static final int ERROR_THRESHOLD = 5;
    private static final long CIRCUIT_RESET_SECONDS = 60;

    /** 调用计数 */
    private final AtomicInteger sessionCallCount = new AtomicInteger(0);
    private final AtomicInteger totalErrorCount = new AtomicInteger(0);
    private final AtomicInteger totalPromptTokens = new AtomicInteger(0);
    private final AtomicInteger totalCompletionTokens = new AtomicInteger(0);
    private volatile long lastCallTime = 0;

    public OpenAiCompatibleAiService(String baseUrl, String apiKey, String model,
                                      double temperature, int maxTokens, int timeoutSeconds,
                                      int maxRequestsPerSession, long delayBetweenCallsMs) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRequestsPerSession = maxRequestsPerSession;
        this.delayBetweenCallsMs = delayBetweenCallsMs;
    }

    // ==================== 公开方法 ====================

    @Override
    public List<ExtractionResult> extractFromHtml(String rawHtml, String source) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return Collections.emptyList();
        }

        // 截断 HTML — 避免超出上下文窗口（约 60,000 字符 ≈ 15,000 tokens）
        String truncated = rawHtml.length() > 60_000
                ? rawHtml.substring(0, 60_000) : rawHtml;

        String systemPrompt = "你是一个精确的HTML数据提取器。从技术文章列表页的HTML中提取所有文章。\n" +
                "输出一个JSON数组，每个元素包含：\n" +
                "- title（必填，字符串）\n" +
                "- summary（摘要，字符串或null）\n" +
                "- author（作者，字符串或null）\n" +
                "- contentUrl（文章链接，字符串或null，相对路径需补全域名）\n" +
                "- tags（标签数组，字符串数组）\n" +
                "- publishTime（发布时间，ISO格式或null）\n\n" +
                "规则：\n" +
                "1. 只输出JSON数组，不要markdown代码块、不要解释\n" +
                "2. 不确定的字段设为null或空数组，不要编造\n" +
                "3. 提取HTML中所有可见的文章，不要遗漏\n" +
                "4. 标题必须是HTML中实际存在的文本";

        // 构造域名提示
        String domainHint = sourceBaseUrl(source);
        String userPrompt = "数据源：" + source + "\n" +
                "域名：" + domainHint + "\n\n" +
                "从以下HTML提取所有文章：\n" + truncated;

        String response = chat(systemPrompt, userPrompt);
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }

        // 解析响应 — 先尝试直接解析，再尝试从 markdown 中提取
        List<ExtractionResult> results = parseExtractionResponse(response);
        if (results.isEmpty()) {
            log.warn("AI extraction returned empty or unparseable for source: {}", source);
        } else {
            log.info("AI extracted {} articles from {} HTML ({} chars)",
                    results.size(), source, truncated.length());
        }

        // 幻觉检测：验证标题是否在 HTML 中存在
        List<ExtractionResult> verified = new ArrayList<>();
        for (ExtractionResult r : results) {
            if (r.isValid() && rawHtml.contains(r.getTitle())) {
                verified.add(r);
            } else if (r.isValid()) {
                log.warn("AI hallucination detected: title '{}' not found in HTML, discarding",
                        r.getTitle().length() > 80 ? r.getTitle().substring(0, 80) + "..." : r.getTitle());
            }
        }
        if (verified.size() < results.size()) {
            log.warn("Hallucination filter: {} of {} results discarded",
                    results.size() - verified.size(), results.size());
        }

        return verified;
    }

    @Override
    public String generateSummary(String title, String originalText) {
        if (title == null || title.isBlank()) return null;

        String systemPrompt = "你是一个技术文章摘要生成器。用2-3句话中文摘要概括技术文章，不超过200字。" +
                "聚焦技术洞见、涉及的框架/工具、实用价值。直接输出摘要文本，不要加'摘要：'等前缀。";

        String userPrompt = "标题：" + title + "\n" +
                "原文参考：" + (originalText != null ? originalText : "（无）") + "\n\n" +
                "生成中文摘要：";

        String response = chat(systemPrompt, userPrompt);
        if (response != null && !response.isBlank()) {
            // 清理响应中的 markdown 标记
            response = response.replaceAll("^摘要[：:]\\s*", "").trim();
            if (response.length() > 300) {
                response = response.substring(0, 300);
            }
            return response;
        }
        return null;
    }

    @Override
    public List<String> generateTags(String title, String summary) {
        if (title == null || title.isBlank()) return Collections.emptyList();

        String systemPrompt = "你是一个技术内容分类器。为技术文章生成2-5个相关标签。" +
                "标签应为简短的技术关键词（如 Spring Boot、微服务、Kubernetes、AI、性能优化）。" +
                "输出JSON字符串数组，不要其他内容。";

        String userPrompt = "标题：" + title + "\n" +
                "摘要：" + (summary != null ? summary : "（无）") + "\n\n" +
                "生成标签JSON数组：";

        String response = chat(systemPrompt, userPrompt);
        if (response != null && !response.isBlank()) {
            try {
                return objectMapper.readValue(response,
                        new TypeReference<List<String>>() {});
            } catch (Exception e) {
                // 尝试从 markdown 代码块中提取
                String cleaned = response.replaceAll("```json\\s*", "")
                        .replaceAll("```\\s*", "").trim();
                try {
                    return objectMapper.readValue(cleaned,
                            new TypeReference<List<String>>() {});
                } catch (Exception ex) {
                    log.warn("Failed to parse AI tag response: {}", response.length() > 100
                            ? response.substring(0, 100) + "..." : response);
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isAvailable() {
        if (apiKey == null || apiKey.isBlank()) return false;
        if (sessionCallCount.get() >= maxRequestsPerSession) return false;
        return !isCircuitOpen();
    }

    @Override
    public CallStats getCallStats() {
        return CallStats.builder()
                .totalCalls(sessionCallCount.get())
                .errorCount(totalErrorCount.get())
                .circuitOpen(circuitOpen)
                .promptTokensUsed(totalPromptTokens.get())
                .completionTokensUsed(totalCompletionTokens.get())
                .build();
    }

    // ==================== 内部方法 ====================

    /**
     * 核心 — 调用 OpenAI 兼容 Chat API
     */
    private String chat(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            log.debug("AI service unavailable: circuit={}, calls={}/{}",
                    circuitOpen, sessionCallCount.get(), maxRequestsPerSession);
            return null;
        }

        // 速率控制
        long now = System.currentTimeMillis();
        long elapsed = now - lastCallTime;
        if (elapsed < delayBetweenCallsMs && lastCallTime > 0) {
            try {
                Thread.sleep(delayBetweenCallsMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        try {
            sessionCallCount.incrementAndGet();
            lastCallTime = System.currentTimeMillis();

            // 构建请求
            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .messages(Arrays.asList(
                            AiChatMessage.system(systemPrompt),
                            AiChatMessage.user(userPrompt)))
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .responseFormat(new AiChatRequest.ResponseFormat("json_object"))
                    .build();

            String jsonBody = objectMapper.writeValueAsString(request);

            // HTTP 调用
            URL url = URI.create(baseUrl + "/chat/completions").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(timeoutSeconds * 1000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                String responseBody = new String(
                        conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                AiChatResponse response = objectMapper.readValue(responseBody, AiChatResponse.class);

                // 更新 token 统计
                if (response.getUsage() != null) {
                    totalPromptTokens.addAndGet(response.getUsage().getPromptTokens());
                    totalCompletionTokens.addAndGet(response.getUsage().getCompletionTokens());
                }

                // 熔断器重置
                consecutiveErrors.set(0);

                String content = response.getFirstContent();
                log.debug("AI API call success: model={}, tokens={}",
                        model, response.getUsage() != null ? response.getUsage().getTotalTokens() : "?");
                return content;

            } else {
                String errorBody = new String(
                        conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.error("AI API returned HTTP {}: {}", status,
                        errorBody.length() > 200 ? errorBody.substring(0, 200) + "..." : errorBody);

                // 401 → 直接熔断
                if (status == 401) {
                    log.error("AI API key invalid (HTTP 401), disabling AI for this session");
                    circuitOpen = true;
                    circuitOpenTime = LocalDateTime.now();
                }

                // 429 → 指数退避重试
                if (status == 429) {
                    long[] backoff = {1000, 2000, 4000};
                    for (int i = 0; i < backoff.length && isAvailable(); i++) {
                        try { Thread.sleep(backoff[i]); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
                        log.info("Retrying AI call after 429 rate limit (attempt {}/{})", i + 1, backoff.length);
                        // 递归重试 — 简化实现
                        String retryResult = chatInternal(systemPrompt, userPrompt);
                        if (retryResult != null) return retryResult;
                    }
                }

                recordError();
                return null;
            }

        } catch (IOException e) {
            log.error("AI API network error: {}", e.getMessage());
            recordError();
            return null;
        } catch (Exception e) {
            log.error("Unexpected error in AI call: {}", e.getMessage(), e);
            recordError();
            return null;
        }
    }

    /** 内部重试用 — 跳过速率限制检查 */
    private String chatInternal(String systemPrompt, String userPrompt) {
        try {
            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .messages(Arrays.asList(
                            AiChatMessage.system(systemPrompt),
                            AiChatMessage.user(userPrompt)))
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .responseFormat(new AiChatRequest.ResponseFormat("json_object"))
                    .build();

            String jsonBody = objectMapper.writeValueAsString(request);
            URL url = URI.create(baseUrl + "/chat/completions").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(timeoutSeconds * 1000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                String responseBody = new String(
                        conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                AiChatResponse response = objectMapper.readValue(responseBody, AiChatResponse.class);
                consecutiveErrors.set(0);
                return response.getFirstContent();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析 AI 响应的 JSON */
    private List<ExtractionResult> parseExtractionResponse(String response) {
        if (response == null || response.isBlank()) return Collections.emptyList();

        // 清理 markdown 代码块
        String cleaned = response
                .replaceAll("^```(?:json)?\\s*\\n?", "")
                .replaceAll("\\n?```\\s*$", "")
                .trim();

        try {
            return objectMapper.readValue(cleaned,
                    new TypeReference<List<ExtractionResult>>() {});
        } catch (JsonProcessingException e) {
            log.debug("Direct JSON parse failed, trying array extraction: {}", e.getMessage());
            // 尝试从响应中提取 JSON 数组部分
            int start = cleaned.indexOf('[');
            int end = cleaned.lastIndexOf(']');
            if (start >= 0 && end > start) {
                try {
                    String arrayPart = cleaned.substring(start, end + 1);
                    return objectMapper.readValue(arrayPart,
                            new TypeReference<List<ExtractionResult>>() {});
                } catch (JsonProcessingException ex) {
                    log.warn("Array extraction also failed for AI response");
                }
            }
            return Collections.emptyList();
        }
    }

    /** 熔断器状态检查 */
    private boolean isCircuitOpen() {
        if (!circuitOpen) return false;
        if (circuitOpenTime != null &&
                LocalDateTime.now().isAfter(circuitOpenTime.plusSeconds(CIRCUIT_RESET_SECONDS))) {
            circuitOpen = false;
            consecutiveErrors.set(0);
            circuitOpenTime = null;
            log.info("AI circuit breaker reset — {} seconds elapsed", CIRCUIT_RESET_SECONDS);
            return false;
        }
        return true;
    }

    /** 记录错误，触发熔断 */
    private void recordError() {
        totalErrorCount.incrementAndGet();
        if (consecutiveErrors.incrementAndGet() >= ERROR_THRESHOLD) {
            circuitOpen = true;
            circuitOpenTime = LocalDateTime.now();
            log.warn("AI circuit breaker OPEN after {} consecutive errors (threshold: {})",
                    consecutiveErrors.get(), ERROR_THRESHOLD);
        }
    }

    /** 根据 source 返回基础 URL */
    private static String sourceBaseUrl(String source) {
        if (source == null) return "";
        switch (source.toUpperCase()) {
            case "JUEJIN":      return "https://juejin.cn";
            case "SEGMENTFAULT": return "https://segmentfault.com";
            case "GITHUB":       return "https://github.com";
            case "OSCHINA":      return "https://www.oschina.net";
            default:             return "";
        }
    }
}