import React, { useState } from 'react'
import { Button, Drawer, Dropdown, Grid, Layout } from 'antd'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  AppstoreOutlined,
  CompassOutlined,
  ExperimentOutlined,
  HomeOutlined,
  MenuOutlined,
  PieChartOutlined,
  RiseOutlined,
  SettingOutlined,
  StarOutlined,
} from '@ant-design/icons'
import {
  primaryNavigation,
  utilityNavigation,
} from '../config/navigation'

const { Header, Content, Footer } = Layout
const { useBreakpoint } = Grid

const navigationIcons = {
  '/': <HomeOutlined />,
  '/rsi-analysis': <RiseOutlined />,
  '/momentum-strategy': <ExperimentOutlined />,
  '/fund-recommendation': <StarOutlined />,
  '/fund-portfolio': <PieChartOutlined />,
  '/rsi-backtest': <ExperimentOutlined />,
  '/ma-strategy': <RiseOutlined />,
  '/etf-management': <AppstoreOutlined />,
  '/system-config': <SettingOutlined />,
}

const utilityGroupIcons = {
  策略工具: <ExperimentOutlined />,
  管理工具: <SettingOutlined />,
}

const utilityPaths = utilityNavigation.flatMap(group => group.items.map(item => item.path))

function MainLayout({ children }) {
  const [drawerOpen, setDrawerOpen] = useState(false)
  const screens = useBreakpoint()
  const isMobile = screens.md === false
  const location = useLocation()
  const navigate = useNavigate()
  const hasActiveUtility = utilityPaths.includes(location.pathname)

  const utilityMenuItems = utilityNavigation.map(group => ({
    type: 'group',
    key: group.label,
    label: (
      <span className="app-tools-group-label">
        {utilityGroupIcons[group.label]}
        <span>{group.label}</span>
      </span>
    ),
    children: group.items.map(item => ({
      key: item.path,
      icon: navigationIcons[item.path],
      label: item.label,
    })),
  }))

  return (
    <Layout className="app-layout">
      <Header className="app-header">
        <div className="app-header-inner">
          <NavLink className="app-brand" to="/" aria-label="基金罗盘首页">
            <CompassOutlined />
            <span>基金罗盘</span>
          </NavLink>

          {!isMobile && (
            <nav className="app-primary-navigation" aria-label="主导航">
              {primaryNavigation.map(item => (
                <NavLink
                  key={item.path}
                  className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}
                  end={item.path === '/'}
                  to={item.path}
                >
                  {item.label}
                </NavLink>
              ))}

              <Dropdown
                menu={{
                  items: utilityMenuItems,
                  selectedKeys: [location.pathname],
                  onClick: ({ key }) => navigate(key),
                }}
                placement="bottomRight"
                trigger={['click']}
              >
                <Button
                  className={`app-tools-trigger${hasActiveUtility ? ' active' : ''}`}
                  type="text"
                  icon={<AppstoreOutlined />}
                  aria-current={hasActiveUtility ? 'page' : undefined}
                >
                  更多工具
                </Button>
              </Dropdown>
            </nav>
          )}

          {isMobile && (
            <Button
              className="app-mobile-menu-button"
              type="text"
              icon={<MenuOutlined />}
              aria-label="打开导航"
              aria-expanded={drawerOpen}
              aria-controls="app-navigation-drawer"
              onClick={() => setDrawerOpen(true)}
            />
          )}
        </div>
      </Header>

      <Content className="app-content">
        <div className="app-content-inner">{children}</div>
      </Content>

      <Footer className="app-footer">
        数据来自公开市场信息，仅供研究参考，不构成投资建议。
      </Footer>

      <Drawer
        id="app-navigation-drawer"
        className="app-navigation-drawer"
        title="基金罗盘"
        placement="right"
        width={320}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      >
        <nav className="app-mobile-navigation" aria-label="移动端导航">
          <div className="app-mobile-navigation-section">
            {primaryNavigation.map(item => (
              <NavLink
                key={item.path}
                className={({ isActive }) => `app-mobile-nav-link${isActive ? ' active' : ''}`}
                end={item.path === '/'}
                to={item.path}
                onClick={() => setDrawerOpen(false)}
              >
                {navigationIcons[item.path]}
                <span>{item.label}</span>
              </NavLink>
            ))}
          </div>

          {utilityNavigation.map(group => (
            <section className="app-mobile-navigation-section" key={group.label}>
              <h2 className="app-mobile-navigation-title">
                {utilityGroupIcons[group.label]}
                <span>{group.label}</span>
              </h2>
              {group.items.map(item => (
                <NavLink
                  key={item.path}
                  className={({ isActive }) => `app-mobile-nav-link${isActive ? ' active' : ''}`}
                  to={item.path}
                  onClick={() => setDrawerOpen(false)}
                >
                  {navigationIcons[item.path]}
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </section>
          ))}
        </nav>
      </Drawer>
    </Layout>
  )
}

export default MainLayout
