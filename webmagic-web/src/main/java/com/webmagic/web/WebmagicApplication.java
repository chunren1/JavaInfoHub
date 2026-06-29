package com.webmagic.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Java 开发者信息聚合平台 — Spring Boot 启动类
 *
 * 【JD 覆盖：Spring】
 *  - @SpringBootApplication：Spring Boot 核心注解
 *  - @MapperScan：MyBatis Mapper 扫描（展示 Spring + MyBatis 集成）
 *  - @EnableScheduling：开启定时任务（展示 Spring @Scheduled）
 *
 * @author webmagic-demo
 */
@SpringBootApplication(scanBasePackages = "com.webmagic")
@MapperScan("com.webmagic.dao.mapper")
@EnableScheduling
public class WebmagicApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebmagicApplication.class, args);
        System.out.println("");
        System.out.println("========================================");
        System.out.println("  Java 开发者信息聚合平台 启动成功！");
        System.out.println("  访问地址：http://localhost:8080");
        System.out.println("  API 接口：");
        System.out.println("    首页  GET  /");
        System.out.println("    文章  GET  /articles");
        System.out.println("    职位  GET  /jobs");
        System.out.println("    爬取  POST /crawl/trigger");
        System.out.println("    状态  GET  /crawl/status");
        System.out.println("========================================");
        System.out.println("");
    }
}