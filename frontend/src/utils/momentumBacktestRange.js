const START_DATE_PARAM = 'startDate'
const END_DATE_PARAM = 'endDate'
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

export function getMomentumBacktestRangeFromSearchParams(searchParams) {
  const startDate = searchParams.get(START_DATE_PARAM)
  const endDate = searchParams.get(END_DATE_PARAM)

  if (!startDate && !endDate) {
    return { range: null, error: null }
  }

  if (!startDate || !endDate) {
    return { range: null, error: '动量策略回测区间 URL 参数不完整' }
  }

  if (!DATE_PATTERN.test(startDate) || !DATE_PATTERN.test(endDate)) {
    return { range: null, error: '动量策略回测区间 URL 参数格式错误' }
  }

  // URL 日期固定为 YYYY-MM-DD，格式校验后可直接按字符串比较先后。
  if (startDate > endDate) {
    return { range: null, error: '动量策略回测区间开始日期不能晚于结束日期' }
  }

  return { range: { startDate, endDate }, error: null }
}

export function createMomentumBacktestSearchParams(range) {
  return {
    [START_DATE_PARAM]: range.startDate,
    [END_DATE_PARAM]: range.endDate,
  }
}
