package com.webmagic.core.processor;

import com.webmagic.common.entity.TechArticle;
import com.webmagic.common.enums.SourceType;
import com.webmagic.common.util.RegexUtils;
import com.webmagic.common.util.SleepUtils;
import com.webmagic.common.util.UserAgentUtils;
import com.webmagic.core.processor.AiProcessorHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SegmentFault 文章爬虫 — 纯 CSS 选择器
 *
 * 【JD 覆盖：Jsoup CSS 选择器】
 *
 *  展示纯 CSS 选择器的完整用法：
 *  - 列表提取：.news-item（获取文章列表容器）
 *  - 标题提取：.news-title a（后代选择器）
 *  - 摘要提取：.news-summary（类选择器）
 *  - 作者提取：.author-name（类选择器）
 *  - 时间提取：.news-time（类选择器 + 正则清洗）
 *  - 翻页逻辑：URL 参数 ?page=N
 *
 * 【面试话术】
 *  \"SegmentFault 的页面结构清晰，直接渲染在 HTML 中，用 Jsoup CSS 选择器最合适。
 *   我用 .news-item 做列表容器，后代选择器提取子元素——.news-title a 拿标题、
 *   .news-summary 拿摘要。CSS 选择器语法直观，适合 DOM 结构规整的站点。\"
 *
 *  CSS 选择器速查（面试时可讲）：
 *   .class          → 类选择器
 *   #id             → ID 选择器
 *   div a           → 后代选择器
 *   div > a         → 子代选择器
 *   div[attr=val]   → 属性选择器
 *   div:first-child → 伪类选择器
 *   div.class1.class2 → 多类选择器
 *
 * @author webmagic-demo
 */
@Slf4j
@Component
@Scope("prototype")
public class SegmentfaultPageProcessor implements PageProcessor {

    @Autowired(required = false)
    private AiProcessorHelper aiProcessorHelper;

    /** SegmentFault 新闻列表 URL 模板 */
    private static final String LIST_URL = "https://segmentfault.com/news?page=%d";

    /** 爬取起点 */
    private static final String START_URL = "https://segmentfault.com/news";

    private int maxPages = 3;

    private Site site = Site.me()
            .setDomain("segmentfault.com")
            .setUserAgent(UserAgentUtils.getRandom())
            .setRetryTimes(3)
            .setSleepTime(3000)
            .setTimeOut(10000)
            .setCharset("UTF-8")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9");

    @Override
    public Site getSite() {
        return site;
    }

    @Override
    public void process(Page page) {
        // ===== 1. 获取文章列表容器 =====
        // 【JD-CSS】选择所有 .news-item 元素
        List<Selectable> newsItems = page.getHtml().css(".news-item").nodes();

        if (newsItems == null || newsItems.isEmpty()) {
            log.warn("SegmentFault 未找到文章列表，可能页面结构已变更");
            // AI fallback：CSS 选择器失效时用 LLM 从原始 HTML 提取
            List<TechArticle> aiArticles = aiFallback(page);
            page.putField("articles", aiArticles);
            if (!aiArticles.isEmpty()) {
                log.info("AI fallback recovered {} articles for SegmentFault", aiArticles.size());
            }
            return;
        }

        List<TechArticle> articles = new ArrayList<>();

        for (Selectable item : newsItems) {
            try {
                // ===== 2. 提取标题（.news-title a）=====
                // 【JD-CSS】后代选择器：.news-title 内的 a 标签
                String title = item.css(".news-title a, .card-title a, a.h5").xpath("//a/text()").get();
                if (title != null) {
                    title = RegexUtils.removeExtraWhitespace(title);
                }
                if (StringUtils.isBlank(title)) {
                    continue;
                }

                // ===== 3. 提取链接 =====
                String contentUrl = item.css(".news-title a, a.h5").links().get();
                if (StringUtils.isBlank(contentUrl)) {
                    contentUrl = item.links().all().stream()
                            .filter(link -> link != null && (link.contains("/a/") || link.contains("/p/")))
                            .findFirst().orElse(null);
                }
                if (contentUrl != null && !contentUrl.startsWith("http")) {
                    contentUrl = "https://segmentfault.com" + contentUrl;
                }

                // ===== 4. 提取摘要（.news-summary / .card-body）=====
                String summary = item.css(".news-summary, .text-secondary, .summary").xpath("//text()").get();
                if (summary != null) {
                    summary = RegexUtils.cleanHtml(summary);
                    summary = RegexUtils.removeExtraWhitespace(summary);
                    summary = RegexUtils.truncate(summary, 500);
                }

                // ===== 5. 提取作者 =====
                String author = item.css(".author-name, .username, [itemprop='name']").xpath("//text()").get();
                if (author != null) {
                    author = RegexUtils.removeExtraWhitespace(author);
                }

                // ===== 6. 提取标签 =====
                StringBuilder tagsBuilder = new StringBuilder();
                item.css(".tag, .badge").xpath("//text()").all().forEach(tag -> {
                    String cleanTag = RegexUtils.removeExtraWhitespace(tag);
                    if (StringUtils.isNotBlank(cleanTag)) {
                        if (tagsBuilder.length() > 0) tagsBuilder.append(",");
                        tagsBuilder.append(cleanTag);
                    }
                });
                String tags = tagsBuilder.length() > 0 ? tagsBuilder.toString() : null;

                // ===== 7. 计算去重键 =====
                String sourceId = contentUrl != null ? contentUrl : title;
                String dedupKey = md5(SourceType.SEGMENTFAULT.name() + ":" + sourceId);

                // ===== 8. 构建实体 =====
                TechArticle article = TechArticle.builder()
                        .title(title)
                        .summary(summary)
                        .contentUrl(contentUrl)
                        .author(author)
                        .tags(tags)
                        .source(SourceType.SEGMENTFAULT.name())
                        .sourceId(sourceId)
                        .viewCount(0)
                        .starCount(0)
                        .dedupKey(dedupKey)
                        .crawlTime(LocalDateTime.now())
                        .build();

                articles.add(article);
            } catch (Exception e) {
                log.debug("解析 SegmentFault 文章条目时出错: {}", e.getMessage());
            }
        }

        log.info("【SegmentFault】第 {} 页，解析 {} 条",
                getCurrentPage(page.getUrl().get()), articles.size());

        // 放入结果集
        page.putField("articles", articles);

        // ===== 9. 翻页逻辑 =====
        int current = getCurrentPage(page.getUrl().get());
        if (current < maxPages) {
            SleepUtils.randomDelay(2000, 4000);
            String nextUrl = String.format(LIST_URL, current + 1);
            page.addTargetRequest(nextUrl);
            log.debug("SegmentFault → 添加第 {} 页: {}", current + 1, nextUrl);
        } else {
            log.info("SegmentFault 爬取完成，共 {} 页", current);
        }
    }

    /**
     * 从 URL 中提取当前页码
     */
    private int getCurrentPage(String url) {
        if (url == null || url.equals(START_URL)) return 1;
        Integer page = RegexUtils.parseInt(url);
        return page != null ? page : 1;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.error("MD5 计算失败", e);
            return input;
        }
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    /**
     * AI fallback — 当 CSS 选择器失效时调用 LLM 从原始 HTML 提取
     */
    private List<TechArticle> aiFallback(Page page) {
        if (aiProcessorHelper != null && aiProcessorHelper.shouldFallback(0)) {
            return aiProcessorHelper.extractArticlesFromHtml(page, SourceType.SEGMENTFAULT);
        }
        return new ArrayList<>();
    }
}