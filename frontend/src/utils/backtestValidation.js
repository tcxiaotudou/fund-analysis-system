// 回测日期区间的默认错误文案。
export const BACKTEST_DATE_RANGE_ERROR = '开始时间不能晚于结束时间'

// RSI 阈值倒挂的默认错误文案。
export const RSI_THRESHOLD_ORDER_ERROR = 'RSI买入阈值必须小于卖出阈值'

// 校验回测日期区间。
export function getDateRangeError(startDate, endDate, emptyMessage = '请选择开始时间和结束时间') {
  if (!startDate || !endDate) {
    return emptyMessage
  }
  if (startDate.isAfter(endDate)) {
    return BACKTEST_DATE_RANGE_ERROR
  }
  return null
}

// 校验正数金额或数量参数。
export function getPositiveNumberError(value, label) {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue) || numericValue <= 0) {
    return `${label}必须大于0`
  }
  return null
}

// 校验 RSI 买入和卖出阈值之间的业务关系。
export function getRsiThresholdOrderError(buyThreshold, sellThreshold) {
  const buyValue = Number(buyThreshold)
  const sellValue = Number(sellThreshold)
  if (!Number.isFinite(buyValue) || !Number.isFinite(sellValue)) {
    return 'RSI阈值不能为空'
  }
  if (buyValue >= sellValue) {
    return RSI_THRESHOLD_ORDER_ERROR
  }
  return null
}
