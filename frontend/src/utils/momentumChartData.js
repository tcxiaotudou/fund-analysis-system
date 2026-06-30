import dayjs from 'dayjs'

// 根据收益曲线计算回测摘要。
export function calculateMomentumPerformanceSummary(performanceData) {
  if (!performanceData || performanceData.length === 0) {
    return {
      startDate: null,
      endDate: null,
      maxDrawdown: null,
      maxDrawdownDate: null,
    }
  }

  const sortedData = [...performanceData].sort((a, b) => {
    const dateA = dayjs(a.date)
    const dateB = dayjs(b.date)
    if (dateA.isBefore(dateB)) return -1
    if (dateA.isAfter(dateB)) return 1
    return 0
  })

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

  return {
    startDate: sortedData[0].date,
    endDate: sortedData[sortedData.length - 1].date,
    maxDrawdown,
    maxDrawdownDate,
  }
}

export function buildMomentumChartData(performanceData, dateRange, transactions) {
  if (!performanceData || performanceData.length === 0) {
    return []
  }

  const sortedData = [...performanceData].sort((a, b) => {
    const dateA = dayjs(a.date)
    const dateB = dayjs(b.date)
    if (dateA.isBefore(dateB)) return -1
    if (dateA.isAfter(dateB)) return 1
    return 0
  })
  const totalLength = sortedData.length
  const startIndex = Math.floor((dateRange[0] / 100) * totalLength)
  const endIndex = Math.ceil((dateRange[1] / 100) * totalLength)

  const filteredData = sortedData.slice(startIndex, endIndex).map(item => ({
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
