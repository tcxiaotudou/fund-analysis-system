/**
 * RSI分析页面
 * 展示ETF的RSI技术指标分析
 */
import React, { useState, useEffect, useMemo } from 'react'
import { Alert, Card, Table, Tag, Input, Button, Space, message } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import TerminalPage from '../components/TerminalPage'
import { rsiApi } from '../services/api'

function RsiAnalysis() {
  // 状态管理
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [searchText, setSearchText] = useState('')
  const [pageError, setPageError] = useState(null)

  // 搜索框只过滤 ETF 名称，避免 AntD 单列 filteredValue 触发表格配置告警。
  const filteredData = useMemo(() => {
    const keyword = searchText.trim().toLowerCase()
    if (!keyword) {
      return data
    }
    return data.filter(item => item.name?.toLowerCase().includes(keyword))
  }, [data, searchText])

  /**
   * 加载RSI分析数据
   */
  const loadData = async () => {
    try {
      setLoading(true)
      setPageError(null)
      const response = await rsiApi.getEtfSignals()

      if (response.code !== 0) {
        console.error('ETF 机会服务返回失败:', response.code, response.message)
        throw new Error(`ETF 机会加载失败（服务返回 ${response.code}）`)
      }

      setData(response.data || [])
    } catch (error) {
      console.error('加载RSI数据失败:', error)
      const errorMessage = error.normalizedMessage || error.message || 'ETF 机会加载失败'
      setPageError(errorMessage)
      message.error(errorMessage)
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
      title: 'ETF',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '代码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '观察周期',
      dataIndex: 'period',
      key: 'period',
      render: (val) => `${val}日`,
    },
    {
      title: '当前 RSI',
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
      title: '阶段高点',
      dataIndex: 'highRsi',
      key: 'highRsi',
      render: (val) => val?.toFixed(2),
    },
    {
      title: '阶段低点',
      dataIndex: 'lowRsi',
      key: 'lowRsi',
      render: (val) => val?.toFixed(2),
    },
    {
      title: '当前位置',
      dataIndex: 'interval',
      key: 'interval',
      ellipsis: true,
    },
    {
      title: '低位信号',
      key: 'isBuySignal',
      render: (_, record) => (
        record.isBuySignal ?
          <Tag color="success">值得关注</Tag> :
          <Tag>继续观察</Tag>
      ),
      filters: [
        { text: '值得关注', value: true },
        { text: '继续观察', value: false },
      ],
      onFilter: (value, record) => record.isBuySignal === value,
    },
    {
      title: '信号说明',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
    },
    {
      title: '数据时间',
      dataIndex: 'dataTime',
      key: 'dataTime',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Link to={`/rsi-backtest?etfCode=${encodeURIComponent(record.code)}`}>
          历史模拟
        </Link>
      ),
    },
  ]

  return (
    <TerminalPage
      title="ETF 机会"
      subtitle="用 RSI 观察价格是否进入短期低位"
      status={<span>共 {data.length} 个标的</span>}
    >

      <Card>
        {/* 搜索和操作栏 */}
        <Space className="terminal-toolbar" wrap>
          <Input
            placeholder="搜索 ETF 名称"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            className="terminal-input-short"
            allowClear
          />
          <Button 
            type="primary" 
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            更新机会
          </Button>
        </Space>

        {/* 数据表格 */}
        {pageError ? (
          <Alert
            type="error"
            showIcon
            message="ETF 机会加载失败"
            description={pageError}
            action={(
              <Button size="small" onClick={loadData} loading={loading}>
                重试
              </Button>
            )}
          />
        ) : (
          <Table
            columns={columns}
            dataSource={filteredData}
            rowKey="code"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 个标的`,
            }}
            locale={{ emptyText: loading ? '数据加载中...' : '暂时没有进入低位的 ETF' }}
            scroll={{ x: 1300 }}
          />
        )}
      </Card>

      {/* 使用说明 */}
      <Card title="如何看 RSI">
        <div className="terminal-copy-block">
          <p><strong>RSI（相对强弱指标）</strong>用于观察近期价格上涨和下跌的相对力度，数值介于 0—100。</p>
          <ul>
            <li><strong>RSI &lt; 30：</strong>近期价格相对偏弱，可加入关注</li>
            <li><strong>30 ≤ RSI ≤ 70：</strong>处于常见波动区间</li>
            <li><strong>RSI &gt; 70：</strong>近期价格相对偏强，需留意波动</li>
          </ul>
          <p><strong>页面中的低位信号：</strong></p>
          <ul>
            <li>当前 RSI ≤ 30，或</li>
            <li>阶段高点 ≥ 70、回落低点在 38—43 之间，且当前 RSI ≤ 43</li>
          </ul>
          <p>RSI 只反映一个观察维度，单一指标不能作为买卖依据；历史信号也不代表未来表现。</p>
        </div>
      </Card>
    </TerminalPage>
  )
}

export default RsiAnalysis
