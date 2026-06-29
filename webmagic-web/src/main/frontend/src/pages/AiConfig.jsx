import { useState, useEffect } from 'react';
import { getAiStatus, testAiConnection } from '../api/client';

export default function AiConfig() {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState(null);

  useEffect(() => {
    getAiStatus()
      .then(res => setStatus(res.data))
      .catch(err => setStatus(null))
      .finally(() => setLoading(false));
  }, []);

  async function handleTest() {
    setTesting(true);
    setTestResult(null);
    try {
      const res = await testAiConnection();
      setTestResult(res);
    } catch (err) {
      setTestResult({ success: false, message: err.message });
    } finally {
      setTesting(false);
    }
  }

  if (loading) return <div className="loading"><div className="spinner" /></div>;

  return (
    <>
      <h2 className="section-title">🤖 AI 配置</h2>

      {status && (
        <div className="detail-container" style={{ marginBottom: 24 }}>
          <div className="detail-meta" style={{ borderBottom: 'none' }}>
            <span className={`badge badge-${status.enabled ? 'oschina' : 'segmentfault'}`}
              style={status.enabled ? {} : { background: '#999' }}>
              {status.enabled ? '已启用' : '已禁用'}
            </span>
            <span>模型：<strong>{status.model}</strong></span>
            <span>API：{status.baseUrl}</span>
            <span>Key：{status.apiKeyMasked}</span>
          </div>
          <div className="detail-meta" style={{ borderBottom: 'none', fontSize: 13, color: '#999' }}>
            <span>提取兜底：{status.extractionFallbackEnabled ? '✅' : '❌'}</span>
            <span>内容增强：{status.enrichmentEnabled ? '✅' : '❌'}</span>
            {status.callStats && (
              <>
                <span>API 调用：{status.callStats.totalCalls} 次</span>
                <span>错误：{status.callStats.errorCount} 次</span>
                <span>熔断：{status.callStats.circuitOpen ? '⚠️ 已熔断' : '正常'}</span>
              </>
            )}
          </div>
          <div style={{ marginTop: 16 }}>
            <button className="btn btn-crawl" onClick={handleTest} disabled={testing}>
              {testing ? '⏳ 测试中...' : '🔌 测试 API 连接'}
            </button>
          </div>
        </div>
      )}

      {testResult && (
        <div className={`error-banner`}
          style={testResult.success ? { background: '#f6ffed', border: '1px solid #b7eb8f', color: '#389e0d' } : {}}>
          <strong>{testResult.success ? '✅ ' : '❌ '}</strong>
          {testResult.message}
          {testResult.latencyMs && <span> · 延迟 {testResult.latencyMs}ms</span>}
          {testResult.sampleOutput && (
            <p style={{ marginTop: 8, fontSize: 13, color: '#666' }}>
              AI 回复：{testResult.sampleOutput}
            </p>
          )}
        </div>
      )}

      {!status && (
        <div className="error-banner">
          ⚠️ 无法获取 AI 配置状态，请确保后端运行中
        </div>
      )}

      <div className="detail-container" style={{ marginTop: 24 }}>
        <h3 style={{ marginBottom: 12, color: '#1a1a2e' }}>如何使用</h3>
        <ol style={{ paddingLeft: 20, lineHeight: 2.2, color: '#666' }}>
          <li>注册 <a href="https://siliconflow.cn" target="_blank" rel="noreferrer">硅基流动</a>，获取免费 API Key</li>
          <li>设置环境变量：<code>export AI_API_KEY=sk-xxxxx</code></li>
          <li>修改 <code>application.yml</code> 中 <code>ai.enabled: true</code></li>
          <li>重启后端，点击上方「测试 API 连接」验证</li>
          <li>爬取数据时，AI 会自动增强摘要和标签</li>
        </ol>
      </div>

      <div className="detail-container" style={{ marginTop: 16 }}>
        <h3 style={{ marginBottom: 12, color: '#1a1a2e' }}>面试展示</h3>
        <ul style={{ paddingLeft: 20, lineHeight: 2.2, color: '#666' }}>
          <li><strong>提取兜底</strong>：当网站改版导致 CSS/XPath 失效时，AI 自动从 HTML 提取结构化数据</li>
          <li><strong>摘要生成</strong>：每篇文章自动生成 2-3 句中文摘要</li>
          <li><strong>标签生成</strong>：自动添加技术标签（Spring Boot、微服务、Kubernetes 等）</li>
          <li><strong>熔断器</strong>：连续 5 次失败自动关闭 AI，60 秒后重试</li>
          <li><strong>幻觉检测</strong>：验证 AI 提取的标题是否在原始 HTML 中存在</li>
        </ul>
      </div>
    </>
  );
}