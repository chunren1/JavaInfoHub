package com.webmagic.web.controller;

import com.github.pagehelper.PageInfo;
import com.webmagic.common.entity.TechArticle;
import com.webmagic.core.scheduler.CrawlTriggerService;
import com.webmagic.web.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API — React 前端数据接口
 *
 * 所有 API 路径以 /api 开头，与 SPA 路由互不冲突。
 * 返回标准 JSON：{ success, data, message } 或 CrawlController 原有格式。
 *
 * @author webmagic-demo
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CrawlTriggerService crawlTriggerService;

    // ======================== Dashboard ========================

    /**
     * GET /api/dashboard — 首页统计 + 最新文章
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        try {
            Map<String, Object> stats = articleService.getDashboardStats();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "DB connection failed: " + e.getMessage()
            ));
        }
    }

    // ======================== Articles ========================

    /**
     * GET /api/articles — 分页搜索文章
     */
    @GetMapping("/articles")
    public ResponseEntity<Map<String, Object>> articles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageInfo<TechArticle> pageInfo = articleService.search(keyword, source, page, size);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("pageNum", pageInfo.getPageNum());
        pagination.put("pageSize", pageInfo.getPageSize());
        pagination.put("pages", pageInfo.getPages());
        pagination.put("total", pageInfo.getTotal());
        pagination.put("list", pageInfo.getList());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", pagination
        ));
    }

    /**
     * GET /api/articles/{id} — 文章详情
     */
    @GetMapping("/articles/{id}")
    public ResponseEntity<Map<String, Object>> articleDetail(@PathVariable Long id) {
        TechArticle article = articleService.getById(id);
        if (article == null) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Article not found"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", article
        ));
    }

    // ======================== Crawl ========================

    /**
     * POST /api/crawl/trigger — 爬取全部 4 个源
     */
    @PostMapping("/crawl/trigger")
    public ResponseEntity<?> triggerAll() {
        try {
            Map<String, CrawlTriggerService.CrawlResult> results = crawlTriggerService.crawlAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Crawl completed",
                    "results", results,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * POST /api/crawl/trigger/{source} — 爬取单个源
     */
    @PostMapping("/crawl/trigger/{source}")
    public ResponseEntity<?> triggerBySource(@PathVariable String source) {
        try {
            CrawlTriggerService.CrawlResult result = crawlTriggerService.crawlBySource(source);
            return ResponseEntity.ok(Map.of(
                    "success", result.isSuccess(),
                    "message", result.getMessage(),
                    "result", result,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * GET /api/crawl/status — 爬取状态轮询
     */
    @GetMapping("/crawl/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "statusMap", crawlTriggerService.getAllStatus(),
                "timestamp", System.currentTimeMillis()
        ));
    }
}