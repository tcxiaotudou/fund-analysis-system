import React from 'react'
import { ArrowDownOutlined, ArrowRightOutlined, ArrowUpOutlined } from '@ant-design/icons'
import { getLevelColor } from '../../utils/dashboardDecision'

// 核心指标横条。
function MetricStrip({ metrics }) {
  // 根据趋势方向渲染稳定图标。
  const renderTrendIcon = (trend) => {
    if (trend === 'up') return <ArrowUpOutlined />
    if (trend === 'down') return <ArrowDownOutlined />
    return <ArrowRightOutlined />
  }

  return (
    <section className="metric-strip">
      {metrics.map(metric => (
        <article className="metric-item" key={metric.key}>
          <div className="metric-label">{metric.label}</div>
          <div className="metric-value" style={{ color: getLevelColor(metric.level) }}>{metric.value}</div>
          <div className="metric-helper">
            {renderTrendIcon(metric.trend)}
            <span>{metric.helper}</span>
          </div>
        </article>
      ))}
    </section>
  )
}

export default MetricStrip
