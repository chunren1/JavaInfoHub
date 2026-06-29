package com.webmagic.dao.mapper;

import com.webmagic.common.entity.TechArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 技术文章 Mapper 接口
 *
 * 【JD 覆盖：MyBatis】
 *  展示 MyBatis 的核心用法：
 *  - 批量 INSERT IGNORE（去重兜底）
 *  - 动态 SQL（if/where/foreach）
 *  - 全文搜索
 *  - 分组统计（首页 Dashboard）
 *
 * @author webmagic-demo
 */
@Mapper
public interface TechArticleMapper {

    /**
     * 批量插入（IGNORE 去重兜底）
     *
     * INSERT IGNORE 会自动跳过 dedup_key 冲突的记录
     * 即使 DedupPipeline 漏了，数据库 UNIQUE 约束也能保证不重复
     *
     * @param articles 文章列表
     * @return 实际插入行数
     */
    int batchInsertIgnore(@Param("list") List<TechArticle> articles);

    /**
     * 根据来源统计文章数量 — 首页 Dashboard 使用
     *
     * 返回示例：[{source: "JUEJIN", cnt: 245}, {source: "GITHUB", cnt: 89}]
     *
     * @return 来源 → 数量 映射列表
     */
    List<Map<String, Object>> countBySource();

    /**
     * 条件搜索文章（关键字 + 来源筛选）— 支持分页
     *
     * 动态 SQL：关键字搜索 title/summary/tags，来源精确匹配
     *
     * @param keyword 搜索关键字（可选）
     * @param source  来源筛选（可选）
     * @return 匹配的文章列表
     */
    List<TechArticle> searchByCondition(@Param("keyword") String keyword,
                                        @Param("source") String source);

    /**
     * 根据 ID 查询文章详情
     *
     * @param id 文章 ID
     * @return 文章对象
     */
    TechArticle selectById(@Param("id") Long id);

    /**
     * 根据去重键查询（DedupPipeline DB 查重使用）
     *
     * @param dedupKey MD5 去重键
     * @return 是否存在
     */
    int countByDedupKey(@Param("dedupKey") String dedupKey);

    /**
     * 获取最新 N 条文章 — 首页展示
     *
     * @param limit 条数
     * @return 最新文章列表
     */
    List<TechArticle> selectLatest(@Param("limit") int limit);

    /**
     * 获取文章总数
     *
     * @return 总数
     */
    int countAll();
}