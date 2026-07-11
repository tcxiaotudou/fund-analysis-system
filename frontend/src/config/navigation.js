export const primaryNavigation = [
  { label: '今日', path: '/' },
  { label: 'ETF 机会', path: '/rsi-analysis' },
  { label: '策略实验', path: '/momentum-strategy' },
  { label: '基金优选', path: '/fund-recommendation' },
  { label: '组合观察', path: '/fund-portfolio' },
]

export const utilityNavigation = [
  {
    label: '策略工具',
    items: [
      { label: 'RSI 模拟', path: '/rsi-backtest' },
      { label: '趋势策略', path: '/ma-strategy' },
    ],
  },
  {
    label: '管理工具',
    items: [
      { label: '关注标的', path: '/etf-management' },
      { label: '服务设置', path: '/system-config' },
    ],
  },
]
