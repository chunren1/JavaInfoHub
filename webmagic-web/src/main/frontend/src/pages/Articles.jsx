import { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getArticles } from '../api/client';
import { useApi } from '../hooks/useApi';
import SearchBar from '../components/SearchBar';
import ArticleCard from '../components/ArticleCard';
import Pagination from '../components/Pagination';
import { CardSkeleton } from '../components/LoadingSkeleton';

export default function Articles() {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyword = searchParams.get('keyword') || '';
  const source = searchParams.get('source') || '';
  const page = parseInt(searchParams.get('page') || '1', 10);

  const { data: pageData, loading, error } =
    useApi(() => getArticles({ keyword, source, page }), [keyword, source, page]);

  const handleSearch = useCallback(({ keyword: kw, source: src }) => {
    setSearchParams(kw || src ? { ...(kw && { keyword: kw }), ...(src && { source: src }) } : {});
  }, [setSearchParams]);

  const handlePageChange = useCallback((newPage) => {
    const params = {};
    if (keyword) params.keyword = keyword;
    if (source) params.source = source;
    params.page = String(newPage);
    setSearchParams(params);
  }, [keyword, source, setSearchParams]);

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

      {loading && <CardSkeleton count={5} />}

      {error && <div className="error-banner">❌ {error}</div>}

      {!loading && !error && pageData?.list?.length > 0 && (
        <>
          <ul className="article-list">
            {pageData.list.map(a => <ArticleCard key={a.id} article={a} />)}
          </ul>
          <Pagination pageNum={pageData.pageNum} pages={pageData.pages} onPageChange={handlePageChange} />
        </>
      )}

      {!loading && !error && pageData?.list?.length === 0 && (
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
