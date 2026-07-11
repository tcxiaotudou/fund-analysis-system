/**
 * 基金推荐页面
 * 展示优质基金推荐列表
 */
import React, { useState, useEffect, useRef } from 'react'
import { Alert, Card, Table, Tag, Button, Space, Tooltip, Modal, Input, message } from 'antd'
import { ReloadOutlined, StarOutlined, StarFilled, StopOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import TerminalPage from '../components/TerminalPage'
import { fundApi } from '../services/api'

const { TextArea } = Input

function FundRecommendation() {
  const navigate = useNavigate()
  const recommendationRequestIdRef = useRef(0)
  // 推荐列表加载状态
  const [loading, setLoading] = useState(false)
  // 页面加载错误
  const [pageError, setPageError] = useState('')
  // 推荐列表数据
  const [data, setData] = useState([])
  // 基金黑名单数据
  const [blacklist, setBlacklist] = useState([])
  // 暂不关注状态加载提示
  const [blacklistWarning, setBlacklistWarning] = useState('')
  // 排除弹窗显示状态
  const [excludeModalVisible, setExcludeModalVisible] = useState(false)
  // 当前操作基金
  const [currentFund, setCurrentFund] = useState(null)
  // 排除原因
  const [excludeReason, setExcludeReason] = useState('')
  // 已持有基金代码集合
  const [holdings, setHoldings] = useState(new Set())
  // 第三方推荐数据刷新状态
  const [refreshingRemote, setRefreshingRemote] = useState(false)
  // 第三方推荐数据刷新阶段
  const [refreshStage, setRefreshStage] = useState('')

  /**
   * 应用基金推荐数据
   * @param {Array} newData 基金推荐数据
   */
  const applyRecommendationData = (newData) => {
    setData(newData)

    // 提取持有状态
    const holdingSet = new Set()
    newData.forEach(fund => {
      if (fund.isHolding === 1) {
        holdingSet.add(fund.fundCode)
      }
    })
    setHoldings(holdingSet)
  }

  /**
   * 加载基金推荐数据
   */
  const loadData = async () => {
    const requestId = ++recommendationRequestIdRef.current
    try {
      setLoading(true)
      setRefreshingRemote(false)
      setRefreshStage('')
      setPageError('')
      
      const response = await fundApi.getRecommendations()
      if (requestId !== recommendationRequestIdRef.current) return

      if (response.code === 0) {
        // 创建全新的数组，避免引用问题
        const newData = [...(response.data || [])]
        applyRecommendationData(newData)
      } else {
        console.error('加载基金优选服务返回失败:', response)
        setPageError(`基金优选暂时无法读取（错误码：${response.code}），请稍后重试。`)
      }
    } catch (error) {
      console.error('加载基金推荐数据失败:', error)
      if (requestId === recommendationRequestIdRef.current) {
        setPageError('基金优选暂时无法读取，请检查网络连接后重试。')
      }
    } finally {
      if (requestId === recommendationRequestIdRef.current) {
        setLoading(false)
      }
    }
  }

  /**
   * 加载黑名单数据
   */
  const loadBlacklist = async () => {
    try {
      setBlacklistWarning('')
      const response = await fundApi.getBlacklist()
      if (response.code === 0) {
        setBlacklist(response.data || [])
      } else {
        console.error('加载暂不关注名单服务返回失败:', response)
        setBlacklistWarning(`部分关注状态暂时无法读取（错误码：${response.code}），请重试。`)
      }
    } catch (error) {
      console.error('加载黑名单数据失败:', error)
      setBlacklistWarning('部分关注状态暂时无法读取，请检查网络连接后重试。')
    }
  }

  /**
   * 检查基金是否在黑名单中
   */
  const isBlacklisted = (fundCode) => {
    return blacklist.find(item => item.fundCode === fundCode)
  }

  /**
   * 显示排除对话框
   */
  const showExcludeModal = (fund) => {
    setCurrentFund(fund)
    setExcludeReason('')
    setExcludeModalVisible(true)
  }

  /**
   * 确认排除基金
   */
  const handleExclude = async () => {
    if (!excludeReason.trim()) {
      message.warning('请填写暂不关注原因')
      return
    }

    try {
      const response = await fundApi.addToBlacklist({
        fundCode: currentFund.fundCode,
        fundName: currentFund.fundName,
        excludeReason: excludeReason.trim(),
      })

      if (response.code === 0) {
        message.success('已暂不关注')
        setExcludeModalVisible(false)
        setCurrentFund(null)
        setExcludeReason('')
        await loadBlacklist()
      } else {
        console.error('暂不关注基金服务返回失败:', response)
        message.error(`暂不关注失败（错误码：${response.code}）`)
      }
    } catch (error) {
      console.error('排除基金失败:', error)
      message.error('暂不关注失败，请稍后重试')
    }
  }

  /**
   * 取消排除基金
   */
  const handleCancelExclude = async (fundCode) => {
    try {
      const response = await fundApi.removeFromBlacklist(fundCode)
      
      if (response.code === 0) {
        message.success('已恢复关注')
        await loadBlacklist()
      } else {
        console.error('恢复关注基金服务返回失败:', response)
        message.error(`恢复关注失败（错误码：${response.code}）`)
      }
    } catch (error) {
      console.error('取消排除失败:', error)
      message.error('恢复关注失败，请稍后重试')
    }
  }

  /**
   * 标记/取消标记持有
   */
  const handleToggleHolding = async (fundCode, currentIsHolding) => {
    try {
      const response = await fundApi.updateHoldingStatus(fundCode, currentIsHolding ? 0 : 1)
      
      if (response.code === 0) {
        message.success(currentIsHolding ? '已移出组合' : '已加入组合')
        // 更新本地状态
        const newHoldings = new Set(holdings)
        if (currentIsHolding) {
          newHoldings.delete(fundCode)
        } else {
          newHoldings.add(fundCode)
        }
        setHoldings(newHoldings)
        
        // 重新加载数据以确保同步
        await loadData()
      } else {
        console.error('更新组合基金服务返回失败:', response)
        message.error(`${currentIsHolding ? '移出组合' : '加入组合'}失败（错误码：${response.code}）`)
      }
    } catch (error) {
      console.error('更新持有状态失败:', error)
      message.error(`${currentIsHolding ? '移出组合' : '加入组合'}失败，请稍后重试`)
    }
  }

  /**
   * 使用系统配置重新获取最新基金推荐
   */
  const runRefreshRecommendations = async () => {
    const requestId = ++recommendationRequestIdRef.current
    try {
      setRefreshingRemote(true)
      setLoading(false)
      setPageError('')
      setRefreshStage('正在根据已保存的筛选条件从数据源筛选基金...')
      const response = await fundApi.refreshRecommendations()
      if (requestId !== recommendationRequestIdRef.current) return

      if (response.code === 0) {
        const newData = [...(response.data || [])]
        applyRecommendationData(newData)
        await loadBlacklist()
        if (requestId !== recommendationRequestIdRef.current) return
        setRefreshStage(`已筛选 ${newData.length} 只基金`)
        message.success('基金优选已更新')
      } else {
        console.error('重新筛选基金服务返回失败:', response)
        setRefreshStage('')
        setPageError(`基金优选重新筛选失败（错误码：${response.code}），请稍后重试。`)
      }
    } catch (error) {
      console.error('重新获取基金推荐数据失败:', error)
      if (requestId === recommendationRequestIdRef.current) {
        setRefreshStage('')
        setPageError('基金优选重新筛选失败，请检查数据源连接后重试。')
      }
    } finally {
      if (requestId === recommendationRequestIdRef.current) {
        setRefreshingRemote(false)
        setTimeout(() => {
          if (requestId === recommendationRequestIdRef.current) {
            setRefreshStage('')
          }
        }, 3000)
      }
    }
  }

  /**
   * 确认后重新获取最新基金推荐
   */
  const handleRefreshRecommendations = () => {
    Modal.confirm({
      title: '确认重新筛选基金？',
      content: '将按已保存的筛选条件从数据源重新生成基金优选名单。',
      okText: '重新筛选',
      cancelText: '取消',
      onOk: runRefreshRecommendations,
    })
  }

  useEffect(() => {
    loadData()
    loadBlacklist()
  }, [])

  /**
   * 表格列配置
   */
  const columns = [
    {
      title: '排名',
      key: 'index',
      width: 70,
      render: (_, record, index) => {
        // 被排除的基金不显示排名
        if (isBlacklisted(record.fundCode)) {
          return <span style={{ color: 'var(--terminal-dim)' }}>-</span>
        }
        
        // 计算实际排名（不包括被排除的基金）
        const validFunds = data.filter(fund => !isBlacklisted(fund.fundCode))
        const rank = validFunds.findIndex(fund => fund.fundCode === record.fundCode) + 1
        
        let icon = null
        if (rank === 1) icon = '🥇'
        else if (rank === 2) icon = '🥈'
        else if (rank === 3) icon = '🥉'
        return <span>{icon} {rank}</span>
      },
    },
    {
      title: '基金名称',
      dataIndex: 'fundName',
      key: 'fundName',
      width: 200,
      render: (text, record) => (
        <div>
          <div>{text}</div>
          <div style={{ fontSize: '12px', color: 'var(--terminal-dim)' }}>{record.fundCode}</div>
        </div>
      ),
    },
    {
      title: '基金经理',
      dataIndex: 'managerName',
      key: 'managerName',
      width: 100,
    },
    {
      title: '管理时长',
      dataIndex: 'managerYears',
      key: 'managerYears',
      width: 100,
    },
    {
      title: '规模',
      dataIndex: 'scale',
      key: 'scale',
      width: 100,
    },
    {
      title: '卡玛比率排名',
      dataIndex: 'calmarRank',
      key: 'calmarRank',
      width: 130,
      render: (val) => {
        if (val == null || val === '') return '-'
        let color = 'var(--terminal-text)'
        if (val <= 10) color = '#cf1322'  // 前10名红色
        else if (val <= 30) color = '#fa8c16'  // 前30名橙色
        else if (val <= 50) color = '#1890ff'  // 前50名蓝色
        return (
          <Tooltip title="卡玛比率排名越小越好，表示单位回撤下的收益越高">
            <strong style={{ color }}>{val}</strong>
          </Tooltip>
        )
      },
    },
    {
      title: '今年收益率',
      dataIndex: 'yearToDateReturn',
      key: 'yearToDateReturn',
      width: 120,
      render: (val) => {
        if (val == null || val === '') return '-'
        const num = parseFloat(val)
        const color = num > 0 ? '#cf1322' : '#3f8600'
        return <span style={{ color }}>{val}</span>
      },
    },
    {
      title: '近5年年化',
      dataIndex: 'fiveYearReturn',
      key: 'fiveYearReturn',
      width: 120,
      render: (val) => {
        if (val == null || val === '') return '-'
        const color = val > 10 ? '#cf1322' : val > 5 ? '#fa8c16' : 'var(--terminal-text)'
        return <strong style={{ color }}>{val?.toFixed(2)}%</strong>
      },
    },
    {
      title: '赎回费率',
      dataIndex: 'redemptionFee',
      key: 'redemptionFee',
      ellipsis: {
        showTitle: false,
      },
      render: (text) => (
        <Tooltip placement="topLeft" title={text}>
          {text}
        </Tooltip>
      ),
    },
    {
      title: '数据时间',
      dataIndex: 'dataTime',
      key: 'dataTime',
      width: 180,
      render: (val) => {
        if (val == null || val === '') return '-'
        // 如果是时间戳，转换为日期字符串
        const date = new Date(val)
        return date.toLocaleString('zh-CN', { 
          year: 'numeric', 
          month: '2-digit', 
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      className: 'terminal-theme-action-column',
      render: (_, record) => {
        const blacklistItem = isBlacklisted(record.fundCode)
        const isHolding = holdings.has(record.fundCode)
        
        if (blacklistItem) {
          return (
            <Space size="small" direction="vertical" className="terminal-full-width">
              <Tooltip title={`原因：${blacklistItem.excludeReason}`}>
                <Tag color="red" icon={<StopOutlined />} style={{ margin: 0 }}>暂不关注</Tag>
              </Tooltip>
              <Button 
                type="link" 
                size="small"
                onClick={() => handleCancelExclude(record.fundCode)}
                style={{ padding: 0, height: 'auto' }}
              >
                恢复关注
              </Button>
            </Space>
          )
        }
        
        return (
          <Space size="small" wrap>
            <Button 
              type={isHolding ? 'primary' : 'default'}
              size="small"
              icon={isHolding ? <StarFilled /> : <StarOutlined />}
              onClick={() => handleToggleHolding(record.fundCode, isHolding)}
            >
              {isHolding ? '移出组合' : '加入组合'}
            </Button>
            <Button 
              type="link" 
              size="small"
              danger
              onClick={() => showExcludeModal(record)}
            >
              暂不关注
            </Button>
          </Space>
        )
      },
    },
  ]

  return (
    <TerminalPage
      title="基金优选"
      subtitle="按长期收益、回撤与基金经理维度整理候选名单"
      status={
        pageError ? (
          <span>数据不可用</span>
        ) : data.length > 0 && data[0]?.dataTime ? (
          <span>
            数据时间：{new Date(data[0].dataTime).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit', 
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit'
            })}
          </span>
        ) : (
          <span>共 {data.length} 只基金</span>
        )
      }
      actions={
        <Button onClick={() => navigate('/fund-portfolio')}>
          查看组合观察
        </Button>
      }
    >

      {pageError && (
        <Alert
          type="error"
          showIcon
          message="基金优选加载失败"
          description={pageError}
          action={<Button size="small" onClick={loadData}>重试</Button>}
          className="terminal-section-gap"
        />
      )}

      {blacklistWarning && (
        <Alert
          type="warning"
          showIcon
          message="部分关注状态加载失败"
          description={blacklistWarning}
          action={<Button size="small" onClick={loadBlacklist}>重试</Button>}
          className="terminal-section-gap"
        />
      )}

      <Card title="优选基金">
        {/* 操作栏 */}
        <Space className="terminal-toolbar" wrap>
          {refreshStage && <Tag color={refreshingRemote ? 'processing' : 'success'}>{refreshStage}</Tag>}
          <Button 
            type="primary" 
            icon={<ReloadOutlined />}
            onClick={handleRefreshRecommendations}
            loading={refreshingRemote}
            disabled={loading}
          >
            重新筛选
          </Button>
          <Button 
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
            disabled={refreshingRemote}
          >
            重新载入
          </Button>
        </Space>

        {/* 数据表格 */}
        <Table
          columns={columns}
          dataSource={data}
          rowKey="fundCode"
          loading={loading || refreshingRemote}
          pagination={false}
          scroll={{ x: 1600 }}
          locale={{
            emptyText: (loading || refreshingRemote)
              ? '数据加载中...'
              : pageError
                ? '基金优选加载失败，请重试'
                : '暂时没有符合条件的基金',
          }}
          rowClassName={(record) => {
            if (isBlacklisted(record.fundCode)) {
              return 'blacklisted-row'
            }
            if (holdings.has(record.fundCode)) {
              return 'holding-row'
            }
            return ''
          }}
          className="terminal-theme-table"
        />
      </Card>

      {/* 优选规则说明 */}
      <Card title="优选规则">
        <div className="terminal-copy-block">
          <h3>候选名单的整理规则：</h3>
          <dl className="terminal-definition-list">
            <div>
              <dt>数据来源</dt>
              <dd>公开基金数据源</dd>
            </div>
            <div>
              <dt>筛选条件</dt>
              <dd>由服务设置中的筛选组合编号决定。</dd>
            </div>
            <div>
              <dt>基金经理去重</dt>
              <dd>同一基金经理只保留一只基金。</dd>
            </div>
            <div>
              <dt>排序依据</dt>
              <dd>按<span style={{ color: '#cf1322', fontWeight: 'bold' }}>卡玛比率排名</span>从优到劣排序，排名越小越好。</dd>
            </div>
            <div>
              <dt>展示数量</dt>
              <dd>展示前 12 只基金。</dd>
            </div>
          </dl>

          <h3 className="terminal-field-offset-xl">关键指标说明：</h3>
          <ul>
            <li><strong>卡玛比率（Calmar Ratio）：</strong>收益与最大回撤的比值，衡量单位回撤下能获得多少收益，数值越大越好。排名越小表示卡玛比率越高。</li>
            <li><strong>近5年年化收益率：</strong>过去5年的年化收益率，反映基金的长期收益能力</li>
            <li><strong>今年收益率：</strong>今年以来的累计收益率，反映基金的短期表现</li>
            <li><strong>赎回费率：</strong>不同持有时长对应的赎回费率</li>
          </ul>

          <div className="terminal-info-box terminal-info-box-cyan terminal-field-offset-lg">
            <strong>为什么使用卡玛比率排序？</strong>
            <p className="terminal-inline-note-space">
              卡玛比率同时纳入收益与最大回撤，可用于比较候选基金在历史收益和回撤之间的相对表现。
            </p>
          </div>

          <div className="terminal-info-box terminal-info-box-amber terminal-field-offset-lg">
            <strong>免责声明：</strong>
            <p className="terminal-inline-note-space">
              本名单仅基于历史数据筛选，不构成投资建议。
              历史业绩不代表未来表现，投资需谨慎。
            </p>
          </div>
        </div>
      </Card>

      {/* 暂不关注对话框 */}
      <Modal
        title="暂不关注此基金"
        open={excludeModalVisible}
        onOk={handleExclude}
        onCancel={() => {
          setExcludeModalVisible(false)
          setCurrentFund(null)
          setExcludeReason('')
        }}
        okText="暂不关注"
        cancelText="取消"
      >
        <div className="terminal-section-gap">
          <p><strong>基金名称：</strong>{currentFund?.fundName}</p>
          <p><strong>基金代码：</strong>{currentFund?.fundCode}</p>
        </div>
        <div>
          <label
            htmlFor="fund-exclude-reason"
            className="terminal-field-label"
            style={{ display: 'block' }}
          >
            <strong style={{ color: 'red' }}>*</strong> 原因：
          </label>
          <TextArea
            id="fund-exclude-reason"
            rows={4}
            placeholder="请填写暂不关注该基金的原因（必填）"
            value={excludeReason}
            onChange={(e) => setExcludeReason(e.target.value)}
            maxLength={500}
            showCount
          />
        </div>
      </Modal>

    </TerminalPage>
  )
}

export default FundRecommendation
