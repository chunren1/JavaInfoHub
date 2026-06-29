import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

export default function SearchBar({ onSearch }) {
  const [searchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [source, setSource] = useState(searchParams.get('source') || '');

  function handleSubmit(e) {
    e.preventDefault();
    onSearch({ keyword: keyword.trim(), source });
  }

  function handleReset() {
    setKeyword('');
    setSource('');
    onSearch({ keyword: '', source: '' });
  }

  return (
    <div className="search-bar">
      <form className="search-form" onSubmit={handleSubmit}>
        <input
          className="search-input"
          type="text"
          placeholder="🔍 搜索文章标题、摘要、标签..."
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
        />
        <select className="search-select" value={source} onChange={e => setSource(e.target.value)}>
          <option value="">全部来源</option>
          <option value="JUEJIN">掘金</option>
          <option value="SEGMENTFAULT">SegmentFault</option>
          <option value="GITHUB">GitHub</option>
          <option value="OSCHINA">开源中国</option>
        </select>
        <button type="submit" className="btn btn-primary">搜索</button>
        <button type="button" className="btn btn-secondary" onClick={handleReset}>重置</button>
      </form>
    </div>
  );
}