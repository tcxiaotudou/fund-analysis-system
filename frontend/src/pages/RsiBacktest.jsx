import React, { useState } from 'react'
import {
  Card, Table, Tag, Button, Space, Input, DatePicker, InputNumber,
  message, Row, Col, Statistic, Divider, Tooltip, Slider
} from 'antd'
import { PlayCircleOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons'
import { useSearchParams } from 'react-router-dom'
import {
  ComposedChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip,
  ResponsiveContainer, ReferenceLine, Scatter, Area, Legend
} from 'recharts'
import TerminalPage from '../components/TerminalPage'
import { rsiBacktestApi } from '../services/api'
import {
  getDateRangeError,
  getPositiveNumberError,
  getRsiThresholdOrderError,
} from '../utils/backtestValidation'
import dayjs from 'dayjs'

const DEFAULT_ETF = 'sh512170'
const DEFAULT_CAPITAL = 100000
const DEFAULT_RSI_PERIOD = 14
const DEFAULT_BUY_THRESHOLD = 30
const DEFAULT_SELL_THRESHOLD = 60
const DEFAULT_FIXED_AMOUNT = 10000

function RsiBacktest() {
  const [searchParams] = useSearchParams()
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [etfCode, setEtfCode] = useState(() => searchParams.get('etfCode')?.trim() || DEFAULT_ETF)
  const [startDate, setStartDate] = useState(dayjs().subtract(5, 'year'))
  const [endDate, setEndDate] = useState(dayjs())
  const [initialCapital, setInitialCapital] = useState(DEFAULT_CAPITAL)
  const [rsiPeriod, setRsiPeriod] = useState(DEFAULT_RSI_PERIOD)
  const [buyThreshold, setBuyThreshold] = useState(DEFAULT_BUY_THRESHOLD)
  const [sellThreshold, setSellThreshold] = useState(DEFAULT_SELL_THRESHOLD)
  const [fixedAmount, setFixedAmount] = useState(DEFAULT_FIXED_AMOUNT)
  const [chartRange, setChartRange] = useState([0, 100])

  const handleRunBacktest = async () => {
    if (!etfCode?.trim()) {
      message.warning('请输入ETF编码')
      return
    }
    if (!startDate || !endDate) {
      message.warning('请选择开始时间和结束时间')
      return
    }
    const dateRangeError = getDateRangeError(startDate, endDate)
    if (dateRangeError) {
      message.warning(dateRangeError)
      return
    }
    const initialCapitalError = getPositiveNumberError(initialCapital, '初始资金')
    if (initialCapitalError) {
      message.warning(initialCapitalError)
      return
    }
    const fixedAmountError = getPositiveNumberError(fixedAmount, '每次模拟金额')
    if (fixedAmountError) {
      message.warning(fixedAmountError)
      return
    }
    const thresholdError = getRsiThresholdOrderError(buyThreshold, sellThreshold)
    if (thresholdError) {
      message.warning('关注阈值必须低于止盈阈值')
      return
    }

    try {
      setLoading(true)
      const response = await rsiBacktestApi.runBacktest(
        etfCode.trim(),
        startDate.format('YYYY-MM-DD'),
        endDate.format('YYYY-MM-DD'),
        initialCapital,
        rsiPeriod,
        buyThreshold,
        sellThreshold,
        fixedAmount
      )

      if (response.code === 0) {
        setResult(response.data)
        setChartRange([0, 100])
        message.success('模拟完成！')
      } else {
        console.error('RSI 模拟服务返回失败:', response.code, response.message)
        message.error(`模拟失败（服务返回 ${response.code}）`)
      }
    } catch (error) {
      message.error('模拟失败，数据源暂时不可用，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  const getChartData = () => {
    if (!result?.dailyValues?.length) return []
    const data = result.dailyValues
    const txByDate = {}
    ;(result.transactions || []).forEach(tx => {
      if (!txByDate[tx.date]) txByDate[tx.date] = []
      txByDate[tx.date].push(tx)
    })

    return data.map(d => {
      const txList = txByDate[d.date] || []
      const hasBuy = txList.some(t => t.type === 'BUY')
      const hasSell = txList.some(t => t.type === 'SELL')
      return {
        date: d.date,
        price: Number(d.price),
        totalValue: Number(d.totalValue),
        returnRate: Number(d.returnRate),
        rsi: Number(d.rsiValue),
        buyPoint: hasBuy ? Number(d.price) : null,
        sellPoint: hasSell ? Number(d.price) : null,
        buyRsi: hasBuy ? Number(d.rsiValue) : null,
        sellRsi: hasSell ? Number(d.rsiValue) : null,
      }
    })
  }

  const getSlicedData = () => {
    const allData = getChartData()
    if (!allData.length) return []
    const start = Math.floor((chartRange[0] / 100) * allData.length)
    const end = Math.ceil((chartRange[1] / 100) * allData.length)
    return allData.slice(start, Math.max(end, start + 1))
  }

  const priceTooltipFormatter = (value, name) => {
    const labels = {
      price: '价格',
      totalValue: '模拟总资产',
      buyPoint: '模拟买入点',
      sellPoint: '模拟卖出点',
    }
    if (name === 'buyPoint' || name === 'sellPoint') {
      return value != null ? [`¥${Number(value).toFixed(3)}`, labels[name]] : null
    }
    if (name === 'totalValue') {
      return [`¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`, labels[name]]
    }
    return [`¥${Number(value).toFixed(3)}`, labels[name] || name]
  }

  const rsiTooltipFormatter = (value, name) => {
    const labels = { rsi: 'RSI', buyRsi: '模拟买入 RSI', sellRsi: '模拟卖出 RSI' }
    return [Number(value).toFixed(2), labels[name] || name]
  }

  const transactionColumns = [
    { title: '日期', dataIndex: 'date', key: 'date', width: 110 },
    {
      title: '类型', dataIndex: 'type', key: 'type', width: 70,
      render: type => <Tag color={type === 'BUY' ? 'success' : 'error'}>{type === 'BUY' ? '模拟买入' : '模拟卖出'}</Tag>,
    },
    {
      title: '价格', dataIndex: 'price', key: 'price', width: 90,
      render: val => `¥${Number(val).toFixed(3)}`,
    },
    {
      title: '数量', dataIndex: 'quantity', key: 'quantity', width: 90,
      render: val => val?.toLocaleString() || '-',
    },
    {
      title: '金额', dataIndex: 'amount', key: 'amount', width: 110,
      render: val => `¥${Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    },
    {
      title: 'RSI', dataIndex: 'rsiValue', key: 'rsiValue', width: 70,
      render: val => Number(val).toFixed(2),
    },
    {
      title: '模拟盈亏', dataIndex: 'profit', key: 'profit', width: 110,
      render: (val, record) => {
        if (record.type === 'BUY') return '-'
        const v = Number(val)
        return (
          <span style={{ color: v >= 0 ? '#52c41a' : '#ff4d4f', fontWeight: 'bold' }}>
            {v >= 0 ? '+' : ''}¥{v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </span>
        )
      },
    },
    {
      title: '模拟剩余持仓', dataIndex: 'holdingQuantityAfter', key: 'holdingQuantityAfter', width: 110,
      render: val => val?.toLocaleString() || '0',
    },
    {
      title: '模拟持仓均价', dataIndex: 'avgCostAfter', key: 'avgCostAfter', width: 110,
      render: val => val && Number(val) > 0 ? `¥${Number(val).toFixed(3)}` : '-',
    },
    {
      title: '模拟剩余现金', dataIndex: 'cashAfter', key: 'cashAfter', width: 120,
      render: val => `¥${Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    },
    {
      title: '模拟总资产', dataIndex: 'totalValue', key: 'totalValue', width: 120,
      render: val => `¥${Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    },
    { title: '信号说明', dataIndex: 'signalDescription', key: 'signalDescription', ellipsis: true, width: 200 },
  ]

  const slicedData = getSlicedData()

  return (
    <TerminalPage
      title="RSI 策略模拟"
      subtitle="用历史数据试算分批规则，不代表未来收益"
      status={<span>{result ? `模拟标的：${result.etfName} (${result.etfCode})` : `默认标的：${etfCode}`}</span>}
    >

      <Card title="设置模拟条件" className="terminal-section-gap">
        <Space direction="vertical" className="terminal-full-width" size="large">
          <Row gutter={16}>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <label htmlFor="rsi-etf-code" className="terminal-field-label">ETF 代码：</label>
                <Input id="rsi-etf-code" placeholder="例如：sh512170" value={etfCode} onChange={e => setEtfCode(e.target.value)} />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <label htmlFor="rsi-start-date" className="terminal-field-label">开始日期：</label>
                <DatePicker id="rsi-start-date" className="terminal-full-width" value={startDate} onChange={setStartDate} format="YYYY-MM-DD" />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <label htmlFor="rsi-end-date" className="terminal-field-label">结束日期：</label>
                <DatePicker id="rsi-end-date" className="terminal-full-width" value={endDate} onChange={setEndDate} format="YYYY-MM-DD" />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <label htmlFor="rsi-initial-capital" className="terminal-field-label">初始资金：</label>
                <InputNumber
                  id="rsi-initial-capital"
                  className="terminal-full-width" value={initialCapital} onChange={setInitialCapital}
                  min={1000} step={10000}
                  formatter={v => `¥ ${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                  parser={v => v.replace(/¥\s?|(,*)/g, '')}
                />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <Tooltip title="每次触发关注或止盈阈值时使用的固定模拟金额，可连续多天试算">
                  <label htmlFor="rsi-fixed-amount" className="terminal-field-label">每次模拟金额：</label>
                </Tooltip>
                <InputNumber
                  id="rsi-fixed-amount"
                  className="terminal-full-width" value={fixedAmount} onChange={setFixedAmount}
                  min={100} step={1000}
                  formatter={v => `¥ ${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                  parser={v => v.replace(/¥\s?|(,*)/g, '')}
                />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <label htmlFor="rsi-period" className="terminal-field-label">RSI 周期：</label>
                <InputNumber id="rsi-period" className="terminal-full-width" value={rsiPeriod} onChange={setRsiPeriod} min={2} max={100} />
              </div>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <label htmlFor="rsi-buy-threshold" className="terminal-field-label">关注阈值（RSI ≤）：</label>
                <InputNumber id="rsi-buy-threshold" className="terminal-full-width" value={buyThreshold} onChange={setBuyThreshold} min={1} max={99} />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <label htmlFor="rsi-sell-threshold" className="terminal-field-label">止盈阈值（RSI ≥）：</label>
                <InputNumber id="rsi-sell-threshold" className="terminal-full-width" value={sellThreshold} onChange={setSellThreshold} min={1} max={99} />
              </div>
            </Col>
          </Row>
          <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleRunBacktest} loading={loading} size="large">
            开始模拟
          </Button>
        </Space>
      </Card>

      {result && (
        <>
          <Card title={`模拟结果：${result.etfName} (${result.etfCode})`} className="terminal-section-gap">
            <Row gutter={16}>
              <Col xs={12} sm={8} lg={4}>
                <Statistic
                  title="模拟总收益率" value={result.totalReturnRate} precision={2} suffix="%"
                  valueStyle={{ color: Number(result.totalReturnRate) >= 0 ? '#3f8600' : '#cf1322' }}
                  prefix={Number(result.totalReturnRate) >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic
                  title="模拟年化收益率" value={result.annualizedReturnRate} precision={2} suffix="%"
                  valueStyle={{ color: Number(result.annualizedReturnRate) >= 0 ? '#3f8600' : '#cf1322' }}
                />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic title="初始模拟资金" value={result.initialCapital} precision={2} prefix="¥" />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic
                  title="模拟期末资产" value={result.finalCapital} precision={2} prefix="¥"
                  valueStyle={{ color: Number(result.finalCapital) >= Number(result.initialCapital) ? '#3f8600' : '#cf1322' }}
                />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic title="历史最大回撤" value={result.maxDrawdown} precision={2} suffix="%" valueStyle={{ color: '#cf1322' }} />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic
                  title="模拟卖出胜率" value={result.winRate} precision={2} suffix="%"
                  valueStyle={{ color: Number(result.winRate) >= 50 ? '#3f8600' : '#cf1322' }}
                />
              </Col>
            </Row>
            <Divider />
            <Row gutter={16}>
              <Col xs={12} sm={8} lg={4}>
                <Statistic title="模拟交易次数" value={result.tradeCount} />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic title="模拟买入次数" value={result.buyCount} valueStyle={{ color: '#52c41a' }} />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic title="模拟卖出次数" value={result.sellCount} valueStyle={{ color: '#ff4d4f' }} />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic title="模拟累计投入" value={result.totalInvested} precision={2} prefix="¥" />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic title="模拟剩余持仓" value={result.finalHoldingQuantity} suffix="份" />
              </Col>
              <Col xs={12} sm={8} lg={4}>
                <Statistic title="模拟持仓均价" value={result.averageCost} precision={3} prefix="¥" />
              </Col>
            </Row>
          </Card>

          <Card title="历史走势与模拟点" className="terminal-section-gap">
            <div className="terminal-section-gap">
              <span className="terminal-muted-text" style={{ marginRight: 8 }}>时间范围缩放：</span>
              <Slider
                range value={chartRange} onChange={setChartRange}
                tooltip={{ formatter: v => {
                  const allData = getChartData()
                  const idx = Math.floor((v / 100) * allData.length)
                  return allData[Math.min(idx, allData.length - 1)]?.date || ''
                }}}
                className="terminal-inline-slider"
                style={{ maxWidth: 600, display: 'inline-block', width: '60%', verticalAlign: 'middle' }}
              />
            </div>
            <ResponsiveContainer width="100%" height={400}>
              <ComposedChart data={slicedData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} interval="preserveStartEnd" />
                <YAxis yAxisId="price" orientation="left" tick={{ fontSize: 11 }} domain={['auto', 'auto']}
                       label={{ value: '价格', angle: -90, position: 'insideLeft', style: { fontSize: 12 } }} />
                <YAxis yAxisId="value" orientation="right" tick={{ fontSize: 11 }} domain={['auto', 'auto']}
                       label={{ value: '模拟总资产', angle: 90, position: 'insideRight', style: { fontSize: 12 } }} />
                <RechartsTooltip formatter={priceTooltipFormatter} />
                <Legend />
                <Line yAxisId="price" type="monotone" dataKey="price" stroke="#1890ff" dot={false}
                      strokeWidth={1.5} name="价格" />
                <Line yAxisId="value" type="monotone" dataKey="totalValue" stroke="#722ed1" dot={false}
                      strokeWidth={1.5} name="模拟总资产" strokeDasharray="5 5" />
                <Scatter yAxisId="price" dataKey="buyPoint" fill="#52c41a" name="模拟买入点"
                         shape={props => {
                           if (props.payload.buyPoint == null) return null
                           return <svg x={props.cx - 8} y={props.cy - 8}><polygon points="8,0 16,16 0,16" fill="#52c41a" /></svg>
                         }} />
                <Scatter yAxisId="price" dataKey="sellPoint" fill="#ff4d4f" name="模拟卖出点"
                         shape={props => {
                           if (props.payload.sellPoint == null) return null
                           return <svg x={props.cx - 8} y={props.cy - 2}><polygon points="8,16 16,0 0,0" fill="#ff4d4f" /></svg>
                         }} />
              </ComposedChart>
            </ResponsiveContainer>
          </Card>

          <Card title="RSI指标走势" className="terminal-section-gap">
            <ResponsiveContainer width="100%" height={300}>
              <ComposedChart data={slicedData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} interval="preserveStartEnd" />
                <YAxis domain={[0, 100]} tick={{ fontSize: 11 }}
                       label={{ value: 'RSI', angle: -90, position: 'insideLeft', style: { fontSize: 12 } }} />
                <RechartsTooltip formatter={rsiTooltipFormatter} />
                <Legend />
                <ReferenceLine y={buyThreshold} stroke="#52c41a" strokeDasharray="5 5"
                               label={{ value: `关注线(${buyThreshold})`, position: 'right', fill: '#52c41a', fontSize: 11 }} />
                <ReferenceLine y={sellThreshold} stroke="#ff4d4f" strokeDasharray="5 5"
                               label={{ value: `止盈线(${sellThreshold})`, position: 'right', fill: '#ff4d4f', fontSize: 11 }} />
                <Area type="monotone" dataKey="rsi" stroke="#2563eb" fill="#087f8c" fillOpacity={0.12}
                      strokeWidth={1.5} name="RSI" />
                <Scatter dataKey="buyRsi" fill="#52c41a" name="模拟买入 RSI"
                         shape={props => {
                           if (props.payload.buyRsi == null) return null
                           return <svg x={props.cx - 6} y={props.cy - 6}><circle cx="6" cy="6" r="6" fill="#52c41a" /></svg>
                         }} />
                <Scatter dataKey="sellRsi" fill="#ff4d4f" name="模拟卖出 RSI"
                         shape={props => {
                           if (props.payload.sellRsi == null) return null
                           return <svg x={props.cx - 6} y={props.cy - 6}><circle cx="6" cy="6" r="6" fill="#ff4d4f" /></svg>
                         }} />
              </ComposedChart>
            </ResponsiveContainer>
          </Card>

          <Card title="模拟记录" className="terminal-section-gap">
            <Table
              columns={transactionColumns}
              dataSource={result.transactions || []}
              rowKey={(record) => `${record.date}-${record.type}-${record.price}-${record.quantity}-${record.amount}`}
              pagination={{
                defaultPageSize: 20,
                showTotal: total => `共 ${total} 条模拟记录`,
                showSizeChanger: true,
                pageSizeOptions: ['10', '20', '50', '100', '200'],
              }}
              scroll={{ x: 1500 }}
              rowClassName={record => record.type === 'BUY' ? 'buy-row' : 'sell-row'}
            />
          </Card>

          <Card title="规则说明" className="terminal-section-gap">
            <div className="terminal-copy-block">
              <p>
                <strong>RSI 定额分批规则</strong>：每天检查 RSI 值，达到关注阈值时按固定金额模拟买入，
                达到止盈阈值时按固定金额模拟卖出，可连续多天试算。
              </p>
              <h3>当前模拟条件：</h3>
              <div className="terminal-info-box terminal-info-box-cyan">
                <ul style={{ margin: 0, paddingLeft: 20 }}>
                  <li>RSI 周期：<strong>{result.rsiPeriod} 日</strong></li>
                  <li>关注条件：当日 RSI ≤ <strong>{Number(result.rsiBuyThreshold)}</strong> → 模拟买入 <strong>¥{Number(result.fixedAmountPerTrade).toLocaleString()}</strong></li>
                  <li>止盈条件：当日 RSI ≥ <strong>{Number(result.rsiSellThreshold)}</strong> → 模拟卖出 <strong>¥{Number(result.fixedAmountPerTrade).toLocaleString()}</strong></li>
                  <li>连续触发：如果连续多天满足条件，每天都会生成一笔模拟交易</li>
                  <li>模拟区间：<strong>{result.startDate}</strong> ~ <strong>{result.endDate}</strong></li>
                </ul>
              </div>

              <div className="terminal-info-box terminal-info-box-amber terminal-field-offset-lg">
                <strong>风险提示：</strong>
                <p className="terminal-inline-note-space">
                  历史模拟结果不代表未来收益，也不能作为买卖依据。结果未计入手续费、滑点、分红和最小交易单位，仅用于理解规则。
                </p>
              </div>
            </div>
          </Card>
        </>
      )}
    </TerminalPage>
  )
}

export default RsiBacktest
