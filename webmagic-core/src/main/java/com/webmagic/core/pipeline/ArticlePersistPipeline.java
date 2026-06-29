package com.webmagic.core.pipeline;

import com.webmagic.common.entity.TechArticle;
import com.webmagic.dao.mapper.TechArticleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章持久化 Pipeline — 批量 INSERT IGNORE 入库
 *
 * 【JD 覆盖：MyBatis 批量插入】
 *
 *  工作流程：
 *  1. 收集每一页的 TechArticle 实体
 *  2. 每页爬完后批量写入（批量大小可配置）
 *  3. INSERT IGNORE 兜底去重（【第二层】数据库 UNIQUE 索引保护）
 *
 * 【面试话术】
 *  "Pipeline 是 WebMagic 的数据出口。我设计了 ArticlePersistPipeline
 *   负责把 Processor 解析出的实体批量写入数据库。
 *   用的是 INSERT IGNORE——如果 dedup_key 重复就自动跳过，
 *   和 DedupPipeline 形成双层去重保护。"
 *
 * @author webmagic-demo
 */
@Slf4j
@Component
public class ArticlePersistPipeline implements Pipeline {

    @Autowired
    private TechArticleMapper techArticleMapper;

    @Override
    public void process(ResultItems resultItems, Task task) {
        // 从 ResultItems 中提取文章列表
        Object articlesObj = resultItems.get("articles");
        if (articlesObj == null) {
            return;
        }

        List<TechArticle> articles;
        if (articlesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<TechArticle> list = (List<TechArticle>) articlesObj;
            articles = list;
        } else if (articlesObj instanceof TechArticle) {
            // 单个实体的场景
            articles = new ArrayList<>();
            articles.add((TechArticle) articlesObj);
        } else {
            log.warn("未知的 articles 类型：{}", articlesObj.getClass());
            return;
        }

        if (articles.isEmpty()) {
            return;
        }

        // 批量 INSERT IGNORE（去重兜底）
        int inserted = techArticleMapper.batchInsertIgnore(articles);
        log.info("【文章入库】批量 {} 条 → 实际新增 {} 条（{} 条重复跳过）",
                articles.size(), inserted, articles.size() - inserted);
    }
}