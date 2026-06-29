package com.webmagic.web.controller;

import com.webmagic.common.service.AiService;
import com.webmagic.core.config.AiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 配置管理 — REST API
 *
 * 提供前端 AI 面板所需的数据：配置状态、API 连通性测试
 *
 * @author webmagic-demo
 */
@RestController
@RequestMapping("/api/ai")
public class AiConfigController {

    @Autowired
    private AiProperties aiProperties;

    @Autowired(required = false)
    private AiService aiService;

    /**
     * GET /api/ai/status — 当前 AI 配置和运行状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        AiService.CallStats stats = aiService != null
                ? aiService.getCallStats()
                : AiService.CallStats.builder().totalCalls(0).errorCount(0).circuitOpen(false).build();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "enabled", aiProperties.isEnabled(),
                        "model", aiProperties.getProvider().getModel(),
                        "baseUrl", aiProperties.getProvider().getBaseUrl(),
                        "apiKeyMasked", maskKey(aiProperties.getProvider().getApiKey()),
                        "extractionFallbackEnabled", aiProperties.isExtractionFallbackEnabled(),
                        "enrichmentEnabled", aiProperties.isEnrichmentEnabled(),
                        "callStats", stats
                )
        ));
    }

    /**
     * POST /api/ai/test — 测试 AI API 连通性
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        if (aiService == null) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "AI service not initialized (ai.enabled=false or no API key)"
            ));
        }

        if (!aiService.isAvailable()) {
            AiService.CallStats stats = aiService.getCallStats();
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "AI service unavailable (circuit: " + stats.isCircuitOpen() + ")",
                    "callStats", stats
            ));
        }

        long start = System.currentTimeMillis();
        try {
            String result = aiService.generateSummary(
                    "Spring Boot 微服务架构实践",
                    "介绍如何使用 Spring Boot 搭建微服务");
            long elapsed = System.currentTimeMillis() - start;

            if (result != null && !result.isBlank()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "AI API 连接成功！",
                        "latencyMs", elapsed,
                        "sampleOutput", result,
                        "callStats", aiService.getCallStats()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "AI API 返回空内容",
                        "latencyMs", elapsed
                ));
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "AI API 调用失败: " + e.getMessage(),
                    "latencyMs", elapsed
            ));
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }
}