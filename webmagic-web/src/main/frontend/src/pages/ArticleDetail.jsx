import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getArticleDetail } from '../api/client';

function formatTime(dateStr) {
  if (!dateStr) return null;
  const d = new Date(dateStr);
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export default function ArticleDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getArticleDetail(id)
      .then(res => {
        if (res.success) {
          setArticle(res.data);
        } else {
          setError(res.message);
        }
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="loading"><div className="spinner" /></div>;

  if (error || !article) {
    return (
      <div className="empty-state">
        <div className="empty-icon">🔍</div>
        <p>{error || '文章不存在'}</p>
        <button className="btn btn-secondary" onClick={() => navigate('/articles')}>返回列表</button>
      </div>
    );
  }

  const s = (article.source || '').toLowerCase();
  const tags = article.tags ? article.tags.split(',').filter(Boolean) : [];

  return (
    <div className="detail-container">
      <h1>{article.title}</h1>

      <div className="detail-meta">
        <span className={`badge badge-${s}`}>{article.source}</span>
        {article.author && <span>👤 作者：<strong>{article.author}</strong></span>}
        {article.publishTime && <span>📅 发布于 {formatTime(article.publishTime)}</span>}
        {article.viewCount > 0 && <span>👁 {article.viewCount} 浏览</span>}
        {article.starCount > 0 && <span>❤ {article.starCount} 收藏</span>}
        {article.crawlTime && <span>🕐 抓取于 {formatTime(article.crawlTime)}</span>}
      </div>

      {tags.length > 0 && (
        <div className="detail-tags">
          {tags.map((tag, i) => <span key={i} className="tag">{tag.trim()}</span>)}
        </div>
      )}

      {article.summary && (
        <div className="detail-summary">
          <h3>📋 摘要</h3>
          <p>{article.summary}</p>
        </div>
      )}

      <div className="detail-actions">
        <button className="btn btn-back" onClick={() => navigate(-1)}>← 返回列表</button>
        {article.contentUrl && (
          <a href={article.contentUrl} target="_blank" rel="noopener noreferrer" className="btn btn-original">
            🔗 查看原文
          </a>
        )}
      </div>
    </div>
  );
}