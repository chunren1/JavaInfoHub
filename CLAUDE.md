# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Start MySQL
docker compose up -d

# Build the React frontend (one-time, or after JS changes)
cd webmagic-web/src/main/frontend && npm install && npm run build && cd ../../../..

# Full build + install to local repo (required before spring-boot:run)
mvn install -DskipTests

# Run the web app
mvn spring-boot:run -pl webmagic-web

# Quick compile check (no install)
mvn clean compile -DskipTests

# Compile single module with dependencies
mvn compile -pl webmagic-core -am

# React dev server (hot reload, proxies /api to :8080)
cd webmagic-web/src/main/frontend && npm run dev

# Access: http://localhost:8080 (or :3000 for React dev server)
```

## Architecture

Multi-module Maven project (Spring Boot 2.7.18, WebMagic 0.9.1, MyBatis 2.3.1):

```
webmagic-common  (zero framework deps — entities, enums, utils)
    ↑
webmagic-dao     (MyBatis mappers + XML, depends on common)
    ↑
webmagic-core    (PageProcessors + Pipelines + Scheduler, depends on dao)
    ↑
webmagic-web     (Spring Boot + React SPA + REST API, depends on core)
```

`webmagic-common` is the only module without framework dependencies. It can be reused in any Java project. Entities are shared across all layers via this module.

## Key Patterns

### PageProcessor → Pipeline chain

Each data source has a PageProcessor that extracts data and puts it into `ResultItems` via `page.putField("articles", list)`. The Pipeline chain is **order-sensitive**:

```
Spider request → PageProcessor.process(page)
  → DedupPipeline    (step 1: ConcurrentHashMap memory dedup)
  → ArticlePersistPipeline (step 2: INSERT IGNORE batch insert)
```

`DedupPipeline` must be added **before** `ArticlePersistPipeline`. If `resultItems.setSkip(true)` is called, downstream pipelines are skipped.

### Dedup key: `MD5(source + ":" + sourceId)`

Not URL-based. Two layers: memory `ConcurrentHashMap` (fast path) + DB `UNIQUE` index on `dedup_key` via `INSERT IGNORE` (safety net).

### Processor creation is manual (not Spring-managed)

Processors are created with `new` in `CrawlTriggerService.createSpider()`. They are NOT `@Component` beans. This means `@Value` injection won't work on them — page limits are passed via `setMaxPages()`.

### HTTP body for POST requests

WebMagic 0.9.1 uses `HttpRequestBody` from `webmagic-extension` for POST JSON:

```java
request.setRequestBody(HttpRequestBody.json(jsonString, "UTF-8"));
```

Not `HttpConstant.Type.JSON` — that class doesn't exist in 0.9.1.

### Frontend: React SPA + REST API

The frontend is a React 18 SPA (`webmagic-web/src/main/frontend/`) built with Vite. It communicates with the backend via `/api/*` REST endpoints (`ApiController.java`).

```bash
cd webmagic-web/src/main/frontend
npm install          # install deps
npm run dev          # dev server on :3000 with /api proxy to :8080
npm run build        # build to ../resources/static/
```

All HTML routes (`/`, `/articles`, `/articles/{id}`) forward to React's `index.html`. React Router handles client-side navigation. The old Thymeleaf templates are still present but no longer wired — the `HomeController` and `ArticleController` now serve `forward:/index.html` exclusively.

REST API (`ApiController`):
- `GET /api/dashboard` — stats + latest articles
- `GET /api/articles?keyword&source&page&size` — paginated search
- `GET /api/articles/{id}` — article detail
- `POST /api/crawl/trigger` — crawl all sources
- `POST /api/crawl/trigger/{source}` — crawl one source
- `GET /api/crawl/status` — crawl status polling

## SQL notes

- `schema.sql` and `init-data.sql` are mounted into Docker MySQL at `/docker-entrypoint-initdb.d/` — they execute automatically on first container start
- To reset: `docker compose down -v && docker compose up -d`
- `init-data.sql` uses `MD5(CONCAT(source, ':', source_id))` in VALUES — this matches the Java `md5()` method
- `characterEncoding=UTF-8` in the JDBC URL, NOT `utf8mb4` — the MySQL driver requires `UTF-8`

## Interview context

This is an interview demo project for a Java web crawler internship position. The JD expects: WebMagic, Jsoup CSS, XPath, regex, JSON/Ajax, HTTP protocol, MySQL/MyBatis, Spring.

Each processor's Javadoc contains interview talking points in Chinese. The 4 processors are designed to demonstrate 4 distinct parsing techniques:
- Juejin: POST JSON API + cursor pagination
- SegmentFault: pure CSS selectors
- GitHub Trending: CSS + regex number extraction (k→integer)
- OsChina: pure XPath (axes, position predicates, functions)

For graceful degradation: pre-seeded data in `init-data.sql` ensures the demo works even if live crawling fails. Each processor has fallback CSS/XPath selectors.