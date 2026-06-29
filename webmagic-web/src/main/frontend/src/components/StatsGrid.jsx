const sources = [
  { key: 'JUEJIN', label: '掘金文章', icon: '📰', className: 'juejin' },
  { key: 'SEGMENTFAULT', label: 'SegmentFault', icon: '📝', className: 'segmentfault' },
  { key: 'GITHUB', label: 'GitHub Trending', icon: '⭐', className: 'github' },
  { key: 'OSCHINA', label: '开源中国', icon: '📢', className: 'oschina' },
];

export default function StatsGrid({ sourceCounts = {} }) {
  return (
    <div className="stats-grid">
      {sources.map(s => (
        <div key={s.key} className={`stat-card ${s.className}`}>
          <div className="stat-icon">{s.icon}</div>
          <div className="stat-num">{sourceCounts[s.key] ?? 0}</div>
          <div className="stat-label">{s.label}</div>
        </div>
      ))}
    </div>
  );
}