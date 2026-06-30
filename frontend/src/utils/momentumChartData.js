import dayjs from 'dayjs'
import {
  MOMENTUM_VISIBLE_RANGE_PRESETS,
} from './momentumBacktestRange.js'

const DATE_RANGE_INDEX_EPSILON = 1e-8

// 按交易日升序排列动量策略每日绩效数据。
function sortMomentumPerformanceData(performanceData) {
  return [...performanceData].sort((a, b) => {
    const dateA = dayjs(a.date)
    const dateB = dayjs(b.date)
    if (dateA.isBefore(dateB)) return -1
    if (dateA.isAfter(dateB)) return 1
    return 0
  })
}

// 根据快捷项和最新交易日计算目标起始边界。
function getMomentumPresetStartBoundary(latestDate, preset) {
  if (preset === MOMENTUM_VISIBLE_RANGE_PRESETS.THIS_YEAR) {
    return latestDate.startOf('year')
  }

  if (preset === MOMENTUM_VISIBLE_RANGE_PRESETS.LAST_YEAR) {
    return latestDate.subtract(1, 'year')
  }

  if (preset === MOMENTUM_VISIBLE_RANGE_PRESETS.LAST_THREE_YEARS) {
    return latestDate.subtract(3, 'year')
  }

  if (preset === MOMENTUM_VISIBLE_RANGE_PRESETS.LAST_FIVE_YEARS) {
    return latestDate.subtract(5, 'year')
  }

  throw new Error('未知的动量策略快捷时间范围')
}

// 根据百分比滑块区间截取当前可视的每日绩效数据。
export function getMomentumVisiblePerformanceData(performanceData, dateRange) {
  if (!performanceData || performanceData.length === 0) {
    return []
  }

  const sortedData = sortMomentumPerformanceData(performanceData)
  const totalLength = sortedData.length
  // 百分比由实际索引反算而来时可能出现 0.999999 这类浮点误差。
  const startIndex = Math.floor((dateRange[0] / 100) * totalLength + DATE_RANGE_INDEX_EPSILON)
  const endIndex = Math.ceil((dateRange[1] / 100) * totalLength - DATE_RANGE_INDEX_EPSILON)

  return sortedData.slice(startIndex, endIndex)
}

// 根据百分比滑块区间换算当前可视的实际日期边界。
export function getMomentumVisibleDateRange(performanceData, dateRange) {
  const visibleData = getMomentumVisiblePerformanceData(performanceData, dateRange)
  if (visibleData.length === 0) {
    return null
  }

  return {
    startDate: visibleData[0].date,
    endDate: visibleData[visibleData.length - 1].date,
  }
}

// 将快捷时间范围换算为收益曲线内实际存在的交易日边界。
export function getMomentumVisibleDateRangeByPreset(performanceData, preset) {
  if (!performanceData || performanceData.length === 0) {
    return null
  }

  const sortedData = sortMomentumPerformanceData(performanceData)
  const latestItem = sortedData[sortedData.length - 1]
  const startBoundary = getMomentumPresetStartBoundary(dayjs(latestItem.date), preset)
  const startItem = sortedData.find(item => !dayjs(item.date).isBefore(startBoundary, 'day')) || sortedData[0]

  return {
    startDate: startItem.date,
    endDate: latestItem.date,
  }
}

// 根据滑块百分比取得最接近的收益曲线日期，用于 Tooltip 展示。
export function getMomentumPerformanceDateByPercent(performanceData, percent) {
  if (!performanceData || performanceData.length === 0) {
    return null
  }

  const sortedData = sortMomentumPerformanceData(performanceData)
  const totalLength = sortedData.length
  const rawIndex = (percent / 100) * totalLength
  const safeIndex = Math.min(
    Math.floor(rawIndex + DATE_RANGE_INDEX_EPSILON),
    totalLength - 1,
  )

  return sortedData[safeIndex].date
}

// 将 URL 中保存的实际日期边界还原为 Slider 使用的百分比区间。
export function getMomentumDateRangePercentByDateRange(performanceData, visibleDateRange) {
  if (!performanceData || performanceData.length === 0 || !visibleDateRange) {
    return null
  }

  const sortedData = sortMomentumPerformanceData(performanceData)
  const startIndex = sortedData.findIndex(item => item.date === visibleDateRange.startDate)
  const endIndex = sortedData.findIndex(item => item.date === visibleDateRange.endDate)
  if (startIndex === -1 || endIndex === -1 || startIndex > endIndex) {
    return null
  }

  const totalLength = sortedData.length
  return [
    (startIndex / totalLength) * 100,
    ((endIndex + 1) / totalLength) * 100,
  ]
}

// 只保留当前可视日期边界内的动量策略交易记录。
export function filterMomentumTransactionsByVisibleDateRange(transactions, visibleDateRange) {
  if (!transactions || transactions.length === 0 || !visibleDateRange) {
    return []
  }

  return transactions.filter(item => (
    item.date >= visibleDateRange.startDate && item.date <= visibleDateRange.endDate
  ))
}

// 汇总当前交易记录列表的买卖次数和数量。
export function calculateMomentumTransactionSummary(transactions) {
  const safeTransactions = transactions || []
  const buyTransactions = safeTransactions.filter(item => item.type === 'buy')
  const sellTransactions = safeTransactions.filter(item => item.type === 'sell')
  const totalBuyQuantity = buyTransactions.reduce((sum, item) => sum + (item.quantity || 0), 0)
  const totalSellQuantity = Math.abs(
    sellTransactions.reduce((sum, item) => sum + (item.quantity || 0), 0)
  )

  return {
    totalCount: safeTransactions.length,
    buyCount: buyTransactions.length,
    sellCount: sellTransactions.length,
    totalBuyQuantity,
    totalSellQuantity,
  }
}

