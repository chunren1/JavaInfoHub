package com.webmagic.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 *
 * 【JD 覆盖：正则表达式 + 能看懂 js 语句和 function】
 *
 *  六大正则方法覆盖全部爬虫场景：
 *   1. extract()       — 提取第一个匹配组
 *   2. extractAll()    — 提取所有匹配（列表提取）
 *   3. cleanHtml()     — 清除 HTML 标签（获取纯文本）
 *   4. removeExtraWhitespace() — 清理多余空白字符
 *   5. parseInt()      — 从字符串中提取数字（star/fork/view 数值）
 *   6. truncate()      — 截断文本到指定长度
 *
 *  外加两个 JS 提取方法（面试重点）：
 *   7. jsVarExtractor()   — 从 <script> 标签中提取 JS 变量
 *   8. jsValueExtractor() — 从 JS 代码中按模式提取值
 *
 * 【面试话术】
 *  \"正则提取是爬虫的基本功。我封装了这 8 个方法覆盖了所有常见场景——
 *    单值提取、列表提取、HTML 清洗、数字提取，以及从 JS 中提取隐藏数据。
 *    每个方法都有明确的边界处理——null 安全、空串安全。\"
 *
 * @author webmagic-demo
 */
public final class RegexUtils {

    private RegexUtils() {
        // 工具类不可实例化
    }

    // ================================================================
    // 基础正则方法
    // ================================================================

