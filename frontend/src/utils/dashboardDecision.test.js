import test from 'node:test'
import assert from 'node:assert/strict'

import {
  EMPTY_DASHBOARD_DECISION,
  getDashboardStatusColor,
  getOperationRoute,
  normalizeDashboardDecision,
} from './dashboardDecision.js'

test('首页数据模型保留决策卡片、指标和表格', () => {
  const result = normalizeDashboardDecision({
    dataStatus: { status: 'normal', message: '数据正常', moduleErrors: [] },
    decisions: [{ key: 'buy', title: '买入机会', value: '5', level: 'success' }],
    metrics: [{ key: 'rsi14', label: '14日RSI', value: '56.32', level: 'info' }],
    trendPoints: [],
    operations: [{ key: 'rsi-backtest', targetPath: '/rsi-backtest' }],
    etfOpportunities: [{ code: '510300.SH', name: '沪深300ETF', currentRsi: 41.25 }],
    maSignals: [],
    fundRecommendations: [{ fundCode: '005827', fundName: '易方达蓝筹精选混合', tag: '已持有' }],
  })

  assert.equal(result.dataStatus.status, 'normal')
  assert.equal(result.decisions[0].value, '5')
  assert.equal(result.metrics[0].label, '14日RSI')
  assert.equal(result.etfOpportunities[0].name, '沪深300ETF')
  assert.equal(result.fundRecommendations[0].tag, '已持有')
})

test('状态颜色和操作路由明确可见', () => {
  assert.equal(EMPTY_DASHBOARD_DECISION.dataStatus.status, 'loading')
  assert.equal(getDashboardStatusColor('normal'), 'success')
  assert.equal(getDashboardStatusColor('partial'), 'warning')
  assert.equal(getDashboardStatusColor('error'), 'error')
  assert.equal(getDashboardStatusColor('loading'), 'processing')
  assert.equal(getOperationRoute({ targetPath: '/fund-portfolio' }), '/fund-portfolio')
  assert.equal(getOperationRoute({ action: 'sendEmailNow' }), null)
})

test('首页数据模型遇到契约漂移时显式报错', () => {
  assert.throws(
    () => normalizeDashboardDecision({ dataStatus: { status: 'normal', message: '数据正常', moduleErrors: [] } }),
    /首页数据字段格式错误：今日决策/
  )
  assert.throws(
    () => normalizeDashboardDecision(null),
    /首页数据格式错误/
  )
})
