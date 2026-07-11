/**
 * 应用程序主组件
 * 包含路由配置和整体布局
 */
import React, { Suspense, lazy } from 'react'
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom'
import { Button, ConfigProvider, Spin } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import MainLayout from './components/MainLayout'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const RsiAnalysis = lazy(() => import('./pages/RsiAnalysis'))
const MaStrategy = lazy(() => import('./pages/MaStrategy'))
const MomentumStrategy = lazy(() => import('./pages/MomentumStrategy'))
const FundRecommendation = lazy(() => import('./pages/FundRecommendation'))
const FundPortfolio = lazy(() => import('./pages/FundPortfolio'))
const EtfManagement = lazy(() => import('./pages/EtfManagement'))
const SystemConfig = lazy(() => import('./pages/SystemConfig'))
const RsiBacktest = lazy(() => import('./pages/RsiBacktest'))

function NotFound() {
  const navigate = useNavigate()

  return (
    <section className="not-found-page">
      <div className="not-found-code">404</div>
      <h1>页面走丢了</h1>
      <p>没有找到你要访问的页面。</p>
      <Button type="primary" onClick={() => navigate('/')}>回到首页</Button>
    </section>
  )
}

function App() {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#171717',
          colorText: '#171717',
          colorTextSecondary: '#4d4d4d',
          colorBorder: '#ebebeb',
          colorBgLayout: '#fafafa',
          colorBgContainer: '#ffffff',
          borderRadius: 6,
          fontFamily: 'Geist, "PingFang SC", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        },
      }}
    >
      {/* 使用React Router进行路由管理 */}
      <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        {/* 主布局组件，包含顶部导航 */}
        <MainLayout>
          <Suspense fallback={
            <div className="loading-container">
              <Spin size="large" tip="正在准备页面…">
                <div className="loading-spin-content" />
              </Spin>
            </div>
          }>
            {/* 定义路由规则 */}
            <Routes>
              {/* 首页：市场概览仪表板 */}
              <Route path="/" element={<Dashboard />} />

              {/* RSI分析页面 */}
              <Route path="/rsi-analysis" element={<RsiAnalysis />} />

              {/* RSI策略回测页面 */}
              <Route path="/rsi-backtest" element={<RsiBacktest />} />

              {/* 移动平均线策略页面 */}
              <Route path="/ma-strategy" element={<MaStrategy />} />

              {/* 21日动量策略页面 */}
              <Route path="/momentum-strategy" element={<MomentumStrategy />} />

              {/* 基金推荐页面 */}
              <Route path="/fund-recommendation" element={<FundRecommendation />} />

              {/* 基金组合页面 */}
              <Route path="/fund-portfolio" element={<FundPortfolio />} />

              {/* ETF管理页面 */}
              <Route path="/etf-management" element={<EtfManagement />} />

              {/* 系统配置页面 */}
              <Route path="/system-config" element={<SystemConfig />} />

              <Route path="*" element={<NotFound />} />
            </Routes>
          </Suspense>
        </MainLayout>
      </Router>
    </ConfigProvider>
  )
}

export default App
