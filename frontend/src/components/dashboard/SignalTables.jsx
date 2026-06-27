import React from 'react'
import { Link } from 'react-router-dom'
import {
  AimOutlined,
  AlertOutlined,
  CheckCircleOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { formatNumber } from '../../utils/formatters'

// RSI 极度超卖阈值用于视觉强调。
const EXTREME_RSI_THRESHOLD = 25

// 根据 RSI 数值获取 ETF 信号等级。
const getEtfSignalLevel = (record) => {
  if (record.isBuySignal && Number(record.currentRsi) <= EXTREME_RSI_THRESHOLD) return 'extreme'
  if (record.isBuySignal) return 'buy'
  return 'watch'
}

// 根据 RSI 信号等级获取展示文案。
const getEtfSignalLabel = (level) => {
  if (level === 'extreme') return '极度超卖'
  if (level === 'buy') return '关注买入'
  return '观望'
}

// 根据 MA 信号获取展示等级。
const getMaSignalLevel = (record) => {
  if (record.isSellSignal) return 'sell'
  if (record.isBuySignal) return 'buy'
  return 'watch'
}

// 根据 MA 信号等级获取展示文案。
const getMaSignalLabel = (level) => {
  if (level === 'sell') return '卖出死叉'
  if (level === 'buy') return '买入金叉'
  return '观望'
}

// ETF 超卖机会矩阵。
function EtfOpportunityMatrix({ etfOpportunities }) {
  return (
    <section className="terminal-panel signal-matrix-panel">
      <div className="terminal-panel-header">
        <div>
          <h2>触发超卖信号的交易标的</h2>
          <p>RSI 监控面板</p>
        </div>
        <Link to="/rsi-analysis">更多</Link>
      </div>

      <div className="terminal-signal-grid">
        {etfOpportunities.map(record => {
          const level = getEtfSignalLevel(record)
          return (
            <article className={`terminal-signal-card terminal-signal-card-${level}`} key={record.code}>
              <div className="terminal-signal-main">
                <strong>{record.name}</strong>
                <span>{record.code}</span>
                <small>时间：{record.dataTime || '未返回'}</small>
              </div>
              <div className="terminal-signal-value">
                <b>RSI {formatNumber(record.currentRsi, 2)}</b>
                <em>{getEtfSignalLabel(level)}</em>
              </div>
            </article>
          )
        })}
        {etfOpportunities.length === 0 && (
          <div className="terminal-empty">当前没有 ETF 机会</div>
        )}
      </div>
    </section>
  )
}

// MA 买卖信号警报栏。
function MaSignalAlertStrip({ maSignals }) {
  return (
    <section className="terminal-panel ma-alert-panel">
      <div className="terminal-panel-header terminal-panel-header-compact">
        <div>
          <h2>MA 均线变盘警报</h2>
          <p>买卖信号：{maSignals.length}</p>
        </div>
        <Link to="/ma-strategy">更多</Link>
      </div>

      <div className="terminal-ma-list">
        {maSignals.map(record => {
          const level = getMaSignalLevel(record)
          return (
            <article className={`terminal-ma-card terminal-ma-card-${level}`} key={record.etfCode}>
              <div>
                <strong>{record.etfName}</strong>
                <span>{record.etfCode}</span>
              </div>
              <div>
                <b>{getMaSignalLabel(level)}</b>
                <small>{record.signalDescription}</small>
              </div>
              <em>当前价 {formatNumber(record.currentDaily, 3)}</em>
            </article>
          )
        })}
        {maSignals.length === 0 && (
          <div className="terminal-empty terminal-empty-inline">当前没有 MA 信号</div>
        )}
      </div>
    </section>
  )
}

// ETF 与 MA 信号中心面板。
function SignalTables({ etfOpportunities, maSignals }) {
  return (
    <div className="terminal-center-stack">
      <div className="terminal-center-title">
        <span><AimOutlined /> 核心战场</span>
        <strong>ETF 降维机会矩阵</strong>
      </div>
      <EtfOpportunityMatrix etfOpportunities={etfOpportunities} />
      <MaSignalAlertStrip maSignals={maSignals} />
    </div>
  )
}

// 基金推荐与持仓状态面板。
export function FundRecommendationPanel({ fundRecommendations }) {
  return (
    <section className="terminal-panel fund-recommend-panel">
      <div className="terminal-panel-header">
        <div>
          <h2>核心持仓与推荐混合</h2>
          <p>基金推荐摘要</p>
        </div>
        <Link to="/fund-recommendation">更多</Link>
      </div>

      <div className="terminal-fund-list">
        {fundRecommendations.map(record => {
          const isHolding = record.tag === '已持有'
          const isExcluded = record.tag === '已排除'
          return (
            <article className={`terminal-fund-card${isHolding ? ' terminal-fund-card-holding' : ''}${isExcluded ? ' terminal-fund-card-excluded' : ''}`} key={record.fundCode}>
              <div>
                <strong>{record.fundName}</strong>
                <span>代码：{record.fundCode}</span>
              </div>
              <em>
                {isHolding ? <WalletOutlined /> : isExcluded ? <AlertOutlined /> : <CheckCircleOutlined />}
                {record.tag || '推荐'}
              </em>
              <small>更新：{record.dataTime || '未返回'}</small>
            </article>
          )
        })}
        {fundRecommendations.length === 0 && (
          <div className="terminal-empty">当前没有基金推荐摘要</div>
        )}
      </div>
    </section>
  )
}

export default SignalTables
