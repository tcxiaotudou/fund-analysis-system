import test from 'node:test'
import assert from 'node:assert/strict'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { StaticRouter } from 'react-router-dom/server.js'
import { createServer } from 'vite'

// 使用 Vite SSR 加载 JSX 组件，避免为临时测试新增依赖。
async function loadDashboardComponents() {
  const server = await createServer({
    server: { middlewareMode: true },
    appType: 'custom',
    logLevel: 'error',
  })
  try {
    const [decisionModule, metricModule, operationModule, chartModule, tablesModule] = await Promise.all([
      server.ssrLoadModule('/src/components/dashboard/DecisionSummary.jsx'),
      server.ssrLoadModule('/src/components/dashboard/MetricStrip.jsx'),
      server.ssrLoadModule('/src/components/dashboard/OperationQueue.jsx'),
      server.ssrLoadModule('/src/components/dashboard/MarketTemperatureChart.jsx'),
      server.ssrLoadModule('/src/components/dashboard/SignalTables.jsx'),
    ])
    return {
      DecisionSummary: decisionModule.default,
      MetricStrip: metricModule.default,
      OperationQueue: operationModule.default,
      MarketTemperatureChart: chartModule.default,
      SignalTables: tablesModule.default,
    }
  } finally {
    await server.close()
  }
}

test('dashboard components render decision, metric and operation content', async () => {
  const { DecisionSummary, MetricStrip, OperationQueue, MarketTemperatureChart, SignalTables } = await loadDashboardComponents()

  const decisionHtml = renderToStaticMarkup(React.createElement(DecisionSummary, {
    decisions: [{
      key: 'stock-bond',
      title: '股债平衡',
      value: '保持 50/50',
      description: '90日RSI位于中性区间',
      level: 'info',
    }],
  }))
  assert.match(decisionHtml, /股债平衡/)
  assert.match(decisionHtml, /保持 50\/50/)

  const metricHtml = renderToStaticMarkup(React.createElement(MetricStrip, {
    metrics: [{
      key: 'rsi90',
      label: '国证A指90日RSI',
      value: '49.12',
      unit: '',
      description: '57点以上减仓',
      level: 'success',
      trend: 'up',
    }],
  }))
  assert.match(metricHtml, /国证A指90日RSI/)
  assert.match(metricHtml, /49.12/)

  const operationHtml = renderToStaticMarkup(React.createElement(OperationQueue, {
    operations: [{
      key: 'rsi-backtest',
      title: '执行RSI回测',
      description: '验证当前买点',
      actionType: 'route',
      targetPath: '/rsi-backtest',
      danger: false,
    }],
    onRunOperation: () => {},
  }))
  assert.match(operationHtml, /执行RSI回测/)
  assert.match(operationHtml, /验证当前买点/)

  const chartHtml = renderToStaticMarkup(React.createElement(MarketTemperatureChart, {
    data: [{ date: '2026-06-19', rsi14: 32.12, rsi90: 49.12, portfolioRsi: 45.3 }],
  }))
  assert.match(chartHtml, /市场温度/)

  const tablesHtml = renderToStaticMarkup(React.createElement(StaticRouter, { location: '/' },
    React.createElement(SignalTables, {
      etfOpportunities: [{
        code: '510300',
        name: '沪深300ETF',
        currentRsi: 28.31,
        interval: '低位',
        message: 'RSI低于阈值',
        dataTime: '2026-06-19',
        isBuySignal: true,
      }],
      maSignals: [{
        etfCode: '159915',
        etfName: '创业板ETF',
        currentDaily: 1.234,
        ma10: 1.2,
        ma30: 1.1,
        signalDescription: '价格突破均线',
        dataTime: '2026-06-19',
        isBuySignal: true,
        isSellSignal: false,
      }],
      fundRecommendations: [],
    })
  ))
  assert.match(tablesHtml, /RSI低于阈值/)
  assert.match(tablesHtml, /价格突破均线/)
  assert.match(tablesHtml, /2026-06-19/)
})
