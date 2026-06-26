import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, Button, Modal, Space, Spin, Tag, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import DecisionSummary from '../components/dashboard/DecisionSummary'
import MarketOverviewWorkbench from '../components/dashboard/MarketOverviewWorkbench'
import MetricStrip from '../components/dashboard/MetricStrip'
import SignalTables from '../components/dashboard/SignalTables'
import { adminApi, dashboardApi, systemConfigApi } from '../services/api'
import {
  EMPTY_DASHBOARD_DECISION,
  getDashboardStatusColor,
  getOperationRoute,
  normalizeDashboardDecision,
} from '../utils/dashboardDecision'

// 后台刷新状态轮询间隔。
const REFRESH_STATUS_POLL_INTERVAL = 3000

// 后台刷新标签颜色映射。
const REFRESH_STATUS_COLORS = {
  running: 'processing',
  success: 'success',
  error: 'error',
  idle: 'default',
}

// 判断后台刷新是否仍在运行。
const isRefreshRunning = (status) => status?.status === 'running'

// 获取后台刷新提示类型。
const getRefreshAlertType = (status) => {
  if (status?.status === 'error') return 'error'
  if (status?.status === 'success') return 'success'
  return 'info'
}

// 获取后台刷新时间说明。
const getRefreshStatusDescription = (status) => {
  if (!status) return ''
  const parts = []
  if (status.startedAt) {
    parts.push(`开始：${status.startedAt}`)
  }
  if (status.finishedAt) {
    parts.push(`结束：${status.finishedAt}`)
  }
  return parts.join('；')
}

// 决策驾驶舱页面。
function Dashboard() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [startingRefresh, setStartingRefresh] = useState(false)
  const [refreshStatus, setRefreshStatus] = useState(null)
  const [dashboard, setDashboard] = useState(EMPTY_DASHBOARD_DECISION)
  const [pageError, setPageError] = useState(null)
  const refreshStatusRef = useRef(null)

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

  // 应用后台刷新状态，刷新结束后重新读取首页数据。
  const applyRefreshStatus = useCallback(async (nextStatus, notifyFinish = false) => {
    const previousStatus = refreshStatusRef.current?.status
    refreshStatusRef.current = nextStatus
    setRefreshStatus(nextStatus)

    if (!notifyFinish || previousStatus !== 'running' || !nextStatus) {
      return
    }
    if (nextStatus.status === 'success') {
      message.success(nextStatus.message || '后台刷新完成')
      await loadDashboard(false)
    }
    if (nextStatus.status === 'error') {
      message.error(nextStatus.message || '后台刷新失败')
    }
  }, [loadDashboard])

  // 加载后台刷新状态。
  const loadBackgroundRefreshStatus = useCallback(async (notifyFinish = false) => {
    try {
      const response = await adminApi.getBackgroundRefreshStatus()
      if (response.code !== 0) {
        throw new Error(response.message || '后台刷新状态获取失败')
      }
      await applyRefreshStatus(response.data, notifyFinish)
    } catch (error) {
      message.error(error.normalizedMessage || error.message || '后台刷新状态获取失败')
    }
  }, [applyRefreshStatus])

  // 启动后台刷新任务。
  const runRefreshAll = useCallback(async () => {
    try {
      setStartingRefresh(true)
      const response = await adminApi.startBackgroundRefresh()
      if (response.code !== 0) {
        throw new Error(response.message || '后台刷新启动失败')
      }
      await applyRefreshStatus(response.data, false)
      message.success(response.data?.message || '后台刷新已启动')
    } catch (error) {
      message.error(error.normalizedMessage || error.message || '后台刷新启动失败')
    } finally {
      setStartingRefresh(false)
    }
  }, [applyRefreshStatus])

  // 确认后触发后台数据刷新。
  const handleRefreshAll = useCallback(() => {
    Modal.confirm({
      title: '确认后台刷新全部数据？',
      content: '系统将刷新市场概览、ETF RSI、MA 策略、基金推荐和组合 RSI。',
      okText: '后台刷新',
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
    loadBackgroundRefreshStatus()
  }, [loadDashboard, loadBackgroundRefreshStatus])

  // 后台刷新运行期间轮询状态。
  useEffect(() => {
    if (!isRefreshRunning(refreshStatus)) {
      return undefined
    }
    const timer = window.setInterval(() => {
      loadBackgroundRefreshStatus(true)
    }, REFRESH_STATUS_POLL_INTERVAL)
    return () => window.clearInterval(timer)
  }, [refreshStatus?.status, loadBackgroundRefreshStatus])

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
        <Button
          icon={<ReloadOutlined />}
          onClick={handleRefreshAll}
          loading={startingRefresh}
          disabled={isRefreshRunning(refreshStatus)}
        >
          {isRefreshRunning(refreshStatus) ? '后台刷新中' : '刷新数据'}
        </Button>
      </header>

      {refreshStatus && refreshStatus.status !== 'idle' && (
        <Alert
          type={getRefreshAlertType(refreshStatus)}
          showIcon
          className="dashboard-alert refresh-status-alert"
          message={
            <Space size={8} wrap>
              <span>{refreshStatus.message}</span>
              <Tag color={REFRESH_STATUS_COLORS[refreshStatus.status] || 'default'}>{refreshStatus.status}</Tag>
            </Space>
          }
          description={getRefreshStatusDescription(refreshStatus)}
        />
      )}

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
      <MarketOverviewWorkbench
        metrics={dashboard.metrics}
        operations={dashboard.operations}
        onRunOperation={handleRunOperation}
      />
      <SignalTables
        etfOpportunities={dashboard.etfOpportunities}
        maSignals={dashboard.maSignals}
        fundRecommendations={dashboard.fundRecommendations}
      />

    </div>
  )
}

export default Dashboard
