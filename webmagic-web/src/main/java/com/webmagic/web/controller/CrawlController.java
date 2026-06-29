package com.webmagic.web.controller;

import com.webmagic.core.scheduler.CrawlTriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 爬虫触发 Controller — Web 界面一键触发 + 状态查询
 *
 * 【JD 覆盖：Spring MVC + 爬虫 Web 操作面板】
 *
 *  面试演示核心 Controller：
 *  - POST /crawl/trigger       → 触发全部爬取（"一键爬取"按钮）
 *  - POST /crawl/trigger/{source} → 按数据源逐个触发
 *  - GET  /crawl/status         → 查询爬取状态（前端轮询）
 *
 * @author webmagic-demo
 */
@Slf4j
@Controller
public class CrawlController {

    @Autowired
    private CrawlTriggerService triggerService;

    @GetMapping("/crawl")
    public String crawlPage() {
        return "redirect:/";
    }

    /**
     * 一键爬取全部数据源（面试演示核心接口）
     */
    @PostMapping("/crawl/trigger")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerAll() {
        log.info("收到手动触发请求：开始爬取全部数据源");

        try {
            Map<String, CrawlTriggerService.CrawlResult> results = triggerService.crawlAll();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "全部爬取完成");
            response.put("results", results);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("手动触发爬取失败: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "爬取失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 按数据源触发爬取（面试时逐个演示）
     */
    @PostMapping("/crawl/trigger/{source}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerBySource(
            @PathVariable String source) {

        log.info("手动触发：单独爬取数据源 [{}]", source);

        try {
            CrawlTriggerService.CrawlResult result = triggerService.crawlBySource(source);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("result", result);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("按来源触发爬取失败 [{}]: {}", source, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "爬取失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 查询爬取状态（前端轮询接口）
     */
    @GetMapping("/crawl/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("statusMap", triggerService.getAllStatus());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}