import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildMomentumChartData,
  shouldRenderMomentumMarker,
} from './momentumChartData.js'

test('资金曲线保留每日绩效点并把买卖点合并到同一数据源', () => {
  const performanceData = [
    { date: '2026-01-08', totalValue: '103000', returnRate: '3.00' },
    { date: '2026-01-05', totalValue: '100000', returnRate: '0.00' },
    { date: '2026-01-06', totalValue: '101000', returnRate: '1.00' },
    { date: '2026-01-07', totalValue: '102000', returnRate: '2.00' },
  ]
  const transactions = [
    {
      date: '2026-01-06',
      type: 'buy',
      code: 'sh513100',
      name: '纳指ETF',
      price: '1.963',
      quantity: 100,
    },
    {
      date: '2026-01-07',
      type: 'sell',
      code: 'sh513100',
      name: '纳指ETF',
      price: '1.970',
      quantity: -100,
    },
  ]

  const result = buildMomentumChartData(performanceData, [0, 100], transactions)

  assert.deepEqual(result.map(item => item.date), [
    '2026-01-05',
    '2026-01-06',
    '2026-01-07',
    '2026-01-08',
  ])
  assert.deepEqual(result.map(item => item.totalValue), [100000, 101000, 102000, 103000])
  assert.equal(result[1].buyValue, 101000)
  assert.equal(result[1].sellValue, null)
  assert.equal(result[1].buyInfo.code, 'sh513100')
  assert.equal(result[2].sellValue, 102000)
  assert.equal(result[2].buyValue, null)
  assert.equal(result[2].sellInfo.quantity, -100)
})

test('空买卖点不渲染交易标记', () => {
  assert.equal(shouldRenderMomentumMarker({ payload: { buyValue: null } }, 'buyValue'), false)
  assert.equal(shouldRenderMomentumMarker({ payload: { buyValue: 101000 } }, 'buyValue'), true)
})
