import React from 'react'
import { Link } from 'react-router-dom'
import {
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
  if (level === 'extreme') return '深度低位'
  if (level === 'buy') return '低位关注'
  return '继续观察'
}

// 根据 MA 信号获取展示等级。
const getMaSignalLevel = (record) => {
  if (record.isSellSignal) return 'sell'
  if (record.isBuySignal) return 'buy'
  return 'watch'
}

// 根据 MA 信号等级获取展示文案。
const getMaSignalLabel = (level) => {
  if (level === 'sell') return '趋势转弱'
  if (level === 'buy') return '趋势转强'
  return '继续观察'
}

// ETF 超卖机会矩阵。
function EtfOpportunityMatrix({ etfOpportunities, canRunSimulation }) {
  return (
    <section className="terminal-panel signal-matrix-panel">
      <div className="terminal-panel-header">
        <div>
          <h2>ETF 机会</h2>
          <p>RSI 进入低位的关注标的</p>
        </div>
        <Link to="/rsi-analysis">查看全部</Link>
      </div>

      <div className="terminal-signal-grid">
        {etfOpportunities.map((record, index) => {
          const level = getEtfSignalLevel(record)
          const hasPeriod = record.period !== null && record.period !== undefined && record.period !== ''
          const cardKey = hasPeriod ? `${record.code}-${record.period}` : `${record.code}-missing-period-${index}`
          const cardContent = (
            <>
              <div className="terminal-signal-main">
                <strong>{record.name}</strong>
                <span>{record.code}</span>
                <small>时间：{record.dataTime || '未返回'}</small>
              </div>
              <div className="terminal-signal-value">
                <b>
                  {hasPeriod
                    ? `RSI ${record.period} 日 ${formatNumber(record.currentRsi, 2)}`
                    : `RSI 周期未返回（当前值 ${formatNumber(record.currentRsi, 2)}）`}
                </b>
                <em>{getEtfSignalLabel(level)}</em>
              </div>
            </>
          )
          return canRunSimulation ? (
            <Link
              className={`terminal-signal-card terminal-signal-card-${level}`}
              key={cardKey}
              to={`/rsi-backtest?etfCode=${encodeURIComponent(record.code)}`}
              aria-label={`查看${record.name}（${record.code}）的 RSI 模拟`}
            >
              {cardContent}
            </Link>
          ) : (
            <article
              className={`terminal-signal-card terminal-signal-card-${level}`}
              key={cardKey}
              aria-label={`${record.name}（${record.code}）的 RSI 观察信息`}
            >
              {cardContent}
            </article>
          )
        })}
        {etfOpportunities.length === 0 && (
          <div className="terminal-empty">暂时没有进入 RSI 低位的 ETF</div>
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
          <h2>趋势变化</h2>
          <p>当前出现 {maSignals.length} 条均线变化</p>
        </div>
        <Link to="/ma-strategy">查看全部</Link>
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
          <div className="terminal-empty terminal-empty-inline">当前没有需要关注的均线变化</div>
        )}
      </div>
    </section>
  )
}

// ETF 与 MA 信号中心面板。
function SignalTables({ etfOpportunities, maSignals, canRunSimulation = true }) {
  return (
    <div className="terminal-center-stack">
      <EtfOpportunityMatrix etfOpportunities={etfOpportunities} canRunSimulation={canRunSimulation} />
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
          <h2>基金优选</h2>
          <p>根据长期收益与回撤筛选</p>
        </div>
        <Link to="/fund-recommendation">查看全部</Link>
      </div>

      <div className="terminal-fund-list">
        {fundRecommendations.map(record => {
          const isHolding = record.tag === '已持有'
          const isExcluded = record.tag === '已排除'
          const displayTag = !record.tag || record.tag === '推荐' ? '候选' : record.tag
          return (
            <article className={`terminal-fund-card${isHolding ? ' terminal-fund-card-holding' : ''}${isExcluded ? ' terminal-fund-card-excluded' : ''}`} key={record.fundCode}>
              <div>
                <strong>{record.fundName}</strong>
                <span>代码：{record.fundCode}</span>
              </div>
              <em>
                {isHolding ? <WalletOutlined /> : isExcluded ? <AlertOutlined /> : <CheckCircleOutlined />}
                {displayTag}
              </em>
              <small>更新：{record.dataTime || '未返回'}</small>
            </article>
          )
        })}
        {fundRecommendations.length === 0 && (
          <div className="terminal-empty">暂时没有符合筛选条件的基金</div>
        )}
      </div>
    </section>
  )
}

export default SignalTables
