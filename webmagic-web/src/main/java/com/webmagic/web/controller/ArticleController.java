package com.webmagic.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 文章页面 — 转发到 React SPA（React Router 处理路由）
 *
 * @author webmagic-demo
 */
@Controller
public class ArticleController {

    @GetMapping("/articles")
    public String list() {
        return "forward:/index.html";
    }

    @GetMapping("/articles/{id}")
    public String detail() {
        return "forward:/index.html";
    }
}