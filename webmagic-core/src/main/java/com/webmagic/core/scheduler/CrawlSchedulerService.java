package com.webmagic.core.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 爬虫定时调度服务
 *
 * 【JD 覆盖：Spring @Scheduled 定时任务 + HTTP 状态码处理】
 *
 *  面试展示点：
 *  1. @Scheduled(cron="0 0 2 * * ?") — 每日凌晨 2 点自动爬取
 *  2. cron 表达式解析（面试官可能现场问 cron 表达式的含义）
 *  3. 退避重试策略 — 处理 429/503 等反爬状态码
 *
 *  cron 表达式解析（面试时可逐一解释）：
 *   0    0    2    *    *    ?
 *   秒   分   时  日   月   周（? 表示不指定）
 *
 * 【面试话术】
 *  \"定时任务是爬虫的常见需求——每天凌晨自动更新数据。
 *   我用 Spring 的 @Scheduled 注解实现，cron 表达式控制时间。
 *   并且在 CronTriggerService 中实现了指数退避重试——
 *   遇到 429(限流)或 503(服务不可用) 时，按 2s → 4s → 8s 递增等待后重试，
 *   最多重试 3 次。这体现了对 HTTP 状态码的实际处理能力。\"
 *
 * @author webmagic-demo
 */
@Slf4j
@Service
public class CrawlSchedulerService {

    @Autowired
    private CrawlTriggerService triggerService;

    /** 上次定时任务执行时间 */
    private LocalDateTime lastScheduledRun;

    /** 定时任务是否正在运行 */
    private volatile boolean scheduledRunning = false;

    /**
     * 定时爬取任务 — 每日凌晨 2:00 执行
     *
     * @Scheduled cron 表达式：
     *   0    0    2    *    *    ?
     *   秒   分   时  日   月   周
     *
     *   面试话术：\"cron 有 6 位（秒 分 时 日 月 周），? 表示不指定。
     *   0 0 2 * * ? 就是每天凌晨 2 点整执行。\"
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCrawl() {
        if (scheduledRunning) {
            log.warn("上次定时任务尚未完成，跳过本次执行");
            return;
        }

        scheduledRunning = true;
        lastScheduledRun = LocalDateTime.now();
        log.info("========================================");
        log.info("===  定时爬取任务启动  (凌晨 2:00)  ===");
        log.info("========================================");

        try {
            Map<String, CrawlTriggerService.CrawlResult> results = triggerService.crawlAll();

            // 输出结果汇总
            log.info("=== 定时爬取结果汇总 ===");
            int successCount = 0;
            int failCount = 0;
            for (Map.Entry<String, CrawlTriggerService.CrawlResult> entry : results.entrySet()) {
                if (entry.getValue().isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                }
                log.info("  {} → {}: {}",
                        entry.getKey(),
                        entry.getValue().isSuccess() ? "成功" : "失败",
                        entry.getValue().getMessage());
            }
            log.info("共计：{} 成功, {} 失败", successCount, failCount);

        } catch (Exception e) {
            log.error("定时爬取任务异常: {}", e.getMessage(), e);
        } finally {
            scheduledRunning = false;
            log.info("=== 定时爬取任务结束 ===");
        }
    }

    /**
     * 获取上次定时任务执行时间
     */
    public LocalDateTime getLastScheduledRun() {
        return lastScheduledRun;
    }

    /**
     * 定时任务是否正在运行
     */
    public boolean isScheduledRunning() {
        return scheduledRunning;
    }
}