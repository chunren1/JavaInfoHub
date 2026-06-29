package com.webmagic.core.scheduler;

import com.webmagic.core.pipeline.AiEnrichmentPipeline;
import com.webmagic.core.pipeline.ArticlePersistPipeline;
import com.webmagic.core.pipeline.DedupPipeline;
import com.webmagic.core.processor.*;
import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import us.codecraft.webmagic.Spider;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crawl trigger service - manages Spider lifecycle.
 *
 * JD Coverage: WebMagic framework usage
 * - Creates and starts Spiders by data source
 * - Manages running state (prevents duplicate starts)
 * - Returns crawl result statistics
 *
 * @author webmagic-demo
 */
@Slf4j
@Service
public class CrawlTriggerService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DedupPipeline dedupPipeline;

    @Autowired
    private AiEnrichmentPipeline aiEnrichmentPipeline;

    @Autowired
    private ArticlePersistPipeline articlePersistPipeline;

    @Value("${crawl.max-pages.juejin:3}")
    private int juejinMaxPages;

    @Value("${crawl.max-pages.segmentfault:3}")
    private int segmentfaultMaxPages;

    @Value("${crawl.max-pages.github:1}")
    private int githubMaxPages;

    @Value("${crawl.max-pages.oschina:3}")
    private int oschinaMaxPages;

    /** Running status map: source -> latest crawl info */
    private final ConcurrentHashMap<String, CrawlStatus> statusMap = new ConcurrentHashMap<>();

    /**
     * Trigger crawl for a specific source
     */
    public CrawlResult crawlBySource(String source) {
        String sourceKey = source.toUpperCase();

        CrawlStatus status = statusMap.get(sourceKey);
        if (status != null && status.isRunning()) {
            log.warn("[{}] Crawler already running, skip duplicate trigger", sourceKey);
            return CrawlResult.builder()
                    .source(sourceKey)
                    .success(false)
                    .message("Crawler is already running for this source")
                    .build();
        }

        CrawlStatus newStatus = new CrawlStatus();
        newStatus.setRunning(true);
        newStatus.setStartTime(LocalDateTime.now());
        statusMap.put(sourceKey, newStatus);

        Spider spider = null;
        try {
            spider = createSpider(sourceKey);
            spider.run();
            spider.close();

            long duration = java.time.Duration.between(
                    newStatus.getStartTime(), LocalDateTime.now()).getSeconds();

            newStatus.setRunning(false);
            newStatus.setLastCrawlTime(LocalDateTime.now());
            newStatus.setSuccess(true);
            statusMap.put(sourceKey, newStatus);

            log.info("[{}] Crawl complete, {} seconds", sourceKey, duration);
            return CrawlResult.builder()
                    .source(sourceKey)
                    .success(true)
                    .message("Crawl complete")
                    .durationSeconds(duration)
                    .build();
        } catch (Exception e) {
            log.error("[{}] Crawl failed: {}", sourceKey, e.getMessage(), e);
            newStatus.setRunning(false);
            newStatus.setSuccess(false);
            newStatus.setLastError(e.getMessage());
            statusMap.put(sourceKey, newStatus);

            if (spider != null) {
                try { spider.close(); } catch (Exception ignored) {}
            }

            return CrawlResult.builder()
                    .source(sourceKey)
                    .success(false)
                    .message("Crawl failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Crawl all 4 article sources (sequential)
     */
    public Map<String, CrawlResult> crawlAll() {
        Map<String, CrawlResult> results = new ConcurrentHashMap<>();
        String[] sources = {"juejin", "segmentfault", "github", "oschina"};
        for (String source : sources) {
            try {
                results.put(source, crawlBySource(source));
            } catch (Exception e) {
                log.error("[{}] Exception: {}", source, e.getMessage());
                results.put(source, CrawlResult.builder()
                        .source(source).success(false).message(e.getMessage()).build());
            }
        }
        return results;
    }

    /**
     * Get all crawl statuses
     */
    public Map<String, CrawlStatus> getAllStatus() {
        return new ConcurrentHashMap<>(statusMap);
    }

    /**
     * Create Spider by source with proper Processor + Pipeline chain
     */
    private Spider createSpider(String source) {
        Spider spider;

        switch (source.toUpperCase()) {
            case "JUEJIN":
                JuejinPageProcessor p1 = applicationContext.getBean(JuejinPageProcessor.class);
                p1.setMaxPages(juejinMaxPages);
                spider = Spider.create(p1)
                        .addUrl("https://juejin.cn")
                        .addPipeline(dedupPipeline)
                        .addPipeline(aiEnrichmentPipeline)
                        .addPipeline(articlePersistPipeline)
                        .thread(1);
                break;

            case "SEGMENTFAULT":
                SegmentfaultPageProcessor p2 = applicationContext.getBean(SegmentfaultPageProcessor.class);
                p2.setMaxPages(segmentfaultMaxPages);
                spider = Spider.create(p2)
                        .addUrl("https://segmentfault.com/news")
                        .addPipeline(dedupPipeline)
                        .addPipeline(aiEnrichmentPipeline)
                        .addPipeline(articlePersistPipeline)
                        .thread(1);
                break;

            case "GITHUB":
                GithubTrendingProcessor p3 = applicationContext.getBean(GithubTrendingProcessor.class);
                p3.setLanguage("java");
                spider = Spider.create(p3)
                        .addUrl("https://github.com/trending/java?since=daily")
                        .addPipeline(dedupPipeline)
                        .addPipeline(aiEnrichmentPipeline)
                        .addPipeline(articlePersistPipeline)
                        .thread(1);
                break;

            case "OSCHINA":
                OsChinaProcessor p4 = applicationContext.getBean(OsChinaProcessor.class);
                p4.setMaxPages(oschinaMaxPages);
                spider = Spider.create(p4)
                        .addUrl("https://www.oschina.net/news")
                        .addPipeline(dedupPipeline)
                        .addPipeline(aiEnrichmentPipeline)
                        .addPipeline(articlePersistPipeline)
                        .thread(1);
                break;

            default:
                throw new IllegalArgumentException("Unknown source: " + source);
        }

        return spider;
    }

    // ---- Inner classes ----

    @Data
    public static class CrawlStatus {
        private boolean running;
        private LocalDateTime startTime;
        private LocalDateTime lastCrawlTime;
        private boolean success;
        private String lastError;
    }

    @Data
    @Builder
    public static class CrawlResult {
        private String source;
        private boolean success;
        private String message;
        private Long durationSeconds;
        private Integer newAdded;
    }
}