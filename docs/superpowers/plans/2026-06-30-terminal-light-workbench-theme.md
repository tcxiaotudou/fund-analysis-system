# Terminal Light Workbench Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert all terminal-style fund analysis pages from the current high-contrast dark theme to a unified light workbench theme.

**Architecture:** Keep the existing React components, routes, state, and class names intact. Implement the change in the shared terminal CSS layer so the dashboard, `TerminalPage` pages, Ant Design overrides, popups, and chart styling inherit one consistent light theme.

**Tech Stack:** React 18, Vite 4, Ant Design 5, Recharts 2, CSS custom properties.

## Global Constraints

- Do not change business logic, data requests, backtest flows, route definitions, or table fields.
- Do not add theme switching, new dependencies, new component libraries, or a layout redesign.
- Do not add fallback behavior, mock success paths, or swallowed errors.
- Keep edits focused on `frontend/src/assets/css/index.css` unless browser QA reveals a visual issue that requires a narrowly scoped class change.
- Use the existing terminal class names so all terminal-style pages inherit the new theme.
- Verify build output and rendered desktop/mobile UI before claiming completion.

---

### Task 1: Shared Terminal Light Theme

**Files:**
- Modify: `frontend/src/assets/css/index.css`

**Interfaces:**
- Consumes: Existing `.app-layout-terminal`, `.terminal-dashboard`, `.terminal-page`, `.terminal-panel`, Ant Design terminal overrides, and Recharts terminal selectors.
- Produces: A light workbench theme through the same CSS selectors and custom properties.

- [ ] **Step 1: Inspect the current terminal theme selectors**

Run: `rg -n "app-layout-terminal|terminal-dashboard|terminal-page|ant-modal|ant-select-dropdown|ant-picker-dropdown|ant-message|recharts" frontend/src/assets/css/index.css`

Expected: Output lists the shared terminal theme block, page-level overrides, popup overrides, message styles, and chart styles.

- [ ] **Step 2: Replace the app shell and terminal custom properties**

In `frontend/src/assets/css/index.css`, change the terminal shell and variable block to this light workbench direction:

```css
.app-layout-terminal,
.app-layout-terminal .ant-layout {
  background: #eef3f8;
}

.app-layout-terminal .app-header {
  background: #f8fafc;
  border-bottom: 1px solid #dbe4ee;
  box-shadow: none;
  color: #243244;
}

.app-layout-terminal .app-header h1 {
  color: #182536;
  letter-spacing: 0;
}

.app-layout-terminal .app-sider {
  background: #f8fafc !important;
  border-right: 1px solid #dbe4ee;
}

.terminal-dashboard {
  --terminal-bg: #eef3f8;
  --terminal-panel: #ffffff;
  --terminal-panel-strong: #f8fafc;
  --terminal-border: #dbe4ee;
  --terminal-border-strong: #b9c7d8;
  --terminal-text: #182536;
  --terminal-muted: #53657a;
  --terminal-dim: #718196;
  --terminal-cyan: #087f8c;
  --terminal-blue: #2563eb;
  --terminal-green: #15803d;
  --terminal-amber: #b45309;
  --terminal-red: #be123c;
}
```

- [ ] **Step 3: Convert shared terminal containers and controls**

Update the terminal CSS block so common containers use light surfaces, subtle borders, and low-intensity shadows:

```css
.dashboard-command-bar,
.terminal-panel,
.terminal-page .ant-card {
  border-color: var(--terminal-border);
  background: var(--terminal-panel);
  box-shadow: 0 10px 26px rgba(24, 37, 54, 0.08);
}

.terminal-live-dot {
  background: var(--terminal-cyan);
  box-shadow: 0 0 0 3px rgba(8, 127, 140, 0.12);
}
```

For child cards, stat chips, info boxes, action buttons, rows, meters, and empty states, replace dark fills such as `rgba(2, 6, 23, ...)`, `rgba(15, 23, 42, ...)`, and `#0f172a` with light fills such as `#f8fafc`, `#f1f5f9`, or semantic transparent tints.

- [ ] **Step 4: Convert Ant Design overrides inside terminal pages**

Update terminal Ant Design selectors so cards, tables, pagination, buttons, inputs, alerts, modal content, dropdowns, picker panels, filter menus, and messages no longer render dark surfaces:

```css
.terminal-page .ant-table-thead > tr > th {
  background: #eef4fb !important;
  color: var(--terminal-muted);
}

.terminal-page .ant-table-tbody > tr > td {
  background: #ffffff;
  color: var(--terminal-text);
}

.terminal-page .ant-input,
.terminal-page .ant-input-number,
.terminal-page .ant-picker,
.terminal-page .ant-select-selector,
.terminal-page .ant-input-affix-wrapper {
  background: #ffffff !important;
  color: var(--terminal-text) !important;
}
```

Use equivalent light styles for `.ant-modal`, `.ant-select-dropdown`, `.ant-picker-dropdown`, `.ant-dropdown`, `.ant-table-filter-dropdown`, and `body .ant-message .ant-message-notice-content`.

- [ ] **Step 5: Convert chart and semantic row styling**

Update chart grid, axis, legend, tooltip, buy/sell/highlight rows, and semantic info boxes to work on light surfaces:

```css
.terminal-page .recharts-cartesian-grid line {
  stroke: rgba(148, 163, 184, 0.42);
}

.terminal-page .buy-row td {
  background: rgba(22, 163, 74, 0.08) !important;
}

.terminal-page .sell-row td {
  background: rgba(225, 29, 72, 0.08) !important;
}
```

- [ ] **Step 6: Run the frontend build**

Run: `npm run build`

Expected: Vite exits with code 0 and writes production assets to `frontend/dist`.

- [ ] **Step 7: Browser QA desktop**

Start or reuse the frontend app and inspect:

- `http://10.55.68.231:8888/`
- `http://10.55.68.231:8888/momentum-strategy`

Expected: Both pages render a light workbench theme; no obvious dark shell, dark table, dark popup, text clipping, or framework error overlay appears.

- [ ] **Step 8: Browser QA mobile**

Inspect `http://10.55.68.231:8888/momentum-strategy` at a mobile-width viewport.

Expected: Content remains readable, navigation opens/closes, and no primary controls overlap or overflow.

- [ ] **Step 9: Commit implementation**

Run:

```bash
git add frontend/src/assets/css/index.css docs/superpowers/plans/2026-06-30-terminal-light-workbench-theme.md
git commit -m "style: add terminal light workbench theme"
```

Expected: Git creates one implementation commit containing the CSS theme update and this plan.
