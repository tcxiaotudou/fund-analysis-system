import React from 'react'
import { Button } from 'antd'
import {
  ArrowDownOutlined,
  ArrowRightOutlined,
  ArrowUpOutlined,
  ExperimentOutlined,
  MailOutlined,
  RadarChartOutlined,
  RightOutlined,
  SettingOutlined,
  SlidersOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { buildMarketMetricSections, getLevelColor } from '../../utils/dashboardDecision'

// 主要操作的展示顺序。
const PRIMARY_OPERATION_KEYS = ['rsi-backtest', 'portfolio-weight']

// 次要操作的展示顺序。
const SECONDARY_OPERATION_KEYS = ['momentum', 'send-email']

// 操作图标映射。
const OPERATION_ICONS = {
  'rsi-backtest': <ExperimentOutlined />,
  'portfolio-weight': <SlidersOutlined />,
  momentum: <ThunderboltOutlined />,
  'send-email': <MailOutlined />,
}

// 根据 key 顺序筛选操作。
const pickOperations = (operations, keys) => {
  const operationMap = new Map((operations || []).map(operation => [operation.key, operation]))
  return keys.map(key => operationMap.get(key)).filter(Boolean)
}

// 根据趋势方向渲染稳定图标。
const renderTrendIcon = (trend) => {
  if (trend === 'up') return <ArrowUpOutlined />
  if (trend === 'down') return <ArrowDownOutlined />
  return <ArrowRightOutlined />
}

// 市场状态与今日行动工作台。
function MarketOverviewWorkbench({ metrics, operations, indexValuations, onRunOperation }) {
  const metricSections = buildMarketMetricSections(metrics)
  const primaryOperations = pickOperations(operations, PRIMARY_OPERATION_KEYS)
  const secondaryOperations = pickOperations(operations, SECONDARY_OPERATION_KEYS)

  return (
    <section className="market-workbench">
      <div className="market-overview-panel">
        <div className="workbench-heading">
          <div>
            <h2>市场概览</h2>
          </div>
          <span className="workbench-icon"><RadarChartOutlined /></span>
        </div>
        <div className="market-metric-sections">
          {metricSections.map(section => (
            <div className="market-metric-section" key={section.key}>
              <div className="market-metric-section-header">
                <strong>{section.title}</strong>
                <span>{section.description}</span>
              </div>
              <div className="market-metric-grid">
                {section.items.map(metric => {
                  const color = getLevelColor(metric.level)
                  return (
                    <article className="market-metric-card" key={metric.key}>
                      <div className="market-metric-label-row">
                        <span>{metric.label}</span>
                        <em>{renderTrendIcon(metric.trend)}</em>
                      </div>
                      <strong style={{ color }}>{metric.value}</strong>
                      <small>{metric.helper}</small>
                    </article>
                  )
                })}
              </div>
            </div>
          ))}
        </div>
        {indexValuations.length > 0 && (
          <div className="index-valuation-section">
            <div className="market-metric-section-header">
              <strong>指数估值</strong>
              <span>蛋卷估值表</span>
            </div>
            <div className="index-valuation-list">
              {indexValuations.map(valuation => (
                <article className={`index-valuation-card index-valuation-card-${valuation.level}`} key={valuation.indexCode}>
                  <div className="index-valuation-main">
                    <strong>{valuation.name}</strong>
                    <span>{valuation.historyLowText}</span>
                    <b>{valuation.valuationLabel}</b>
                  </div>
                  <div className="index-valuation-divider" />
                  <div className="index-valuation-stat">
                    <span>PE {valuation.peDate}</span>
                    <strong>{valuation.pe}</strong>
                  </div>
                  <div className="index-valuation-stat">
                    <span>PE百分位</span>
                    <strong>{valuation.pePercentile}</strong>
                  </div>
                </article>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="action-command-panel">
        <div className="workbench-heading">
          <div>
            <h2>今日行动</h2>
          </div>
        </div>
        <div className="primary-action-grid">
          {primaryOperations.map(operation => (
            <button
              className="primary-action-card"
              key={operation.key}
              type="button"
              onClick={() => onRunOperation(operation)}
            >
              <span className="primary-action-icon">{OPERATION_ICONS[operation.key] || <RightOutlined />}</span>
              <span className="primary-action-copy">
                <strong>{operation.title}</strong>
                <small>{operation.description}</small>
              </span>
              <RightOutlined />
            </button>
          ))}
        </div>
        <div className="secondary-action-list">
          {secondaryOperations.map(operation => (
            <button
              className={operation.danger ? 'secondary-action secondary-action-danger' : 'secondary-action'}
              key={operation.key}
              type="button"
              onClick={() => onRunOperation(operation)}
            >
              <span>{OPERATION_ICONS[operation.key] || <RightOutlined />}</span>
              <strong>{operation.title}</strong>
              <RightOutlined />
            </button>
          ))}
        </div>
        <Button
          block
          className="operation-all-button"
          icon={<SettingOutlined />}
          onClick={() => onRunOperation({ targetPath: '/system-config' })}
        >
          系统设置
        </Button>
      </div>
    </section>
  )
}

export default MarketOverviewWorkbench
