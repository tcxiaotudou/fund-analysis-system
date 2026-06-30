/**
 * 21日动量策略页面
 * 展示基于21日动量的ETF轮动策略交易记录
 */
import React, { useState, useEffect, useMemo } from 'react'
import { Alert, Card, Table, Tag, Button, Space, DatePicker, InputNumber, message, Modal, Slider, Row, Col } from 'antd'
import { ReloadOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { useSearchParams } from 'react-router-dom'
import TerminalPage from '../components/TerminalPage'
import { momentumStrategyApi } from '../services/api'
import {
  createMomentumBacktestSearchParams,
  getMomentumBacktestRangeFromSearchParams,
} from '../utils/momentumBacktestRange'
import {
  buildMomentumChartData,
  shouldRenderMomentumMarker,
} from '../utils/momentumChartData'
import {
  getDateRangeError,
  getPositiveNumberError,
} from '../utils/backtestValidation'
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
  const initialBacktestRange = initialBacktestState.range
  const [urlRangeError, setUrlRangeError] = useState(initialBacktestState.error)
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [performanceData, setPerformanceData] = useState([])
  const [performanceLoading, setPerformanceLoading] = useState(false)
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
  // 时间范围选择器状态（0-100的百分比）
  const [dateRange, setDateRange] = useState([0, 100])

  /**
   * 加载交易记录数据
   */
  const loadData = async (range = activeBacktestRange) => {
    try {
      setLoading(true)
      const response = range
        ? await momentumStrategyApi.getTransactionsByRange(range.startDate, range.endDate)
        : await momentumStrategyApi.getTransactions()
      
      if (response.code === 0) {
        setData(response.data || [])
        return true
      } else {
        message.error(response.message || '加载数据失败')
        return false
      }
    } catch (error) {
      console.error('加载交易记录失败:', error)
      message.error('加载交易记录失败')
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
      const response = range
        ? await momentumStrategyApi.getPerformanceByRange(range.startDate, range.endDate)
        : await momentumStrategyApi.getPerformance()
      
      if (response.code === 0) {
        const perfData = response.data || []
        setPerformanceData(perfData)
        // 如果有数据，重置时间范围选择器
        if (perfData.length > 0) {
          setDateRange([0, 100])
        }
        return true
      } else {
        message.error(response.message || '加载收益曲线数据失败')
        return false
      }
    } catch (error) {
      console.error('加载收益曲线数据失败:', error)
      message.error('加载收益曲线数据失败')
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
    setUrlRangeError(nextState.error)
    setActiveBacktestRange(nextState.range)
    setBacktestStartDate(nextState.range ? dayjs(nextState.range.startDate) : null)
    setBacktestEndDate(nextState.range ? dayjs(nextState.range.endDate) : null)
    reloadStrategyData(nextState.range)
  }, [searchParams])

  /**
   * 执行回测
   */
  const handleRunBacktest = async () => {
    if (!backtestStartDate || !backtestEndDate) {
      message.warning('请选择回测日期范围')
      return
    }
    const dateRangeError = getDateRangeError(backtestStartDate, backtestEndDate, '请选择回测日期范围')
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
          message.success('回测完成，数据已刷新！')
        } else {
          message.warning('回测完成，但刷新最新数据失败')
        }
      } else {
        message.error(response.message || '执行回测失败')
      }
    } catch (error) {
      console.error('执行回测失败:', error)
      message.error('执行回测失败')
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
          return <Tag color="success">买入</Tag>
        } else if (type === 'sell') {
          return <Tag color="error">卖出</Tag>
        }
        return <Tag>{type}</Tag>
      },
      filters: [
        { text: '买入', value: 'buy' },
        { text: '卖出', value: 'sell' },
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

  // 计算统计信息
  const buyCount = data.filter(item => item.type === 'buy').length
  const sellCount = data.filter(item => item.type === 'sell').length
  const totalBuyQuantity = data
    .filter(item => item.type === 'buy')
    .reduce((sum, item) => sum + (item.quantity || 0), 0)
  const totalSellQuantity = Math.abs(
    data
      .filter(item => item.type === 'sell')
      .reduce((sum, item) => sum + (item.quantity || 0), 0)
  )

  // 处理图表数据：资金曲线和买卖点必须共用同一份每日绩效数据源
  const chartData = useMemo(
    () => buildMomentumChartData(performanceData, dateRange, data),
    [performanceData, dateRange, data],
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

  // 格式化金额
  const formatCurrency = (value) => {
    return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
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
            资产总值: {formatCurrency(data.totalValue || 0)}
          </p>
          <p style={{ margin: '5px 0', color: (data.returnRate || 0) >= 0 ? '#52c41a' : '#ff4d4f' }}>
            收益率: {data.returnRate ? data.returnRate.toFixed(2) : '0.00'}%
          </p>
          {data.holdingEtfName && (
            <p style={{ margin: '5px 0', color: '#53657a' }}>
              持仓: {data.holdingEtfCode} {data.holdingEtfName}
            </p>
          )}
          {/* 显示买卖操作信息 */}
          {(data.buyInfo || data.sellInfo) && (
            <div style={{ marginTop: '8px', paddingTop: '8px', borderTop: '1px solid #dbe4ee' }}>
              {data.buyInfo && (
                <div style={{ marginBottom: data.sellInfo ? '8px' : '0' }}>
                  <p style={{ margin: '3px 0', color: '#52c41a', fontWeight: 'bold' }}>
                    买入操作{data.buyInfo.multiple ? ` (${data.buyInfo.count}笔)` : ''}：
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
                    卖出操作{data.sellInfo.multiple ? ` (${data.sellInfo.count}笔)` : ''}：
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
      title="21日动量策略"
      subtitle="ETF 轮动交易记录、资金曲线和回测区间"
      status={<span>交易 {data.length} / 买入 {buyCount} / 卖出 {sellCount}</span>}
    >
      {urlRangeError && (
        <Alert
          type="error"
          showIcon
          message="回测区间参数错误"
          description={urlRangeError}
          className="terminal-section-gap"
        />
      )}

      {/* 统计信息卡片 */}
      <Card className="terminal-section-gap">
        <Space className="terminal-stat-strip" size="middle" wrap>
          <div className="terminal-stat-chip">
            <span style={{ color: '#999' }}>总交易次数：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {data.length}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#52c41a' }}>买入次数：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#52c41a', marginLeft: '8px' }}>
              {buyCount}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#ff4d4f' }}>卖出次数：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#ff4d4f', marginLeft: '8px' }}>
              {sellCount}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#999' }}>累计买入：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {totalBuyQuantity.toLocaleString()}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#999' }}>累计卖出：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {totalSellQuantity.toLocaleString()}
            </span>
          </div>
        </Space>
      </Card>

      {/* 资金曲线图表 */}
      <Card 
        title="资金曲线" 
        className="terminal-section-gap"
        extra={
          <Button 
            icon={<ReloadOutlined />}
            onClick={() => loadPerformanceData()}
            loading={performanceLoading}
            size="small"
          >
            刷新曲线
          </Button>
        }
      >
        {performanceData.length > 0 ? (
          <>
            {/* 时间范围选择器 */}
            <div className="terminal-section-gap">
              <Row gutter={16} align="middle">
                <Col xs={24} sm={4} lg={2}>
                  <span className="terminal-muted-text terminal-small-text">时间范围:</span>
                </Col>
                <Col xs={24} sm={16} lg={20}>
                  <Slider
                    range
                    value={dateRange}
                    onChange={setDateRange}
                    marks={sliderMarks}
                    tooltip={{
                      formatter: (value) => {
                        if (sortedPerformanceData.length === 0) return ''
                        const index = Math.floor((value / 100) * sortedPerformanceData.length)
                        return formatDate(sortedPerformanceData[Math.min(index, sortedPerformanceData.length - 1)].date)
                      }
                    }}
                  />
                </Col>
                <Col xs={24} sm={4} lg={2}>
                  <Button 
                    size="small" 
                    onClick={() => setDateRange([0, 100])}
                  >
                    重置
                  </Button>
                </Col>
              </Row>
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
                    label={{ value: '资产总值 (¥)', angle: -90, position: 'insideLeft' }}
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
                    name="资产总值"
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
                      name="买入点"
                      shape={renderTradeMarker('buyValue', '#52c41a')}
                    />
                  )}
                  {/* 卖出点 */}
                  {hasSellPoints && (
                    <Scatter
                      yAxisId="left"
                      dataKey="sellValue"
                      fill="#ff4d4f"
                      name="卖出点"
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
                  买入点
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
                  卖出点
                </span>
              </Space>
            </div>
          </>
        ) : (
          <div className="terminal-empty-state">
            {performanceLoading ? '加载中...' : '暂无收益曲线数据，请先执行回测'}
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
            刷新数据
          </Button>
          <Button 
            type="default" 
            icon={<PlayCircleOutlined />}
            onClick={() => setBacktestModalVisible(true)}
          >
            执行回测
          </Button>
        </Space>

        {/* 数据表格 */}
        <Table
          columns={columns}
          dataSource={data}
          rowKey={(record) => `${record.date}-${record.code}-${record.type}-${record.quantity}-${record.price}`}
          loading={loading}
          scroll={{ x: 1000 }}
          pagination={{
            pageSize: 20,
            showTotal: (total) => `共 ${total} 条交易记录`,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
          locale={{ emptyText: loading ? '数据加载中...' : '当前没有动量交易记录' }}
        />
      </Card>

      {/* 回测对话框 */}
      <Modal
        title="执行回测"
        open={backtestModalVisible}
        onOk={handleRunBacktest}
        onCancel={() => setBacktestModalVisible(false)}
        confirmLoading={backtestLoading}
        okText="执行回测"
        cancelText="取消"
      >
        <div style={{ padding: '20px 0' }}>
          <div className="terminal-section-gap">
            <label className="terminal-field-label">回测日期范围：</label>
            <RangePicker
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
            <label className="terminal-field-label">初始资金：</label>
            <InputNumber
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
              <strong>提示：</strong>回测功能由Java后端直接执行，无需额外工具。
              <br />
              选择日期范围和初始资金后，点击"执行回测"即可开始。
              <br />
              回测可能需要几分钟时间，请耐心等待。
            </p>
          </div>
        </div>
      </Modal>

      {/* 策略说明 */}
      <Card title="21日动量策略说明">
        <div className="terminal-copy-block">
          <p>
            <strong>21日动量策略</strong>是一种基于动量的ETF轮动策略，
            通过计算各ETF的21日收益率（动量），选择动量最强的ETF进行投资。
          </p>
          
          <h3>策略原理：</h3>
          <ul>
            <li><strong>动量计算：</strong>21日动量 = (当前价格 - 21天前价格) / 21天前价格</li>
            <li><strong>选股逻辑：</strong>每日选择21日动量最强的ETF进行投资</li>
            <li><strong>调仓规则：</strong>当动量最强的ETF发生变化时，卖出当前持有，买入新的最强ETF</li>
            <li><strong>当前固定投资范围：</strong>黄金ETF(518880)、纳指ETF(513100)、创业板ETF(159915)，与 ETF 管理页的监控列表不是同一组配置</li>
          </ul>

          <h3>策略优势：</h3>
          <ol>
            <li><strong>趋势跟踪：</strong>动量指标能够捕捉价格趋势，选择表现最好的ETF</li>
            <li><strong>轮动机制：</strong>自动在不同ETF之间切换，捕捉各阶段的机会</li>
            <li><strong>简单有效：</strong>策略逻辑简单，易于理解和执行</li>
            <li><strong>风险分散：</strong>在多个ETF之间轮动，降低单一资产风险</li>
          </ol>

          <h3>使用建议：</h3>
          <ul>
            <li>回测历史数据以评估策略表现</li>
            <li>关注交易频率，避免过度交易</li>
            <li>结合市场环境调整策略参数</li>
            <li>注意交易成本和滑点影响</li>
          </ul>

          <div className="terminal-info-box terminal-info-box-amber terminal-field-offset-lg">
            <strong>⚠️ 风险提示：</strong>
            <p className="terminal-inline-note-space">
              动量策略在趋势明显的市场中表现较好，但在震荡市场中可能产生频繁交易。
              本策略仅供参考，不构成投资建议。请结合自身风险承受能力，谨慎决策。
              投资有风险，入市需谨慎。
            </p>
          </div>
        </div>
      </Card>
    </TerminalPage>
  )
}

export default MomentumStrategy
