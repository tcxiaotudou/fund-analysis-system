import React from 'react'
import { Button } from 'antd'
import {
  ExperimentOutlined,
  MailOutlined,
  RadarChartOutlined,
  RightOutlined,
  SettingOutlined,
  SlidersOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { buildMarketStatusItems, getLevelColor } from '../../utils/dashboardDecision'

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

// 市场状态与今日行动工作台。
function MarketOverviewWorkbench({ metrics, operations, onRunOperation }) {
  const statusItems = buildMarketStatusItems(metrics)
  const primaryOperations = pickOperations(operations, PRIMARY_OPERATION_KEYS)
  const secondaryOperations = pickOperations(operations, SECONDARY_OPERATION_KEYS)

  return (
    <section className="market-workbench">
      <div className="market-state-panel">
        <div className="workbench-heading">
          <div>
            <h2>市场状态雷达</h2>
          </div>
          <span className="workbench-icon"><RadarChartOutlined /></span>
        </div>
        <div className="market-state-list">
          {statusItems.map(item => {
            const color = getLevelColor(item.level)
            return (
              <article className="market-state-item" key={item.key}>
                <div className="market-state-copy">
                  <span>{item.title}</span>
                  <strong style={{ color }}>{item.value}</strong>
                  <small>{item.description}</small>
                </div>
                <div className="market-state-meta">
                  <span>{item.caption}</span>
                  <em>{item.helper}</em>
                </div>
              </article>
            )
          })}
        </div>
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
