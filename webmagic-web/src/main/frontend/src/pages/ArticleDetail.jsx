import { useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { getArticleDetail } from '../api/client';
import { formatTime } from '../utils/format';
import { CardSkeleton } from '../components/LoadingSkeleton';

export default function ArticleDetail({ id: propId }) {
  const id = propId || window.location.pathname.split('/').pop();
  const navigate = useNavigate();
  const { data: article, loading, error } = useApi(() => getArticleDetail(id), [id]);

  if (loading) return <CardSkeleton count={1} />;

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
        {tags.includes('[AI]') && <span className="tag tag-ai">🤖 AI 增强</span>}
        {article.author && <span>👤 <strong>{article.author}</strong></span>}
        {article.publishTime && <span>📅 {formatTime(article.publishTime)}</span>}
        {article.viewCount > 0 && <span>👁 {article.viewCount.toLocaleString()} 浏览</span>}
        {article.starCount > 0 && <span>❤ {article.starCount.toLocaleString()} 收藏</span>}
        {article.crawlTime && <span>🕐 抓取于 {formatTime(article.crawlTime)}</span>}
      </div>

      {tags.length > 0 && (
        <div className="detail-tags">
          {tags.filter(t => t !== '[AI]').map((tag, i) => (
            <span key={i} className="tag">{tag.trim()}</span>
          ))}
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
