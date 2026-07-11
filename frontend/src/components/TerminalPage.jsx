import React from 'react'

// 统一非首页产品工具的标题和内容壳层。
function TerminalPage({ title, subtitle, status, actions, children }) {
  return (
    <div className="terminal-dashboard terminal-page product-tool-page">
      <header className="terminal-page-command-bar product-page-heading">
        <div className="product-page-title">
          <h1>{title}</h1>
          {subtitle && <p>{subtitle}</p>}
        </div>
        {(status || actions) && (
          <div className="product-page-meta">
            {status && <div className="terminal-page-status">{status}</div>}
            {actions && <div className="terminal-page-actions">{actions}</div>}
          </div>
        )}
      </header>
      <main className="terminal-page-body">
        {children}
      </main>
    </div>
  )
}

export default TerminalPage
