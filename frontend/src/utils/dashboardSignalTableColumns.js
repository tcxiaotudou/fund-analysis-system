// ETF机会表格列定义。
export const ETF_OPPORTUNITY_COLUMN_DEFINITIONS = [
  { title: 'ETF代码', dataIndex: 'code', key: 'code', width: 120 },
  { title: 'ETF名称', dataIndex: 'name', key: 'name', width: 160 },
  { title: '14日RSI', dataIndex: 'currentRsi', key: 'currentRsi', width: 100, renderKey: 'rsiValue' },
  { title: '数据时间', dataIndex: 'dataTime', key: 'dataTime', width: 160 },
  { title: '操作建议', key: 'signal', width: 110, renderKey: 'etfSignal' },
]

// MA买卖信号表格列定义。
export const MA_SIGNAL_COLUMN_DEFINITIONS = [
  { title: 'ETF代码', dataIndex: 'etfCode', key: 'etfCode', width: 120 },
  { title: 'ETF名称', dataIndex: 'etfName', key: 'etfName', width: 160 },
  { title: '当前价', dataIndex: 'currentDaily', key: 'currentDaily', width: 100, renderKey: 'priceValue' },
  { title: '信号', key: 'signal', width: 100, renderKey: 'maSignal' },
  { title: '说明', dataIndex: 'signalDescription', key: 'signalDescription', width: 220, ellipsis: true },
  { title: '数据时间', dataIndex: 'dataTime', key: 'dataTime', width: 160 },
]

// 基金推荐表格列定义。
export const FUND_RECOMMENDATION_COLUMN_DEFINITIONS = [
  { title: '基金代码', dataIndex: 'fundCode', key: 'fundCode', width: 120 },
  { title: '基金名称', dataIndex: 'fundName', key: 'fundName', ellipsis: true },
  { title: '标签', dataIndex: 'tag', key: 'tag', width: 100, renderKey: 'fundTag' },
  { title: '数据时间', dataIndex: 'dataTime', key: 'dataTime', width: 160 },
]
