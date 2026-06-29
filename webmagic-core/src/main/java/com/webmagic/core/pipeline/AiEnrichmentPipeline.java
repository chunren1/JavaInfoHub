package com.webmagic.core.pipeline;

import com.webmagic.common.entity.TechArticle;
import com.webmagic.common.service.AiService;
import com.webmagic.core.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 内容增强 Pipeline — 自动生成摘要和标签
 *
 * 放在 DedupPipeline 之后、ArticlePersistPipeline 之前。
 * 如果 AI 被禁用，直接透传（零开销）。
 *
 * 【面试重点】
 *  \"Pipeline 的职责是数据出口处理。我在 Dedup 和 Persist 之间
 *   插入了 AIEnrichmentPipeline——对每批文章自动生成中文摘要和标签。
 *   这是 Chain of Responsibility 模式的典型应用：
 *   新增功能完全不影响上下游 Pipeline。AI 不可用时零性能损耗。\"
 *
 * @author webmagic-demo
 */
@Slf4j
@Component
public class AiEnrichmentPipeline implements Pipeline {

    @Autowired
    private AiService aiService;

    @Autowired
    private AiProperties aiProperties;

    @Override
    public void process(ResultItems resultItems, Task task) {
        if (!aiProperties.isEnrichmentEnabled()) {
            return; // AI 增强未启用，直接透传
        }

        Object articlesObj = resultItems.get("articles");
        if (!(articlesObj instanceof List)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<TechArticle> articles = (List<TechArticle>) articlesObj;
        if (articles.isEmpty()) {
            return;
        }

        int summaryEnriched = 0;
        int tagsEnriched = 0;
        int totalEnriched = 0;

        List<TechArticle> enriched = new ArrayList<>();
        for (TechArticle a : articles) {
            boolean articleEnriched = false;

            try {
                // 摘要增强
                if (aiProperties.isSummaryEnrichEnabled()
                        && (a.getSummary() == null
                        || a.getSummary().isBlank()
                        || a.getSummary().length() < aiProperties.getFeatures()
                                .getEnrichment().getSummaryMinLength())) {

                    String aiSummary = aiService.generateSummary(
                            a.getTitle(),
                            a.getSummary()); // 原始摘要作为参考
                    if (aiSummary != null && !aiSummary.isBlank()) {
                        a.setSummary(aiSummary);
                        summaryEnriched++;
                        articleEnriched = true;
                    }
                }

                // 标签增强
                if (aiProperties.isTagsEnrichEnabled()
                        && (a.getTags() == null
                        || a.getTags().isBlank()
                        || a.getTags().split(",").length < 2)) {

                    List<String> aiTags = aiService.generateTags(
                            a.getTitle(),
                            a.getSummary());
                    if (aiTags != null && !aiTags.isEmpty()) {
                        // 合并已有标签与 AI 标签
                        String existing = (a.getTags() != null && !a.getTags().isBlank())
                                ? a.getTags() + "," : "";
                        a.setTags(existing + String.join(",", aiTags));
                        tagsEnriched++;
                        articleEnriched = true;
                    }
                }

                if (articleEnriched) {
                    totalEnriched++;
                    // 在 tags 末尾追加 AI 标记（前端显示 🤖 徽章用）
                    if (a.getTags() != null && !a.getTags().contains("[AI]")) {
                        a.setTags(a.getTags() + ",[AI]");
                    } else if (a.getTags() == null || a.getTags().isBlank()) {
                        a.setTags("[AI]");
                    }
                }
            } catch (Exception e) {
                log.warn("AI enrichment failed for article '{}': {}",
                        a.getTitle() != null && a.getTitle().length() > 60
                                ? a.getTitle().substring(0, 60) + "..."
                                : a.getTitle(),
                        e.getMessage());
            }

            enriched.add(a);
        }

        // 替换原列表
        resultItems.put("articles", enriched);

        if (totalEnriched > 0) {
            log.info("AI enriched {}/{} articles (summaries: {}, tags: {})",
                    totalEnriched, articles.size(), summaryEnriched, tagsEnriched);
        } else {
            log.debug("AI enrichment: 0/{} articles needed enhancement", articles.size());
        }
    }
}