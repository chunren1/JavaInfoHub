import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDashboard, triggerCrawl, getCrawlStatus } from '../api/client';
import { useApi } from '../hooks/useApi';
import { usePolling } from '../hooks/usePolling';
import StatsGrid from '../components/StatsGrid';
import SearchBar from '../components/SearchBar';
import ArticleCard from '../components/ArticleCard';
import { StatsSkeleton } from '../components/LoadingSkeleton';
import { useToast } from '../components/Toast';
import { SOURCE_LABELS } from '../utils/constants';

export default function Home() {
  const navigate = useNavigate();
  const addToast = useToast();
  const [crawling, setCrawling] = useState(null);
  const [statusText, setStatusText] = useState('');
  const [dbError, setDbError] = useState(null);

  // 加载 Dashboard
  const {
    data: dashData,
    loading: dashLoading,
    error: dashError,
    reload: reloadDashboard
  } = useApi(getDashboard, []);

  // 提取数据
  const sourceCounts = {};
  if (dashData?.sourceStats) {
    dashData.sourceStats.forEach(row => { sourceCounts[row.source] = row.cnt; });
  }
  const latestArticles = dashData?.latestArticles || [];

  // DB 错误
  const apiError = dbError || dashError;
  if (dashError?.message) {
    if (!dbError) setDbError(dashError.message);
  }

  // 初始化爬取状态
  usePolling(() => getCrawlStatus(), 0, res => {
    const map = res?.statusMap || {};
    const vals = Object.values(map);
    const latest = vals[vals.length - 1];
    if (latest?.lastCrawlTime) {
      setStatusText(`最后爬取: ${latest.lastCrawlTime}`);
    }
  });

  // 爬取完成后轮询
  usePolling(
    () => (crawling ? getCrawlStatus() : Promise.resolve(null)),
    crawling ? 2000 : 0,
    res => {
      if (!res) return;
      const map = res.statusMap || {};
      const vals = Object.values(map);
      const stillRunning = vals.some(v => v?.running);
      if (!stillRunning) {
        setStatusText(`爬取完成`);
        setCrawling(null);
        reloadDashboard();
        addToast('✅ 爬取完成，数据已更新', 'success');
      }
    }
  );

  const handleSearch = useCallback(({ keyword, source }) => {
    const params = new URLSearchParams();
    if (keyword) params.set('keyword', keyword);
    if (source) params.set('source', source);
    navigate(`/articles?${params.toString()}`);
  }, [navigate]);

  const handleCrawl = useCallback(async (source) => {
    setCrawling(source || 'all');
    setStatusText('正在爬取...');
    const label = source ? SOURCE_LABELS[source] || source : '全部数据源';
    addToast(`🚀 开始爬取 ${label}...`, 'info');

    try {
      const res = await triggerCrawl(source);
      if (res.success) {
        if (res.results) {
          const entries = Object.entries(res.results);
          const ok = entries.filter(([, r]) => r.success).length;
          addToast(`✅ 爬取完成！${ok}/${entries.length} 个数据源成功`, 'success');
        }
        setCrawling(null);
        reloadDashboard();
        setStatusText('爬取完成');
      } else {
        addToast(`❌ 爬取失败: ${res.message}`, 'error');
        setCrawling(null);
        setStatusText('爬取失败');
      }
    } catch (err) {
      addToast(`❌ 网络错误: ${err.message}`, 'error');
      setCrawling(null);
      setStatusText('网络错误');
    }
  }, [addToast, reloadDashboard]);

  return (
    <>
      {apiError && (
        <div className="error-banner">
          ⚠️ 数据库连接错误：{apiError}<br />
          请确保 MySQL 容器已启动：<code>docker compose up -d</code>
        </div>
      )}

      {/* Stats Grid */}
      {dashLoading ? <StatsSkeleton /> : <StatsGrid sourceCounts={sourceCounts} />}

      {/* Search */}
      <SearchBar onSearch={handleSearch} placeholder="搜索文章（如 Spring, MyBatis, 微服务）" />

      {/* Crawl Actions */}
      <div className="action-panel">
        <strong>数据采集</strong>
        <span className="action-sep">|</span>
        <button className="btn btn-crawl" disabled={!!crawling} onClick={() => handleCrawl()}>
          {crawling === 'all' ? '⏳ 爬取中...' : '📥 爬取全部'}
        </button>
        {Object.entries(SOURCE_LABELS).map(([key, label]) => (
          <button key={key} className="btn btn-sm btn-secondary" disabled={!!crawling}
            onClick={() => handleCrawl(key)}>
            {crawling === key ? '⏳' : label}
          </button>
        ))}
        <span className="status-text">
          {crawling ? (
            <><span className="status-dot running" /> 正在爬取...</>
          ) : statusText ? (
            <><span className="status-dot idle" /> {statusText}</>
          ) : null}
        </span>
      </div>

      {/* Latest Articles */}
      <h3 className="section-title">最新数据预览</h3>

      {latestArticles.length > 0 ? (
        <>
          <ul className="article-list">
            {latestArticles.map(a => <ArticleCard key={a.id} article={a} />)}
          </ul>
          <div style={{ textAlign: 'center', marginTop: 20 }}>
            <button className="btn btn-primary" onClick={() => navigate('/articles')}>
              查看全部文章
            </button>
          </div>
        </>
      ) : !dashLoading ? (
        <div className="empty-state">
          <div className="empty-icon">📭</div>
          <p>暂无数据</p>
          <p style={{ fontSize: 13 }}>点击「爬取全部」开始采集数据，或启动预置数据。</p>
        </div>
      ) : null}
    </>
  );
}
