import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getArticles } from '../api/client';
import SearchBar from '../components/SearchBar';
import ArticleCard from '../components/ArticleCard';
import Pagination from '../components/Pagination';

export default function Articles() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);
  const [pageData, setPageData] = useState(null);
  const [error, setError] = useState(null);

  const keyword = searchParams.get('keyword') || '';
  const source = searchParams.get('source') || '';
  const page = parseInt(searchParams.get('page') || '1', 10);

  const loadArticles = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getArticles({ keyword, source, page });
      if (res.success) {
        setPageData(res.data);
      } else {
        setError(res.message);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [keyword, source, page]);

  useEffect(() => { loadArticles(); }, [loadArticles]);

  function handleSearch({ keyword: kw, source: src }) {
    const params = {};
    if (kw) params.keyword = kw;
    if (src) params.source = src;
    setSearchParams(params); // page resets to 1 implicitly
  }

  function handlePageChange(newPage) {
    const params = {};
    if (keyword) params.keyword = keyword;
    if (source) params.source = source;
    params.page = String(newPage);
    setSearchParams(params);
  }

  return (
    <>
      <h2 className="section-title">文章列表</h2>

      <SearchBar onSearch={handleSearch} />

      {pageData && (
        <div className="stats-summary">
          共 <strong>{pageData.total}</strong> 条文章
          {keyword && <>，搜索「<strong>{keyword}</strong>」</>}
          {source && <>，来源：<strong>{source}</strong></>}
        </div>
      )}

      {loading && <div className="loading"><div className="spinner" /></div>}

      {error && <div className="error-banner">{error}</div>}

      {!loading && !error && pageData && pageData.list.length > 0 && (
        <>
          <ul className="article-list">
            {pageData.list.map(a => <ArticleCard key={a.id} article={a} />)}
          </ul>
          <Pagination pageNum={pageData.pageNum} pages={pageData.pages} onPageChange={handlePageChange} />
        </>
      )}

      {!loading && !error && pageData && pageData.list.length === 0 && (
        <div className="empty-state">
          <div className="empty-icon">📭</div>
          <p>{keyword || source ? '未找到匹配的文章' : '暂无文章数据'}</p>
          <p style={{ fontSize: 13, marginTop: 8 }}>
            <a href="/">返回首页</a> 触发爬取获取数据
          </p>
        </div>
      )}
    </>
  );
}