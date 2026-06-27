import React from 'react'
import { Space, Table, Tag } from 'antd'
import { Link } from 'react-router-dom'
import {
  ETF_OPPORTUNITY_COLUMN_DEFINITIONS,
  FUND_RECOMMENDATION_COLUMN_DEFINITIONS,
  MA_SIGNAL_COLUMN_DEFINITIONS,
} from '../../utils/dashboardSignalTableColumns'
import { formatNumber } from '../../utils/formatters'

// 表格列渲染器映射。
const TABLE_COLUMN_RENDERERS = {
  rsiValue: value => formatNumber(value, 2),
  priceValue: value => formatNumber(value, 3),
  etfSignal: (_, record) => <Tag color={record.isBuySignal ? 'green' : 'default'}>{record.isBuySignal ? '关注买入' : '观望'}</Tag>,
  maSignal: (_, record) => {
    if (record.isBuySignal) return <Tag color="green">买入</Tag>
    if (record.isSellSignal) return <Tag color="red">卖出</Tag>
    return <Tag>观望</Tag>
  },
  fundTag: tag => <Tag color={tag === '已持有' ? 'blue' : tag === '已排除' ? 'red' : 'green'}>{tag || '推荐'}</Tag>,
}

// 根据列定义挂载需要的渲染函数。
const buildTableColumns = columnDefinitions => columnDefinitions.map(({ renderKey, ...column }) => {
  if (!renderKey) {
    return column
  }
  return {
    ...column,
    render: TABLE_COLUMN_RENDERERS[renderKey],
  }
})

// ETF机会表格列。
const ETF_COLUMNS = buildTableColumns(ETF_OPPORTUNITY_COLUMN_DEFINITIONS)

// MA买卖信号表格列。
const MA_COLUMNS = buildTableColumns(MA_SIGNAL_COLUMN_DEFINITIONS)

// 基金推荐表格列。
const FUND_COLUMNS = buildTableColumns(FUND_RECOMMENDATION_COLUMN_DEFINITIONS)

// ETF、MA 与基金推荐信号表。
function SignalTables({ etfOpportunities, maSignals, fundRecommendations }) {
  return (
    <section className="dashboard-table-grid">
      <div className="dashboard-panel">
        <div className="dashboard-panel-header">
          <h2 className="panel-title-with-count">
            ETF机会
            <Tag className="panel-count-tag" color="blue">{etfOpportunities.length}</Tag>
          </h2>
          <Space><Link to="/rsi-analysis">更多</Link></Space>
        </div>
        <Table columns={ETF_COLUMNS} dataSource={etfOpportunities} rowKey="code" pagination={false} size="small" scroll={{ x: 680 }} locale={{ emptyText: '当前没有 ETF 机会' }} />
      </div>
      <div className="dashboard-table-stack">
        <div className="dashboard-panel">
          <div className="dashboard-panel-header">
            <h2 className="panel-title-with-count">
              MA买卖信号
              <Tag className="panel-count-tag" color="purple">{maSignals.length}</Tag>
            </h2>
            <Space><Link to="/ma-strategy">更多</Link></Space>
          </div>
          <Table columns={MA_COLUMNS} dataSource={maSignals} rowKey="etfCode" pagination={false} size="small" scroll={{ x: 860 }} locale={{ emptyText: '当前没有 MA 信号' }} />
        </div>
        <div className="dashboard-panel">
          <div className="dashboard-panel-header">
            <h2 className="panel-title-with-count">
              基金推荐
              <Tag className="panel-count-tag" color="green">{fundRecommendations.length}</Tag>
            </h2>
            <Space><Link to="/fund-recommendation">更多</Link></Space>
          </div>
          <Table columns={FUND_COLUMNS} dataSource={fundRecommendations} rowKey="fundCode" pagination={false} size="small" scroll={{ x: 680 }} locale={{ emptyText: '当前没有基金推荐摘要' }} />
        </div>
      </div>
    </section>
  )
}

export default SignalTables
