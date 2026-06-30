/**
 * 分页组件 — 显示页码列表 + 首尾导航
 */
export default function Pagination({ pageNum, pages, onPageChange }) {
  if (pages <= 1) return null;

  // 生成页码列表（当前页前后各 2 页 + 首尾）
  const pageNumbers = [];
  const start = Math.max(1, pageNum - 2);
  const end = Math.min(pages, pageNum + 2);

  if (start > 1) {
    pageNumbers.push(1);
    if (start > 2) pageNumbers.push('...');
  }
  for (let i = start; i <= end; i++) {
    pageNumbers.push(i);
  }
  if (end < pages) {
    if (end < pages - 1) pageNumbers.push('...');
    pageNumbers.push(pages);
  }

  return (
    <div className="pagination">
      <button disabled={pageNum <= 1} onClick={() => onPageChange(1)}
        title="首页">«</button>
      <button disabled={pageNum <= 1} onClick={() => onPageChange(pageNum - 1)}
        title="上一页">‹</button>

      {pageNumbers.map((p, i) =>
        p === '...' ? (
          <span key={`dots-${i}`} className="page-dots">…</span>
        ) : (
          <button
            key={p}
            className={`page-btn${p === pageNum ? ' current' : ''}`}
            onClick={() => onPageChange(p)}
            aria-current={p === pageNum ? 'page' : undefined}
          >
            {p}
          </button>
        )
      )}

      <button disabled={pageNum >= pages} onClick={() => onPageChange(pageNum + 1)}
        title="下一页">›</button>
      <button disabled={pageNum >= pages} onClick={() => onPageChange(pages)}
        title="末页">»</button>

      <span className="page-info">{pageNum} / {pages} 页</span>
    </div>
  );
}
