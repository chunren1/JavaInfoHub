# Java 开发者信息聚合平台

> 面试项目 — 4 源技术文章聚合 + AI 智能增强，React 18 前端 + Spring Boot 后端，覆盖 WebMagic / Jsoup / XPath / 正则 / Ajax / JSON / HTTP / MyBatis / Spring / AI 集成全部要求

## Quick Start

### 第一次使用（初始化）

```bash
npm run setup
```

> 自动完成：安装前端依赖 → 启动 MySQL → 编译后端

### 生产模式（一键启动，访问 :8080）

```bash
npm start
```

> 自动完成：启动 MySQL → 构建前端 → 编译后端 → 启动 Spring Boot  
> 浏览器访问 **http://localhost:8080**

### 开发模式（前后端分离 + 热更新）

```bash
# 终端 1：启动后端（含 MySQL）
npm run dev

# 终端 2：启动 React 开发服务器（热更新 :3000）
npm run frontend:dev
```

> 浏览器访问 **http://localhost:3000**（自动代理 /api 到 :8080）

### 启用 AI 增强（可选，面试加分项）

```bash
# 1. 注册 https://siliconflow.cn → 获取免费 API Key
# 2. 设置环境变量
export AI_API_KEY=sk-xxxxx

# 3. 修改 application.yml
#    ai.enabled: true

# 4. 重启后端，前端访问 /ai 测试连通性
```

---

### 全部可用脚本

```bash
npm run setup          # 首次初始化：安装依赖 + 启动 MySQL + 编译后端
npm start              # 生产模式一键启动
npm run dev            # 开发模式：启动 MySQL + 后端
npm run frontend:dev   # 单独启动前端热更新服务器 (:3000)
npm run frontend:build # 单独构建前端到 static/
npm run backend:build  # 单独编译后端 (mvn install)
npm run backend:run    # 单独启动后端 Spring Boot
npm run db:up          # 单独启动 MySQL 容器
npm run db:down        # 停止 MySQL 容器
npm run db:logs        # 查看 MySQL 日志
npm stop               # 停止所有容器
```

## 4 Data Sources

| Source | Technique | Key Skill |
|--------|-----------|-----------|
| **Juejin** | POST JSON API + cursor pagination | Ajax / JSON / HTTP Headers |
| **SegmentFault** | Pure CSS selectors | Jsoup CSS 选择器 |
| **GitHub Trending** | CSS + Regex (star/fork number extraction) | 正则表达式 |
| **OsChina** | Pure XPath (axes, predicates, functions) | XPath |

## AI 智能增强系统（🆕 面试亮点）

### 两大功能模块

| 功能 | 说明 | 面试价值 |
|------|------|----------|
| **提取兜底** | 当 CSS/XPath 因网站改版失效时，AI 自动从 HTML 提取结构化数据 | 展示容错设计 + 新技术的实际应用 |
| **内容增强** | 自动生成中文摘要、补充技术标签 | 展示 Pipeline 扩展性 + AI 工程化思维 |

### 技术架构

```
Processor → DedupPipeline → AIEnrichmentPipeline → ArticlePersistPipeline
               (去重)         (AI 摘要+标签)          (入库)
```

- **零侵入**：`ai.enabled: false` 时 AI Pipeline 直接透传，零 API 调用
- **熔断器**：连续 5 次失败自动关闭 AI，60 秒后重试
- **幻觉检测**：验证 AI 提取的标题是否在原始 HTML 中存在
- **速率控制**：单次爬取最多 100 次调用，500ms 间隔
- **免费模型**：硅基流动 Qwen2.5-7B-Instruct（国内直连，无需翻墙）

## JD Coverage

All 9 JD requirements covered + **AI integration bonus** — see source code comments for interview talking points.

## Project Structure

