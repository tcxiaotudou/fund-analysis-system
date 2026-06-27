// 首页默认数据，避免页面在请求前访问空对象。
export const EMPTY_DASHBOARD_DECISION = {
  dataStatus: { status: 'loading', message: '数据加载中', moduleErrors: [] },
  decisions: [],
  metrics: [],
  trendPoints: [],
  operations: [],
  etfOpportunities: [],
  maSignals: [],
  fundRecommendations: [],
  indexValuations: [],
  updateTime: '',
}

// 归一化首页聚合数据。
export function normalizeDashboardDecision(data) {
  if (!data || typeof data !== 'object') {
    throw new Error('首页数据格式错误')
  }
  if (!data.dataStatus || !data.dataStatus.status || !data.dataStatus.message) {
    throw new Error('首页数据缺少健康状态')
  }
  const moduleErrors = [...readRequiredArray(data.dataStatus.moduleErrors, '模块错误')]
  const indexValuations = readDashboardExtensionArray(data.indexValuations, '指数估值', moduleErrors)
  const hasExtensionError = moduleErrors.length > data.dataStatus.moduleErrors.length
  return {
    dataStatus: {
      status: hasExtensionError && data.dataStatus.status === 'normal' ? 'partial' : data.dataStatus.status,
      message: hasExtensionError && data.dataStatus.status === 'normal' ? '部分模块加载失败' : data.dataStatus.message,
      moduleErrors,
    },
    decisions: readRequiredArray(data.decisions, '今日决策'),
    metrics: readRequiredArray(data.metrics, '核心指标'),
    trendPoints: readRequiredArray(data.trendPoints, '趋势点'),
    operations: readRequiredArray(data.operations, '操作队列'),
    etfOpportunities: readRequiredArray(data.etfOpportunities, 'ETF机会'),
    maSignals: readRequiredArray(data.maSignals, 'MA信号'),
    fundRecommendations: readRequiredArray(data.fundRecommendations, '基金推荐'),
    indexValuations,
    updateTime: data.updateTime || '',
  }
}

// 读取后端必需数组字段，契约漂移时直接暴露错误。
function readRequiredArray(value, label) {
  if (!Array.isArray(value)) {
    throw new Error(`首页数据字段格式错误：${label}`)
  }
  return value
}

// 读取可扩展数组字段，旧后端未返回时显式暴露模块错误。
function readDashboardExtensionArray(value, label, moduleErrors) {
  if (value === undefined) {
    moduleErrors.push({ module: label, message: `首页数据缺少${label}字段，请确认后端已更新` })
    return []
  }
  return readRequiredArray(value, label)
}

// 获取数据状态展示颜色。
export function getDashboardStatusColor(status) {
  if (status === 'normal') return 'success'
  if (status === 'partial') return 'warning'
  if (status === 'error') return 'error'
  if (status === 'loading') return 'processing'
  return 'default'
}

// 获取风险级别颜色。
export function getLevelColor(level) {
  if (level === 'success') return '#0f9f6e'
  if (level === 'warning') return '#d97706'
  if (level === 'danger') return '#dc2626'
  if (level === 'info') return '#2563eb'
  return '#475569'
}

// 市场概览指标分组定义，避免同一批指标在页面里重复展示。
const MARKET_METRIC_SECTION_DEFINITIONS = [
  {
    key: 'temperature',
    title: '市场温度',
    description: '短中期 RSI 与组合持仓温度',
    metricKeys: ['rsi14', 'rsi90', 'portfolioRsi'],
  },
  {
    key: 'allocation',
    title: '配置与估值',
    description: '风险溢价和长期趋势',
    metricKeys: ['riskPremium', 'ma5yDeviation'],
  },
]

// 构建市场概览指标分组。
export function buildMarketMetricSections(metrics) {
  const metricMap = new Map((metrics || []).map(metric => [metric.key, metric]))
  return MARKET_METRIC_SECTION_DEFINITIONS.map(section => ({
    ...section,
    items: section.metricKeys.map(key => metricMap.get(key)).filter(Boolean),
  })).filter(section => section.items.length > 0)
}

// 获取操作路由，API动作没有路由。
export function getOperationRoute(operation) {
  return operation?.targetPath || null
}
