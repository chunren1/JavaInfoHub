import { useState, useEffect, useCallback } from 'react';

/**
 * 通用异步数据加载 Hook
 * @param {Function} apiFn  - 返回 Promise 的 API 函数
 * @param {Array}    deps   - 依赖数组（传入参数）
 * @returns {{ data, loading, error, reload }}
 */
export function useApi(apiFn, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFn();
      if (res.success) {
        setData(res.data);
      } else {
        setError(res.message || '请求失败');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, deps); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { reload(); }, [reload]);

  return { data, loading, error, reload };
}
