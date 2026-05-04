import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createMomentumBacktestSearchParams,
  getMomentumBacktestRangeFromSearchParams,
} from './momentumBacktestRange.js'

test('从 URL 参数恢复动量策略回测区间', () => {
  const searchParams = new URLSearchParams('startDate=2025-01-01&endDate=2025-12-31')

  assert.deepEqual(getMomentumBacktestRangeFromSearchParams(searchParams), {
    startDate: '2025-01-01',
    endDate: '2025-12-31',
  })
})

test('没有 URL 参数时不启用回测区间', () => {
  assert.equal(getMomentumBacktestRangeFromSearchParams(new URLSearchParams()), null)
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
