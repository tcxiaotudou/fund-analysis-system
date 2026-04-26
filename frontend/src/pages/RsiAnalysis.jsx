/**
 * RSI分析页面
 * 展示ETF的RSI技术指标分析
 */
import React, { useState, useEffect } from 'react'
import { Card, Table, Tag, Input, Button, Space, Spin } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { rsiApi } from '../services/api'

function RsiAnalysis() {
  // 状态管理
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [searchText, setSearchText] = useState('')

  /**
   * 加载RSI分析数据
   */
  const loadData = async () => {
    try {
      setLoading(true)
      const response = await rsiApi.getEtfSignals()
      
      if (response.code === 0) {
        setData(response.data || [])
      }
    } catch (error) {
      console.error('加载RSI数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  /**
   * 表格列配置
   */
  const columns = [
    {
      title: 'ETF名称',
      dataIndex: 'name',
      key: 'name',
      filteredValue: searchText ? [searchText] : null,
      onFilter: (value, record) => 
        record.name?.toLowerCase().includes(value.toLowerCase()),
    },
    {
      title: 'ETF代码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: 'RSI周期',
      dataIndex: 'period',
      key: 'period',
      render: (val) => `${val}日`,
    },
    {
      title: '当前RSI',
      dataIndex: 'currentRsi',
      key: 'currentRsi',
      render: (val) => {
        let color = 'blue'
        if (val < 30) color = 'red'
        else if (val < 45) color = 'orange'
        else if (val > 70) color = 'green'
        
        return <Tag color={color}>{val?.toFixed(2)}</Tag>
      },
      sorter: (a, b) => a.currentRsi - b.currentRsi,
    },
    {
      title: 'RSI最高值',
      dataIndex: 'highRsi',
      key: 'highRsi',
      render: (val) => val?.toFixed(2),
    },
    {
      title: 'RSI最低值',
      dataIndex: 'lowRsi',
      key: 'lowRsi',
      render: (val) => val?.toFixed(2),
    },
    {
      title: 'RSI区间',
      dataIndex: 'interval',
      key: 'interval',
      ellipsis: true,
    },
    {
      title: '买入信号',
      key: 'isBuySignal',
      render: (_, record) => (
        record.isBuySignal ? 
          <Tag color="success">是</Tag> : 
          <Tag>否</Tag>
      ),
      filters: [
        { text: '是', value: true },
        { text: '否', value: false },
      ],
      onFilter: (value, record) => record.isBuySignal === value,
    },
    {
      title: '分析信息',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
    },
    {
      title: '数据时间',
      dataIndex: 'dataTime',
      key: 'dataTime',
    },
  ]

  return (
    <div>
      <h1 className="page-title">📊 RSI技术指标分析</h1>

      <Card>
        {/* 搜索和操作栏 */}
        <Space style={{ marginBottom: 16 }}>
          <Input
            placeholder="搜索ETF名称"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 200 }}
            allowClear
          />
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
          rowKey="code"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 使用说明 */}
      <Card title="📖 RSI指标说明" style={{ marginTop: 16 }}>
        <div style={{ lineHeight: '2' }}>
          <p><strong>RSI（相对强弱指标）</strong>是一种动量指标，用于衡量价格变动的速度和幅度。</p>
          <ul>
            <li>RSI值介于0-100之间</li>
            <li><strong>RSI &lt; 30：</strong>超卖区域，可能是买入机会</li>
            <li><strong>30 ≤ RSI ≤ 70：</strong>正常区域</li>
            <li><strong>RSI &gt; 70：</strong>超买区域，可能是卖出机会</li>
          </ul>
          <p><strong>买入信号条件：</strong></p>
          <ul>
            <li>RSI ≤ 30，或</li>
            <li>RSI最高值 ≥ 70 且 最高点到当前的最低值在38-43之间 且 当前RSI ≤ 43</li>
          </ul>
        </div>
      </Card>
    </div>
  )
}

export default RsiAnalysis

