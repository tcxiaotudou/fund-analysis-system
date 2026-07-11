/**
 * 21日动量策略页面
 * 展示基于21日动量的ETF轮动策略交易记录
 */
import React, { useState, useEffect, useMemo } from 'react'
import { Alert, Card, Table, Tag, Button, Space, DatePicker, InputNumber, message, Modal, Slider, Segmented } from 'antd'
import { ReloadOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { useSearchParams } from 'react-router-dom'
import TerminalPage from '../components/TerminalPage'
import { momentumStrategyApi } from '../services/api'
import {
  MOMENTUM_VISIBLE_RANGE_PRESET_OPTIONS,
  createMomentumBacktestSearchParams,
  getMomentumBacktestRangeFromSearchParams,
  getMomentumVisibleRangePreference,
  saveMomentumVisibleRangePresetToStorage,
  saveMomentumVisibleRangeToStorage,
  upsertMomentumVisibleRangeSearchParams,
} from '../utils/momentumBacktestRange'
import {
  calculateMomentumTransactionSummary,
  calculateMomentumPerformanceSummary,
  buildMomentumChartData,
  filterMomentumTransactionsByVisibleDateRange,
  getMomentumDateRangePercentByDateRange,
  getMomentumPerformanceDateByPercent,
  getMomentumVisibleDateRange,
  getMomentumVisibleDateRangeByPreset,
  shouldRenderMomentumMarker,
} from '../utils/momentumChartData'
import {
  getDateRangeError,
  getPositiveNumberError,
} from '../utils/backtestValidation'
import {
  formatCurrency,
  formatPercent,
} from '../utils/formatters'
import dayjs from 'dayjs'
import {
  ComposedChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Scatter
} from 'recharts'

const { RangePicker } = DatePicker

function MomentumStrategy() {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialBacktestState = useMemo(
    () => getMomentumBacktestRangeFromSearchParams(searchParams),
    [searchParams],
  )
  const initialVisibleRangeState = useMemo(
    () => getMomentumVisibleRangePreference(searchParams, window.localStorage),
    [searchParams],
  )
  const initialBacktestRange = initialBacktestState.range
  const [urlRangeError, setUrlRangeError] = useState(initialBacktestState.error)
  const [visibleRangeError, setVisibleRangeError] = useState(initialVisibleRangeState.error)
  const [loading, setLoading] = useState(true)
  const [data, setData] = useState([])
  const [transactionsError, setTransactionsError] = useState(null)
  const [performanceData, setPerformanceData] = useState([])
  const [performanceLoading, setPerformanceLoading] = useState(true)
  const [performanceError, setPerformanceError] = useState(null)
  const [backtestLoading, setBacktestLoading] = useState(false)
  const [backtestModalVisible, setBacktestModalVisible] = useState(false)
  const [backtestStartDate, setBacktestStartDate] = useState(() => (
    initialBacktestRange ? dayjs(initialBacktestRange.startDate) : null
  ))
  const [backtestEndDate, setBacktestEndDate] = useState(() => (
    initialBacktestRange ? dayjs(initialBacktestRange.endDate) : null
  ))
  const [initialCapital, setInitialCapital] = useState(100000)
  const [activeBacktestRange, setActiveBacktestRange] = useState(initialBacktestRange)
  const [requestedVisibleDateRange, setRequestedVisibleDateRange] = useState(initialVisibleRangeState.range)
  const [selectedVisibleRangePreset, setSelectedVisibleRangePreset] = useState(initialVisibleRangeState.preset)
  // 时间范围选择器状态（0-100的百分比）
  const [dateRange, setDateRange] = useState([0, 100])

  /**
   * 加载交易记录数据
   */
  const loadData = async (range = activeBacktestRange) => {
    try {
      setLoading(true)
      setTransactionsError(null)
      const response = range
        ? await momentumStrategyApi.getTransactionsByRange(range.startDate, range.endDate)
        : await momentumStrategyApi.getTransactions()
      
      if (response.code === 0) {
        setData(response.data || [])
        return true
      } else {
        console.error('轮动模拟记录服务返回失败:', response.code, response.message)
        const errorMessage = `加载模拟记录失败（服务返回 ${response.code}）`
        setTransactionsError(errorMessage)
        message.error(errorMessage)
        return false
      }
    } catch (error) {
      console.error('加载交易记录失败:', error)
      const errorMessage = '加载模拟记录失败，请检查网络后重试'
      setTransactionsError(errorMessage)
      message.error(errorMessage)
      return false
    } finally {
      setLoading(false)
    }
  }

  /**
   * 加载收益曲线数据
   */
  const loadPerformanceData = async (range = activeBacktestRange) => {
    try {
      setPerformanceLoading(true)
      setPerformanceError(null)
      const response = range
        ? await momentumStrategyApi.getPerformanceByRange(range.startDate, range.endDate)
        : await momentumStrategyApi.getPerformance()
      
      if (response.code === 0) {
        const perfData = response.data || []
        setPerformanceData(perfData)
        return true
      } else {
        console.error('轮动模拟资产曲线服务返回失败:', response.code, response.message)
        const errorMessage = `加载模拟资产曲线失败（服务返回 ${response.code}）`
        setPerformanceError(errorMessage)
        message.error(errorMessage)
        return false
      }
    } catch (error) {
      console.error('加载收益曲线数据失败:', error)
      const errorMessage = '加载模拟资产曲线失败，请检查网络后重试'
      setPerformanceError(errorMessage)
      message.error(errorMessage)
      return false
    } finally {
      setPerformanceLoading(false)
    }
  }

  const reloadStrategyData = async (range = activeBacktestRange) => {
    const [transactionsLoaded, performanceLoaded] = await Promise.all([
      loadData(range),
      loadPerformanceData(range),
    ])
    return transactionsLoaded && performanceLoaded
  }

  useEffect(() => {
    const nextState = getMomentumBacktestRangeFromSearchParams(searchParams)
    const nextVisibleRangeState = getMomentumVisibleRangePreference(searchParams, window.localStorage)
    setUrlRangeError(nextState.error)
    setVisibleRangeError(nextVisibleRangeState.error)
    setActiveBacktestRange(nextState.range)
    setRequestedVisibleDateRange(nextVisibleRangeState.range)
    setSelectedVisibleRangePreset(nextVisibleRangeState.preset)
    if (nextVisibleRangeState.range) {
      saveMomentumVisibleRangeToStorage(window.localStorage, nextVisibleRangeState.range)
    }
    setBacktestStartDate(nextState.range ? dayjs(nextState.range.startDate) : null)
    setBacktestEndDate(nextState.range ? dayjs(nextState.range.endDate) : null)
    reloadStrategyData(nextState.range)
  }, [searchParams])

  useEffect(() => {
    if (performanceData.length === 0) {
      return
    }

    if (selectedVisibleRangePreset) {
      const nextVisibleDateRange = getMomentumVisibleDateRangeByPreset(
        performanceData,
        selectedVisibleRangePreset,
      )
      const nextDateRange = getMomentumDateRangePercentByDateRange(performanceData, nextVisibleDateRange)
      if (!nextDateRange) {
        setDateRange([0, 100])
        setVisibleRangeError('动量策略快捷时间范围不在当前模拟资产曲线内')
        return
      }

      setDateRange(nextDateRange)
      setVisibleRangeError(null)
      return
    }

    if (!requestedVisibleDateRange) {
      setDateRange([0, 100])
      return
    }

    const nextDateRange = getMomentumDateRangePercentByDateRange(performanceData, requestedVisibleDateRange)
    if (!nextDateRange) {
      setDateRange([0, 100])
      setVisibleRangeError('动量策略可视时间范围不在当前模拟资产曲线内')
      return
    }

    setDateRange(nextDateRange)
    setVisibleRangeError(null)
  }, [performanceData, requestedVisibleDateRange, selectedVisibleRangePreset])

  /**
   * 执行回测
   */
  const handleRunBacktest = async () => {
    if (!backtestStartDate || !backtestEndDate) {
      message.warning('请选择模拟日期范围')
      return
    }
    const dateRangeError = getDateRangeError(backtestStartDate, backtestEndDate, '请选择模拟日期范围')
    if (dateRangeError) {
      message.warning(dateRangeError)
      return
    }
    const initialCapitalError = getPositiveNumberError(initialCapital, '初始资金')
    if (initialCapitalError) {
      message.warning(initialCapitalError)
      return
    }

    try {
      setBacktestLoading(true)
      const startDateStr = backtestStartDate.format('YYYY-MM-DD')
      const endDateStr = backtestEndDate.format('YYYY-MM-DD')
      
      const response = await momentumStrategyApi.runBacktest(
        startDateStr,
        endDateStr,
        initialCapital
      )
      
      if (response.code === 0) {
        setBacktestModalVisible(false)
        const backtestRange = { startDate: startDateStr, endDate: endDateStr }
        setActiveBacktestRange(backtestRange)
        setSearchParams(createMomentumBacktestSearchParams(backtestRange), { replace: true })
        const refreshed = await reloadStrategyData(backtestRange)
        if (refreshed) {
          message.success('区间模拟完成，数据已更新！')
        } else {
          message.warning('区间模拟完成，但更新数据失败')
        }
      } else {
        console.error('轮动区间模拟服务返回失败:', response.code, response.message)
        message.error(`区间模拟失败（服务返回 ${response.code}）`)
      }
    } catch (error) {
      console.error('执行回测失败:', error)
      message.error('区间模拟失败')
    } finally {
      setBacktestLoading(false)
    }
  }

  /**
   * 表格列配置
   */
  const columns = [
    {
      title: '日期',
      dataIndex: 'date',
      key: 'date',
      width: 120,
      fixed: 'left',
      sorter: (a, b) => a.date.localeCompare(b.date),
    },
    {
      title: '代码',
      dataIndex: 'code',
      key: 'code',
      width: 100,
      fixed: 'left',
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type) => {
        if (type === 'buy') {
          return <Tag color="success">模拟买入</Tag>
        } else if (type === 'sell') {
          return <Tag color="error">模拟卖出</Tag>
        }
        return <Tag>{type}</Tag>
      },
      filters: [
        { text: '模拟买入', value: 'buy' },
        { text: '模拟卖出', value: 'sell' },
      ],
      onFilter: (value, record) => record.type === value,
    },
    {
      title: '数量',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 120,
      render: (quantity) => {
        const isNegative = quantity < 0
        return (
          <span style={{ color: isNegative ? '#ff4d4f' : '#52c41a' }}>
            {isNegative ? '' : '+'}{quantity.toLocaleString()}
          </span>
        )
      },
      sorter: (a, b) => a.quantity - b.quantity,
    },
    {
      title: '价格',
      dataIndex: 'price',
      key: 'price',
      width: 100,
      render: (price) => <strong>{price?.toFixed(3)}</strong>,
      sorter: (a, b) => a.price - b.price,
    },
    {
      title: '21日动量',
      dataIndex: 'momentum21',
      key: 'momentum21',
      width: 120,
      render: (momentum) => {
        if (momentum == null) return '-'
        const percent = (momentum * 100).toFixed(2)
        const color = momentum > 0 ? '#52c41a' : '#ff4d4f'
        return <span style={{ color }}>{percent}%</span>
      },
      sorter: (a, b) => {
        const aVal = a.momentum21 || 0
        const bVal = b.momentum21 || 0
        return aVal - bVal
      },
    },
  ]

  // 当前滑块对应的实际日期边界。
  const visibleDateRange = useMemo(
    () => getMomentumVisibleDateRange(performanceData, dateRange),
    [performanceData, dateRange],
  )
  // 当前可视日期边界内的交易记录，曲线缺失时保留原交易表行为。
  const visibleTransactions = useMemo(
    () => (
      visibleDateRange
        ? filterMomentumTransactionsByVisibleDateRange(data, visibleDateRange)
        : data
    ),
    [data, visibleDateRange],
  )
  // 计算当前可视时间范围的交易统计信息。
  const transactionSummary = useMemo(
    () => calculateMomentumTransactionSummary(visibleTransactions),
    [visibleTransactions],
  )

  // 处理图表数据：资金曲线和买卖点必须共用同一份每日绩效数据源
  const chartData = useMemo(
    () => buildMomentumChartData(performanceData, dateRange, visibleTransactions),
    [performanceData, dateRange, visibleTransactions],
  )
  // 根据当前可视收益曲线计算回测摘要。
  const performanceSummary = useMemo(
    () => calculateMomentumPerformanceSummary(chartData),
    [chartData],
  )
  const hasBuyPoints = chartData.some(d => d.buyValue !== null)
  const hasSellPoints = chartData.some(d => d.sellValue !== null)

  const renderTradeMarker = (markerKey, color) => (props) => {
    if (!shouldRenderMomentumMarker(props, markerKey)) {
      return null
    }

    const { cx, cy } = props
    return <circle cx={cx} cy={cy} r={6} fill={color} stroke="#fff" strokeWidth={2} />
  }

  // 格式化日期显示
  const formatDate = (dateStr) => {
    return dayjs(dateStr).format('YYYY-MM-DD')
  }

  // 将当前可视日期边界写入地址栏，用于刷新页面后恢复滑块状态。
  const persistVisibleDateRange = (nextDateRange) => {
    const isFullRange = nextDateRange[0] <= 0 && nextDateRange[1] >= 100
    const nextVisibleDateRange = isFullRange
      ? null
      : getMomentumVisibleDateRange(performanceData, nextDateRange)
    setRequestedVisibleDateRange(nextVisibleDateRange)
    setSelectedVisibleRangePreset(null)
    saveMomentumVisibleRangeToStorage(window.localStorage, nextVisibleDateRange)
    setSearchParams(
      upsertMomentumVisibleRangeSearchParams(searchParams, nextVisibleDateRange),
      { replace: true },
    )
  }

  // 响应滑块拖动并即时刷新页面上的统计、曲线和交易表。
  const handleDateRangeChange = (nextDateRange) => {
    setDateRange(nextDateRange)
  }

  // 拖动完成后把当前可视时间范围保存到地址栏。
  const handleDateRangeChangeComplete = (nextDateRange) => {
    persistVisibleDateRange(nextDateRange)
  }

  // 选择快捷范围后保存快捷项本身，刷新页面时再基于最新交易日动态计算。
  const handleVisibleRangePresetChange = (preset) => {
    const nextVisibleDateRange = getMomentumVisibleDateRangeByPreset(performanceData, preset)
    const nextDateRange = getMomentumDateRangePercentByDateRange(performanceData, nextVisibleDateRange)
    if (!nextDateRange) {
      setVisibleRangeError('动量策略快捷时间范围不在当前模拟资产曲线内')
      return
    }

    setDateRange(nextDateRange)
    setRequestedVisibleDateRange(null)
    setSelectedVisibleRangePreset(preset)
    setVisibleRangeError(null)
    saveMomentumVisibleRangePresetToStorage(window.localStorage, preset)
    setSearchParams(
      upsertMomentumVisibleRangeSearchParams(searchParams, null),
      { replace: true },
    )
  }

  // 重置为完整收益曲线，同时清除地址栏中的可视时间范围。
  const handleResetDateRange = () => {
    const fullDateRange = [0, 100]
    setDateRange(fullDateRange)
    persistVisibleDateRange(fullDateRange)
  }

  // 计算排序后的性能数据（用于 Slider marks）
  const sortedPerformanceData = useMemo(() => {
    if (!performanceData || performanceData.length === 0) {
      return []
    }
    return [...performanceData].sort((a, b) => {
      const dateA = dayjs(a.date)
      const dateB = dayjs(b.date)
      if (dateA.isBefore(dateB)) return -1
      if (dateA.isAfter(dateB)) return 1
      return 0
    })
  }, [performanceData])

  // 计算 Slider marks
  const sliderMarks = useMemo(() => {
    if (sortedPerformanceData.length === 0) return {}
    return {
      0: formatDate(sortedPerformanceData[0].date),
      100: formatDate(sortedPerformanceData[sortedPerformanceData.length - 1].date),
    }
  }, [sortedPerformanceData])

  // 自定义 Tooltip
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length > 0) {
      // 从 payload 中获取数据
      const payloadItem = payload.find(p => p.dataKey === 'totalValue') || payload[0]
      const data = payloadItem.payload || payloadItem
      
      if (!data || !data.date) {
        return null
      }
      
      return (
        <div style={{
          backgroundColor: '#ffffff',
          border: '1px solid #dbe4ee',
          borderRadius: '4px',
          color: '#182536',
          padding: '10px',
          boxShadow: '0 18px 42px rgba(24, 37, 54, 0.14)',
          minWidth: '200px'
        }}>
          <p style={{ margin: 0, fontWeight: 'bold' }}>{formatDate(data.date)}</p>
          <p style={{ margin: '5px 0', color: '#1890ff' }}>
            模拟资产: {formatCurrency(data.totalValue || 0)}
          </p>
          <p style={{ margin: '5px 0', color: (data.returnRate || 0) >= 0 ? '#52c41a' : '#ff4d4f' }}>
            模拟收益率: {data.returnRate ? data.returnRate.toFixed(2) : '0.00'}%
          </p>
          {data.holdingEtfName && (
            <p style={{ margin: '5px 0', color: '#53657a' }}>
              模拟持仓: {data.holdingEtfCode} {data.holdingEtfName}
            </p>
          )}
          {/* 显示买卖操作信息 */}
          {(data.buyInfo || data.sellInfo) && (
            <div style={{ marginTop: '8px', paddingTop: '8px', borderTop: '1px solid #dbe4ee' }}>
              {data.buyInfo && (
                <div style={{ marginBottom: data.sellInfo ? '8px' : '0' }}>
                  <p style={{ margin: '3px 0', color: '#52c41a', fontWeight: 'bold' }}>
                    模拟买入{data.buyInfo.multiple ? ` (${data.buyInfo.count}笔)` : ''}：
                  </p>
                  <p style={{ margin: '2px 0', fontSize: '12px' }}>
                    {data.buyInfo.code} {data.buyInfo.name}
                  </p>
                  <p style={{ margin: '2px 0', fontSize: '12px' }}>
                    价格: {data.buyInfo.price ? data.buyInfo.price.toFixed(3) : '0.000'} | 数量: {data.buyInfo.quantity ? Math.abs(data.buyInfo.quantity).toLocaleString() : '0'}
                  </p>
                </div>
              )}
              {data.sellInfo && (
                <div>
                  <p style={{ margin: '3px 0', color: '#ff4d4f', fontWeight: 'bold' }}>
                    模拟卖出{data.sellInfo.multiple ? ` (${data.sellInfo.count}笔)` : ''}：
                  </p>
                  <p style={{ margin: '2px 0', fontSize: '12px' }}>
                    {data.sellInfo.code} {data.sellInfo.name}
                  </p>
                  <p style={{ margin: '2px 0', fontSize: '12px' }}>
                    价格: {data.sellInfo.price ? data.sellInfo.price.toFixed(3) : '0.000'} | 数量: {data.sellInfo.quantity ? Math.abs(data.sellInfo.quantity).toLocaleString() : '0'}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      )
    }
    return null
  }

  return (
    <TerminalPage
      title="轮动策略"
      subtitle="21 日动量轮动的模拟记录、模拟资产曲线与区间试算"
      status={<span>{transactionsError ? '模拟记录暂不可用' : `模拟 ${transactionSummary.totalCount} / 买入 ${transactionSummary.buyCount} / 卖出 ${transactionSummary.sellCount}`}</span>}
    >
      {urlRangeError && (
        <Alert
          type="error"
          showIcon
          message="模拟区间参数错误"
          description={urlRangeError.replace('回测', '模拟')}
          className="terminal-section-gap"
        />
      )}
      {visibleRangeError && (
        <Alert
          type="error"
          showIcon
          message="可视时间范围参数错误"
          description={visibleRangeError}
          className="terminal-section-gap"
        />
      )}

      {/* 统计信息卡片 */}
      {!transactionsError && !performanceError && (
      <Card className="terminal-section-gap">
        <Space className="terminal-stat-strip" size="middle" wrap>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>模拟交易次数：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {transactionSummary.totalCount}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#52c41a' }}>模拟买入次数：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#52c41a', marginLeft: '8px' }}>
              {transactionSummary.buyCount}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#ff4d4f' }}>模拟卖出次数：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#ff4d4f', marginLeft: '8px' }}>
              {transactionSummary.sellCount}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>累计模拟买入：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {transactionSummary.totalBuyQuantity.toLocaleString()}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>累计模拟卖出：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {transactionSummary.totalSellQuantity.toLocaleString()}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>模拟收益率：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {formatPercent(performanceSummary.totalReturn)}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>模拟年化收益率：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {formatPercent(performanceSummary.annualizedReturn)}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>模拟开始日期：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {performanceSummary.startDate || '-'}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>模拟结束日期：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {performanceSummary.endDate || '-'}
            </span>
          </div>
          <div className="terminal-stat-chip terminal-stat-chip-danger">
            <span style={{ color: '#ff4d4f' }}>历史最大回撤：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#ff4d4f', marginLeft: '8px' }}>
              {formatPercent(performanceSummary.maxDrawdown)}
            </span>
          </div>
        </Space>
      </Card>
      )}

      {/* 资金曲线图表 */}
      <Card 
        title="模拟资产曲线"
        className="terminal-section-gap"
        extra={
          <Button 
            icon={<ReloadOutlined />}
            onClick={() => loadPerformanceData()}
            loading={performanceLoading}
            size="small"
          >
            更新曲线
          </Button>
        }
      >
        {performanceError ? (
          <Alert
            type="error"
            showIcon
            message="模拟资产曲线加载失败"
            description={performanceError}
            action={(
              <Button size="small" onClick={() => loadPerformanceData()} loading={performanceLoading}>
                重试
              </Button>
            )}
          />
        ) : performanceData.length > 0 ? (
          <>
            {/* 时间范围选择器 */}
            <div className="momentum-range-controls terminal-section-gap">
              <div className="momentum-range-toolbar">
                <span className="terminal-muted-text terminal-small-text">时间范围:</span>
                <Segmented
                  className="momentum-range-presets"
                  size="small"
                  value={selectedVisibleRangePreset}
                  options={MOMENTUM_VISIBLE_RANGE_PRESET_OPTIONS}
                  onChange={handleVisibleRangePresetChange}
                />
                <Button
                  size="small"
                  onClick={handleResetDateRange}
                >
                  重置
                </Button>
              </div>
              <Slider
                className="momentum-range-slider"
                range
                value={dateRange}
                onChange={handleDateRangeChange}
                onChangeComplete={handleDateRangeChangeComplete}
                marks={sliderMarks}
                tooltip={{
                  formatter: (value) => {
                    const date = getMomentumPerformanceDateByPercent(sortedPerformanceData, value)
                    return date ? formatDate(date) : ''
                  }
                }}
              />
            </div>

            {/* 图表 */}
            {chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={400}>
                <ComposedChart
                  data={chartData}
                  margin={{ top: 5, right: 30, left: 20, bottom: 80 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis 
                    dataKey="dateValue"
                    type="number"
                    scale="time"
                    domain={['dataMin', 'dataMax']}
                    tickFormatter={formatDate}
                    angle={-45}
                    textAnchor="end"
                    height={80}
                    tickCount={8}
                  />
                  <YAxis 
                    yAxisId="left"
                    label={{ value: '模拟资产 (¥)', angle: -90, position: 'insideLeft' }}
                    tickFormatter={(value) => (value / 10000).toFixed(1) + '万'}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend />
                  <Line
                    yAxisId="left"
                    type="monotone"
                    dataKey="totalValue"
                    stroke="#1890ff"
                    strokeWidth={2}
                    dot={false}
                    name="模拟资产"
                    activeDot={{ r: 6 }}
                    connectNulls={false}
                    isAnimationActive={false}
                  />
                  {/* 买入点 */}
                  {hasBuyPoints && (
                    <Scatter
                      yAxisId="left"
                      dataKey="buyValue"
                      fill="#52c41a"
                      name="模拟买入点"
                      shape={renderTradeMarker('buyValue', '#52c41a')}
                    />
                  )}
                  {/* 卖出点 */}
                  {hasSellPoints && (
                    <Scatter
                      yAxisId="left"
                      dataKey="sellValue"
                      fill="#ff4d4f"
                      name="模拟卖出点"
                      shape={renderTradeMarker('sellValue', '#ff4d4f')}
                    />
                  )}
                </ComposedChart>
              </ResponsiveContainer>
            ) : (
              <div className="terminal-empty-state">
                当前时间范围内暂无数据
              </div>
            )}

            {/* 买卖点标注说明 */}
            <div className="terminal-info-box terminal-field-offset-lg">
              <Space>
                <span>
                  <span style={{ 
                    display: 'inline-block', 
                    width: '12px', 
                    height: '12px', 
                    borderRadius: '50%', 
                    backgroundColor: '#52c41a',
                    marginRight: '8px'
                  }}></span>
                  模拟买入点
                </span>
                <span>
                  <span style={{ 
                    display: 'inline-block', 
                    width: '12px', 
                    height: '12px', 
                    borderRadius: '50%', 
                    backgroundColor: '#ff4d4f',
                    marginRight: '8px'
                  }}></span>
                  模拟卖出点
                </span>
              </Space>
            </div>
          </>
        ) : (
          <div className="terminal-empty-state">
            {performanceLoading ? '加载中...' : '暂无模拟资产曲线数据，请先运行区间模拟'}
          </div>
        )}
      </Card>

      <Card>
        {/* 操作栏 */}
        <Space className="terminal-toolbar" wrap>
          <Button 
            type="primary" 
            icon={<ReloadOutlined />}
            onClick={() => loadData()}
            loading={loading}
          >
            更新记录
          </Button>
          <Button 
            type="default" 
            icon={<PlayCircleOutlined />}
            onClick={() => setBacktestModalVisible(true)}
          >
            运行区间模拟
          </Button>
        </Space>

        {/* 数据表格 */}
        {transactionsError ? (
          <Alert
            type="error"
            showIcon
            message="模拟记录加载失败"
            description={transactionsError}
            action={(
              <Button size="small" onClick={() => loadData()} loading={loading}>
                重试
              </Button>
            )}
          />
        ) : (
          <Table
            columns={columns}
            dataSource={visibleTransactions}
            rowKey={(record) => `${record.date}-${record.code}-${record.type}-${record.quantity}-${record.price}`}
            loading={loading}
            scroll={{ x: 1000 }}
            pagination={{
              pageSize: 20,
              showTotal: (total) => `共 ${total} 条模拟记录`,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50', '100'],
            }}
            locale={{ emptyText: loading ? '数据加载中...' : '当前没有轮动模拟记录' }}
          />
        )}
      </Card>

      {/* 回测对话框 */}
      <Modal
        title="运行区间模拟"
        open={backtestModalVisible}
        onOk={handleRunBacktest}
        onCancel={() => setBacktestModalVisible(false)}
        confirmLoading={backtestLoading}
        okText="开始模拟"
        cancelText="取消"
      >
        <div style={{ padding: '20px 0' }}>
          <div className="terminal-section-gap">
            <div className="terminal-field-label">模拟日期范围：</div>
            <div className="terminal-range-labels">
              <label htmlFor="momentum-simulation-start-date" className="terminal-field-label">开始日期</label>
              <label htmlFor="momentum-simulation-end-date" className="terminal-field-label">结束日期</label>
            </div>
            <RangePicker
              id={{ start: 'momentum-simulation-start-date', end: 'momentum-simulation-end-date' }}
              className="terminal-full-width"
              value={[backtestStartDate, backtestEndDate]}
              onChange={(dates) => {
                if (dates && dates.length === 2) {
                  setBacktestStartDate(dates[0])
                  setBacktestEndDate(dates[1])
                } else {
                  setBacktestStartDate(null)
                  setBacktestEndDate(null)
                }
              }}
            />
          </div>
          <div>
            <label htmlFor="momentum-initial-capital" className="terminal-field-label">初始模拟资金：</label>
            <InputNumber
              id="momentum-initial-capital"
              className="terminal-full-width"
              value={initialCapital}
              onChange={(value) => setInitialCapital(value || 100000)}
              min={1000}
              step={10000}
              formatter={(value) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value.replace(/¥\s?|(,*)/g, '')}
            />
          </div>
          <div className="terminal-info-box terminal-info-box-cyan terminal-field-offset-lg">
            <p className="terminal-small-text">
              <strong>提示：</strong>区间模拟基于历史数据，运行可能需要几分钟，请耐心等待。
            </p>
          </div>
        </div>
      </Modal>

      {/* 策略说明 */}
      <Card title="轮动策略如何工作">
        <div className="terminal-copy-block">
          <p>
            <strong>21 日动量轮动规则</strong>比较固定 ETF 范围内的 21 日历史涨跌幅，
            并用历史数据模拟持有动量相对较强的标的。
          </p>
          
          <h3>规则概览：</h3>
          <ul>
            <li><strong>动量计算：</strong>21 日动量 =（当前价格 - 21 天前价格）/ 21 天前价格</li>
            <li><strong>标的选择：</strong>每日比较范围内 ETF 的 21 日动量</li>
            <li><strong>轮动规则：</strong>领先标的变化时，模拟卖出原持仓并模拟买入新的领先标的</li>
            <li><strong>当前固定范围：</strong>黄金 ETF（518880）、纳指 ETF（513100）、创业板 ETF（159915），与 ETF 管理页的监控列表不是同一组配置</li>
          </ul>

          <h3>适用场景与局限：</h3>
          <ul>
            <li>趋势较持续时，动量可帮助观察标的之间的相对强弱</li>
            <li>动量来自历史价格，存在滞后，不能预测下一阶段表现</li>
            <li>震荡期领先标的可能频繁变化，产生较多模拟交易</li>
            <li>固定范围较小，轮动不等于风险分散，也不能作为买卖依据</li>
          </ul>

          <div className="terminal-info-box terminal-info-box-amber terminal-field-offset-lg">
            <strong>风险提示：</strong>
            <p className="terminal-inline-note-space">
              历史模拟结果不代表未来表现，未计入手续费、滑点、分红与最小交易单位，仅用于理解规则。
            </p>
          </div>
        </div>
      </Card>
    </TerminalPage>
  )
}

export default MomentumStrategy
