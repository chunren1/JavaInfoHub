import { NavLink, Outlet } from 'react-router-dom';
import Toast from './Toast';
import ErrorBoundary from './ErrorBoundary';

export default function Layout() {
  return (
    <>
      <nav className="nav" role="navigation" aria-label="主导航">
        <NavLink to="/" className="nav-brand" aria-label="首页">
          ☕ Java<span>信息聚合</span>
        </NavLink>
        <div className="nav-links">
          <NavLink to="/" end>首页</NavLink>
          <NavLink to="/articles">文章列表</NavLink>
          <NavLink to="/ai-config">🤖 AI 配置</NavLink>
        </div>
      </nav>
      <main className="main" role="main">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
      <footer className="footer">
        Java 开发者信息聚合平台 · WebMagic + Spring Boot + React · 面试演示项目
      </footer>
      <Toast />
    </>
  );
}
