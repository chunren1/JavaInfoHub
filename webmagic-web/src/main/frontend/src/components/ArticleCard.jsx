import { Link } from 'react-router-dom';
import { SOURCE_ICONS } from '../utils/constants';
import { formatTimeShort } from '../utils/format';

export default function ArticleCard({ article }) {
  const s = (article.source || '').toLowerCase();
  return (
    <li className="article-card">
      <div className="article-icon">{SOURCE_ICONS[article.source] || '📄'}</div>
      <div className="article-body">
        <div className="article-title">
          <Link to={`/articles/${article.id}`}>{article.title}</Link>
        </div>
        <div className="article-meta">
          <span className={`badge badge-${s}`}>{article.source}</span>
          {article.tags?.includes('[AI]') && (
            <span className="tag tag-ai">🤖 AI 增强</span>
          )}
          {article.author && <span>👤 {article.author}</span>}
          {article.viewCount > 0 && <span>👁 {article.viewCount.toLocaleString()}</span>}
          {article.starCount > 0 && <span>❤ {article.starCount.toLocaleString()}</span>}
          {article.publishTime && <span>{formatTimeShort(article.publishTime)}</span>}
          {article.crawlTime && (
            <span className="text-muted">🕐 {formatTimeShort(article.crawlTime)}</span>
          )}
        </div>
        {article.summary && (
          <p className="article-summary">{article.summary}</p>
        )}
      </div>
    </li>
  );
}