```
D:/JavaInfoHub/
├── pom.xml                     # Parent POM (Spring Boot 2.7.18)
├── package.json                # npm 一键启动脚本
├── docker-compose.yml          # MySQL 8.0 container
├── schema.sql                  # DB schema (tech_article + crawl_log)
├── init-data.sql               # Pre-seeded demo data (10 articles)
├── webmagic-common/            # Entities, enums, utils, AI DTOs (zero framework deps)
│   ├── entity/                 #   TechArticle.java
│   ├── enums/                  #   SourceType.java (4 sources with displayName/baseUrl)
│   ├── dto/                    #   AiChatRequest/Response, ExtractionResult (AI 数据结构)
│   ├── service/                #   AiService.java (AI API 调用接口)
│   └── util/                   #   RegexUtils(8 methods), SleepUtils, HttpHeaderUtils, UserAgentUtils
├── webmagic-dao/               # MyBatis mappers + XML
│   └── mapper/                 #   TechArticleMapper.java + TechArticleMapper.xml
├── webmagic-core/              # Crawl engine (processors + pipelines + scheduler)
│   ├── processor/              #   4 PageProcessors (Juejin/SegmentFault/GitHub/OsChina)
│   ├── pipeline/               #   DedupPipeline + ArticlePersistPipeline + AiEnrichmentPipeline
│   ├── scheduler/              #   CrawlTriggerService (Spider lifecycle management)
│   └── config/                 #   AiProperties.java (AI 配置模型)
└── webmagic-web/               # Spring Boot + React SPA
    ├── controller/             #   ApiController + CrawlController + AiConfigController
    ├── service/                #   ArticleService (search + dashboard stats)
    ├── resources/              #   application.yml + static/ (built React app)
    └── frontend/               #   React source (Vite + React 18)
        ├── pages/              #     Home, Articles, ArticleDetail, AiConfig
        ├── components/         #     StatsGrid, SearchBar, ArticleCard, Pagination, Toast, Layout
        └── api/                #     client.js (fetch-based API client)
```

## Frontend Tech Stack

- **React 18** with React Router 6 — SPA with client-side routing
- **Vite 5** — fast build tool with HMR dev server
- **Pure CSS** — no framework; custom variables, responsive grid, toast notifications
- **Proxy dev mode** — `npm run dev` on :3000 proxies `/api` to Spring Boot on :8080
- **5 pages**: Home (dashboard) / Articles (search+pagination) / ArticleDetail / Crawl / AiConfig

## REST API Endpoints

### Article APIs

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard` | Dashboard stats + latest articles |
| GET | `/api/articles?keyword&source&page&size` | Paginated article search |
| GET | `/api/articles/{id}` | Article detail |

### Crawl APIs

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/crawl/trigger` | Crawl all 4 sources |
| POST | `/api/crawl/trigger/{source}` | Crawl single source |
| GET | `/api/crawl/status` | Crawl status polling |

### AI APIs（🆕）

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/ai/status` | AI config + call statistics |
| POST | `/api/ai/test` | Test AI API connectivity |

## Interview Demo Flow

1. Open http://localhost:8080 — React dashboard with 4 animated source stat cards
2. Click "爬取全部" — backend spiders start crawling, toast notifications appear in real-time
3. Watch console logs — processors parsing, pipelines executing (Dedup → AI Enrichment → DB Insert)
4. Search "Spring" — client-side SPA routing, paginated results via REST API
5. Click article → detail page with tags, metadata, original link
6. Visit AiConfig page `http://localhost:3000/ai` → AI status panel + connectivity test
7. Show AI-enhanced articles: `🤖` badge on articles enriched by AI
8. Open IDE — walk through React components, REST API, crawl engine, and AI pipeline code

## Tech Highlights

- **React 18 SPA**: Vite build, React Router 6, custom hooks, toast notifications, 5 pages
- **REST API**: `/api/*` endpoints with standard `{success, data}` JSON response format
- **Dedup**: ConcurrentHashMap (memory) + INSERT IGNORE (DB) dual-layer dedup
- **HTTP**: 11 header types managed, 429/503 exponential backoff retry
- **Regex + JS**: RegexUtils with 8 methods including jsVarExtractor() for script tag data
- **AI Integration** 🆕: AI extraction fallback + content enrichment (summary/tags), circuit breaker, hallucination detection
- **Pipeline Pattern**: Chain of Responsibility — Dedup → AI Enrichment → Persist, zero-coupling extensibility
- **Graceful degradation**: Single source failure doesn't affect others, manual selectors + AI fallback dual-mode, pre-seeded data for offline demo

---