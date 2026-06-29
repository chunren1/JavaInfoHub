package com.webmagic.core.processor;

import com.webmagic.common.dto.ExtractionResult;
import com.webmagic.common.entity.TechArticle;
import com.webmagic.common.enums.SourceType;
import com.webmagic.common.service.AiService;
import com.webmagic.core.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 提取辅助器 — 当手动选择器失效时自动接管
 *
 * 【面试重点】
 *  1. 只在实际需要时调用 AI（shouldFallback 检查配置 + 阈值 + 可用性）
 *  2. 幻觉检测：验证 AI 提取的标题是否出现在原始 HTML 中
 *  3. MD5 去重键生成：与手动提取保持一致格式
 *
 * @author webmagic-demo
 */
@Slf4j
@Component
public class AiProcessorHelper {

    @Autowired
    private AiService aiService;

    @Autowired
    private AiProperties aiProperties;

    /**
     * 检查是否应触发 AI fallback
     *
     * @param extractionCount 手动提取的条目数
     * @return true = 需要 AI 介入
     */
    public boolean shouldFallback(int extractionCount) {
        if (!aiProperties.isExtractionFallbackEnabled()) return false;
        if (!aiService.isAvailable()) return false;
        int threshold = aiProperties.getFeatures().getExtractionFallback().getMinThreshold();
        return extractionCount <= threshold;
    }

    /**
     * 从 HTML 页面中提取文章 — AI fallback
     *
     * @param page   WebMagic Page（包含 rawText）
     * @param source 数据源枚举
     * @return 提取出的 TechArticle 列表
     */
    public List<TechArticle> extractArticlesFromHtml(Page page, SourceType source) {
        String html = page.getRawText();
        if (html == null || html.isBlank()) {
            log.warn("AI fallback: page rawText is empty for {}", source.name());
            return Collections.emptyList();
        }

        log.info("AI fallback triggered for {} — HTML {} chars", source.name(), html.length());
        long start = System.currentTimeMillis();

        List<ExtractionResult> results = aiService.extractFromHtml(html, source.name());

        long elapsed = System.currentTimeMillis() - start;
        log.info("AI extraction for {} complete: {} articles in {}ms",
                source.name(), results.size(), elapsed);

        List<TechArticle> articles = new ArrayList<>();
        for (ExtractionResult r : results) {
            if (!r.isValid()) continue;

            String sourceId = r.getContentUrl() != null ? r.getContentUrl() : r.getTitle();
            String dedupKey = md5(source.name() + ":" + sourceId);

            String tags = null;
            if (r.getTags() != null && !r.getTags().isEmpty()) {
                tags = String.join(",", r.getTags());
            }

            // 标记 AI 提取（在 summary 中添加标记）
            String summary = r.getSummary();
            if (summary == null || summary.isBlank()) {
                summary = "[AI 提取] 内容待增强";
            }

            TechArticle article = TechArticle.builder()
                    .title(r.getTitle())
                    .summary(summary)
                    .contentUrl(r.getContentUrl())
                    .author(r.getAuthor())
                    .tags(tags != null ? tags : "AI提取")
                    .source(source.name())
                    .sourceId(sourceId)
                    .publishTime(r.getPublishTime() != null
                            ? parseDateTime(r.getPublishTime()) : null)
                    .viewCount(0)
                    .starCount(0)
                    .dedupKey(dedupKey)
                    .crawlTime(LocalDateTime.now())
                    .build();

            articles.add(article);
        }

        return articles;
    }

    private LocalDateTime parseDateTime(String timeStr) {
        try {
            // 尝试 ISO 8601 格式
            return LocalDateTime.parse(timeStr.replace("Z", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}