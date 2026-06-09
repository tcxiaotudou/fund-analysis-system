/**
 * 基金组合页面
 * 展示持有的基金列表和组合 RSI 指标
 */
import React, { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Modal, Input, message, Statistic, Row, Col, Spin, InputNumber, Tooltip } from 'antd'
import { ReloadOutlined, PlusOutlined, DeleteOutlined, LineChartOutlined, SaveOutlined, EditOutlined } from '@ant-design/icons'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, Legend, ResponsiveContainer, ReferenceLine } from 'recharts'
import { portfolioApi, fundApi } from '../services/api'

function FundPortfolio() {
  const [loading, setLoading] = useState(false)
  const [holdingFunds, setHoldingFunds] = useState([])
  const [rsiData, setRsiData] = useState(null)
  const [rsiLoading, setRsiLoading] = useState(false)
  const [rsiHistoryData, setRsiHistoryData] = useState([])
  const [rsiHistoryLoading, setRsiHistoryLoading] = useState(false)
  const [addModalVisible, setAddModalVisible] = useState(false)
  const [fundCode, setFundCode] = useState('')
  const [fundName, setFundName] = useState('')
  const [editingWeights, setEditingWeights] = useState(false)
  const [weights, setWeights] = useState({})
  const [savingWeights, setSavingWeights] = useState(false)

  /**
   * 加载持有基金列表
   */
  const loadHoldingFunds = async () => {
    try {
      setLoading(true)
      const response = await portfolioApi.getHoldingFunds()
      
      if (response.code === 0) {
        const funds = response.data || []
        setHoldingFunds(funds)
        
        // 初始化权重数据
        const initialWeights = {}
        funds.forEach(fund => {
          initialWeights[fund.fundCode] = fund.portfolioWeight || 0
        })
        setWeights(initialWeights)
      } else {
        message.error(response.message || '加载持有基金失败')
      }
    } catch (error) {
      console.error('加载持有基金失败:', error)
      message.error('加载持有基金失败')
    } finally {
      setLoading(false)
    }
  }

  /**
   * 加载组合 RSI 数据
   */
  const loadPortfolioRsi = async () => {
    try {
      setRsiLoading(true)
      const response = await portfolioApi.getPortfolioRsi()
      
      if (response.code === 0) {
        setRsiData(response.data)
      } else {
        message.error(response.message || '加载组合 RSI 失败')
      }
    } catch (error) {
      console.error('加载组合 RSI 失败:', error)
      message.error('加载组合 RSI 失败')
    } finally {
      setRsiLoading(false)
    }
  }

  /**
   * 加载组合 RSI 历史数据
   */
  const loadPortfolioRsiHistory = async () => {
    try {
      setRsiHistoryLoading(true)
      const response = await portfolioApi.getPortfolioRsiHistory(60)
      
      if (response.code === 0) {
        // 按日期排序
        const sortedData = (response.data || []).sort((a, b) => {
          return new Date(a.date) - new Date(b.date)
        })
        setRsiHistoryData(sortedData)
      } else {
        message.error(response.message || '加载 RSI 历史数据失败')
      }
    } catch (error) {
      console.error('加载 RSI 历史数据失败:', error)
      message.error('加载 RSI 历史数据失败')
    } finally {
      setRsiHistoryLoading(false)
    }
  }

  /**
   * 刷新所有数据
   */
  const refreshAll = async () => {
    await Promise.all([loadHoldingFunds(), loadPortfolioRsi(), loadPortfolioRsiHistory()])
  }

  /**
   * 取消持有基金
   */
  const handleRemoveHolding = async (fundCode) => {
    try {
      const response = await fundApi.updateHoldingStatus(fundCode, 0)
      
      if (response.code === 0) {
        message.success('已取消持有')
        await refreshAll()
      } else {
        message.error(response.message || '操作失败')
      }
    } catch (error) {
      console.error('取消持有失败:', error)
      message.error('操作失败')
    }
  }

  /**
   * 显示添加基金对话框
   */
  const showAddModal = () => {
    setFundCode('')
    setFundName('')
    setAddModalVisible(true)
  }

  /**
   * 添加基金
   */
  const handleAddFund = async () => {
    if (!fundCode.trim()) {
      message.warning('请输入基金代码')
      return
    }
    if (!fundName.trim()) {
      message.warning('请输入基金名称')
      return
    }

    try {
      const response = await fundApi.addHoldingFund(fundCode.trim(), fundName.trim())
      
      if (response.code === 0) {
        message.success('基金已添加')
        setAddModalVisible(false)
        await refreshAll()
      } else {
        message.error(response.message || '添加失败')
      }
    } catch (error) {
      console.error('添加基金失败:', error)
      message.error('添加失败')
    }
  }

  /**
   * 开始编辑权重
   */
  const startEditWeights = () => {
    setEditingWeights(true)
  }

  /**
   * 取消编辑权重
   */
  const cancelEditWeights = () => {
    // 恢复原始权重
    const initialWeights = {}
    holdingFunds.forEach(fund => {
      initialWeights[fund.fundCode] = fund.portfolioWeight || 0
    })
    setWeights(initialWeights)
    setEditingWeights(false)
  }

  /**
   * 权重变化处理
   */
  const handleWeightChange = (fundCode, value) => {
    setWeights({
      ...weights,
      [fundCode]: value || 0
    })
  }

  /**
   * 计算权重总和
   */
  const getTotalWeight = () => {
    return Object.values(weights).reduce((sum, weight) => sum + (parseFloat(weight) || 0), 0)
  }

  /**
   * 保存权重
   */
  const saveWeights = async () => {
    try {
      // 验证权重总和
      const totalWeight = getTotalWeight()
      if (Math.abs(totalWeight - 100) > 0.01) {
        message.error(`权重总和必须等于 100%，当前为 ${totalWeight.toFixed(2)}%`)
        return
      }

      // 验证每个权重的小数位数
      for (const [fundCode, weight] of Object.entries(weights)) {
        const weightStr = weight.toString()
        if (weightStr.includes('.')) {
          const decimalPlaces = weightStr.split('.')[1].length
          if (decimalPlaces > 2) {
            message.error('权重最多保留2位小数')
            return
          }
        }
      }

      setSavingWeights(true)
      const response = await portfolioApi.updateWeights(weights)
      
      if (response.code === 0) {
        message.success('权重保存成功')
        setEditingWeights(false)
        await refreshAll()
      } else {
        message.error(response.message || '权重保存失败')
      }
    } catch (error) {
      console.error('保存权重失败:', error)
      message.error('保存权重失败')
    } finally {
      setSavingWeights(false)
    }
  }

  useEffect(() => {
    refreshAll()
  }, [])

  /**
   * 表格列配置
   */
  const columns = [
    {
      title: '序号',
      key: 'index',
      width: 70,
      render: (_, record, index) => index + 1,
    },
    {
      title: '基金名称',
      dataIndex: 'fundName',
      key: 'fundName',
      width: 200,
      render: (text, record) => (
        <div>
          <div>{text}</div>
          <div style={{ fontSize: '12px', color: '#999' }}>{record.fundCode}</div>
        </div>
      ),
    },
    {
      title: () => (
        <span>
          权重 (%)
          {editingWeights && (
            <Tooltip title={`总和: ${getTotalWeight().toFixed(2)}%`}>
              <span style={{ 
                marginLeft: 8, 
                color: Math.abs(getTotalWeight() - 100) < 0.01 ? '#52c41a' : '#ff4d4f',
                fontWeight: 'bold'
              }}>
                [{getTotalWeight().toFixed(2)}%]
              </span>
            </Tooltip>
          )}
        </span>
      ),
      dataIndex: 'portfolioWeight',
      key: 'portfolioWeight',
      width: 150,
      render: (text, record) => {
        if (editingWeights) {
          return (
            <InputNumber
              min={0}
              max={100}
              precision={2}
              step={0.01}
              value={weights[record.fundCode] || 0}
              onChange={(value) => handleWeightChange(record.fundCode, value)}
              style={{ width: '100%' }}
              placeholder="0.00"
            />
          )
        }
        return (
          <span style={{ fontWeight: 'bold' }}>
            {text != null ? text.toFixed(2) : '0.00'}%
          </span>
        )
      },
    },
    {
      title: '基金经理',
      dataIndex: 'managerName',
      key: 'managerName',
      width: 100,
      render: (text) => text == null || text === '' ? '-' : text,
    },
    {
      title: '规模',
      dataIndex: 'scale',
      key: 'scale',
      width: 100,
      render: (text) => text == null || text === '' ? '-' : text,
    },
    {
      title: '卡玛比率排名',
      dataIndex: 'calmarRank',
      key: 'calmarRank',
      width: 130,
      render: (val) => {
        if (val == null || val === '') return '-'
        let color = '#000'
        if (val <= 10) color = '#cf1322'
        else if (val <= 30) color = '#fa8c16'
        else if (val <= 50) color = '#1890ff'
        return (
          <Tooltip title="卡玛比率排名越小越好">
            <strong style={{ color }}>{val}</strong>
          </Tooltip>
        )
      },
    },
    {
      title: '夏普比率排名',
      dataIndex: 'sharpeRank',
      key: 'sharpeRank',
      width: 130,
      render: (val) => {
        if (val == null || val === '') return '-'
        let color = '#000'
        if (val <= 10) color = '#cf1322'
        else if (val <= 30) color = '#fa8c16'
        else if (val <= 50) color = '#1890ff'
        return (
          <Tooltip title="夏普比率排名越小越好">
            <strong style={{ color }}>{val}</strong>
          </Tooltip>
        )
      },
    },
    {
      title: '最大回撤',
      dataIndex: 'maxDrawdown',
      key: 'maxDrawdown',
      width: 110,
      render: (val) => {
        if (val == null || val === '') return '-'
        const num = parseFloat(val)
        let color = '#000'
        if (num < -30) color = '#cf1322'
        else if (num < -20) color = '#fa8c16'
        else if (num < -10) color = '#1890ff'
        else color = '#52c41a'
        return (
          <Tooltip title="近5年最大回撤">
            <span style={{ color, fontWeight: 'bold' }}>{val}%</span>
          </Tooltip>
        )
      },
    },
    {
      title: '今年收益率',
      dataIndex: 'yearToDateReturn',
      key: 'yearToDateReturn',
      width: 120,
      render: (val) => {
        if (val == null || val === '') return '-'
        const num = parseFloat(val)
        const color = num > 0 ? '#cf1322' : '#3f8600'
        return <span style={{ color }}>{val}</span>
      },
    },
    {
      title: '近5年年化',
      dataIndex: 'fiveYearReturn',
      key: 'fiveYearReturn',
      width: 120,
      render: (val) => {
        if (val == null || val === '') return '-'
        const color = val > 10 ? '#cf1322' : val > 5 ? '#fa8c16' : '#000'
        return <strong style={{ color }}>{val.toFixed(2)}%</strong>
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180,
      render: (val) => {
        if (val == null || val === '') return '-'
        const date = new Date(val)
        return date.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button 
          type="link" 
          size="small"
          danger
          icon={<DeleteOutlined />}
          onClick={() => {
            Modal.confirm({
              title: '确认取消持有',
              content: `确定要取消持有 ${record.fundName}？`,
              onOk: () => handleRemoveHolding(record.fundCode),
            })
          }}
        >
          取消持有
        </Button>
      ),
    },
  ]

  /**
   * 获取 RSI 颜色
   */
  const getRsiColor = (value) => {
    if (value < 30) return '#52c41a' // 绿色 - 超卖
    if (value < 50) return '#1890ff' // 蓝色 - 中性偏低
    if (value < 70) return '#fa8c16' // 橙色 - 中性偏高
    return '#cf1322' // 红色 - 超买
  }

  /**
   * 获取 RSI 建议
   */
  const getRsiSuggestion = (rsi14, rsi90) => {
    if (rsi14 < 30) return { text: '超卖，可考虑买入', color: '#52c41a' }
    if (rsi14 > 70) return { text: '超买，建议谨慎', color: '#cf1322' }
    if (rsi90 < 43) return { text: '中长期超卖', color: '#1890ff' }
    if (rsi90 > 57) return { text: '中长期超买', color: '#fa8c16' }
    return { text: '中性区间', color: '#666' }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h1 className="page-title" style={{ margin: 0 }}>💼 基金组合</h1>
      </div>

      {/* 组合 RSI 指标 */}
      <Card 
        title={
          <span>
            <LineChartOutlined style={{ marginRight: 8 }} />
            组合 RSI 指标
          </span>
        }
        extra={
          <Button 
            type="link" 
            icon={<ReloadOutlined spin={rsiLoading} />}
            onClick={loadPortfolioRsi}
            loading={rsiLoading}
          >
            刷新 RSI
          </Button>
        }
        style={{ marginBottom: 16 }}
      >
        {rsiLoading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin tip="计算中..." />
          </div>
        ) : rsiData ? (
          <>
            <Row gutter={16}>
              <Col xs={24} sm={8}>
                <Statistic
                  title="14日 RSI"
                  value={rsiData.rsi14 != null ? rsiData.rsi14.toFixed(2) : 'N/A'}
                  valueStyle={{ color: rsiData.rsi14 != null ? getRsiColor(rsiData.rsi14) : '#000' }}
                  suffix={rsiData.rsi14 != null ? '' : ''}
                />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic
                  title="90日 RSI"
                  value={rsiData.rsi90 != null ? rsiData.rsi90.toFixed(2) : 'N/A'}
                  valueStyle={{ color: rsiData.rsi90 != null ? getRsiColor(rsiData.rsi90) : '#000' }}
                  suffix={rsiData.rsi90 != null ? '' : ''}
                />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic
                  title="14周 RSI"
                  value={rsiData.weeklyRsi14 != null ? rsiData.weeklyRsi14.toFixed(2) : 'N/A'}
                  valueStyle={{ color: rsiData.weeklyRsi14 != null ? getRsiColor(rsiData.weeklyRsi14) : '#000' }}
                  suffix={rsiData.weeklyRsi14 != null ? '' : ''}
                />
              </Col>
            </Row>
            {rsiData.rsi14 != null && rsiData.rsi90 != null && (
              <div style={{ marginTop: 16, padding: '12px', background: '#f0f2f5', borderRadius: '4px' }}>
                <strong>建议：</strong>
                <span style={{ color: getRsiSuggestion(rsiData.rsi14, rsiData.rsi90).color, marginLeft: 8 }}>
                  {getRsiSuggestion(rsiData.rsi14, rsiData.rsi90).text}
                </span>
              </div>
            )}
            <div style={{ marginTop: 8, fontSize: '12px', color: '#999' }}>
              基金数量: {rsiData.fundCount || 0} | 
              更新时间: {rsiData.updateTime ? new Date(rsiData.updateTime).toLocaleString('zh-CN') : '-'}
            </div>
          </>
        ) : (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
            暂无数据，请先添加持有基金
          </div>
        )}
      </Card>

      {/* 组合 RSI 历史趋势 */}
      <Card 
        title={
          <span>
            <LineChartOutlined style={{ marginRight: 8 }} />
            组合 14日 RSI 历史趋势（最近60个交易日）
          </span>
        }
        extra={
          <Button 
            type="link" 
            icon={<ReloadOutlined spin={rsiHistoryLoading} />}
            onClick={loadPortfolioRsiHistory}
            loading={rsiHistoryLoading}
          >
            刷新图表
          </Button>
        }
        style={{ marginBottom: 16 }}
      >
        {rsiHistoryLoading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin tip="加载中..." />
          </div>
        ) : rsiHistoryData.length > 0 ? (
          <div style={{ width: '100%', height: 400 }}>
            <ResponsiveContainer>
              <LineChart
                data={rsiHistoryData}
                margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="date" 
                  tick={{ fontSize: 12 }}
                  angle={-45}
                  textAnchor="end"
                  height={80}
                />
                <YAxis 
                  domain={[0, 100]}
                  ticks={[0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100]}
                  tick={{ fontSize: 12 }}
                />
                <RechartsTooltip 
                  formatter={(value) => [value.toFixed(2), 'RSI']}
                  labelStyle={{ color: '#000' }}
                />
                <Legend />
                
                {/* 关键水平线 - 虚线标注 */}
                <ReferenceLine 
                  y={30} 
                  stroke="#52c41a" 
                  strokeDasharray="5 5"
                  label={{ value: '超卖 (30)', position: 'right', fill: '#52c41a', fontSize: 12 }}
                />
                <ReferenceLine 
                  y={40} 
                  stroke="#1890ff" 
                  strokeDasharray="5 5"
                  label={{ value: '低位 (40)', position: 'right', fill: '#1890ff', fontSize: 12 }}
                />
                <ReferenceLine 
                  y={60} 
                  stroke="#fa8c16" 
                  strokeDasharray="5 5"
                  label={{ value: '高位 (60)', position: 'right', fill: '#fa8c16', fontSize: 12 }}
                />
                <ReferenceLine 
                  y={70} 
                  stroke="#cf1322" 
                  strokeDasharray="5 5"
                  label={{ value: '超买 (70)', position: 'right', fill: '#cf1322', fontSize: 12 }}
                />
                
                {/* RSI 曲线 */}
                <Line 
                  type="monotone" 
                  dataKey="rsi" 
                  stroke="#1890ff" 
                  strokeWidth={2}
                  dot={false}
                  name="14日 RSI"
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
            暂无历史数据，请先添加持有基金
          </div>
        )}
      </Card>

      {/* 持有基金列表 */}
      <Card>
        {/* 操作栏 */}
        <Space style={{ marginBottom: 16 }}>
          <Button 
            type="primary" 
            icon={<ReloadOutlined />}
            onClick={refreshAll}
            loading={loading}
            disabled={editingWeights}
          >
            刷新数据
          </Button>
          <Button 
            type="default" 
            icon={<PlusOutlined />}
            onClick={showAddModal}
            disabled={editingWeights}
          >
            添加基金
          </Button>
          {!editingWeights ? (
            <Button 
              type="default" 
              icon={<EditOutlined />}
              onClick={startEditWeights}
              disabled={holdingFunds.length === 0}
            >
              编辑权重
            </Button>
          ) : (
            <>
              <Button 
                type="primary" 
                icon={<SaveOutlined />}
                onClick={saveWeights}
                loading={savingWeights}
              >
                保存权重
              </Button>
              <Button 
                onClick={cancelEditWeights}
                disabled={savingWeights}
              >
                取消
              </Button>
            </>
          )}
        </Space>

        {/* 数据表格 */}
        <Table
          columns={columns}
          dataSource={holdingFunds}
          rowKey="fundCode"
          loading={loading}
          pagination={{
            pageSize: 20,
            showTotal: (total) => `共 ${total} 只基金`,
          }}
          scroll={{ x: 1600 }}
        />
      </Card>

      {/* 说明 */}
      <Card title="📖 使用说明" style={{ marginTop: 16 }}>
        <div style={{ lineHeight: '2' }}>
          <h3>基金组合功能：</h3>
          <ul>
            <li><strong>组合 RSI：</strong>基于所有持有基金的加权组合计算 RSI 指标</li>
            <li><strong>14日 RSI：</strong>短期指标，低于30超卖，高于70超买</li>
            <li><strong>90日 RSI：</strong>中长期指标，建议在43和57之间做再平衡</li>
            <li><strong>14周 RSI：</strong>周线指标，反映中期趋势</li>
          </ul>

          <h3 style={{ marginTop: '20px' }}>关键指标说明：</h3>
          <ul>
            <li><strong>卡玛比率排名：</strong>衡量单位回撤下的收益，排名越小越好</li>
            <li><strong>夏普比率排名：</strong>衡量风险调整后的收益，排名越小越好</li>
            <li><strong>最大回撤：</strong>近5年内的最大跌幅，反映风险控制能力</li>
          </ul>

          <h3 style={{ marginTop: '20px' }}>操作说明：</h3>
          <ul>
            <li><strong>添加基金：</strong>在"基金推荐"页面标记为持有，或手动添加基金代码和名称</li>
            <li><strong>编辑权重：</strong>点击"编辑权重"按钮，修改每只基金在组合中的权重</li>
            <li><strong>权重规则：</strong>所有基金的权重总和必须等于 100%，最多保留小数点后 2 位</li>
            <li><strong>权重计算：</strong>如果未设置权重或权重总和不为 100%，系统将使用等权重计算</li>
            <li><strong>取消持有：</strong>从组合中移除基金（不会删除基金数据）</li>
            <li><strong>刷新数据：</strong>重新加载基金列表和计算组合 RSI</li>
          </ul>

          <div style={{ 
            background: '#fff7e6', 
            border: '1px solid #ffd591',
            borderRadius: '4px',
            padding: '12px',
            marginTop: '12px'
          }}>
            <strong>⚠️ 提示：</strong>
            <p style={{ margin: '8px 0 0 0' }}>
              组合 RSI 仅作为参考指标，投资决策请结合市场环境和个人风险承受能力。
            </p>
          </div>
        </div>
      </Card>

      {/* 添加基金对话框 */}
      <Modal
        title="添加持有基金"
        open={addModalVisible}
        onOk={handleAddFund}
        onCancel={() => setAddModalVisible(false)}
        okText="添加"
        cancelText="取消"
      >
        <div style={{ marginBottom: 16 }}>
          <p style={{ marginBottom: 8 }}>
            <strong style={{ color: 'red' }}>*</strong> 基金代码：
          </p>
          <Input
            placeholder="请输入基金代码，例如：004475"
            value={fundCode}
            onChange={(e) => setFundCode(e.target.value)}
            maxLength={10}
          />
        </div>
        <div>
          <p style={{ marginBottom: 8 }}>
            <strong style={{ color: 'red' }}>*</strong> 基金名称：
          </p>
          <Input
            placeholder="请输入基金名称，例如：华泰柏瑞富利混合A"
            value={fundName}
            onChange={(e) => setFundName(e.target.value)}
            maxLength={100}
          />
        </div>
      </Modal>
    </div>
  )
}

export default FundPortfolio
