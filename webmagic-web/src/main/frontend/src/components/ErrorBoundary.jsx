import { Component } from 'react';

/**
 * React 错误边界 — 捕获子组件渲染时的运行时错误
 * 避免一个页面崩溃导致整个 SPA 白屏
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="empty-state" style={{ padding: '80px 20px' }}>
          <div className="empty-icon">💥</div>
          <p style={{ fontSize: 18, marginBottom: 12 }}>
            {this.props.message || '页面加载出错'}
          </p>
          <p style={{ fontSize: 13, color: '#999', marginBottom: 24 }}>
            {this.state.error?.message}
          </p>
          <button
            className="btn btn-primary"
            onClick={() => {
              this.setState({ hasError: false, error: null });
              window.location.reload();
            }}
          >
            🔄 刷新页面
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