    /**
     * 从输入字符串中提取第一个匹配组
     *
     * 适用场景：从 HTML 中提取单个字段（标题、作者、链接等）
     *
     * 示例：
     *   extract("<a href='/p/123'>文章标题</a>", "href='([^']+)'", 1)
     *   → 返回 "/p/123"
     *
     * @param input 输入文本
     * @param regex 正则表达式（需包含捕获组）
     * @param group 捕获组编号（1-based）
     * @return 匹配的字符串，未匹配返回 null
     */
    public static String extract(String input, String regex, int group) {
        if (input == null || regex == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(regex).matcher(input);
        if (matcher.find() && group <= matcher.groupCount()) {
            return matcher.group(group);
        }
        return null;
    }

    /**
     * 从输入字符串中提取所有匹配
     *
     * 适用场景：从列表页 HTML 中批量提取同类型元素
     *
     * 示例（GitHub Trending）：提取所有仓库名
     *   extractAll(html, "href=\"/[^/]+/([^\"]+)\"", 1)
     *   → 返回 ["spring-boot", "fastjson", "vue", ...]
     *
     * 面试话术："WebMagic 的 XPath/CSS 能提取元素列表，但有时正则更灵活——
     *   比如需要从属性值中提取一部分，或从文本中按模式提取。"
     *
     * @param input 输入文本
     * @param regex 正则表达式（需包含捕获组）
     * @param group 捕获组编号（1-based）
     * @return 所有匹配的列表，未匹配返回空列表
     */
    public static List<String> extractAll(String input, String regex, int group) {
        if (input == null || regex == null) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>();
        Matcher matcher = Pattern.compile(regex).matcher(input);
        while (matcher.find() && group <= matcher.groupCount()) {
            results.add(matcher.group(group));
        }
        return results;
    }

    /**
     * 清除 HTML 标签，获取纯文本
     *
     * 适用场景：从包含 HTML 的摘要/描述中提取纯文本
     *
     * 示例：
     *   cleanHtml("<p>这是一段<b>重要</b>内容</p>")
     *   → 返回 "这是一段重要内容"
     *
     * @param html 包含 HTML 标签的文本
     * @return 纯文本，null 安全
     */
    public static String cleanHtml(String html) {
        if (html == null) {
            return null;
        }
        // 先移除 <script>/<style> 及其内容，再移除所有 HTML 标签
        String result = html.replaceAll("(?s)<script[^>]*>.*?</script>", "");
        result = result.replaceAll("(?s)<style[^>]*>.*?</style>", "");
        result = result.replaceAll("<[^>]+>", "");
        return result;
    }

    /**
     * 移除多余空白字符（换行、缩进、连续空格、首尾空白）
     *
     * 适用场景：清洗从 HTML 中提取的文本，去除渲染时的空白干扰
     *
     * 示例：
     *   removeExtraWhitespace("  Spring\n    Boot  教程  ")
     *   → 返回 "Spring Boot 教程"
     *
     * @param text 原始文本
     * @return 清理后的文本，null 安全
     */
    public static String removeExtraWhitespace(String text) {
        if (text == null) {
            return null;
        }
        // 把所有空白字符（换行、tab、连续空格）替换为单个空格，再 trim
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * 从字符串中提取第一个整数
     *
     * 适用场景：GitHub Trending 的 star/fork 数字提取
     *   原始文本："52.3k stars" → 提取数字 "52300"（自动处理 k/m 单位）
     *
     * 示例：
     *   parseInt(" 52.3k ")  → 返回 52300
     *   parseInt("1,234")    → 返回 1234
     *   parseInt("abc")      → 返回 null
     *
     * @param text 包含数字的文本
     * @return 解析后的整数，无数字返回 null
     */
    public static Integer parseInt(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        // 先清洗文本
        String cleaned = text.trim().toLowerCase()
                .replaceAll(",", "");  // 去掉千位分隔符

        // 处理 k/m 单位
        double multiplier = 1.0;
        if (cleaned.endsWith("k")) {
            multiplier = 1000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("m")) {
            multiplier = 1000000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        // 提取数字部分
        Matcher matcher = Pattern.compile("[\\d.]+").matcher(cleaned);
        if (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group());
                return (int) Math.round(value * multiplier);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 截断文本到指定长度（超出部分用 "..." 替换）
     *
     * 适用场景：控制摘要长度，避免过长文本入库
     *
     * 示例：
     *   truncate("这是一段很长的文本内容需要截断", 10)
     *   → 返回 "这是一段很长的文..."
     *
     * @param text      原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本，null 安全
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    // ================================================================
    // JS 提取方法 — 【JD 覆盖：能看懂 js 语句和 function】
    // ================================================================

    /**
     * 【面试重点】从 HTML 的 <script> 标签内容中提取 JS 变量值
     *
     * 适用场景（面试时举例）：
     *  - 页面数据藏在 &lt;script&gt;window.__INITIAL_STATE__ = {...}&lt;/script&gt;
     *  - 需要提取其中的 JSON 数据进行解析
     *  - 部分站点的接口 token、签名参数等藏在 JS 变量中
     *
     * 示例：
     *   String html = "&lt;script&gt;window.__DATA__ = {\"list\": [...]};&lt;/script&gt;";
     *   String json = jsVarExtractor(html, "window\\\\.__DATA__\\\\s*=\\\\s*(\\\\{.+?\\\\});");
     *   → 返回 "{\"list\": [...]}"（可直接用 fastjson 解析）
     *
     * 面试话术：
     *   "很多网站把数据藏在 JS 变量里而不是直接渲染到 HTML。
     *    这个方法就是用正则从 &lt;script&gt; 标签中把变量值捞出来。
     *    JD 里要求 '能看懂 js 语句和 function'，这个场景就是体现——
     *    我能分析 JS 代码找到数据所在位置，再用正则提取。"
     *
     * @param html       页面 HTML 源码
     * @param varPattern 变量匹配模式（需包含一个捕获组）
     * @return 变量值字符串，未匹配返回 null
     */
    public static String jsVarExtractor(String html, String varPattern) {
        if (html == null || varPattern == null) {
            return null;
        }
        // 先提取所有 <script> 标签的内容（非 src 引用的内联脚本）
        List<String> scriptContents = extractAll(html,
                "(?is)<script[^>]*>((?:(?!</script>).)*)</script>", 1);

        // 在每个 script 内容中匹配目标变量
        for (String script : scriptContents) {
            String result = extract(script, varPattern, 1);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }
        return null;
    }

    /**
     * 【面试重点】从 JS 代码中按模式提取值
     *
     * 适用场景：
     *  - 提取 API 参数：fetch('/api?token=abc123')
     *  - 提取函数返回值：return { key: 'value' }
     *  - 从混淆的 JS 中提取特定模式
     *
     * 示例：
     *   jsValueExtractor("fetch('/api/list', {headers: {'X-Token': 'abc123'}})",
     *                    "'X-Token'\\\\s*:\\\\s*'([^']+)'")
     *   → 返回 "abc123"
     *
     * @param jsCode  JS 代码片段
     * @param pattern 正则模式（需包含一个捕获组）
     * @return 匹配的值，未匹配返回 null
     */
    public static String jsValueExtractor(String jsCode, String pattern) {
        if (jsCode == null || pattern == null) {
            return null;
        }
        return extract(jsCode, pattern, 1);
    }
}