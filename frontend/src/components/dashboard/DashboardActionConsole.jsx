import React from 'react'
import {
  ExperimentOutlined,
  MailOutlined,
  RightOutlined,
  SettingOutlined,
  SlidersOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'

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

// 获取未被固定顺序命中的其他操作。
const getRemainingOperations = (operations) => {
  const orderedKeys = new Set([...PRIMARY_OPERATION_KEYS, ...SECONDARY_OPERATION_KEYS])
  return (operations || []).filter(operation => !orderedKeys.has(operation.key))
}

// 渲染单个行动按钮。
function OperationButton({ operation, variant = 'secondary', onRunOperation }) {
  return (
    <button
      className={`terminal-action-button terminal-action-button-${variant}${operation.danger ? ' terminal-action-button-danger' : ''}`}
      type="button"
      onClick={() => onRunOperation(operation)}
    >
      <span className="terminal-action-icon">{OPERATION_ICONS[operation.key] || <RightOutlined />}</span>
      <span className="terminal-action-copy">
        <strong>{operation.title}</strong>
        {operation.description && <small>{operation.description}</small>}
      </span>
      <RightOutlined className="terminal-action-arrow" />
    </button>
  )
}

// 首页右侧战术行动控制台。
function DashboardActionConsole({ operations, onRunOperation }) {
  const primaryOperations = pickOperations(operations, PRIMARY_OPERATION_KEYS)
  const secondaryOperations = [
    ...pickOperations(operations, SECONDARY_OPERATION_KEYS),
    ...getRemainingOperations(operations),
  ]

  return (
    <section className="terminal-panel terminal-action-console">
      <div className="terminal-panel-header">
        <div>
          <h2>战术行动控制台</h2>
          <p>执行回测、调仓、轮动与日报动作</p>
        </div>
        <SettingOutlined />
      </div>

      <div className="terminal-primary-actions">
        {primaryOperations.map(operation => (
          <OperationButton
            key={operation.key}
            operation={operation}
            variant="primary"
            onRunOperation={onRunOperation}
          />
        ))}
      </div>

      <div className="terminal-secondary-actions">
        {secondaryOperations.map(operation => (
          <OperationButton
            key={operation.key}
            operation={operation}
            onRunOperation={onRunOperation}
          />
        ))}
      </div>

      <button
        className="terminal-settings-button"
        type="button"
        onClick={() => onRunOperation({ targetPath: '/system-config' })}
      >
        <SettingOutlined />
        <span>系统基础策略参数设置</span>
      </button>
    </section>
  )
}

export default DashboardActionConsole
