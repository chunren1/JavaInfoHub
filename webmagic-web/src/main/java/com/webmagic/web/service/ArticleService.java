package com.webmagic.web.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.webmagic.common.entity.TechArticle;
import com.webmagic.common.enums.SourceType;
import com.webmagic.dao.mapper.TechArticleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章业务服务
 *
 * 【JD 覆盖：Spring @Service + MyBatis + PageHelper 分页】
 *
 * @author webmagic-demo
 */
@Slf4j
@Service
public class ArticleService {

    @Autowired
    private TechArticleMapper techArticleMapper;

    /**
     * 条件搜索 + 分页
     *
     * 【JD 覆盖：PageHelper 分页插件 + MyBatis 动态 SQL】
     *
     *  面试话术："PageHelper 是 MyBatis 生态最常用的分页插件——
     *   调用 PageHelper.startPage() 之后，紧接着的查询自动被拦截并加上 LIMIT，
     *   完全不侵入业务 SQL。这就是 MyBatis 插件机制的优势。"
     *
     * @param keyword 搜索关键字（可选）
     * @param source  来源筛选（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageInfo<TechArticle> search(String keyword, String source,
                                         int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        // 校验 source（非法值按 null 处理）
        String validSource = null;
        if (source != null && !source.isEmpty()) {
            SourceType type = SourceType.findByName(source);
            if (type != null) {
                validSource = type.name();
            }
        }

        List<TechArticle> articles = techArticleMapper.searchByCondition(keyword, validSource);
        return new PageInfo<>(articles);
    }

    /**
     * 获取文章详情
     */
    public TechArticle getById(Long id) {
        return techArticleMapper.selectById(id);
    }

    /**
     * 获取各来源统计 — 首页 Dashboard
     *
     * 【面试重点】首页不再空白，展示各来源数据量
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 各来源文章数
        List<Map<String, Object>> countBySource = techArticleMapper.countBySource();
        stats.put("sourceStats", countBySource);

        // 2. 总数
        stats.put("totalArticles", techArticleMapper.countAll());

        // 3. 最新 N 条（首页展示用）
        stats.put("latestArticles", techArticleMapper.selectLatest(10));

        return stats;
    }

    /**
     * 获取最新文章
     */
    public List<TechArticle> getLatest(int limit) {
        return techArticleMapper.selectLatest(limit);
    }
}