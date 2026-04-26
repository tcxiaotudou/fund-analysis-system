/**
 * 应用程序入口文件
 * 负责渲染React应用到DOM
 */
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './assets/css/index.css'

// 将应用挂载到id为root的DOM节点
ReactDOM.createRoot(document.getElementById('root')).render(
  <App />
)

