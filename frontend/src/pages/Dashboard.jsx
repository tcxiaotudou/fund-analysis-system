/**
 * 市场概览仪表板页面
 * 展示市场整体情况和投资机会
 */
import React, { useState, useEffect } from 'react'
import { Card, Row, Col, Table, Tag, Spin, Alert, Space } from 'antd'
import { 
  ArrowUpOutlined, 
  ArrowDownOutlined,
  DashboardOutlined,
} from '@ant-design/icons'
import { marketApi, rsiApi, maStrategyApi, portfolioApi } from '../services/api'

function Dashboard() {
  // 状态管理
  const [loading, setLoading] = useState(true)
  const [marketData, setMarketData] = useState(null)
  const [portfolioRsiData, setPortfolioRsiData] = useState(null)
  const [portfolioRsiError, setPortfolioRsiError] = useState(null)
  const [error, setError] = useState(null)

  /**
   * 加载市场概览数据
   */
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true)
        setError(null)
        
        const marketResponse = await marketApi.getOverview()
        if (marketResponse.code === 0) {
          setMarketData(marketResponse.data)
        } else {
          setError(marketResponse.message)
          return
        }

        try {
          const portfolioRsiResponse = await portfolioApi.getPortfolioRsi()
          if (portfolioRsiResponse.code === 0) {
            setPortfolioRsiData(portfolioRsiResponse.data)
            setPortfolioRsiError(null)
          } else {
            setPortfolioRsiData(null)
            setPortfolioRsiError(portfolioRsiResponse.message || '组合 RSI 加载失败')
          }
        } catch (portfolioError) {
          setPortfolioRsiData(null)
          setPortfolioRsiError(portfolioError.normalizedMessage || portfolioError.message || '组合 RSI 加载失败')
        }
      } catch (err) {
        setError('加载数据失败，请检查后端服务是否启动')
        console.error('加载市场数据失败:', err)
      } finally {
        setLoading(false)
      }
    }

    loadData()
    
    // 每分钟刷新一次数据
    const interval = setInterval(loadData, 60000)
    
    return () => clearInterval(interval)
  }, [])

  /**
   * ETF买入机会表格列配置
   */
  const etfColumns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '当前RSI',
      dataIndex: 'currentRsi',
      key: 'currentRsi',
      render: (val) => (
        <Tag color={val < 30 ? 'red' : 'orange'}>
          {val?.toFixed(2)}
        </Tag>
      ),
    },
    {
      title: 'RSI区间',
      dataIndex: 'interval',
      key: 'interval',
    },
    {
      title: '分析信息',
      dataIndex: 'message',
      key: 'message',
    },
    {
      title: '信号',
      key: 'signal',
      render: (_, record) => (
        record.isBuySignal ? 
          <Tag color="success">买入</Tag> : 
          <Tag>观望</Tag>
      ),
    },
  ]

  /**
   * MA策略信号表格列配置
   */
  const maColumns = [
    {
      title: 'ETF名称',
      dataIndex: 'etfName',
      key: 'etfName',
      width: 150,
    },
    {
      title: 'ETF代码',
      dataIndex: 'etfCode',
      key: 'etfCode',
      width: 100,
    },
    {
      title: '当前价格',
      dataIndex: 'currentDaily',
      key: 'currentDaily',
      width: 100,
      render: (val) => <strong>{val?.toFixed(3)}</strong>,
    },
    {
      title: '10日均线',
      dataIndex: 'ma10',
      key: 'ma10',
      width: 100,
      render: (val) => val?.toFixed(3),
    },
    {
      title: '30日均线',
      dataIndex: 'ma30',
      key: 'ma30',
      width: 100,
      render: (val) => val?.toFixed(3),
    },
    {
      title: '信号',
      key: 'signal',
      width: 80,
      render: (_, record) => {
        if (record.isBuySignal) {
          return <Tag color="success">买入</Tag>
        } else if (record.isSellSignal) {
          return <Tag color="error">卖出</Tag>
        } else {
          return <Tag color="default">观望</Tag>
        }
      },
    },
    {
      title: '信号说明',
      dataIndex: 'signalDescription',
      key: 'signalDescription',
      width: 200,
    },
  ]

  // 加载中状态
  if (loading) {
    return (
      <div className="loading-container">
        <Spin size="large" tip="加载市场数据中..." />
      </div>
    )
  }

  // 错误状态
  if (error) {
    return (
      <Alert
        message="数据加载失败"
        description={error}
        type="error"
        showIcon
      />
    )
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h1 className="page-title" style={{ margin: 0 }}>
          <DashboardOutlined /> 市场概览
        </h1>
        {marketData?.updateTime && (
          <span style={{ color: '#999', fontSize: '14px' }}>
            数据更新时间: {marketData.updateTime}
          </span>
        )}
      </div>

      {/* 核心指标卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
            <div className="stat-card-title">14日RSI</div>
            <div className="stat-card-value">{marketData?.rsi14 || 'N/A'}</div>
            <div className="stat-card-desc">低于30买买买</div>
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card" style={{ background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' }}>
            <div className="stat-card-title">90日RSI</div>
            <div className="stat-card-value">{marketData?.rsi90 || 'N/A'}</div>
            <div className="stat-card-desc">57点和70点再平衡</div>
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card" style={{ background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' }}>
            <div className="stat-card-title">股债再平衡建议</div>
            <div className="stat-card-value" style={{ fontSize: '24px' }}>
              {marketData?.balanceSuggestion || 'N/A'}
            </div>
            <div className="stat-card-desc">基于90日RSI</div>
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card" style={{ background: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' }}>
            <div className="stat-card-title">5年均线偏离度</div>
            <div className="stat-card-value">
              {(() => {
                const ma5yStr = marketData?.ma5yDeviation || 'N/A'
                if (ma5yStr === 'N/A') return ma5yStr
                // 解析格式: 「2025-11-21」 17.93%
                const match = ma5yStr.match(/「(.+?)」\s*(.+)/)
                if (match) {
                  const date = match[1]
                  const value = match[2]
                  return (
                    <>
                      <div style={{ fontSize: '32px' }}>{value}</div>
                      <div style={{ fontSize: '12px', marginTop: '4px', opacity: 0.8 }}>
                        数据日期: {date}
                      </div>
                    </>
                  )
                }
                return ma5yStr
              })()}
            </div>
            <div className="stat-card-desc">1250日均线</div>
          </Card>
        </Col>
      </Row>

      {/* 市场数据 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="📊 市场数据">
            <div style={{ lineHeight: '2' }}>
              <p><strong>30年国债14日RSI：</strong>{marketData?.bondRsi14 || 'N/A'}</p>
              <p><strong>沪深300风险溢价：</strong>{marketData?.riskPremium || 'N/A'}</p>
            </div>
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card title="💼 养老基金组合RSI">
            {portfolioRsiError && (
              <Alert
                type="warning"
                showIcon
                message="组合 RSI 加载失败"
                description={portfolioRsiError}
                style={{ marginBottom: 12 }}
              />
            )}
            <div style={{ lineHeight: '2' }}>
              <p><strong>14日RSI：</strong>{portfolioRsiData?.rsi14 != null ? portfolioRsiData.rsi14.toFixed(2) : 'N/A'}</p>
              <p><strong>90日RSI：</strong>{portfolioRsiData?.rsi90 != null ? portfolioRsiData.rsi90.toFixed(2) : 'N/A'}</p>
              <p><strong>14周RSI：</strong>{portfolioRsiData?.weeklyRsi14 != null ? portfolioRsiData.weeklyRsi14.toFixed(2) : 'N/A'}</p>
            </div>
            {portfolioRsiData?.updateTime && (
              <div style={{ marginTop: 8, fontSize: '12px', color: '#999' }}>
                更新时间: {new Date(portfolioRsiData.updateTime).toLocaleString('zh-CN')}
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* 场内ETF投资机会 */}
      <Card 
        title="🎯 场内ETF投资机会" 
        style={{ marginBottom: 24 }}
        extra={<Tag color="red">买入信号</Tag>}
      >
        <Table
          columns={etfColumns}
          dataSource={marketData?.etfOpportunities || []}
          rowKey="code"
          pagination={false}
          scroll={{ x: 800 }}
        />
        {(!marketData?.etfOpportunities || marketData.etfOpportunities.length === 0) && (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            暂无符合条件的ETF投资机会
          </div>
        )}
      </Card>

      {/* 双均线+布林带策略 */}
      <Card 
        title="📈 双均线+布林带策略"
        extra={
          <Space>
            <span style={{ color: '#999', fontSize: '14px' }}>
              {marketData?.maSignals?.filter(s => s.isBuySignal).length || 0} 买入 / 
              {marketData?.maSignals?.filter(s => s.isSellSignal).length || 0} 卖出
            </span>
          </Space>
        }
      >
        <Table
          columns={maColumns}
          dataSource={marketData?.maSignals || []}
          rowKey="etfCode"
          pagination={false}
          scroll={{ x: 800 }}
        />
        {(!marketData?.maSignals || marketData.maSignals.length === 0) && (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            暂无符合条件的MA策略信号
          </div>
        )}
      </Card>
    </div>
  )
}

export default Dashboard
