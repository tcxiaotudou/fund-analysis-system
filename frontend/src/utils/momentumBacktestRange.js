const START_DATE_PARAM = 'startDate'
const END_DATE_PARAM = 'endDate'

export function getMomentumBacktestRangeFromSearchParams(searchParams) {
  const startDate = searchParams.get(START_DATE_PARAM)
  const endDate = searchParams.get(END_DATE_PARAM)

  if (!startDate && !endDate) {
    return null
  }

  if (!startDate || !endDate) {
    throw new Error('动量策略回测区间 URL 参数不完整')
  }

  return { startDate, endDate }
}

export function createMomentumBacktestSearchParams(range) {
  return {
    [START_DATE_PARAM]: range.startDate,
    [END_DATE_PARAM]: range.endDate,
  }
}
