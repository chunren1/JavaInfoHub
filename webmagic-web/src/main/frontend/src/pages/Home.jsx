import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDashboard, triggerCrawl, getCrawlStatus } from '../api/client';
import StatsGrid from '../components/StatsGrid';
import ArticleCard from '../components/ArticleCard';
import { useToast } from '../components/Toast';

const sourceNames = {
  juejin: '掘金',
  segmentfault: 'SegmentFault',
  github: 'GitHub Trending',
  oschina: '开源中国',
};

export default function Home() {
  const navigate = useNavigate();
  const addToast = useToast();
  const [dbError, setDbError] = useState(null);
  const [sourceCounts, setSourceCounts] = useState({});
  const [latestArticles, setLatestArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [crawling, setCrawling] = useState(null);
  const [statusText, setStatusText] = useState('');

  // Load dashboard data
  const loadDashboard = useCallback(async () => {
    try {
      const res = await getDashboard();
      if (!res.success) {
        setDbError(res.message);
        return;
      }
      setDbError(null);
      const { sourceStats, latestArticles: latest } = res.data;
      const counts = {};
      if (sourceStats) {
        sourceStats.forEach(row => { counts[row.source] = row.cnt; });
      }
      setSourceCounts(counts);
      setLatestArticles(latest || []);
    } catch (err) {
      setDbError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadDashboard(); }, [loadDashboard]);

  // Poll crawl status
  useEffect(() => {
    if (!crawling) return;
    const timer = setInterval(async () => {
      try {
        const res = await getCrawlStatus();
        const map = res.statusMap || {};
        const vals = Object.values(map);
        const latest = vals[vals.length - 1];
        if (!latest?.running && latest?.lastCrawlTime) {
          setStatusText(`最后爬取: ${latest.lastCrawlTime}`);
          setCrawling(null);
          clearInterval(timer);
          loadDashboard();
          addToast('爬取完成，数据已更新', 'success');
        }
      } catch { /* ignore */ }
    }, 2000);
    return () => clearInterval(timer);
  }, [crawling, loadDashboard, addToast]);

  // Initial status
  useEffect(() => {
    getCrawlStatus().then(res => {
      const map = res.statusMap || {};
      const vals = Object.values(map);
      const latest = vals[vals.length - 1];
      if (latest?.lastCrawlTime) {
        setStatusText(`最后爬取: ${latest.lastCrawlTime}`);
      }
    }).catch(() => {});
  }, []);

  async function handleCrawl(source) {
    setCrawling(source || 'all');
    setStatusText('正在爬取...');
    const label = source ? sourceNames[source] || source : '全部数据源';
    addToast(`开始爬取 ${label}...`, 'info');

    try {
      const res = await triggerCrawl(source);
      if (res.success) {
        if (res.results) {
          const entries = Object.entries(res.results);
          const ok = entries.filter(([, r]) => r.success).length;
          addToast(`爬取完成！${ok}/${entries.length} 个数据源成功`, 'success');
        } else if (res.result) {
          addToast(res.result.success ? `${label} 爬取成功` : `${label} 爬取失败: ${res.result.message}`, res.result.success ? 'success' : 'error');
        }
        setCrawling(null);
        loadDashboard();
        setStatusText(`爬取完成`);
      } else {
        addToast(`爬取失败: ${res.message}`, 'error');
        setCrawling(null);
        setStatusText('爬取失败');
      }
    } catch (err) {
      addToast(`网络错误: ${err.message}`, 'error');
      setCrawling(null);
      setStatusText('网络错误');
    }
  }

  function handleSearch({ keyword, source }) {
    const params = new URLSearchParams();
    if (keyword) params.set('keyword', keyword);
    if (source) params.set('source', source);
    navigate(`/articles?${params.toString()}`);
  }

  return (
    <>
      {dbError && (
        <div className="error-banner">
          ⚠️ 数据库连接错误：{dbError}<br />
          请确保 MySQL 容器已启动：<code>docker compose up -d</code>
        </div>
      )}

      {/* Stats Grid */}
      {loading ? (
        <div className="loading"><div className="spinner" /></div>
      ) : (
        <StatsGrid sourceCounts={sourceCounts} />
      )}

      {/* Search */}
      <div className="search-bar">
        <form className="search-form" onSubmit={e => {
          e.preventDefault();
          const fd = new FormData(e.target);
          handleSearch({ keyword: fd.get('keyword'), source: fd.get('source') });
        }}>
          <input className="search-input" type="text" name="keyword" placeholder="搜索文章（如 Spring, MyBatis, 微服务）" />
          <select className="search-select" name="source">
            <option value="">全部来源</option>
            <option value="JUEJIN">掘金</option>
            <option value="SEGMENTFAULT">SegmentFault</option>
            <option value="GITHUB">GitHub</option>
            <option value="OSCHINA">开源中国</option>
          </select>
          <button type="submit" className="btn btn-primary">搜索</button>
        </form>
      </div>

      {/* Crawl Actions */}
      <div className="action-panel">
        <strong>数据采集</strong>
        <span className="action-sep">|</span>
        <button className="btn btn-crawl" disabled={!!crawling} onClick={() => handleCrawl()}>
          {crawling === 'all' ? '⏳ 爬取中...' : '📥 爬取全部'}
        </button>
        {Object.entries(sourceNames).map(([key, label]) => (
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
      ) : (
        <div className="empty-state">
          <div className="empty-icon">📭</div>
          <p>暂无数据</p>
          <p style={{ fontSize: 13 }}>
            点击「爬取全部」开始采集数据，或启动预置数据。
          </p>
        </div>
      )}
    </>
  );
}