package com.webmagic.core.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.webmagic.common.entity.TechArticle;
import com.webmagic.common.enums.SourceType;
import com.webmagic.common.util.SleepUtils;
import com.webmagic.common.util.UserAgentUtils;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 掘金推荐文章爬虫 — 展示 Ajax/JSON 抓取能力
 *
 * 【JD 覆盖：了解 Ajax 请求 + JSON 数据格式 + HTTP Headers + 状态码】
 *
 * ================================================================
 * 【面试重点：抓包分析过程】
 * ================================================================
 *
 * 1. 打开掘金首页 https://juejin.cn → F12 打开 DevTools → Network 面板
 * 2. 筛选 XHR/Fetch 请求 → 找到 recommend_api/v1/article/recommend_all_feed
 * 3. 查看请求详情：
 *    - Method: POST（不是 GET！这是关键发现）
 *    - Request URL: https://api.juejin.cn/recommend_api/v1/article/recommend_all_feed
 *    - Request Headers:
 *        Content-Type: application/json;charset=UTF-8
 *        X-Requested-With: XMLHttpRequest   ← 标记 Ajax 请求
 *        Referer: https://juejin.cn/
 *        Origin: https://juejin.cn
 *    - Request Body:
 *        {
 *          "id_type": 2,
 *          "sort_type": 200,
 *          "cursor": "0",        ← 第一页，后续用返回的 cursor 值
 *          "limit": 20
 *        }
 * 4. 查看响应：JSON 格式
 *    {
 *      "err_no": 0,
 *      "err_msg": "success",
 *      "data": [
 *        {
 *          "article_id": "740836...",
 *          "article_info": {
 *            "article_id": "740836...",
 *            "title": "Spring Boot 3.x 新特性详解",
 *            "brief_content": "本文介绍 Spring Boot 3.x ...",
 *            "ctime": "1715000000",     ← Unix 秒级时间戳
 *            "view_count": 12345,
 *            "digg_count": 567,
 *            "user": {
 *              "user_name": "张三"
 *            },
 *            "tags": [
 *              {"tag_name": "Spring Boot"},
 *              {"tag_name": "Java"}
 *            ]
 *          }
 *        }
 *      ],
 *      "cursor": "{\"v\":\"12345\"}",  ← 下一页的 cursor 值
 *      "has_more": true
 *    }
 *
 * 面试话术：
 *   "很多网站的数据不是直接在 HTML 里，而是通过 Ajax 异步加载的 JSON 数据。
 *    我用 Chrome DevTools 的 Network 面板抓包，分析请求的 URL、Method、
 *    Headers 和 Body 参数，然后在代码中模拟这个请求。
 *    掘金是个典型案例——它是 POST JSON API，分页用的是 cursor 而不是页码。"
 *
 * @author webmagic-demo
 */
@Slf4j
public class JuejinPageProcessor implements PageProcessor {

    /** 掘金推荐 API 地址 */
    private static final String API_URL =
            "https://api.juejin.cn/recommend_api/v1/article/recommend_all_feed";

    /** 爬取起点（根请求 URL） */
    private static final String BASE_URL = "https://juejin.cn";

    /** 最大翻页次数（防止无限循环） */
    private int maxPages = 3;
    private int currentPage = 0;

    /**
     * Site 配置 — 【JD 覆盖：HTTP Headers / Cookie / 状态码】
     *
     * 每个配置项都在面试中可逐条讲解：
     *  - setDomain: 域名（Cookie 管理的基础）
     *  - setUserAgent: UA 伪装（随机轮换，避免单 UA 被识别）
     *  - setRetryTimes: 网络波动重试（应对 5xx 服务端错误）
     *  - setCycleRetryTimes: 循环重试（应对 429/403 反爬）
     *  - setSleepTime: 请求间隔（礼貌爬虫）
     *  - setTimeOut: 超时设置（避免某个请求卡死整个爬虫）
     *  - addHeader: 自定义 Header（Accept / Referer / Origin / X-Requested-With）
     */
    private Site site = Site.me()
            .setDomain("juejin.cn")
            .setUserAgent(UserAgentUtils.getRandom())
            .setRetryTimes(3)               // 【状态码处理】网络错误重试
            .setCycleRetryTimes(3)          // 【反爬处理】被限流后重新加入队列
            .setSleepTime(3000)             // 【礼貌爬虫】3 秒间隔
            .setTimeOut(10000)              // 10 秒超时
            .setCharset("UTF-8")
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            // 【JD-Ajax】X-Requested-With 标记 Ajax 请求
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Referer", "https://juejin.cn/")
            .addHeader("Origin", "https://juejin.cn");

    @Override
    public Site getSite() {
        return site;
    }

