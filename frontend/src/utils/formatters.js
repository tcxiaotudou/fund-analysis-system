import dayjs from 'dayjs'

// 判断值是否为空值，0 不视为空值。
export function isNil(value) {
  return value === null || value === undefined || value === ''
}

// 格式化普通数字，空值显示为占位符。
export function formatNumber(value, digits = 2, emptyText = '-') {
  if (isNil(value)) {
    return emptyText
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return emptyText
  }
  return number.toFixed(digits)
}

// 格式化百分比数字，空值显示为占位符。
export function formatPercent(value, digits = 2, emptyText = '-') {
  const formatted = formatNumber(value, digits, emptyText)
  return formatted === emptyText ? formatted : `${formatted}%`
}

// 格式化金额，空值显示为占位符。
export function formatCurrency(value, digits = 2, emptyText = '-') {
  if (isNil(value)) {
    return emptyText
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return emptyText
  }
  return `¥${number.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })}`
}

// 格式化日期时间，非法日期显示为占位符。
export function formatDateTime(value, emptyText = '-') {
  if (isNil(value)) {
    return emptyText
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : emptyText
}
