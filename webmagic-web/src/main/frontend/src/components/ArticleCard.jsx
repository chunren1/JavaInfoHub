import { Link } from 'react-router-dom';

const iconMap = {
  JUEJIN: '📰',
  SEGMENTFAULT: '📝',
  GITHUB: '⭐',
  OSCHINA: '📢',
};

function formatTime(dateStr) {
  if (!dateStr) return null;
  const d = new Date(dateStr);
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function ArticleCard({ article }) {
  const s = (article.source || '').toLowerCase();
  return (
    <li className="article-card">
      <div className="article-icon">{iconMap[article.source] || '📄'}</div>
      <div className="article-body">
        <div className="article-title">
          <Link to={`/articles/${article.id}`}>{article.title}</Link>
        </div>
        <div className="article-meta">
          <span className={`badge badge-${s}`}>{article.source}</span>
          {article.tags && article.tags.includes('[AI]') && (
            <span className="tag" style={{ background: '#fff7e6', color: '#fa8c16', fontWeight: 600 }}>🤖 AI 增强</span>
          )}
          {article.author && <span>👤 {article.author}</span>}
          {article.viewCount > 0 && <span>👁 {article.viewCount}</span>}
          {article.starCount > 0 && <span>❤ {article.starCount}</span>}
          {article.publishTime && <span>{formatTime(article.publishTime)}</span>}
          {article.crawlTime && (
            <span style={{ color: '#bbb' }}>抓取于 {formatTime(article.crawlTime)}</span>
          )}
        </div>
        {article.summary && (
          <p className="article-summary">{article.summary}</p>
        )}
      </div>
    </li>
  );
}