    /**
     * 页面处理入口
     *
     * 掘金通过 POST JSON API 获取数据，不走传统的 HTML 页面下载。
     * 因此我们手动构造 POST Request，不需要依赖 WebMagic 的 GET 下载。
     */
    @Override
    public void process(Page page) {
        String url = page.getUrl().get();

        // 如果是首页（根请求），手动发起 POST JSON API 请求
        if (BASE_URL.equals(url)) {
            log.info("========== 掘金爬虫启动 ==========");
            String initCursor = "0";
            currentPage = 0;

            // 构造第一页 POST 请求
            Request postRequest = buildPostRequest(initCursor);
            page.addTargetRequest(postRequest);
            // 跳过当前页（首页只是一个入口，没有可解析的数据）
            page.setSkip(true);
            return;
        }

        // 如果是 API 响应（JSON）
        if (url.startsWith(API_URL)) {
            currentPage++;
            String rawJson = page.getRawText();

            if (rawJson == null || rawJson.isEmpty()) {
                log.warn("掘金 API 返回空响应，结束爬取");
                return;
            }

            // 解析 JSON
            JSONObject response = JSON.parseObject(rawJson);
            int errNo = response.getIntValue("err_no");

            // 【状态码处理】检查 API 业务状态码
            if (errNo != 0) {
                log.error("掘金 API 返回错误：err_no={}, err_msg={}",
                        errNo, response.getString("err_msg"));
                return;
            }

            // 提取文章数据
            JSONArray data = response.getJSONArray("data");
            List<TechArticle> articles = parseArticles(data);

            // 放入 ResultItems → 后续 Pipeline 处理
            page.putField("articles", articles);
            log.info("【掘金】第 {} 页，解析 {} 条", currentPage, articles.size());

            // 检查是否还有下一页
            boolean hasMore = response.getBooleanValue("has_more");
            String nextCursor = response.getString("cursor");

            if (hasMore && nextCursor != null && currentPage < maxPages) {
                // 礼貌延迟
                SleepUtils.randomDelay(2000, 4000);

                // 添加下一页请求
                Request nextRequest = buildPostRequest(nextCursor);
                page.addTargetRequest(nextRequest);
                log.debug("掘金 → 添加下一页 cursor={}", nextCursor);
            } else {
                log.info("掘金爬取完成，共 {} 页", currentPage);
            }
        }
    }

    /**
     * 构造 POST JSON 请求
     *
     * 【JD 覆盖：AJAX POST 请求构建】
     *  关键点：
     *  1. Method: POST（通过 setMethod(HttpConstant.Method.POST)）
     *  2. Request Body: JSON 字符串
     *  3. 包含 cursor 分页参数
     *  4. 携带必要的 Header（Content-Type / X-Requested-With）
     *
     * @param cursor 分页游标（初始值为 "0"）
     * @return POST Request
     */
    private Request buildPostRequest(String cursor) {
        // 构造请求体
        Map<String, Object> body = new HashMap<>();
        body.put("id_type", 2);           // 文章类型
        body.put("sort_type", 200);       // 排序方式（推荐）
        body.put("cursor", cursor);       // 分页游标
        body.put("limit", 20);            // 每页条数

        // 构造 Request
        Request request = new Request(API_URL);
        request.setMethod(HttpConstant.Method.POST);
        request.setRequestBody(HttpRequestBody.json(JSON.toJSONString(body), "UTF-8"));

        return request;
    }

    /**
     * 解析 JSON 数组 → TechArticle 列表
     */
    private List<TechArticle> parseArticles(JSONArray data) {
        List<TechArticle> articles = new ArrayList<>();

        if (data == null || data.isEmpty()) {
            return articles;
        }

        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (item == null) continue;

            JSONObject articleInfo = item.getJSONObject("article_info");
            if (articleInfo == null) continue;

            // 提取标题
            String title = articleInfo.getString("title");
            if (title == null || title.isEmpty()) continue;

            // 提取文章 ID
            String articleId = articleInfo.getString("article_id");

            // 提取摘要
            String summary = articleInfo.getString("brief_content");

            // 提取作者
            String author = null;
            JSONObject user = articleInfo.getJSONObject("author_user_info");
            if (user != null) {
                author = user.getString("user_name");
            }

            // 提取标签
            String tags = null;
            JSONArray tagArray = articleInfo.getJSONArray("tags");
            if (tagArray != null && !tagArray.isEmpty()) {
                StringBuilder tagBuilder = new StringBuilder();
                for (int j = 0; j < tagArray.size(); j++) {
                    JSONObject tag = tagArray.getJSONObject(j);
                    if (tag != null) {
                        String tagName = tag.getString("tag_name");
                        if (tagName != null) {
                            if (tagBuilder.length() > 0) tagBuilder.append(",");
                            tagBuilder.append(tagName);
                        }
                    }
                }
                tags = tagBuilder.toString();
            }

            // 提取时间（Unix 秒级时间戳 → LocalDateTime）
            LocalDateTime publishTime = null;
            Long ctime = articleInfo.getLong("ctime");
            if (ctime != null) {
                publishTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(ctime), ZoneId.of("Asia/Shanghai"));
            }

            // 提取浏览/点赞数
            Integer viewCount = articleInfo.getInteger("view_count");
            Integer starCount = articleInfo.getInteger("digg_count");

            // 构建实体
            String contentUrl = "https://juejin.cn/post/" + articleId;
            String sourceId = articleId;

            TechArticle article = TechArticle.builder()
                    .title(title)
                    .summary(summary)
                    .contentUrl(contentUrl)
                    .author(author)
                    .tags(tags)
                    .source(SourceType.JUEJIN.name())
                    .sourceId(sourceId)
                    .publishTime(publishTime)
                    .viewCount(viewCount != null ? viewCount : 0)
                    .starCount(starCount != null ? starCount : 0)
                    .dedupKey(md5(SourceType.JUEJIN.name() + ":" + sourceId))
                    .crawlTime(LocalDateTime.now())
                    .build();

            articles.add(article);
        }

        return articles;
    }

    /**
     * MD5 计算（去重键生成）
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("MD5 计算失败", e);
            return input;
        }
    }

    /**
     * 设置最大翻页数（从 application.yml 注入）
     */
    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }
}