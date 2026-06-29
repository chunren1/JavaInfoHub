-- ================================================================
-- Java Developer Info Hub - Pre-seeded Demo Data
-- Purpose: Import before interview to ensure data for live demo
-- Usage: mysql -u root -p java_info_hub < init-data.sql
-- ================================================================

USE java_info_hub;

-- ================================================================
-- Pre-seeded Tech Articles (~10, covering all 4 sources)
-- ================================================================

INSERT IGNORE INTO tech_article (title, summary, content_url, author, tags, source, source_id, publish_time, view_count, star_count, dedup_key, crawl_time)
VALUES
-- Juejin articles
('Spring Boot 3.x New Features: Full Guide',
 'Comprehensive guide to Spring Boot 3.x key features including GraalVM native image support, Jakarta EE 9 migration, enhanced observability, and ProblemDetail error handling.',
 'https://juejin.cn/post/demo-spring-boot-3',
 'Zhang San',
 'Spring Boot,Java,Backend',
 'JUEJIN', 'demo-spring-boot-3',
 '2026-06-25 10:30:00', 15200, 328,
 MD5(CONCAT('JUEJIN:', 'demo-spring-boot-3')), NOW()),

('MyBatis-Plus in Practice: Stop Writing SQL by Hand',
 'MyBatis-Plus extends MyBatis. This article covers Lambda queries, pagination plugin, logical deletion, auto-fill and other core features.',
 'https://juejin.cn/post/demo-mybatis-plus',
 'Li Si',
 'MyBatis,Java,ORM',
 'JUEJIN', 'demo-mybatis-plus',
 '2026-06-24 14:00:00', 8900, 156,
 MD5(CONCAT('JUEJIN:', 'demo-mybatis-plus')), NOW()),

('WebMagic Web Crawler: Beginner to Practice',
 'A from-scratch guide to WebMagic framework core components: PageProcessor, Pipeline, Scheduler. Includes a complete multi-source data collection case study.',
 'https://juejin.cn/post/demo-webmagic',
 'Wang Wu',
 'WebMagic,Java,Web Crawler',
 'JUEJIN', 'demo-webmagic',
 '2026-06-23 09:15:00', 6700, 98,
 MD5(CONCAT('JUEJIN:', 'demo-webmagic')), NOW()),

-- SegmentFault articles
('Deep Dive into Jsoup: CSS Selector Complete Guide',
 'Jsoup is Java''s most popular HTML parsing library. This article covers all common CSS selector syntaxes and their crawling applications.',
 'https://segmentfault.com/a/demo-jsoup',
 'Zhao Liu',
 'Jsoup,CSS,Web Crawler,Java',
 'SEGMENTFAULT', 'demo-jsoup',
 '2026-06-26 11:00:00', 4300, 45,
 MD5(CONCAT('SEGMENTFAULT:', 'demo-jsoup')), NOW()),

('XPath Complete Guide: Axes, Functions and Best Practices',
 'Comprehensive introduction to XPath core concepts - node selection, position predicates, built-in functions, and axis (ancestor/descendant/following) in practice.',
 'https://segmentfault.com/a/demo-xpath',
 'Qian Qi',
 'XPath,Web Crawler,Java',
 'SEGMENTFAULT', 'demo-xpath',
 '2026-06-22 16:45:00', 3200, 67,
 MD5(CONCAT('SEGMENTFAULT:', 'demo-xpath')), NOW()),

-- GitHub Trending projects
('spring-projects/spring-boot',
 'Spring Boot core repository. Simplifies Spring application setup and development with auto-configuration, starter dependencies, Actuator monitoring, and more.',
 'https://github.com/spring-projects/spring-boot',
 'spring-projects',
 'Java,Spring,Framework,star:72000',
 'GITHUB', 'spring-projects/spring-boot',
 '2026-06-28 08:00:00', 520, 72300,
 MD5(CONCAT('GITHUB:', 'spring-projects/spring-boot')), NOW()),

('iluwatar/java-design-patterns',
 'Java design patterns best practices collection. Demonstrates GoF 23 design patterns in Java with real-world examples - excellent reference for learning design patterns.',
 'https://github.com/iluwatar/java-design-patterns',
 'iluwatar',
 'Java,Design Patterns,Learning,star:86000',
 'GITHUB', 'iluwatar/java-design-patterns',
 '2026-06-28 08:00:00', 410, 86500,
 MD5(CONCAT('GITHUB:', 'iluwatar/java-design-patterns')), NOW()),

-- OsChina news
('Java 24 Officially Released: Virtual Threads Go GA',
 'Oracle releases Java 24 with Virtual Threads graduating from preview to stable, marking a new era for Java concurrent programming. Also includes structured concurrency and scoped values.',
 'https://www.oschina.net/news/demo-java24',
 'OsChina',
 'Java,JDK24,Virtual Threads',
 'OSCHINA', 'demo-java24',
 '2026-06-27 10:00:00', 12000, 230,
 MD5(CONCAT('OSCHINA:', 'demo-java24')), NOW()),

('Spring AI 1.0 Official Release',
 'Spring AI 1.0 officially released, providing unified AI integration for Java developers. Supports OpenAI, Azure OpenAI, Ollama and more, with built-in vector stores, RAG, and tool calling.',
 'https://www.oschina.net/news/demo-spring-ai',
 'OsChina',
 'Spring,AI,Java,LLM',
 'OSCHINA', 'demo-spring-ai',
 '2026-06-26 15:30:00', 18000, 450,
 MD5(CONCAT('OSCHINA:', 'demo-spring-ai')), NOW());