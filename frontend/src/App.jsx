/**
 * 应用程序主组件
 * 包含路由配置和整体布局
 */
import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import MainLayout from './components/MainLayout'
import Dashboard from './pages/Dashboard'
import RsiAnalysis from './pages/RsiAnalysis'
import MaStrategy from './pages/MaStrategy'
import MomentumStrategy from './pages/MomentumStrategy'
import FundRecommendation from './pages/FundRecommendation'
import FundPortfolio from './pages/FundPortfolio'
import EtfManagement from './pages/EtfManagement'
import SystemConfig from './pages/SystemConfig'
import RsiBacktest from './pages/RsiBacktest'

function App() {
  return (
    // 使用Ant Design的ConfigProvider组件设置中文语言
    <ConfigProvider locale={zhCN}>
      {/* 使用React Router进行路由管理 */}
      <Router>
        {/* 主布局组件，包含导航栏和侧边栏 */}
        <MainLayout>
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
          </Routes>
        </MainLayout>
      </Router>
    </ConfigProvider>
  )
}

export default App

