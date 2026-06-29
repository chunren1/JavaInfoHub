export default function Pagination({ pageNum, pages, onPageChange }) {
  if (pages <= 1) return null;

  return (
    <div className="pagination">
      <button disabled={pageNum <= 1} onClick={() => onPageChange(1)}>首页</button>
      <button disabled={pageNum <= 1} onClick={() => onPageChange(pageNum - 1)}>← 上一页</button>

      <span className="page-btn current">{pageNum}</span>
      <span style={{ padding: '8px 4px', fontSize: 13, color: '#999' }}>
        / {pages} 页
      </span>

      <button disabled={pageNum >= pages} onClick={() => onPageChange(pageNum + 1)}>下一页 →</button>
      <button disabled={pageNum >= pages} onClick={() => onPageChange(pages)}>末页</button>
    </div>
  );
}