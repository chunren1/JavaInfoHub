import { useEffect, useRef } from 'react';

/**
 * 轮询 Hook — 指定间隔重复调用 API
 * @param {Function} apiFn   返回 Promise 的 API 函数
 * @param {number}   interval 轮询间隔(ms)，0 = 仅调用一次
 * @param {Function} onData  数据回调
 */
export function usePolling(apiFn, interval, onData) {
  const savedCallback = useRef(onData);
  savedCallback.current = onData;

  useEffect(() => {
    if (interval <= 0) {
      // 仅调用一次
      apiFn().then(res => savedCallback.current?.(res)).catch(() => {});
      return;
    }

    const timer = setInterval(async () => {
      try {
        const res = await apiFn();
        savedCallback.current?.(res);
      } catch { /* ignore poll errors */ }
    }, interval);

    return () => clearInterval(timer);
  }, [interval]); // eslint-disable-line react-hooks/exhaustive-deps
}
