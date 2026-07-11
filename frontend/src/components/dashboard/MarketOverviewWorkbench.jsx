import React from 'react'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
} from '@ant-design/icons'
import { buildMarketMetricSections, getLevelColor } from '../../utils/dashboardDecision'

// 根据趋势方向渲染稳定图标。
const renderTrendIcon = (trend) => {
  if (trend === 'up') return <ArrowUpOutlined />
  if (trend === 'down') return <ArrowDownOutlined />
  return null
}

// 将可解析的数值限定到进度条的展示区间。
const getMetricProgress = (value) => {
  const numericValue = Number.parseFloat(String(value).replace('%', ''))
  if (!Number.isFinite(numericValue)) {
    return null
  }
  return Math.min(100, Math.max(0, numericValue))
}

// 市场温度与估值概览。
function MarketOverviewWorkbench({ metrics, indexValuations }) {
  const metricSections = buildMarketMetricSections(metrics)

  return (
    <section className="terminal-panel market-overview-panel">
      <div className="terminal-panel-header">
        <div>
          <h2>市场温度与估值</h2>
          <p>短期情绪、持仓状态和长期位置</p>
        </div>
      </div>

      <div className="terminal-metric-sections">
        {metricSections.map(section => (
          <div className="terminal-metric-section" key={section.key}>
            <div className="terminal-section-title">
              <strong>{section.title}</strong>
              <span>{section.description}</span>
            </div>
            <div className="terminal-metric-list">
              {section.items.map(metric => {
                const color = getLevelColor(metric.level)
                const progress = ['rsi14', 'rsi90', 'portfolioRsi'].includes(metric.key)
                  ? getMetricProgress(metric.value)
                  : null
                return (
                  <article className="terminal-metric-row" key={metric.key}>
                    <div className="terminal-metric-row-main">
                      <span>{metric.label}</span>
                      <em style={{ color }}>{renderTrendIcon(metric.trend)}</em>
                    </div>
                    <div className="terminal-metric-value" style={{ color }}>{metric.value}</div>
                    {progress !== null && (
                      <div className="terminal-meter-track" aria-hidden="true">
                        <div className={`terminal-meter-fill terminal-meter-fill-${metric.level}`} style={{ width: `${progress}%` }} />
                      </div>
                    )}
                    <small>{metric.helper}</small>
                  </article>
                )
              })}
            </div>
          </div>
        ))}
        {metricSections.length === 0 && (
          <div className="terminal-empty">暂无市场温度数据</div>
        )}
      </div>

      <div className="terminal-valuation-section">
        <div className="terminal-section-title">
          <strong>长期估值</strong>
          <span>指数位置参考</span>
        </div>
        <div className="terminal-valuation-list">
          {indexValuations.map(valuation => (
            <article className={`terminal-valuation-card terminal-valuation-card-${valuation.level}`} key={valuation.indexCode}>
              <div>
                <strong>{valuation.name}</strong>
                <span>{valuation.historyLowText}</span>
              </div>
              <b>{valuation.valuationLabel}</b>
              <dl>
                <div>
                  <dt>PE {valuation.peDate}</dt>
                  <dd>{valuation.pe}</dd>
                </div>
                <div>
                  <dt>PE百分位</dt>
                  <dd>{valuation.pePercentile}</dd>
                </div>
              </dl>
            </article>
          ))}
          {indexValuations.length === 0 && (
            <div className="terminal-empty">暂无指数估值数据</div>
          )}
        </div>
      </div>
    </section>
  )
}

export default MarketOverviewWorkbench
