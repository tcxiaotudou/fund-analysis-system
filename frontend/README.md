# 基金分析系统 - 前端

## 技术栈

- React 18.2
- React Router 6.14
- Ant Design 5.8
- Axios 1.4
- Vite 4.4
- Recharts 2.7

## 项目结构

```
frontend/
├── src/
│   ├── components/          # 公共组件
│   │   └── MainLayout.jsx         # 主布局组件
│   │
│   ├── pages/               # 页面组件
│   │   ├── Dashboard.jsx          # 市场概览仪表板
│   │   ├── RsiAnalysis.jsx        # RSI分析页面
│   │   ├── MaStrategy.jsx         # MA策略页面
│   │   ├── FundRecommendation.jsx # 基金推荐页面
│   │   ├── EtfManagement.jsx      # ETF管理页面
│   │   └── SystemConfig.jsx       # 系统配置页面
│   │
│   ├── services/            # API服务
│   │   └── api.js                 # API接口封装
│   │
│   ├── assets/              # 静态资源
│   │   └── css/
│   │       └── index.css          # 全局样式
│   │
│   ├── App.jsx              # 主应用组件
│   └── main.jsx             # 入口文件
│
├── index.html               # HTML模板
├── vite.config.js           # Vite配置
└── package.json             # npm配置
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

应用将运行在 `http://localhost:3000`

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist/` 目录

### 预览生产构建

```bash
npm run preview
```

## 页面说明

### 1. 市场概览 (Dashboard)

**路径**: `/`

**功能**:
- 显示核心市场指标（14日RSI、90日RSI等）
- 展示ETF买入机会列表
- 展示MA策略买入信号
- 实时数据，每分钟自动刷新

**组件**: `src/pages/Dashboard.jsx`

### 2. RSI分析 (RsiAnalysis)

**路径**: `/rsi-analysis`

**功能**:
- 显示所有ETF的RSI分析结果
- 支持按名称搜索
- 支持按买入信号筛选
- 显示详细的RSI指标数据

**组件**: `src/pages/RsiAnalysis.jsx`

### 3. MA策略 (MaStrategy)

**路径**: `/ma-strategy`

**功能**:
- 显示移动平均线策略分析结果
- 展示60周均线、60日均线数据
- 标识买入信号
- 提供策略说明

**组件**: `src/pages/MaStrategy.jsx`

### 4. 基金推荐 (FundRecommendation)

**路径**: `/fund-recommendation`

**功能**:
- 展示优质基金推荐列表
- 显示基金经理、规模、收益率等信息
- 支持按收益率排序
- 显示赎回费率

**组件**: `src/pages/FundRecommendation.jsx`

### 5. ETF管理 (EtfManagement)

**路径**: `/etf-management`

**功能**:
- ETF列表的增删改查
- 配置ETF监控参数
- 设置RSI买入/卖出阈值
- 启用/禁用监控

**组件**: `src/pages/EtfManagement.jsx`

### 6. 系统配置 (SystemConfig)

**路径**: `/system-config`

**功能**:
- 配置邮件发送参数
- 配置分析阈值
- 配置缓存刷新间隔
- 保存系统配置

**组件**: `src/pages/SystemConfig.jsx`

## API服务

### API封装 (`src/services/api.js`)

所有后端接口调用都封装在 `api.js` 中，使用axios进行HTTP请求。

#### 市场数据API

```javascript
marketApi.getOverview()       // 获取市场概览
marketApi.getRiskPremium()    // 获取风险溢价
marketApi.getMa5yDeviation()  // 获取5年均线偏离度
```

#### RSI分析API

```javascript
rsiApi.calculateRsi(code, period)  // 计算RSI
rsiApi.getEtfSignals()             // 获取ETF买入信号
```

#### MA策略API

```javascript
maStrategyApi.calculateMa(code)   // 计算MA策略
maStrategyApi.getBuySignals()     // 获取买入信号
```

#### 基金API

```javascript
fundApi.getRecommendations()  // 获取基金推荐
```

#### ETF管理API

```javascript
etfApi.getList()        // 获取ETF列表
etfApi.add(data)        // 添加ETF
etfApi.update(data)     // 更新ETF
etfApi.delete(id)       // 删除ETF
```

### API响应格式

所有API返回统一格式：

```javascript
{
  code: 0,           // 0-成功 -1-失败
  message: "success",
  data: {}           // 返回数据
}
```

## 组件说明

### MainLayout

主布局组件，提供：
- 响应式侧边栏导航
- 顶部导航栏
- 内容区域容器
- 路由集成

### 公共样式

全局样式定义在 `src/assets/css/index.css` 中：

- 统计卡片样式 (`.stat-card`)
- 页面标题样式 (`.page-title`)
- 加载动画样式 (`.loading-container`)
- 响应式布局

## 开发指南

### 1. 添加新页面

```javascript
// 1. 创建页面组件
// src/pages/NewPage.jsx
import React from 'react'

function NewPage() {
  return <div>New Page</div>
}

export default NewPage

// 2. 在App.jsx中添加路由
import NewPage from './pages/NewPage'

<Route path="/new-page" element={<NewPage />} />

// 3. 在MainLayout.jsx中添加菜单项
const menuItems = [
  // ...
  {
    key: '/new-page',
    icon: <IconName />,
    label: '新页面',
  },
]
```

### 2. 调用API

```javascript
import { marketApi } from '../services/api'

// 在组件中使用
useEffect(() => {
  const loadData = async () => {
    const response = await marketApi.getOverview()
    if (response.code === 0) {
      setData(response.data)
    }
  }
  loadData()
}, [])
```

### 3. 使用Ant Design组件

```javascript
import { Card, Table, Button } from 'antd'

function MyComponent() {
  return (
    <Card title="标题">
      <Table dataSource={data} columns={columns} />
      <Button type="primary">按钮</Button>
    </Card>
  )
}
```

## 代码规范

### 1. 组件规范

- 使用函数组件和Hooks
- 组件名使用PascalCase
- 文件名与组件名一致
- 添加必要的注释

### 2. 样式规范

- 优先使用Ant Design组件样式
- 自定义样式使用className
- 避免内联样式（特殊情况除外）

### 3. 注释规范

- 文件头部添加功能说明注释
- 重要函数添加注释
- 复杂逻辑添加说明注释

## 性能优化

### 1. 代码分割

Vite自动进行代码分割，按路由懒加载。

### 2. 图片优化

- 使用适当的图片格式
- 压缩图片大小
- 使用CDN加速

### 3. 请求优化

- 使用缓存避免重复请求
- 合并相似的API请求
- 实现请求防抖和节流

## 部署

### 构建

```bash
npm run build
```

### 部署到Nginx

```nginx
server {
  listen 80;
  server_name your-domain.com;
  
  root /path/to/dist;
  index index.html;
  
  location / {
    try_files $uri $uri/ /index.html;
  }
  
  location /api {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

## 常见问题

### Q: 开发时跨域问题？
A: 已在vite.config.js中配置代理，确保后端服务已启动。

### Q: 构建后页面空白？
A: 检查base路径配置，确保路由模式正确。

### Q: Ant Design样式不生效？
A: 确保正确导入了Ant Design的CSS。

## 浏览器支持

- Chrome (最新版)
- Firefox (最新版)
- Safari (最新版)
- Edge (最新版)

不支持IE浏览器。

