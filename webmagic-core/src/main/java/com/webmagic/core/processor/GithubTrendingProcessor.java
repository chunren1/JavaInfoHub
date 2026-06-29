package com.webmagic.core.processor;

import com.webmagic.common.entity.TechArticle;
import com.webmagic.common.enums.SourceType;
import com.webmagic.common.util.RegexUtils;
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
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub Trending 爬虫 — CSS 选择器 + 正则表达式
 *
 * 【JD 覆盖：Jsoup CSS 选择器 + 正则表达式 + UA 轮换】
 *
 *  技术点：
 *  - CSS 选择器：提取仓库名 (.h3 a)、描述 (p)、语言 ([itemprop="programmingLanguage"])
 *  - 正则表达式：从 "52.3k stars" 中提取数字 52300
 *  - 正则表达式：从 "built by @张三" 中提取作者信息
 *  - UA 轮换：每次请求使用随机 UA（GitHub 对同一 UA 高频访问有频率限制）
 *
 * 【面试话术】
 *  "GitHub Trending 页面结构规整，主数据用 CSS 选择器提取。
 *   但 star/fork 数字格式是 '52.3k' 这种——需要正则去掉 'k' 并换算成整数。
 *   RegexUtils.parseInt() 就是为这个场景设计的。
 *   另外 GitHub 对 UA 比较敏感，我用 UserAgentUtils 做随机轮换避免限流。"
 *
 * @author webmagic-demo
 */
@Slf4j
@Component
@Scope("prototype")
public class GithubTrendingProcessor implements PageProcessor {

    /** GitHub Trending 页面 URL */
    private static final String TRENDING_URL = "https://github.com/trending";

    /** 可选：按语言筛选 (如 /trending/java) */
    private static final String TRENDING_URL_TEMPLATE = "https://github.com/trending/%s?since=daily";

    private String language = "java";

    @Autowired(required = false)
    private AiProcessorHelper aiProcessorHelper;

    private Site site = Site.me()
            .setDomain("github.com")
            .setUserAgent(UserAgentUtils.getRandom())
            .setRetryTimes(3)
            .setCycleRetryTimes(2)
            .setSleepTime(5000)          // GitHub 要求更长的间隔
            .setTimeOut(15000)
            .setCharset("UTF-8")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("Cache-Control", "no-cache");

    @Override
    public Site getSite() {
        return site;
    }

