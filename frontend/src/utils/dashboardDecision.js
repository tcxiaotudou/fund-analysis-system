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
  return {
    dataStatus: {
      status: data.dataStatus.status,
      message: data.dataStatus.message,
      moduleErrors: readRequiredArray(data.dataStatus.moduleErrors, '模块错误'),
    },
    decisions: readRequiredArray(data.decisions, '今日决策'),
    metrics: readRequiredArray(data.metrics, '核心指标'),
    trendPoints: readRequiredArray(data.trendPoints, '趋势点'),
    operations: readRequiredArray(data.operations, '操作队列'),
    etfOpportunities: readRequiredArray(data.etfOpportunities, 'ETF机会'),
    maSignals: readRequiredArray(data.maSignals, 'MA信号'),
    fundRecommendations: readRequiredArray(data.fundRecommendations, '基金推荐'),
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

// 获取操作路由，API动作没有路由。
export function getOperationRoute(operation) {
  return operation?.targetPath || null
}
