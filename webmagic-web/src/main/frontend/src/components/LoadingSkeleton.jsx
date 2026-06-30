/**
 * 骨架屏加载组件 — 数据加载时展示占位动画，提升感知性能
 */
export function CardSkeleton({ count = 3 }) {
  return (
    <ul className="article-list" style={{ opacity: 0.6 }}>
      {Array.from({ length: count }, (_, i) => (
        <li key={i} className="article-card">
          <div className="skeleton-icon" />
          <div className="article-body" style={{ flex: 1 }}>
            <div className="skeleton-line" style={{ width: '60%', height: 18, marginBottom: 10 }} />
            <div className="skeleton-line" style={{ width: '40%', height: 13, marginBottom: 8 }} />
            <div className="skeleton-line" style={{ width: '90%', height: 13 }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

export function StatsSkeleton() {
  return (
    <div className="stats-grid" style={{ opacity: 0.6 }}>
      {Array.from({ length: 4 }, (_, i) => (
        <div key={i} className="stat-card">
          <div className="skeleton-icon" style={{ margin: '0 auto 8px' }} />
          <div className="skeleton-line" style={{ width: 40, height: 28, margin: '0 auto 8px' }} />
          <div className="skeleton-line" style={{ width: 60, height: 13, margin: '0 auto' }} />
        </div>
      ))}
    </div>
  );
}
