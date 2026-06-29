# Java 开发者信息聚合平台

> 面试项目 — 4 源技术文章聚合，React 18 前端 + Spring Boot 后端，覆盖 WebMagic / Jsoup / XPath / 正则 / JSON Ajax / HTTP / MyBatis / Spring JD 全部要求

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

- **Juejin** - POST JSON API + cursor pagination
- **SegmentFault** - Pure CSS selectors
- **GitHub Trending** - CSS + Regex (star/fork number extraction)
- **OsChina** - Pure XPath (axes, predicates, functions)

## JD Coverage

All 9 JD requirements covered — see source code comments for interview talking points.

## Project Structure

```
D:/JavaInfoHub/
├── pom.xml                     # Parent POM (Spring Boot 2.7.18)
├── docker-compose.yml          # MySQL 8.0 container
├── schema.sql                  # DB schema
├── init-data.sql               # Pre-seeded demo data
├── webmagic-common/            # Entities, enums, utils (zero framework deps)
├── webmagic-dao/               # MyBatis mappers + XML
├── webmagic-core/              # 4 PageProcessors + DedupPipeline + Scheduler
└── webmagic-web/               # Spring Boot + React SPA
    ├── src/main/java/          # REST API controllers + services
    ├── src/main/resources/     # Built React app served as static files
    └── src/main/frontend/      # React source (Vite + React 18)
        ├── src/pages/          # Home, Articles, ArticleDetail
        ├── src/components/     # Navbar, StatsGrid, etc.
        └── src/api/            # API client
```

## Frontend Tech Stack

- **React 18** with React Router 6 — SPA with client-side routing
- **Vite 5** — fast build tool with HMR dev server
- **Pure CSS** — no framework; custom variables, responsive grid, toast notifications
- **Proxy dev mode** — `npm run dev` on :3000 proxies `/api` to Spring Boot on :8080

## REST API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard` | Dashboard stats + latest articles |
| GET | `/api/articles?keyword&source&page&size` | Paginated article search |
| GET | `/api/articles/{id}` | Article detail |
| POST | `/crawl/trigger` | Crawl all 4 sources (JSON) |
| POST | `/crawl/trigger/{source}` | Crawl single source (JSON) |
| GET | `/crawl/status` | Crawl status polling |

## Interview Demo Flow

1. Open http://localhost:8080 — React dashboard with animated source stat cards
2. Click "爬取全部" — backend spiders start crawling, toast notifications appear in real-time
3. Watch console logs — processors parsing, pipeline writing, SQL executing
4. Dashboard auto-refreshes with new data when crawl completes
5. Search "Spring" — client-side SPA routing, paginated results via REST API
6. Click article → detail page with tags, metadata, original link
7. Open IDE — walk through React components, REST API, and crawl engine code

## Tech Highlights

- **React 18 SPA**: Vite build, React Router 6, custom hooks, toast notifications
- **REST API**: `/api/*` endpoints with standard `{success, data}` JSON response format
- **Dedup**: ConcurrentHashMap (memory) + INSERT IGNORE (DB) dual-layer dedup
- **HTTP**: 11 header types managed, 429/503 exponential backoff retry
- **Regex + JS**: RegexUtils with 8 methods including jsVarExtractor() for script tag data
- **Template config**: YAML/DB rule configuration design (verbal during interview)
- **Graceful degradation**: Single source failure doesn't affect others, pre-seeded data for fallback

---