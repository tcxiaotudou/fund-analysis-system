import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Modal, Space, Spin, Tag, message } from 'antd'
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

// 驾驶舱全量刷新拆分为可见的模块步骤。
const REFRESH_TASKS = [
  { key: 'market', title: '市场概览', run: () => adminApi.refreshMarket() },
  { key: 'rsi', title: 'ETF RSI', run: () => adminApi.refreshRsi() },
  { key: 'ma', title: 'MA 策略', run: () => adminApi.refreshMa() },
  { key: 'fund', title: '基金推荐', run: () => adminApi.refreshFund() },
  { key: 'portfolio', title: '组合 RSI', run: () => adminApi.refreshPortfolioRsi() },
]

// 构建刷新步骤的初始展示状态。
const createRefreshSteps = () => REFRESH_TASKS.map(task => ({
  key: task.key,
  title: task.title,
  status: 'pending',
  message: '等待刷新',
}))

// 解析刷新接口返回的可读摘要。
const getRefreshSummary = (response) => {
  if (!response?.data || typeof response.data !== 'object') {
    return response?.message || '刷新完成'
  }
  const entries = Object.entries(response.data)
  if (entries.length === 0) {
    return response.message || '刷新完成'
  }
  return entries.map(([key, value]) => `${key}: ${value}`).join('，')
}

// 决策驾驶舱页面。
function Dashboard() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [refreshModalOpen, setRefreshModalOpen] = useState(false)
  const [refreshSteps, setRefreshSteps] = useState(createRefreshSteps)
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

  // 更新单个刷新步骤的状态。
  const updateRefreshStep = useCallback((key, patch) => {
    setRefreshSteps(prevSteps => prevSteps.map(step => (
      step.key === key ? { ...step, ...patch } : step
    )))
  }, [])

  // 逐模块刷新数据并重新加载首页。
  const runRefreshAll = useCallback(async () => {
    let activeTaskKey = null
    try {
      setRefreshing(true)
      setRefreshModalOpen(true)
      setRefreshSteps(createRefreshSteps())

      for (const task of REFRESH_TASKS) {
        activeTaskKey = task.key
        updateRefreshStep(task.key, { status: 'processing', message: '刷新中...' })
        const response = await task.run()
        if (response.code !== 0) {
          throw new Error(response.message || `${task.title}刷新失败`)
        }
        updateRefreshStep(task.key, { status: 'success', message: getRefreshSummary(response) })
      }

      message.success('数据刷新完成')
      await loadDashboard(false)
    } catch (error) {
      if (activeTaskKey) {
        updateRefreshStep(activeTaskKey, {
          status: 'error',
          message: error.normalizedMessage || error.message || '刷新失败',
        })
      }
      message.error(error.normalizedMessage || error.message || '刷新数据失败')
    } finally {
      setRefreshing(false)
    }
  }, [loadDashboard, updateRefreshStep])

  // 确认后触发分项数据刷新。
  const handleRefreshAll = useCallback(() => {
    Modal.confirm({
      title: '确认刷新全部数据？',
      content: '系统将依次刷新市场概览、ETF RSI、MA 策略、基金推荐和组合 RSI，过程可能需要 1-3 分钟。',
      okText: '开始刷新',
      cancelText: '取消',
      onOk: runRefreshAll,
    })
  }, [runRefreshAll])

  // 执行操作队列中的路由或API动作。
  const handleRunOperation = useCallback(async (operation) => {
    const route = getOperationRoute(operation)
    if (route) {
      navigate(route)
      return
    }
    if (operation.action === 'sendEmailNow') {
      Modal.confirm({
        title: '确认发送今日投资分析日报？',
        content: '该操作会立即向系统配置的收件人发送真实邮件。',
        okText: '发送日报',
        cancelText: '取消',
        onOk: async () => {
          try {
            const response = await systemConfigApi.sendEmailNow()
            if (response.code !== 0) {
              throw new Error(response.message || '日报发送失败')
            }
            message.success('日报发送成功')
          } catch (error) {
            message.error(error.normalizedMessage || error.message || '日报发送失败')
          }
        },
      })
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

      <Modal
        title="数据刷新进度"
        open={refreshModalOpen}
        closable={!refreshing}
        maskClosable={!refreshing}
        onCancel={() => setRefreshModalOpen(false)}
        footer={refreshing ? null : (
          <Button type="primary" onClick={() => setRefreshModalOpen(false)}>关闭</Button>
        )}
      >
        <Space direction="vertical" size={10} style={{ width: '100%' }}>
          {refreshSteps.map(step => (
            <div className="refresh-step-row" key={step.key}>
              <Tag color={
                step.status === 'success' ? 'success'
                  : step.status === 'error' ? 'error'
                    : step.status === 'processing' ? 'processing'
                      : 'default'
              }>
                {step.status === 'success' ? '完成'
                  : step.status === 'error' ? '失败'
                    : step.status === 'processing' ? '进行中'
                      : '等待'}
              </Tag>
              <div>
                <strong>{step.title}</strong>
                <div className="refresh-step-message">{step.message}</div>
              </div>
            </div>
          ))}
        </Space>
      </Modal>
    </div>
  )
}

export default Dashboard
