import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createMomentumBacktestSearchParams,
  getMomentumBacktestRangeFromSearchParams,
} from './momentumBacktestRange.js'

test('从 URL 参数恢复动量策略回测区间', () => {
  const searchParams = new URLSearchParams('startDate=2025-01-01&endDate=2025-12-31')

  assert.deepEqual(getMomentumBacktestRangeFromSearchParams(searchParams), {
    range: {
      startDate: '2025-01-01',
      endDate: '2025-12-31',
    },
    error: null,
  })
})

test('没有 URL 参数时不启用回测区间', () => {
  assert.deepEqual(getMomentumBacktestRangeFromSearchParams(new URLSearchParams()), {
    range: null,
    error: null,
  })
})

test('URL 参数不完整时返回显式错误', () => {
  assert.deepEqual(getMomentumBacktestRangeFromSearchParams(new URLSearchParams('startDate=2025-01-01')), {
    range: null,
    error: '动量策略回测区间 URL 参数不完整',
  })
})

test('生成可写入地址栏的动量策略回测区间参数', () => {
  assert.deepEqual(
    createMomentumBacktestSearchParams({
      startDate: '2025-01-01',
      endDate: '2025-12-31',
    }),
    {
      startDate: '2025-01-01',
      endDate: '2025-12-31',
    },
  )
})
