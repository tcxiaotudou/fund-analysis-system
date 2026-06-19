import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Space, Spin, Tag, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import DecisionSummary from '../components/dashboard/DecisionSummary'
import MarketTemperatureChart from '../components/dashboard/MarketTemperatureChart'
import MetricStrip from '../components/dashboard/MetricStrip'
import OperationQueue from '../components/dashboard/OperationQueue'
import SignalTables from '../components/dashboard/SignalTables'
import { adminApi, dashboardApi, systemConfigApi } from '../services/api'
import {
  EMPTY_DASHBOARD_DECISION,
  getDashboardStatusColor,
  getOperationRoute,
  normalizeDashboardDecision,
} from '../utils/dashboardDecision'

// 决策驾驶舱页面。
function Dashboard() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [dashboard, setDashboard] = useState(EMPTY_DASHBOARD_DECISION)
  const [pageError, setPageError] = useState(null)

  // 加载首页聚合数据。
  const loadDashboard = useCallback(async (showGlobalLoading = true) => {
    try {
      if (showGlobalLoading) {
        setLoading(true)
      }
      setPageError(null)
      const response = await dashboardApi.getDecision()
      if (response.code !== 0) {
        throw new Error(response.message || '首页数据加载失败')
      }
      setDashboard(normalizeDashboardDecision(response.data))
    } catch (error) {
      setPageError(error.normalizedMessage || error.message || '首页数据加载失败')
    } finally {
      if (showGlobalLoading) {
        setLoading(false)
      }
    }
  }, [])

  // 触发后端全量刷新并重新加载首页。
  const handleRefreshAll = useCallback(async () => {
    try {
      setRefreshing(true)
      const response = await adminApi.refreshAll()
      if (response.code !== 0) {
        throw new Error(response.message || '刷新数据失败')
      }
      message.success('数据刷新完成')
      await loadDashboard(false)
    } catch (error) {
      message.error(error.normalizedMessage || error.message || '刷新数据失败')
    } finally {
      setRefreshing(false)
    }
  }, [loadDashboard])

  // 执行操作队列中的路由或API动作。
  const handleRunOperation = useCallback(async (operation) => {
    const route = getOperationRoute(operation)
    if (route) {
      navigate(route)
      return
    }
    if (operation.action === 'sendEmailNow') {
      try {
        const response = await systemConfigApi.sendEmailNow()
        if (response.code !== 0) {
          throw new Error(response.message || '日报发送失败')
        }
        message.success('日报发送成功')
      } catch (error) {
        message.error(error.normalizedMessage || error.message || '日报发送失败')
      }
      return
    }
    message.error('未知操作类型')
  }, [navigate])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  if (loading) {
    return <div className="loading-container"><Spin size="large" /></div>
  }

  if (pageError) {
    return (
      <Alert
        message="首页数据加载失败"
        description={pageError}
        type="error"
        showIcon
        action={<Button onClick={() => loadDashboard()}>重试</Button>}
      />
    )
  }

  return (
    <div className="decision-dashboard">
      <header className="dashboard-topbar">
        <div>
          <h1>基金和ETF投资策略分析系统</h1>
          <Space size={12} wrap>
            <Tag color={getDashboardStatusColor(dashboard.dataStatus.status)}>数据状态：{dashboard.dataStatus.message}</Tag>
            {dashboard.updateTime && <span className="dashboard-update-time">最后更新：{dashboard.updateTime}</span>}
          </Space>
        </div>
        <Button icon={<ReloadOutlined />} onClick={handleRefreshAll} loading={refreshing}>刷新数据</Button>
      </header>

      {dashboard.dataStatus.moduleErrors.length > 0 && (
        <Alert
          type="warning"
          showIcon
          className="dashboard-alert"
          message="部分模块加载失败"
          description={dashboard.dataStatus.moduleErrors.map(error => `${error.module}：${error.message}`).join('；')}
        />
      )}

      <DecisionSummary decisions={dashboard.decisions} />
      <MetricStrip metrics={dashboard.metrics} />
      <div className="dashboard-main-grid">
        <MarketTemperatureChart data={dashboard.trendPoints} />
        <OperationQueue operations={dashboard.operations} onRunOperation={handleRunOperation} />
      </div>
      <SignalTables
        etfOpportunities={dashboard.etfOpportunities}
        maSignals={dashboard.maSignals}
        fundRecommendations={dashboard.fundRecommendations}
      />
    </div>
  )
}

export default Dashboard
