/**
 * API服务文件
 * 封装所有后端接口调用
 */
import axios from 'axios'

// 创建axios实例，设置基础URL和超时时间
const api = axios.create({
  baseURL: '/api',  // 通过Vite代理转发到后端
  timeout: 30000,   // 30秒超时
})

// 请求拦截器：在请求发送前做一些处理
api.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理响应和错误
api.interceptors.response.use(
  response => {
    // 直接返回data部分
    return response.data
  },
  error => {
    const message = error.response?.data?.message || error.message || '网络错误'
    error.normalizedMessage = message
    console.error('API请求错误:', message)
    return Promise.reject(error)
  }
)

/**
 * 市场数据相关API
 */
export const marketApi = {
  // 获取市场概览
  getOverview: () => api.get('/market/overview'),
}

/**
 * RSI分析相关API
 */
export const rsiApi = {
  // 计算指定标的的RSI
  calculateRsi: (code, period = 14) => 
    api.get('/rsi/calculate', { params: { code, period } }),
  
  // 获取ETF买入信号
  getEtfSignals: () => api.get('/rsi/etf-signals'),
}

/**
 * 移动平均线策略相关API
 */
export const maStrategyApi = {
  // 计算MA策略
  calculateMa: (code) => api.get('/ma-strategy/calculate', { params: { code } }),
  
  // 获取买入信号
  getBuySignals: () => api.get('/ma-strategy/buy-signals'),

  // 获取所有ETF最新MA策略
  getLatest: () => api.get('/ma-strategy/latest'),
  
  // 执行回测
  runBacktest: (etfCode, startDate, endDate, initialCapital = 100000) =>
    api.post('/ma-strategy/backtest', null, {
      params: { etfCode, startDate, endDate, initialCapital }
    }),
}

/**
 * 基金相关API
 */
export const fundApi = {
  // 获取基金推荐列表
  getRecommendations: () => api.get('/fund/recommendations'),

  // 重新获取基金推荐列表
  refreshRecommendations: () => api.post('/fund/recommendations/refresh'),
  
  // 获取黑名单列表
  getBlacklist: () => api.get('/fund/blacklist'),
  
  // 添加基金到黑名单
  addToBlacklist: (data) => api.post('/fund/blacklist', data),
  
  // 从黑名单移除基金
  removeFromBlacklist: (fundCode) => api.delete(`/fund/blacklist/${fundCode}`),
  
  // 检查基金是否在黑名单中
  checkBlacklist: (fundCode) => api.get(`/fund/blacklist/${fundCode}`),
  
  // 更新基金持有状态
  updateHoldingStatus: (fundCode, isHolding) => api.post('/fund/holding', { fundCode, isHolding }),
  
  // 手动添加基金到持有列表
  addHoldingFund: (fundCode, fundName) => api.post('/fund/add-holding', { fundCode, fundName }),
}

/**
 * 基金组合相关API
 */
export const portfolioApi = {
  // 获取持有的基金列表
  getHoldingFunds: () => api.get('/fund/portfolio/holdings'),
  
  // 获取基金组合 RSI 数据
  getPortfolioRsi: () => api.get('/fund/portfolio/rsi'),
  
  // 批量更新基金权重
  updateWeights: (weights) => api.post('/fund/portfolio/weights/batch', weights),
  
  // 获取组合RSI历史数据
  getPortfolioRsiHistory: (days = 60) => api.get('/fund/portfolio/rsi/history', { params: { days } }),
}

/**
 * ETF管理相关API
 */
export const etfApi = {
  // 获取ETF列表
  getList: () => api.get('/etf/list'),
  
  // 获取启用的ETF列表
  getEnabledList: () => api.get('/etf/enabled'),
  
  // 添加ETF
  add: (data) => api.post('/etf/add', data),
  
  // 更新ETF
  update: (data) => api.post('/etf/update', data),
  
  // 删除ETF
  delete: (id) => api.post(`/etf/delete/${id}`),
}

/**
 * 21日动量策略相关API
 */
export const momentumStrategyApi = {
  // 获取所有交易记录
  getTransactions: () => api.get('/momentum-strategy/transactions'),
  
  // 获取指定日期范围内的交易记录
  getTransactionsByRange: (startDate, endDate) => 
    api.get('/momentum-strategy/transactions/range', { 
      params: { startDate, endDate } 
    }),
  
  // 获取指定ETF的交易记录
  getTransactionsByEtf: (etfCode) => 
    api.get('/momentum-strategy/transactions/etf', { 
      params: { etfCode } 
    }),
  
  // 执行回测
  runBacktest: (startDate, endDate, initialCapital = 100000) =>
    api.post('/momentum-strategy/backtest/run', null, {
      params: { startDate, endDate, initialCapital }
    }),
  
  // 获取收益曲线数据
  getPerformance: () => api.get('/momentum-strategy/performance'),

  // 获取指定日期范围内的收益曲线数据
  getPerformanceByRange: (startDate, endDate) =>
    api.get('/momentum-strategy/performance/range', {
      params: { startDate, endDate }
    }),
}

/**
 * RSI策略回测相关API
 */
export const rsiBacktestApi = {
  runBacktest: (etfCode, startDate, endDate, initialCapital = 100000,
                rsiPeriod = 14, rsiBuyThreshold = 30, rsiSellThreshold = 60,
                fixedAmountPerTrade = 10000) =>
    api.post('/rsi-backtest/run', null, {
      params: { etfCode, startDate, endDate, initialCapital, rsiPeriod, rsiBuyThreshold, rsiSellThreshold, fixedAmountPerTrade }
    }),
}

/**
 * 系统配置相关API
 */
export const systemConfigApi = {
  // 获取邮件配置
  getEmailConfig: () => api.get('/system-config/email'),

  // 保存邮件配置
  saveEmailConfig: (data) => api.post('/system-config/email', data),

  // 立即发送邮件
  sendEmailNow: () => api.post('/system-config/email/send-now'),

  // 获取基金推荐配置
  getFundRecommendationConfig: () => api.get('/system-config/fund-recommendation'),

  // 保存基金推荐配置
  saveFundRecommendationConfig: (data) => api.post('/system-config/fund-recommendation', data),
}

export default api
