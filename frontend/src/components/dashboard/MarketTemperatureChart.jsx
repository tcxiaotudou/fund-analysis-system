import React from 'react'
import { Alert } from 'antd'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

// 市场温度趋势图。
function MarketTemperatureChart({ data }) {
  // 后端当前可能只返回最新点，单点折线需要显示点位。
  const trendDot = data.length === 1 ? { r: 4, strokeWidth: 2 } : false

  return (
    <section className="dashboard-panel market-chart-panel">
      <div className="dashboard-panel-header">
        <div>
          <h2>市场温度（RSI趋势）</h2>
          <p>阈值说明：&gt;70 超买，57-70 偏热，43-57 中性，30-43 偏冷，&lt;30 超卖</p>
        </div>
      </div>
      <div className="market-chart-body">
        {data.length === 0 ? (
          <Alert type="warning" showIcon message="趋势数据为空" />
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={data}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Legend />
              <ReferenceLine y={70} stroke="#dc2626" strokeDasharray="4 4" label="70" />
              <ReferenceLine y={57} stroke="#d97706" strokeDasharray="4 4" label="57" />
              <ReferenceLine y={43} stroke="#2563eb" strokeDasharray="4 4" label="43" />
              <ReferenceLine y={30} stroke="#0f9f6e" strokeDasharray="4 4" label="30" />
              <Line type="monotone" dataKey="rsi14" name="14日RSI" stroke="#2563eb" dot={trendDot} strokeWidth={2} />
              <Line type="monotone" dataKey="rsi90" name="90日RSI" stroke="#0f9f6e" dot={trendDot} strokeWidth={2} />
              <Line type="monotone" dataKey="portfolioRsi" name="组合RSI" stroke="#f59e0b" dot={trendDot} strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </section>
  )
}

export default MarketTemperatureChart
