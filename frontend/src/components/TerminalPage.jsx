import React from 'react'

// 统一非首页页面的终端 HUD 标题和内容壳层。
function TerminalPage({ title, subtitle, status, actions, children }) {
  return (
    <div className="terminal-dashboard terminal-page">
      <header className="dashboard-command-bar terminal-page-command-bar">
        <div className="terminal-brand">
          <span className="terminal-live-dot" />
          <div>
            <h1>{title}</h1>
            {subtitle && <p>{subtitle}</p>}
          </div>
        </div>
        {status && <div className="terminal-status-strip terminal-page-status">{status}</div>}
        {actions && <div className="terminal-page-actions">{actions}</div>}
      </header>
      <main className="terminal-page-body">
        {children}
      </main>
    </div>
  )
}

export default TerminalPage