    @Override
    public void process(Page page) {
        // 每次请求更新 UA（UA 轮换）
        site.setUserAgent(UserAgentUtils.getRandom());

        // ===== 1. 获取项目列表容器 =====
        // 【JD-CSS】选择 article.Box-row 元素（GitHub Trending 的列表项）
        List<Selectable> repos = page.getHtml().css("article.Box-row").nodes();

        if (repos == null || repos.isEmpty()) {
            // 尝试备用选择器（页面可能改版）
            repos = page.getHtml().css(".Box-row").nodes();
        }

        if (repos == null || repos.isEmpty()) {
            log.warn("GitHub Trending 未找到项目列表，可能被限流或页面结构变更");
            // AI fallback：CSS 选择器失效时用 LLM 从原始 HTML 提取
            List<TechArticle> aiArticles = aiFallback(page);
            page.putField("articles", aiArticles);
            if (!aiArticles.isEmpty()) {
                log.info("AI fallback recovered {} articles for GitHub Trending", aiArticles.size());
            }
            return;
        }

        List<TechArticle> articles = new ArrayList<>();

        for (Selectable repo : repos) {
            try {
                // ===== 2. 提取仓库名（CSS：.h3 a 内的文本）=====
                String fullName = repo.css("h2 a, h3 a").xpath("//text()").get();
                if (fullName != null) {
                    fullName = fullName.replaceAll("\\s+", "").trim(); // GitHub 的仓库名含大量空格
                }
                if (StringUtils.isBlank(fullName)) {
                    continue;
                }

                // 分割 owner/repo
                String[] parts = fullName.split("/");
                String owner = parts.length > 0 ? parts[0].trim() : "";
                String repoName = parts.length > 1 ? parts[1].trim() : "";
                String title = fullName.trim();

                // ===== 3. 提取项目描述（CSS：p 标签）=====
                String summary = repo.css("p").xpath("//text()").get();
                if (summary != null) {
                    summary = RegexUtils.removeExtraWhitespace(summary);
                    summary = RegexUtils.truncate(summary, 500);
                }

                // ===== 4. 提取编程语言 =====
                String lang = repo.css("[itemprop='programmingLanguage']").xpath("//text()").get();
                if (lang == null) {
                    // 备用：从 span 中提取
                    List<String> spans = repo.xpath("//span[@itemprop='programmingLanguage']/text()").all();
                    if (!spans.isEmpty()) {
                        lang = spans.get(0);
                    }
                }
                if (lang != null) {
                    lang = RegexUtils.removeExtraWhitespace(lang);
                }

                // ===== 5. 提取 Star 数 — 【JD-正则：从 "52.3k" 提取数字】=====
                Integer starCount = 0;
                String starText = repo.css(".octicon-star + *, [aria-label$='star']").xpath("//text()").get();
                if (starText == null) {
                    // 备用 XPath 方式
                    List<String> texts = repo.xpath(
                            "//a[contains(@href, '/stargazers')]/text()").all();
                    if (!texts.isEmpty()) starText = texts.get(0);
                }
                if (starText != null) {
                    starCount = RegexUtils.parseInt(starText);
                }

                // ===== 6. 提取 Fork 数 — 【JD-正则：从 "1.2k" 提取数字】=====
                Integer forkCount = 0;
                String forkText = repo.css(".octicon-repo-forked + *, [aria-label$='fork']").xpath("//text()").get();
                if (forkText == null) {
                    List<String> fTexts = repo.xpath(
                            "//a[contains(@href, '/forks')]/text()").all();
                    if (!fTexts.isEmpty()) forkText = fTexts.get(0);
                }
                if (forkText != null) {
                    forkCount = RegexUtils.parseInt(forkText);
                }

                // ===== 7. 提取今日 Star 数 =====
                Integer todayStars = 0;
                String todayText = repo.css(".float-sm-right, .d-inline-block").xpath("//text()").get();
                if (todayText != null) {
                    todayStars = RegexUtils.parseInt(todayText);
                }

                // ===== 8. 构建标签 =====
                StringBuilder tagsBuilder = new StringBuilder();
                if (lang != null) {
                    tagsBuilder.append(lang);
                }
                if (forkCount != null && forkCount > 0) {
                    if (tagsBuilder.length() > 0) tagsBuilder.append(",");
                    tagsBuilder.append("fork:").append(forkCount);
                }
                String tags = tagsBuilder.length() > 0 ? tagsBuilder.toString() : null;

                // ===== 9. 构建实体 =====
                String contentUrl = "https://github.com/" + fullName.trim();
                String sourceId = fullName.trim();
                String dedupKey = md5(SourceType.GITHUB.name() + ":" + sourceId);

                TechArticle article = TechArticle.builder()
                        .title(title)
                        .summary(summary)
                        .contentUrl(contentUrl)
                        .author(owner)
                        .tags(tags)
                        .source(SourceType.GITHUB.name())
                        .sourceId(sourceId)
                        .viewCount(todayStars != null ? todayStars : 0)
                        .starCount(starCount != null ? starCount : 0)
                        .dedupKey(dedupKey)
                        .crawlTime(LocalDateTime.now())
                        .build();

                articles.add(article);
            } catch (Exception e) {
                log.debug("解析 GitHub Trending 条目时出错: {}", e.getMessage());
            }
        }

        log.info("【GitHub Trending】解析 {} 个项目", articles.size());
        page.putField("articles", articles);

        // GitHub Trending 仅一页即完成（无需翻页）
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

    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * AI fallback — 当 CSS 选择器失效时调用 LLM 从原始 HTML 提取
     */
    private List<TechArticle> aiFallback(Page page) {
        if (aiProcessorHelper != null && aiProcessorHelper.shouldFallback(0)) {
            return aiProcessorHelper.extractArticlesFromHtml(page, SourceType.GITHUB);
        }
        return new ArrayList<>();
    }
}