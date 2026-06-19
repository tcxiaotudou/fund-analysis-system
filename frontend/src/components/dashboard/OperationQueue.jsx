import React from 'react'
import { Button, Space } from 'antd'
import { MailOutlined, RightOutlined } from '@ant-design/icons'

// 操作队列。
function OperationQueue({ operations, onRunOperation }) {
  return (
    <section className="dashboard-panel operation-panel">
      <div className="dashboard-panel-header">
        <h2>操作队列</h2>
      </div>
      <Space direction="vertical" size={10} style={{ width: '100%' }}>
        {operations.map(operation => (
          <button
            className={operation.danger ? 'operation-row operation-row-danger' : 'operation-row'}
            key={operation.key}
            type="button"
            onClick={() => onRunOperation(operation)}
          >
            <span className="operation-icon">{operation.action === 'sendEmailNow' ? <MailOutlined /> : <RightOutlined />}</span>
            <span className="operation-copy">
              <strong>{operation.title}</strong>
              <small>{operation.description}</small>
            </span>
            <RightOutlined />
          </button>
        ))}
      </Space>
      <Button block className="operation-all-button" onClick={() => onRunOperation({ targetPath: '/system-config' })}>系统设置</Button>
    </section>
  )
}

export default OperationQueue
