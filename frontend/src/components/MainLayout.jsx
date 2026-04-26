/**
 * 主布局组件
 * 提供应用的整体布局结构，包括头部、侧边栏和内容区域
 */
import React, { useState } from 'react'
import { Layout, Menu } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  DashboardOutlined,
  LineChartOutlined,
  RiseOutlined,
  ThunderboltOutlined,
  FundOutlined,
  WalletOutlined,
  AppstoreOutlined,
  SettingOutlined,
  ExperimentOutlined,
} from '@ant-design/icons'

const { Header, Sider, Content } = Layout

/**
 * 侧边栏菜单配置
 */
const menuItems = [
  {
    key: '/',
    icon: <DashboardOutlined />,
    label: '市场概览',
  },
  {
    key: '/rsi-analysis',
    icon: <LineChartOutlined />,
    label: 'RSI分析',
  },
  {
    key: '/rsi-backtest',
    icon: <ExperimentOutlined />,
    label: 'RSI回测',
  },
  {
    key: '/ma-strategy',
    icon: <RiseOutlined />,
    label: 'MA策略',
  },
  {
    key: '/momentum-strategy',
    icon: <ThunderboltOutlined />,
    label: '动量策略',
  },
  {
    key: '/fund-recommendation',
    icon: <FundOutlined />,
    label: '基金推荐',
  },
  {
    key: '/fund-portfolio',
    icon: <WalletOutlined />,
    label: '基金组合',
  },
  {
    key: '/etf-management',
    icon: <AppstoreOutlined />,
    label: 'ETF管理',
  },
  {
    key: '/system-config',
    icon: <SettingOutlined />,
    label: '系统配置',
  },
]

function MainLayout({ children }) {
  // 控制侧边栏的折叠状态
  const [collapsed, setCollapsed] = useState(false)
  
  // 路由导航钩子
  const navigate = useNavigate()
  
  // 获取当前路由位置
  const location = useLocation()

  /**
   * 处理菜单点击事件
   * @param {Object} item - 被点击的菜单项
   */
  const handleMenuClick = ({ key }) => {
    navigate(key)
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* 左侧边栏 */}
      <Sider 
        collapsible 
        collapsed={collapsed} 
        onCollapse={setCollapsed}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
        }}
      >
        {/* Logo区域 */}
        <div style={{
          height: '64px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontSize: '18px',
          fontWeight: 'bold',
          background: 'rgba(255, 255, 255, 0.1)',
        }}>
          {collapsed ? '基金' : '基金分析系统'}
        </div>
        
        {/* 导航菜单 */}
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>

      {/* 右侧内容区域 */}
      <Layout style={{ marginLeft: collapsed ? 80 : 200, transition: 'all 0.2s' }}>
        {/* 顶部导航栏 */}
        <Header style={{
          padding: '0 24px',
          background: '#fff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 1px 4px rgba(0,21,41,.08)',
        }}>
          <h1 style={{ margin: 0, fontSize: '20px', fontWeight: '500' }}>
            基金和ETF投资策略分析系统
          </h1>
        </Header>

        {/* 主内容区域 */}
        <Content style={{
          margin: '24px',
          padding: '24px',
          minHeight: 280,
        }}>
          {children}
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout

