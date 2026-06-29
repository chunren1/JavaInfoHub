const BASE = '/api';

async function request(url, options = {}) {
  const res = await fetch(BASE + url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || `HTTP ${res.status}`);
  }
  return res.json();
}

export function getDashboard() {
  return request('/dashboard');
}

export function getArticles({ keyword, source, page = 1, size = 20 } = {}) {
  const params = new URLSearchParams();
  if (keyword) params.set('keyword', keyword);
  if (source) params.set('source', source);
  params.set('page', String(page));
  params.set('size', String(size));
  return request(`/articles?${params.toString()}`);
}

export function getArticleDetail(id) {
  return request(`/articles/${id}`);
}

export function triggerCrawl(source) {
  const path = source ? `/crawl/trigger/${source}` : '/crawl/trigger';
  return request(path, { method: 'POST' });
}

export function getCrawlStatus() {
  return request('/crawl/status');
}

export function getAiStatus() {
  return request('/ai/status');
}

export function testAiConnection() {
  return request('/ai/test', { method: 'POST' });
}