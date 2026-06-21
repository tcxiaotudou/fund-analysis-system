import React from 'react'
import { Space, Table, Tag } from 'antd'
import { Link } from 'react-router-dom'
import { formatNumber } from '../../utils/formatters'

// ETF、MA 与基金推荐信号表。
function SignalTables({ etfOpportunities, maSignals, fundRecommendations }) {
  // ETF机会表格列。
  const etfColumns = [
    { title: 'ETF代码', dataIndex: 'code', key: 'code', width: 120 },
    { title: 'ETF名称', dataIndex: 'name', key: 'name', width: 160 },
    {
      title: '14日RSI',
      dataIndex: 'currentRsi',
      key: 'currentRsi',
      width: 100,
      render: value => formatNumber(value, 2),
    },
    { title: '区间', dataIndex: 'interval', key: 'interval', width: 160, ellipsis: true },
    { title: '说明', dataIndex: 'message', key: 'message', width: 220, ellipsis: true },
    { title: '数据时间', dataIndex: 'dataTime', key: 'dataTime', width: 160 },
    {
      title: '操作建议',
      key: 'signal',
      width: 110,
      render: (_, record) => <Tag color={record.isBuySignal ? 'green' : 'default'}>{record.isBuySignal ? '关注买入' : '观望'}</Tag>,
    },
  ]

  // MA买卖信号表格列。
  const maColumns = [
    { title: 'ETF代码', dataIndex: 'etfCode', key: 'etfCode', width: 120 },
    { title: 'ETF名称', dataIndex: 'etfName', key: 'etfName', width: 160 },
    {
      title: '当前价',
      dataIndex: 'currentDaily',
      key: 'currentDaily',
      width: 100,
      render: value => formatNumber(value, 3),
    },
    {
      title: 'MA10',
      dataIndex: 'ma10',
      key: 'ma10',
      width: 100,
      render: value => formatNumber(value, 3),
    },
    {
      title: 'MA30',
      dataIndex: 'ma30',
      key: 'ma30',
      width: 100,
      render: value => formatNumber(value, 3),
    },
    {
      title: '信号',
      key: 'signal',
      width: 100,
      render: (_, record) => {
        if (record.isBuySignal) return <Tag color="green">买入</Tag>
        if (record.isSellSignal) return <Tag color="red">卖出</Tag>
        return <Tag>观望</Tag>
      },
    },
    { title: '说明', dataIndex: 'signalDescription', key: 'signalDescription', width: 220, ellipsis: true },
    { title: '数据时间', dataIndex: 'dataTime', key: 'dataTime', width: 160 },
  ]

  // 基金推荐表格列。
  const fundColumns = [
    { title: '基金代码', dataIndex: 'fundCode', key: 'fundCode', width: 120 },
    { title: '基金名称', dataIndex: 'fundName', key: 'fundName', ellipsis: true },
    { title: '推荐条件ID', dataIndex: 'conditionId', key: 'conditionId', width: 130 },
    {
      title: '标签',
      dataIndex: 'tag',
      key: 'tag',
      width: 100,
      render: tag => <Tag color={tag === '已持有' ? 'blue' : tag === '已排除' ? 'red' : 'green'}>{tag || '推荐'}</Tag>,
    },
  ]

  return (
    <section className="dashboard-table-grid">
      <div className="dashboard-panel">
        <div className="dashboard-panel-header">
          <h2>ETF机会</h2>
          <Space><Link to="/rsi-analysis">更多</Link></Space>
        </div>
        <Table columns={etfColumns} dataSource={etfOpportunities} rowKey="code" pagination={false} size="small" scroll={{ x: 1000 }} locale={{ emptyText: '当前没有 ETF 机会' }} />
      </div>
      <div className="dashboard-panel">
        <div className="dashboard-panel-header">
          <h2>MA买卖信号</h2>
          <Space><Link to="/ma-strategy">更多</Link></Space>
        </div>
        <Table columns={maColumns} dataSource={maSignals} rowKey="etfCode" pagination={false} size="small" scroll={{ x: 1140 }} locale={{ emptyText: '当前没有 MA 信号' }} />
      </div>
      <div className="dashboard-panel">
        <div className="dashboard-panel-header">
          <h2>基金推荐</h2>
          <Space><Link to="/fund-recommendation">更多</Link></Space>
        </div>
        <Table columns={fundColumns} dataSource={fundRecommendations} rowKey="fundCode" pagination={false} size="small" scroll={{ x: 680 }} locale={{ emptyText: '当前没有基金推荐摘要' }} />
      </div>
    </section>
  )
}

export default SignalTables
