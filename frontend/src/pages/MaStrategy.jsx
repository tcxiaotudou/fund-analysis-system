/**
 * 移动平均线策略页面
 * 展示基于10日/30日均线的双均线交易策略
 */
import React, { useState, useEffect } from 'react'
import { Alert, Card, Table, Tag, Button, Space, Tooltip, Input, DatePicker, InputNumber, message, Row, Col, Statistic, Divider } from 'antd'
import { ReloadOutlined, PlayCircleOutlined } from '@ant-design/icons'
import TerminalPage from '../components/TerminalPage'
import { maStrategyApi } from '../services/api'
import {
  getDateRangeError,
  getPositiveNumberError,
} from '../utils/backtestValidation'
import dayjs from 'dayjs'

function MaStrategy() {
  const [loading, setLoading] = useState(true)
  const [data, setData] = useState([])
  const [dataError, setDataError] = useState(null)
  
  // 回测相关状态
  const [backtestLoading, setBacktestLoading] = useState(false)
  const [backtestResult, setBacktestResult] = useState(null)
  const [etfCode, setEtfCode] = useState('')
  const [startDate, setStartDate] = useState(null)
  const [endDate, setEndDate] = useState(null)
  const [initialCapital, setInitialCapital] = useState(100000)

  /**
   * 加载MA策略数据
   */
  const loadData = async () => {
    try {
      setLoading(true)
      setDataError(null)
      const response = await maStrategyApi.getLatest()
      
      if (response.code === 0) {
        setData(response.data || [])
      } else {
        console.error('趋势信号服务返回失败:', response.code, response.message)
        const errorMessage = `加载趋势信号失败（服务返回 ${response.code}）`
        setDataError(errorMessage)
        message.error(errorMessage)
      }
    } catch (error) {
      console.error('加载MA策略数据失败:', error)
      const errorMessage = '加载趋势信号失败，请检查网络后重试'
      setDataError(errorMessage)
      message.error(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])
  
  /**
   * 执行回测
   */
  const handleRunBacktest = async () => {
    if (!etfCode || !etfCode.trim()) {
      message.warning('请输入ETF编码')
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
    
    try {
      setBacktestLoading(true)
      const startDateStr = startDate.format('YYYY-MM-DD')
      const endDateStr = endDate.format('YYYY-MM-DD')
      
      const response = await maStrategyApi.runBacktest(
        etfCode.trim(),
        startDateStr,
        endDateStr,
        initialCapital
      )
      
      if (response.code === 0) {
        setBacktestResult(response.data)
        message.success('模拟完成！')
      } else {
        console.error('趋势模拟服务返回失败:', response.code, response.message)
        message.error(`模拟失败（服务返回 ${response.code}）`)
      }
    } catch (error) {
      console.error('回测失败:', error)
      message.error('模拟失败，数据源暂时不可用，请稍后重试')
    } finally {
      setBacktestLoading(false)
    }
  }

  /**
   * 表格列配置
   */
  const columns = [
    {
      title: 'ETF名称',
      dataIndex: 'etfName',
      key: 'etfName',
      width: 180,
      fixed: 'left',
    },
    {
      title: 'ETF代码',
      dataIndex: 'etfCode',
      key: 'etfCode',
      width: 100,
      fixed: 'left',
    },
    {
      title: '当前价格',
      dataIndex: 'currentDaily',
      key: 'currentDaily',
      width: 100,
      render: (val) => <strong style={{ fontSize: '14px' }}>{val?.toFixed(3)}</strong>,
      sorter: (a, b) => a.currentDaily - b.currentDaily,
    },
    {
      title: '10日均线',
      dataIndex: 'ma10',
      key: 'ma10',
      width: 100,
      render: (val) => val?.toFixed(3),
      sorter: (a, b) => a.ma10 - b.ma10,
    },
    {
      title: '30日均线',
      dataIndex: 'ma30',
      key: 'ma30',
      width: 100,
      render: (val) => val?.toFixed(3),
      sorter: (a, b) => a.ma30 - b.ma30,
    },
    {
      title: '策略信号',
      key: 'signal',
      width: 100,
      fixed: 'right',
      render: (_, record) => {
        if (record.isBuySignal) {
          return <Tag color="success">趋势转强</Tag>
        } else if (record.isSellSignal) {
          return <Tag color="error">趋势转弱</Tag>
        } else {
          return <Tag color="default">继续观察</Tag>
        }
      },
      filters: [
        { text: '趋势转强', value: 'buy' },
        { text: '趋势转弱', value: 'sell' },
        { text: '继续观察', value: 'hold' },
      ],
      onFilter: (value, record) => {
        if (value === 'buy') return record.isBuySignal
        if (value === 'sell') return record.isSellSignal
        return !record.isBuySignal && !record.isSellSignal
      },
    },
    {
      title: '信号说明',
      dataIndex: 'signalDescription',
      key: 'signalDescription',
      width: 200,
      fixed: 'right',
      render: (text) => (
        <Tooltip title={text}>
          <span style={{ 
            display: 'block',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            maxWidth: '180px'
          }}>
            {text}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '数据时间',
      dataIndex: 'dataTime',
      key: 'dataTime',
      width: 180,
    },
  ]

  // 计算统计信息
  const buySignalCount = data.filter(item => item.isBuySignal).length
  const sellSignalCount = data.filter(item => item.isSellSignal).length
  const holdCount = data.length - buySignalCount - sellSignalCount

  return (
    <TerminalPage
      title="趋势策略"
      subtitle="用 MA10 / MA30 观察趋势变化，并用历史数据模拟规则"
      status={<span>转强 {buySignalCount} / 转弱 {sellSignalCount} / 观察 {holdCount}</span>}
    >
      
      {/* 回测功能区域 */}
      <Card 
        title="设置趋势模拟"
        className="terminal-section-gap"
        extra={
          <Tooltip title="根据 MA10 与 MA30 的交叉，在历史数据中记录模拟买入和模拟卖出点">
            <span className="terminal-muted-text terminal-small-text">仅使用均线交叉规则</span>
          </Tooltip>
        }
      >
        <Space direction="vertical" className="terminal-full-width" size="large">
          <Row gutter={16}>
            <Col xs={24} sm={12} lg={6}>
              <div>
                <label htmlFor="ma-etf-code" className="terminal-field-label">ETF 代码：</label>
                <Input
                  id="ma-etf-code"
                  placeholder="例如：sh510500"
                  value={etfCode}
                  onChange={(e) => setEtfCode(e.target.value)}
                  className="terminal-full-width"
                />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <div>
                <label htmlFor="ma-start-date" className="terminal-field-label">开始日期：</label>
                <DatePicker
                  id="ma-start-date"
                  className="terminal-full-width"
                  value={startDate}
                  onChange={setStartDate}
                  format="YYYY-MM-DD"
                  placeholder="选择开始日期"
                />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <div>
                <label htmlFor="ma-end-date" className="terminal-field-label">结束日期：</label>
                <DatePicker
                  id="ma-end-date"
                  className="terminal-full-width"
                  value={endDate}
                  onChange={setEndDate}
                  format="YYYY-MM-DD"
                  placeholder="选择结束日期"
                />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <div>
                <label htmlFor="ma-initial-capital" className="terminal-field-label">初始资金：</label>
                <InputNumber
                  id="ma-initial-capital"
                  className="terminal-full-width"
                  value={initialCapital}
                  onChange={setInitialCapital}
                  min={1000}
                  step={10000}
                  formatter={value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                  parser={value => value.replace(/¥\s?|(,*)/g, '')}
                />
              </div>
            </Col>
          </Row>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleRunBacktest}
            loading={backtestLoading}
            size="large"
          >
            开始模拟
          </Button>
        </Space>
      </Card>
      
      {/* 回测结果展示 */}
      {backtestResult && (
        <>
          {/* 回测统计信息 */}
          <Card title={`模拟结果：${backtestResult.etfName} (${backtestResult.etfCode})`} className="terminal-section-gap">
            <Row gutter={16}>
              <Col xs={12} sm={12} lg={6}>
                <Statistic
                  title="模拟总收益率"
                  value={backtestResult.totalReturnRate}
                  precision={2}
                  suffix="%"
                  valueStyle={{ color: Number(backtestResult.totalReturnRate) >= 0 ? '#3f8600' : '#cf1322' }}
                />
              </Col>
              <Col xs={12} sm={12} lg={6}>
                <Statistic
                  title="模拟年化收益率"
                  value={backtestResult.annualizedReturnRate}
                  precision={2}
                  suffix="%"
                  valueStyle={{ color: Number(backtestResult.annualizedReturnRate) >= 0 ? '#3f8600' : '#cf1322' }}
                />
              </Col>
              <Col xs={12} sm={12} lg={6}>
                <Statistic
                  title="初始模拟资金"
                  value={backtestResult.initialCapital}
                  precision={2}
                  prefix="¥"
                />
              </Col>
              <Col xs={12} sm={12} lg={6}>
                <Statistic
                  title="模拟期末资产"
                  value={backtestResult.finalCapital}
                  precision={2}
                  prefix="¥"
                  valueStyle={{ color: Number(backtestResult.finalCapital) >= Number(backtestResult.initialCapital) ? '#3f8600' : '#cf1322' }}
                />
              </Col>
            </Row>
            <Divider />
            <Row gutter={16}>
              <Col xs={24} sm={8}>
                <Statistic title="模拟交易次数" value={backtestResult.tradeCount} />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic title="模拟买入次数" value={backtestResult.buyCount} valueStyle={{ color: '#52c41a' }} />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic title="模拟卖出次数" value={backtestResult.sellCount} valueStyle={{ color: '#ff4d4f' }} />
              </Col>
            </Row>
          </Card>
          
          {/* 交易记录表格 */}
          <Card title="模拟记录" className="terminal-section-gap">
            <Table
              columns={[
                {
                  title: '日期',
                  dataIndex: 'date',
                  key: 'date',
                  width: 120,
                },
                {
                  title: '类型',
                  dataIndex: 'type',
                  key: 'type',
                  width: 80,
                  render: (type) => (
                    <Tag color={type === 'BUY' ? 'success' : 'error'}>
                      {type === 'BUY' ? '模拟买入' : '模拟卖出'}
                    </Tag>
                  ),
                },
                {
                  title: '价格',
                  dataIndex: 'price',
                  key: 'price',
                  width: 120,
                  render: (val) => `¥${Number(val).toFixed(3)}`,
                },
                {
                  title: '数量',
                  dataIndex: 'quantity',
                  key: 'quantity',
                  width: 120,
                  render: (val) => val?.toLocaleString() || '-',
                },
                {
                  title: '金额',
                  dataIndex: 'amount',
                  key: 'amount',
                  width: 120,
                  render: (val) => `¥${Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
                },
                {
                  title: '模拟总资产',
                  dataIndex: 'totalValue',
                  key: 'totalValue',
                  width: 150,
                  render: (val) => `¥${Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
                },
                {
                  title: '信号说明',
                  dataIndex: 'signalDescription',
                  key: 'signalDescription',
                  ellipsis: true,
                },
              ]}
              dataSource={backtestResult.transactions || []}
              rowKey={(record) => `${record.date}-${record.type}-${record.price}-${record.quantity}-${record.amount}`}
              pagination={{
                pageSize: 20,
                showTotal: (total) => `共 ${total} 条模拟记录`,
              }}
            />
          </Card>
        </>
      )}

      {dataError && (
        <Alert
          type="error"
          showIcon
          message="趋势信号加载失败"
          description={dataError}
          className="terminal-section-gap"
          action={(
            <Button size="small" onClick={loadData} loading={loading}>
              重试
            </Button>
          )}
        />
      )}

      {!dataError && (
        <>
          {/* 统计信息卡片 */}
          <Card className="terminal-section-gap">
        <Space className="terminal-stat-strip" size="middle" wrap>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>总计：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {data.length}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#52c41a' }}>趋势转强：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#52c41a', marginLeft: '8px' }}>
              {buySignalCount}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#ff4d4f' }}>趋势转弱：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#ff4d4f', marginLeft: '8px' }}>
              {sellSignalCount}
            </span>
          </div>
          <div className="terminal-stat-chip">
            <span style={{ color: '#666' }}>继续观察：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {holdCount}
            </span>
          </div>
            </Space>
          </Card>

          <Card>
            {/* 操作栏 */}
            <Space className="terminal-toolbar" wrap>
              <Button
                type="primary"
                icon={<ReloadOutlined />}
                onClick={loadData}
                loading={loading}
              >
                刷新数据
              </Button>
            </Space>

            {/* 数据表格 */}
            <Table
              columns={columns}
              dataSource={data}
              rowKey="etfCode"
              loading={loading}
              scroll={{ x: 1400 }}
              pagination={{
                pageSize: 20,
                showTotal: (total) => `共 ${total} 条数据`,
                showSizeChanger: true,
                pageSizeOptions: ['10', '20', '50', '100'],
              }}
              locale={{ emptyText: loading ? '数据加载中...' : '当前没有 MA 策略数据' }}
            />
          </Card>
        </>
      )}

      {/* 策略说明 */}
      <Card title="均线策略如何工作">
        <div className="terminal-copy-block">
          <p>
            <strong>双均线规则</strong>通过观察 10 日均线和 30 日均线的交叉，记录趋势可能发生变化的时点。
          </p>
          
          <h3>指标说明：</h3>
          <ul>
            <li><strong>10 日均线（MA10）：</strong>反映相对短期的价格趋势</li>
            <li><strong>30 日均线（MA30）：</strong>反映相对中期的价格趋势</li>
          </ul>

          <h3>趋势转强条件：</h3>
          <div className="terminal-info-box terminal-info-box-cyan terminal-field-offset-lg">
            <p style={{ margin: 0 }}>
              <strong>条件：</strong>MA10 上穿 MA30（金叉）
            </p>
          </div>

          <h3 className="terminal-field-offset-xl">趋势转弱条件：</h3>
          <div className="terminal-info-box terminal-info-box-red terminal-field-offset-lg">
            <p style={{ margin: 0 }}>
              <strong>条件：</strong>MA10 下穿 MA30（死叉）
            </p>
          </div>

          <h3 className="terminal-field-offset-xl">适用场景与局限：</h3>
          <ul>
            <li>均线可帮助观察持续趋势，但信号会滞后于价格变化</li>
            <li>震荡行情中可能频繁出现交叉，增加模拟交易次数</li>
            <li>单一均线信号不能作为买卖依据，应结合更多信息判断</li>
          </ul>

          <h3 className="terminal-field-offset-xl">查看建议：</h3>
          <ul>
            <li>趋势转强或转弱时，可同时观察成交量和市场环境</li>
            <li>可结合 RSI、MACD 等指标交叉验证，不应依赖单一信号</li>
          </ul>

          <div className="terminal-info-box terminal-info-box-amber terminal-field-offset-lg">
            <strong>风险提示：</strong>
            <p className="terminal-inline-note-space">
              页面结果来自历史模拟，不代表未来收益，未计入交易成本与滑点，仅用于理解规则，不构成买卖依据。
            </p>
          </div>
        </div>
      </Card>
    </TerminalPage>
  )
}

export default MaStrategy
