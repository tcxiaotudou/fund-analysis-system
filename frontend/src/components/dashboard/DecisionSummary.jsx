import React from 'react'
import { InfoCircleOutlined } from '@ant-design/icons'
import { getLevelColor } from '../../utils/dashboardDecision'

// 今日决策摘要区。
function DecisionSummary({ decisions }) {
  return (
    <section className="dashboard-section">
      <div className="dashboard-section-title">今日决策</div>
      <div className="decision-grid">
        {decisions.map(decision => (
          <article className={`decision-card decision-card-${decision.level}`} key={decision.key}>
            <div className="decision-card-header">
              <span>{decision.title}</span>
              <InfoCircleOutlined />
            </div>
            <div className="decision-card-value" style={{ color: getLevelColor(decision.level) }}>
              {decision.value}
            </div>
            <div className="decision-card-subtitle">{decision.subtitle}</div>
            <div className="decision-card-desc">{decision.description}</div>
          </article>
        ))}
      </div>
    </section>
  )
}

export default DecisionSummary
