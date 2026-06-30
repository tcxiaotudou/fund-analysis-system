const START_DATE_PARAM = 'startDate'
const END_DATE_PARAM = 'endDate'
const VIEW_START_DATE_PARAM = 'viewStartDate'
const VIEW_END_DATE_PARAM = 'viewEndDate'
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

// 解析一组 URL 日期参数，并返回显式错误信息。
function getMomentumDateRangeFromSearchParams(searchParams, startParam, endParam, label) {
  const startDate = searchParams.get(startParam)
  const endDate = searchParams.get(endParam)

  if (!startDate && !endDate) {
    return { range: null, error: null }
  }

  if (!startDate || !endDate) {
    return { range: null, error: `${label} URL 参数不完整` }
  }

  if (!DATE_PATTERN.test(startDate) || !DATE_PATTERN.test(endDate)) {
    return { range: null, error: `${label} URL 参数格式错误` }
  }

  // URL 日期固定为 YYYY-MM-DD，格式校验后可直接按字符串比较先后。
  if (startDate > endDate) {
    return { range: null, error: `${label}开始日期不能晚于结束日期` }
  }

  return { range: { startDate, endDate }, error: null }
}

export function getMomentumBacktestRangeFromSearchParams(searchParams) {
  return getMomentumDateRangeFromSearchParams(
    searchParams,
    START_DATE_PARAM,
    END_DATE_PARAM,
    '动量策略回测区间',
  )
}

export function getMomentumVisibleRangeFromSearchParams(searchParams) {
  return getMomentumDateRangeFromSearchParams(
    searchParams,
    VIEW_START_DATE_PARAM,
    VIEW_END_DATE_PARAM,
    '动量策略可视时间范围',
  )
}

export function createMomentumBacktestSearchParams(range) {
  return {
    [START_DATE_PARAM]: range.startDate,
    [END_DATE_PARAM]: range.endDate,
  }
}

export function upsertMomentumVisibleRangeSearchParams(searchParams, range) {
  const nextParams = new URLSearchParams(searchParams)
  if (!range) {
    nextParams.delete(VIEW_START_DATE_PARAM)
    nextParams.delete(VIEW_END_DATE_PARAM)
    return nextParams
  }

  nextParams.set(VIEW_START_DATE_PARAM, range.startDate)
  nextParams.set(VIEW_END_DATE_PARAM, range.endDate)
  return nextParams
}
