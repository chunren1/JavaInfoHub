package com.webmagic.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页 — 转发到 React SPA
 *
 * @author webmagic-demo
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}