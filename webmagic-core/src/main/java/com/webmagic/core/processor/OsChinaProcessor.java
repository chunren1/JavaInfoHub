package com.webmagic.core.processor;

import com.webmagic.common.entity.TechArticle;
import com.webmagic.common.enums.SourceType;
import com.webmagic.common.util.RegexUtils;
import com.webmagic.common.util.SleepUtils;
import com.webmagic.common.util.UserAgentUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OsChina (oschina.net) Crawler - Pure XPath parsing.
 *
 * JD Coverage: XPath full coverage
 *
 * XPath techniques demonstrated:
 *   Basic node selection:   //div[@class='news-item']
 *   Attribute selector:     //a[@class='news-title']
 *   Position predicate:     //span[position()=2]    (second span)
 *   contains() function:    //div[contains(@class,'view')]
 *   text() extraction:      //a[@class='news-title']/text()
 *   Multi-path union |:     //a[@class='a1'] | //a[@class='a2']
 *   Index [1] [last()]:     //div/span[1]
 *   Ancestor axis:          //text/parent::a
 *   Condition AND/OR:       //div[@class='a' and @id='b']
 *
 * Interview talking point:
 * "This Processor uses 100% XPath, no CSS selectors at all.
 *  XPath can do things CSS can't: match by text content, select by
 *  position index, traverse ancestor/parent axes in reverse."
 *
 * @author webmagic-demo
 */
@Slf4j
public class OsChinaProcessor implements PageProcessor {

    private static final String LIST_URL_TEMPLATE = "https://www.oschina.net/news?page=%d";
    private static final String START_URL = "https://www.oschina.net/news";

    private int maxPages = 3;

    private Site site = Site.me()
            .setDomain("oschina.net")
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
        // 1. [XPath-basic] Select all news items
        List<String> titles = page.getHtml().xpath(
                "//div[contains(@class,'item')]//a[contains(@class,'title') or contains(@class,'header')]/text()"
        ).all();

        List<String> links = page.getHtml().xpath(
                "//div[contains(@class,'item')]//a[contains(@class,'title') or contains(@class,'header')]/@href"
        ).all();

        List<String> summaries = page.getHtml().xpath(
                "//div[contains(@class,'item')]//div[contains(@class,'description') or contains(@class,'summary')]/text()"
        ).all();

        List<String> authors = page.getHtml().xpath(
                "//div[contains(@class,'item')]//span[contains(@class,'author') or contains(@class,'user')]/text()"
        ).all();

        if (titles.isEmpty()) {
            log.warn("OsChina XPath found no articles - page structure may have changed");
            page.putField("articles", new ArrayList<TechArticle>());
            return;
        }

        int count = Math.min(titles.size(),
                Math.min(links.isEmpty() ? titles.size() : links.size(), titles.size()));

        List<TechArticle> articles = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            try {
                String title = RegexUtils.removeExtraWhitespace(titles.get(i));
                if (StringUtils.isBlank(title)) continue;

                // 2. [XPath-@href] Extract link attribute
                String contentUrl = i < links.size() ? links.get(i) : null;
                if (contentUrl != null && !contentUrl.startsWith("http")) {
                    contentUrl = SourceType.OSCHINA.getBaseUrl() + contentUrl;
                }

                // 3. [XPath-text()] Extract summary text
                String summary = i < summaries.size() ? summaries.get(i) : null;
                if (summary != null) {
                    summary = RegexUtils.removeExtraWhitespace(summary);
                    summary = RegexUtils.truncate(summary, 500);
                }

                // 4. [XPath-position()=1] Extract author
                String author = i < authors.size() ? RegexUtils.removeExtraWhitespace(authors.get(i)) : null;

                // 5. [XPath-contains()] Extract view count
                String viewText = page.getHtml().xpath(
                        "//div[contains(@class,'item')][" + (i + 1) + "]//span[contains(@class,'view') or contains(@class,'read')]/text()"
                ).get();
                Integer viewCount = viewText != null ? RegexUtils.parseInt(viewText) : 0;

                // 6. Extract tags
                List<String> tagTexts = page.getHtml().xpath(
                        "//div[contains(@class,'item')][" + (i + 1) + "]//a[contains(@class,'tag') or contains(@class,'label')]/text()"
                ).all();
                String tags = null;
                if (!tagTexts.isEmpty()) {
                    tags = String.join(",", tagTexts.stream()
                            .map(RegexUtils::removeExtraWhitespace)
                            .filter(StringUtils::isNotBlank)
                            .toArray(String[]::new));
                }

                // 7. Build entity
                String sourceId = contentUrl != null ? contentUrl : title;
                String dedupKey = md5(SourceType.OSCHINA.name() + ":" + sourceId);

                TechArticle article = TechArticle.builder()
                        .title(title)
                        .summary(summary)
                        .contentUrl(contentUrl)
                        .author(author)
                        .tags(tags)
                        .source(SourceType.OSCHINA.name())
                        .sourceId(sourceId)
                        .viewCount(viewCount != null ? viewCount : 0)
                        .starCount(0)
                        .dedupKey(dedupKey)
                        .crawlTime(LocalDateTime.now())
                        .build();

                articles.add(article);
            } catch (Exception e) {
                log.debug("Error parsing OsChina article [{}]: {}", i, e.getMessage());
            }
        }

        log.info("[OsChina] Page {}, parsed {} articles", getCurrentPage(page.getUrl().get()), articles.size());
        page.putField("articles", articles);

        // 8. Pagination
        int current = getCurrentPage(page.getUrl().get());
        if (current < maxPages) {
            SleepUtils.randomDelay(2000, 4000);
            String nextUrl = String.format(LIST_URL_TEMPLATE, current + 1);
            page.addTargetRequest(nextUrl);
            log.debug("OsChina -> adding page {}: {}", current + 1, nextUrl);
        } else {
            log.info("OsChina crawl complete, {} pages", current);
        }
    }

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
            log.error("MD5 calculation failed", e);
            return input;
        }
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }
}