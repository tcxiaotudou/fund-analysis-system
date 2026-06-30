const START_DATE_PARAM = 'startDate'
const END_DATE_PARAM = 'endDate'
const VIEW_START_DATE_PARAM = 'viewStartDate'
const VIEW_END_DATE_PARAM = 'viewEndDate'
const VISIBLE_RANGE_STORAGE_KEY = 'momentumStrategy.visibleDateRange'
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

// 解析一组日期值，并返回显式错误信息。
function getMomentumDateRangeFromValues(startDate, endDate, label, sourceLabel = ' URL 参数') {
  if (!startDate && !endDate) {
    return { range: null, error: null }
  }

  if (!startDate || !endDate) {
    return { range: null, error: `${label}${sourceLabel}不完整` }
  }

  if (!DATE_PATTERN.test(startDate) || !DATE_PATTERN.test(endDate)) {
    return { range: null, error: `${label}${sourceLabel}格式错误` }
  }

  // URL 日期固定为 YYYY-MM-DD，格式校验后可直接按字符串比较先后。
  if (startDate > endDate) {
    return { range: null, error: `${label}开始日期不能晚于结束日期` }
  }

  return { range: { startDate, endDate }, error: null }
}

// 解析一组 URL 日期参数，并返回显式错误信息。
function getMomentumDateRangeFromSearchParams(searchParams, startParam, endParam, label) {
  const startDate = searchParams.get(startParam)
  const endDate = searchParams.get(endParam)

  return getMomentumDateRangeFromValues(startDate, endDate, label)
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

export function getMomentumVisibleRangeFromStorage(storage) {
  const savedValue = storage.getItem(VISIBLE_RANGE_STORAGE_KEY)
  if (!savedValue) {
    return { range: null, error: null }
  }

  let parsedValue
  try {
    parsedValue = JSON.parse(savedValue)
  } catch (error) {
    return { range: null, error: '浏览器保存的动量策略可视时间范围格式错误' }
  }

  return getMomentumDateRangeFromValues(
    parsedValue?.startDate,
    parsedValue?.endDate,
    '浏览器保存的动量策略可视时间范围',
    '保存值',
  )
}

export function getMomentumVisibleRangePreference(searchParams, storage) {
  const urlState = getMomentumVisibleRangeFromSearchParams(searchParams)
  if (urlState.range || urlState.error) {
    return urlState
  }

  return getMomentumVisibleRangeFromStorage(storage)
}

export function saveMomentumVisibleRangeToStorage(storage, range) {
  if (!range) {
    storage.removeItem(VISIBLE_RANGE_STORAGE_KEY)
    return
  }

  storage.setItem(VISIBLE_RANGE_STORAGE_KEY, JSON.stringify({
    startDate: range.startDate,
    endDate: range.endDate,
  }))
}
