/**
 * 移动平均线策略页面
 * 展示基于10日/30日均线的双均线交易策略
 */
import React, { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Tooltip, Input, DatePicker, InputNumber, message, Row, Col, Statistic, Divider } from 'antd'
import { ReloadOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { maStrategyApi } from '../services/api'
import dayjs from 'dayjs'

function MaStrategy() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  
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
      const response = await maStrategyApi.getLatest()
      
      if (response.code === 0) {
        setData(response.data || [])
      } else {
        message.error(response.message || '加载MA策略数据失败')
      }
    } catch (error) {
      console.error('加载MA策略数据失败:', error)
      message.error(error.normalizedMessage || '加载MA策略数据失败')
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
    
    if (!startDate || !endDate) {
      message.warning('请选择开始时间和结束时间')
      return
    }
    
    if (startDate.isAfter(endDate)) {
      message.warning('开始时间不能晚于结束时间')
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
        message.success('回测完成！')
      } else {
        message.error(response.message || '回测失败')
      }
    } catch (error) {
      console.error('回测失败:', error)
      message.error('回测失败: ' + (error.message || '未知错误'))
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
          return <Tag color="success">买入</Tag>
        } else if (record.isSellSignal) {
          return <Tag color="error">卖出</Tag>
        } else {
          return <Tag color="default">观望</Tag>
        }
      },
      filters: [
        { text: '买入', value: 'buy' },
        { text: '卖出', value: 'sell' },
        { text: '观望', value: 'hold' },
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
    <div>
      <h1 className="page-title">📈 双均线策略</h1>
      
      {/* 回测功能区域 */}
      <Card 
        title="📊 双均线策略回测" 
        style={{ marginBottom: 16 }}
        extra={
          <Tooltip title="基于10日均线和30日均线的金叉（买入）和死叉（卖出）信号进行回测">
            <span style={{ color: '#999', fontSize: '12px' }}>仅使用双均线策略</span>
          </Tooltip>
        }
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Row gutter={16}>
            <Col span={6}>
              <div>
                <div style={{ marginBottom: 8 }}>ETF编码：</div>
                <Input
                  placeholder="例如：sh510500"
                  value={etfCode}
                  onChange={(e) => setEtfCode(e.target.value)}
                  style={{ width: '100%' }}
                />
              </div>
            </Col>
            <Col span={6}>
              <div>
                <div style={{ marginBottom: 8 }}>开始时间：</div>
                <DatePicker
                  style={{ width: '100%' }}
                  value={startDate}
                  onChange={setStartDate}
                  format="YYYY-MM-DD"
                  placeholder="选择开始日期"
                />
              </div>
            </Col>
            <Col span={6}>
              <div>
                <div style={{ marginBottom: 8 }}>结束时间：</div>
                <DatePicker
                  style={{ width: '100%' }}
                  value={endDate}
                  onChange={setEndDate}
                  format="YYYY-MM-DD"
                  placeholder="选择结束日期"
                />
              </div>
            </Col>
            <Col span={6}>
              <div>
                <div style={{ marginBottom: 8 }}>初始资金：</div>
                <InputNumber
                  style={{ width: '100%' }}
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
            开始回测
          </Button>
        </Space>
      </Card>
      
      {/* 回测结果展示 */}
      {backtestResult && (
        <>
          {/* 回测统计信息 */}
          <Card title={`回测结果：${backtestResult.etfName} (${backtestResult.etfCode})`} style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={6}>
                <Statistic
                  title="总收益率"
                  value={backtestResult.totalReturnRate}
                  precision={2}
                  suffix="%"
                  valueStyle={{ color: Number(backtestResult.totalReturnRate) >= 0 ? '#3f8600' : '#cf1322' }}
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="年化收益率"
                  value={backtestResult.annualizedReturnRate}
                  precision={2}
                  suffix="%"
                  valueStyle={{ color: Number(backtestResult.annualizedReturnRate) >= 0 ? '#3f8600' : '#cf1322' }}
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="初始资金"
                  value={backtestResult.initialCapital}
                  precision={2}
                  prefix="¥"
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="最终资金"
                  value={backtestResult.finalCapital}
                  precision={2}
                  prefix="¥"
                  valueStyle={{ color: Number(backtestResult.finalCapital) >= Number(backtestResult.initialCapital) ? '#3f8600' : '#cf1322' }}
                />
              </Col>
            </Row>
            <Divider />
            <Row gutter={16}>
              <Col span={8}>
                <Statistic title="交易次数" value={backtestResult.tradeCount} />
              </Col>
              <Col span={8}>
                <Statistic title="买入次数" value={backtestResult.buyCount} valueStyle={{ color: '#52c41a' }} />
              </Col>
              <Col span={8}>
                <Statistic title="卖出次数" value={backtestResult.sellCount} valueStyle={{ color: '#ff4d4f' }} />
              </Col>
            </Row>
          </Card>
          
          {/* 交易记录表格 */}
          <Card title="交易记录" style={{ marginBottom: 16 }}>
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
                      {type === 'BUY' ? '买入' : '卖出'}
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
                  title: '总资产',
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
              rowKey={(record, index) => `${record.date}-${record.type}-${index}`}
              pagination={{
                pageSize: 20,
                showTotal: (total) => `共 ${total} 条交易记录`,
              }}
            />
          </Card>
        </>
      )}

      {/* 统计信息卡片 */}
      <Card style={{ marginBottom: 16 }}>
        <Space size="large">
          <div>
            <span style={{ color: '#999' }}>总计：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {data.length}
            </span>
          </div>
          <div>
            <span style={{ color: '#52c41a' }}>买入信号：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#52c41a', marginLeft: '8px' }}>
              {buySignalCount}
            </span>
          </div>
          <div>
            <span style={{ color: '#ff4d4f' }}>卖出信号：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#ff4d4f', marginLeft: '8px' }}>
              {sellSignalCount}
            </span>
          </div>
          <div>
            <span style={{ color: '#999' }}>观望：</span>
            <span style={{ fontSize: '20px', fontWeight: 'bold', marginLeft: '8px' }}>
              {holdCount}
            </span>
          </div>
        </Space>
      </Card>

      <Card>
        {/* 操作栏 */}
        <Space style={{ marginBottom: 16 }}>
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
        />
      </Card>

      {/* 策略说明 */}
      <Card title="📖 双均线策略说明" style={{ marginTop: 16 }}>
        <div style={{ lineHeight: '2' }}>
          <p>
            <strong>双均线策略</strong>是一种基于趋势跟踪的交易策略，
            通过观察10日均线和30日均线的交叉来判断买卖时机。
          </p>
          
          <h3>技术指标说明：</h3>
          <ul>
            <li><strong>10日均线（MA10）：</strong>短期移动平均线，反映短期趋势</li>
            <li><strong>30日均线（MA30）：</strong>中期移动平均线，反映中期趋势</li>
          </ul>

          <h3>买入信号条件：</h3>
          <div style={{ 
            background: '#f0f9ff', 
            border: '1px solid #bae7ff',
            borderRadius: '4px',
            padding: '16px',
            marginTop: '12px'
          }}>
            <p style={{ margin: 0 }}>
              <strong>条件：</strong>10日均线上穿30日均线（金叉）
            </p>
          </div>

          <h3 style={{ marginTop: '20px' }}>卖出信号条件：</h3>
          <div style={{ 
            background: '#fff1f0', 
            border: '1px solid #ffccc7',
            borderRadius: '4px',
            padding: '16px',
            marginTop: '12px'
          }}>
            <p style={{ margin: 0 }}>
              <strong>条件：</strong>10日均线下穿30日均线（死叉）
            </p>
          </div>

          <h3 style={{ marginTop: '20px' }}>策略优势：</h3>
          <ol>
            <li><strong>趋势确认：</strong>均线交叉能够有效捕捉趋势变化</li>
            <li><strong>简单明了：</strong>策略逻辑清晰，易于理解和执行</li>
            <li><strong>减少噪音：</strong>通过均线平滑价格波动，过滤短期市场噪音</li>
            <li><strong>适用性强：</strong>适用于不同市场环境和时间周期</li>
          </ol>

          <h3 style={{ marginTop: '20px' }}>使用建议：</h3>
          <ul>
            <li>买入信号出现后，建议观察成交量是否配合</li>
            <li>卖出信号出现后，可以考虑止盈或减仓</li>
            <li>在震荡市场中，可能出现频繁的买卖信号，需要结合其他指标综合判断</li>
            <li>可结合其他指标（如RSI、MACD）综合判断，提高策略准确性</li>
          </ul>

          <div style={{ 
            background: '#fff7e6', 
            border: '1px solid #ffd591',
            borderRadius: '4px',
            padding: '12px',
            marginTop: '12px'
          }}>
            <strong>⚠️ 风险提示：</strong>
            <p style={{ margin: '8px 0 0 0' }}>
              任何技术分析策略都有局限性，市场走势受多种因素影响。
              本策略仅供参考，不构成投资建议。请结合自身风险承受能力，谨慎决策。
              投资有风险，入市需谨慎。
            </p>
          </div>
        </div>
      </Card>
    </div>
  )
}

export default MaStrategy