// 根据起止资产值和自然日间隔计算当前区间收益率。
function calculateMomentumReturnMetrics(startDate, endDate, startValue, endValue) {
  const startTotalValue = Number(startValue)
  const endTotalValue = Number(endValue)
  if (!Number.isFinite(startTotalValue) || !Number.isFinite(endTotalValue) || startTotalValue <= 0) {
    return {
      totalReturn: null,
      annualizedReturn: null,
    }
  }

  const totalReturn = ((endTotalValue - startTotalValue) / startTotalValue) * 100
  const totalDays = dayjs(endDate).diff(dayjs(startDate), 'day')
  const valueRatio = endTotalValue / startTotalValue
  const annualizedReturn = totalDays > 0 && valueRatio > 0
    ? (Math.pow(valueRatio, 365 / totalDays) - 1) * 100
    : null

  return {
    totalReturn,
    annualizedReturn,
  }
}

// 根据收益曲线计算回测摘要。
export function calculateMomentumPerformanceSummary(performanceData) {
  if (!performanceData || performanceData.length === 0) {
    return {
      startDate: null,
      endDate: null,
      maxDrawdown: null,
      maxDrawdownDate: null,
      totalReturn: null,
      annualizedReturn: null,
    }
  }

  const sortedData = sortMomentumPerformanceData(performanceData)

  let peakValue = null
  let maxDrawdown = 0
  let maxDrawdownDate = null

  sortedData.forEach(item => {
    const totalValue = Number(item.totalValue)
    if (!Number.isFinite(totalValue) || totalValue <= 0) {
      return
    }
    if (peakValue === null || totalValue > peakValue) {
      peakValue = totalValue
    }

    const drawdown = peakValue > 0 ? ((peakValue - totalValue) / peakValue) * 100 : 0
    if (drawdown > maxDrawdown) {
      maxDrawdown = drawdown
      maxDrawdownDate = item.date
    }
  })

  const returnMetrics = calculateMomentumReturnMetrics(
    sortedData[0].date,
    sortedData[sortedData.length - 1].date,
    sortedData[0].totalValue,
    sortedData[sortedData.length - 1].totalValue,
  )

  return {
    startDate: sortedData[0].date,
    endDate: sortedData[sortedData.length - 1].date,
    maxDrawdown,
    maxDrawdownDate,
    totalReturn: returnMetrics.totalReturn,
    annualizedReturn: returnMetrics.annualizedReturn,
  }
}

export function buildMomentumChartData(performanceData, dateRange, transactions) {
  if (!performanceData || performanceData.length === 0) {
    return []
  }

  const filteredData = getMomentumVisiblePerformanceData(performanceData, dateRange).map(item => ({
    date: item.date,
    dateValue: dayjs(item.date).valueOf(),
    totalValue: item.totalValue ? parseFloat(item.totalValue) : 0,
    returnRate: item.returnRate ? parseFloat(item.returnRate) : 0,
    holdingEtfCode: item.holdingEtfCode,
    holdingEtfName: item.holdingEtfName,
    buyValue: null,
    sellValue: null,
    buyInfo: null,
    sellInfo: null,
  }))

  if (transactions && filteredData.length > 0) {
    const chartDates = new Set(filteredData.map(d => d.date))
    const transactionsByDate = {}

    transactions.forEach(transaction => {
      if (chartDates.has(transaction.date)) {
        if (!transactionsByDate[transaction.date]) {
          transactionsByDate[transaction.date] = []
        }
        transactionsByDate[transaction.date].push(transaction)
      }
    })

    Object.keys(transactionsByDate).forEach(date => {
      const chartPoint = filteredData.find(d => d.date === date)
      if (!chartPoint) {
        return
      }

      const dayTransactions = transactionsByDate[date]
      const buyTransactions = dayTransactions.filter(t => t.type === 'buy')
      if (buyTransactions.length > 0) {
        const buyTxn = buyTransactions[0]
        chartPoint.buyValue = chartPoint.totalValue
        chartPoint.buyInfo = {
          code: buyTxn.code,
          name: buyTxn.name,
          price: buyTxn.price ? parseFloat(buyTxn.price) : 0,
          quantity: buyTxn.quantity,
        }
        if (buyTransactions.length > 1) {
          chartPoint.buyInfo.multiple = true
          chartPoint.buyInfo.count = buyTransactions.length
        }
      }

      const sellTransactions = dayTransactions.filter(t => t.type === 'sell')
      if (sellTransactions.length > 0) {
        const sellTxn = sellTransactions[0]
        chartPoint.sellValue = chartPoint.totalValue
        chartPoint.sellInfo = {
          code: sellTxn.code,
          name: sellTxn.name,
          price: sellTxn.price ? parseFloat(sellTxn.price) : 0,
          quantity: sellTxn.quantity,
        }
        if (sellTransactions.length > 1) {
          chartPoint.sellInfo.multiple = true
          chartPoint.sellInfo.count = sellTransactions.length
        }
      }
    })
  }

  return filteredData.sort((a, b) => a.dateValue - b.dateValue)
}

export function shouldRenderMomentumMarker(props, markerKey) {
  const value = props?.payload?.[markerKey]
  return value !== null && value !== undefined
}
