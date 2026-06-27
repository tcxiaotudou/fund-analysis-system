import React from 'react'
import { InfoCircleOutlined, SignalFilled } from '@ant-design/icons'
import { getLevelColor } from '../../utils/dashboardDecision'

// 今日决策摘要区。
function DecisionSummary({ decisions }) {
  return (
    <section className="terminal-panel decision-summary-panel">
      <div className="terminal-panel-header">
        <div>
          <h2>今日决策信号</h2>
          <p>RSI、均线与配置建议汇总</p>
        </div>
        <SignalFilled />
      </div>
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
        {decisions.length === 0 && (
          <div className="terminal-empty">暂无今日决策数据</div>
        )}
      </div>
    </section>
  )
}

export default DecisionSummary
