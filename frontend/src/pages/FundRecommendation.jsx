/**
 * 基金推荐页面
 * 展示优质基金推荐列表
 */
import React, { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Tooltip, Modal, Input, message } from 'antd'
import { ReloadOutlined, SaveOutlined, StarOutlined, StarFilled, StopOutlined } from '@ant-design/icons'
import TerminalPage from '../components/TerminalPage'
import { fundApi, systemConfigApi } from '../services/api'

const { TextArea } = Input

function FundRecommendation() {
  // 推荐列表加载状态
  const [loading, setLoading] = useState(false)
  // 推荐列表数据
  const [data, setData] = useState([])
  // 基金黑名单数据
  const [blacklist, setBlacklist] = useState([])
  // 排除弹窗显示状态
  const [excludeModalVisible, setExcludeModalVisible] = useState(false)
  // 当前操作基金
  const [currentFund, setCurrentFund] = useState(null)
  // 排除原因
  const [excludeReason, setExcludeReason] = useState('')
  // 已持有基金代码集合
  const [holdings, setHoldings] = useState(new Set())
  // 基金推荐条件ID
  const [conditionId, setConditionId] = useState('')
  // 基金推荐配置加载状态
  const [configLoading, setConfigLoading] = useState(false)
  // 基金推荐配置保存状态
  const [savingConfig, setSavingConfig] = useState(false)
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
    try {
      setLoading(true)
      // 先清空数据，确保不会累加
      setData([])
      
      const response = await fundApi.getRecommendations()

      if (response.code === 0) {
        // 创建全新的数组，避免引用问题
        const newData = [...(response.data || [])]
        applyRecommendationData(newData)
      }
    } catch (error) {
      console.error('加载基金推荐数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  /**
   * 加载基金推荐配置
   */
  const loadRecommendationConfig = async () => {
    try {
      setConfigLoading(true)
      const response = await systemConfigApi.getFundRecommendationConfig()
      if (response.code === 0) {
        setConditionId(response.data?.conditionId || '')
      } else {
        message.error(response.message || '加载基金推荐配置失败')
      }
    } catch (error) {
      console.error('加载基金推荐配置失败:', error)
      message.error('加载基金推荐配置失败: ' + (error.normalizedMessage || error.message || '网络错误'))
    } finally {
      setConfigLoading(false)
    }
  }

  /**
   * 加载黑名单数据
   */
  const loadBlacklist = async () => {
    try {
      const response = await fundApi.getBlacklist()
      if (response.code === 0) {
        setBlacklist(response.data || [])
      }
    } catch (error) {
      console.error('加载黑名单数据失败:', error)
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
      message.warning('请填写排除原因')
      return
    }

    try {
      const response = await fundApi.addToBlacklist({
        fundCode: currentFund.fundCode,
        fundName: currentFund.fundName,
        excludeReason: excludeReason.trim(),
      })

      if (response.code === 0) {
        message.success('基金已排除')
        setExcludeModalVisible(false)
        setCurrentFund(null)
        setExcludeReason('')
        await loadBlacklist()
      } else {
        message.error(response.message || '排除失败')
      }
    } catch (error) {
      console.error('排除基金失败:', error)
      message.error('排除失败')
    }
  }

  /**
   * 取消排除基金
   */
  const handleCancelExclude = async (fundCode) => {
    try {
      const response = await fundApi.removeFromBlacklist(fundCode)
      
      if (response.code === 0) {
        message.success('已取消排除')
        await loadBlacklist()
      } else {
        message.error(response.message || '取消排除失败')
      }
    } catch (error) {
      console.error('取消排除失败:', error)
      message.error('取消排除失败')
    }
  }

  /**
   * 标记/取消标记持有
   */
  const handleToggleHolding = async (fundCode, currentIsHolding) => {
    try {
      const response = await fundApi.updateHoldingStatus(fundCode, currentIsHolding ? 0 : 1)
      
      if (response.code === 0) {
        message.success(currentIsHolding ? '已取消持有' : '已标记为持有')
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
        message.error(response.message || '操作失败')
      }
    } catch (error) {
      console.error('更新持有状态失败:', error)
      message.error('操作失败')
    }
  }

  /**
   * 保存基金推荐条件ID
   */
  const handleSaveConditionId = async () => {
    const nextConditionId = conditionId.trim()
    if (!nextConditionId) {
      message.warning('请输入 condition_id')
      return false
    }

    try {
      setSavingConfig(true)
      const response = await systemConfigApi.saveFundRecommendationConfig({
        conditionId: nextConditionId,
      })

      if (response.code === 0) {
        setConditionId(nextConditionId)
        message.success('基金推荐配置已保存')
        return true
      }

      message.error(response.message || '保存基金推荐配置失败')
      return false
    } catch (error) {
      console.error('保存基金推荐配置失败:', error)
      message.error('保存基金推荐配置失败: ' + (error.normalizedMessage || error.message || '网络错误'))
      return false
    } finally {
      setSavingConfig(false)
    }
  }

  /**
   * 保存条件ID并重新获取最新基金推荐
   */
  const runRefreshRecommendations = async () => {
    setRefreshStage('保存推荐配置中...')
    const saved = await handleSaveConditionId()
    if (!saved) {
      setRefreshStage('')
      return
    }

    try {
      setRefreshingRemote(true)
      setRefreshStage('正在从第三方数据源获取最新推荐...')
      setData([])
      const response = await fundApi.refreshRecommendations()

      if (response.code === 0) {
        const newData = [...(response.data || [])]
        applyRecommendationData(newData)
        await loadBlacklist()
        setRefreshStage(`已获取 ${newData.length} 条推荐数据`)
        message.success('基金推荐数据已重新获取')
      } else {
        message.error(response.message || '重新获取基金推荐数据失败')
      }
    } catch (error) {
      console.error('重新获取基金推荐数据失败:', error)
      message.error('重新获取基金推荐数据失败: ' + (error.normalizedMessage || error.message || '网络错误'))
    } finally {
      setRefreshingRemote(false)
      setTimeout(() => setRefreshStage(''), 3000)
    }
  }

  /**
   * 确认后保存配置并重新获取最新基金推荐
   */
  const handleRefreshRecommendations = () => {
    const nextConditionId = conditionId.trim()
    if (!nextConditionId) {
      message.warning('请输入 condition_id')
      return
    }
    Modal.confirm({
      title: '确认重新获取基金推荐？',
      content: '系统会先保存当前 condition_id，然后调用第三方基金数据源重新生成推荐列表。',
      okText: '重新获取',
      cancelText: '取消',
      onOk: runRefreshRecommendations,
    })
  }

  useEffect(() => {
    loadData()
    loadBlacklist()
    loadRecommendationConfig()
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
          return <span style={{ color: '#999' }}>-</span>
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
          <div style={{ fontSize: '12px', color: '#999' }}>{record.fundCode}</div>
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
        let color = '#000'
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
        const color = val > 10 ? '#cf1322' : val > 5 ? '#fa8c16' : '#000'
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
      render: (_, record) => {
        const blacklistItem = isBlacklisted(record.fundCode)
        const isHolding = holdings.has(record.fundCode)
        
        if (blacklistItem) {
          return (
            <Space size="small" direction="vertical" className="terminal-full-width">
              <Tooltip title={`排除原因: ${blacklistItem.excludeReason}`}>
                <Tag color="red" icon={<StopOutlined />} style={{ margin: 0 }}>已排除</Tag>
              </Tooltip>
              <Button 
                type="link" 
                size="small"
                onClick={() => handleCancelExclude(record.fundCode)}
                style={{ padding: 0, height: 'auto' }}
              >
                取消排除
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
              {isHolding ? '已持有' : '标记持有'}
            </Button>
            <Button 
              type="link" 
              size="small"
              danger
              onClick={() => showExcludeModal(record)}
            >
              排除
            </Button>
          </Space>
        )
      },
    },
  ]

  return (
    <TerminalPage
      title="基金推荐"
      subtitle="第三方筛选条件、推荐列表和持有/排除状态"
      status={
        data.length > 0 && data[0]?.dataTime ? (
          <span>
            数据更新时间：{new Date(data[0].dataTime).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit', 
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit'
            })}
          </span>
        ) : (
          <span>推荐数：{data.length}</span>
        )
      }
    >

      <Card title="基金推荐列表">
        {/* 操作栏 */}
        <Space className="terminal-toolbar" wrap>
          <div style={{ display: 'inline-flex', maxWidth: '100%' }}>
            <span className="terminal-input-addon">
              condition_id
            </span>
            <Input
              placeholder="请输入 condition_id"
              value={conditionId}
              onChange={(event) => setConditionId(event.target.value)}
              disabled={configLoading || savingConfig || refreshingRemote}
              style={{ width: 220, borderTopLeftRadius: 0, borderBottomLeftRadius: 0 }}
            />
          </div>
          {refreshStage && <Tag color={refreshingRemote ? 'processing' : 'success'}>{refreshStage}</Tag>}
          <Button
            icon={<SaveOutlined />}
            onClick={handleSaveConditionId}
            loading={savingConfig}
            disabled={configLoading || refreshingRemote}
          >
            保存配置
          </Button>
          <Button 
            type="primary" 
            icon={<ReloadOutlined />}
            onClick={handleRefreshRecommendations}
            loading={refreshingRemote}
            disabled={configLoading || savingConfig}
          >
            重新获取最新数据
          </Button>
          <Button 
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
            disabled={refreshingRemote}
          >
            重新加载列表
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
          locale={{ emptyText: (loading || refreshingRemote) ? '数据加载中...' : '当前没有基金推荐数据' }}
          rowClassName={(record) => {
            if (isBlacklisted(record.fundCode)) {
              return 'blacklisted-row'
            }
            if (holdings.has(record.fundCode)) {
              return 'holding-row'
            }
            return ''
          }}
        />
      </Card>

      {/* 筛选标准说明 */}
      <Card title="筛选标准说明">
        <div className="terminal-copy-block">
          <h3>本推荐列表的筛选标准：</h3>
          <ul>
            <li><strong>数据来源：</strong>第三方基金数据API</li>
            <li><strong>condition_id：</strong>第三方基金筛选条件 ID，用于指定本次推荐采用哪组筛选条件</li>
            <li><strong>基金经理去重：</strong>同一基金经理只保留一只基金</li>
            <li><strong>排序依据：</strong>按<span style={{ color: '#cf1322', fontWeight: 'bold' }}>卡玛比率排名</span>从优到劣排序（排名越小越好）</li>
            <li><strong>数量限制：</strong>展示前12只基金</li>
          </ul>

          <h3 className="terminal-field-offset-xl">关键指标说明：</h3>
          <ul>
            <li><strong>卡玛比率（Calmar Ratio）：</strong>收益与最大回撤的比值，衡量单位回撤下能获得多少收益，数值越大越好。排名越小表示卡玛比率越高。</li>
            <li><strong>近5年年化收益率：</strong>过去5年的年化收益率，反映基金的长期收益能力</li>
            <li><strong>今年收益率：</strong>今年以来的累计收益率，反映基金的短期表现</li>
            <li><strong>赎回费率：</strong>不同持有时长对应的赎回费率</li>
          </ul>

          <div className="terminal-info-box terminal-info-box-cyan terminal-field-offset-lg">
            <strong>💡 为什么使用卡玛比率排序？</strong>
            <p className="terminal-inline-note-space">
              卡玛比率综合考虑了收益和风险（最大回撤），比单纯的收益率排序更能反映基金的长期投资价值。
              卡玛比率高的基金意味着在控制回撤的前提下获得了较好的收益，更适合长期持有。
            </p>
          </div>

          <div className="terminal-info-box terminal-info-box-amber terminal-field-offset-lg">
            <strong>⚠️ 免责声明：</strong>
            <p className="terminal-inline-note-space">
              本推荐仅基于历史数据筛选，不构成投资建议。
              历史业绩不代表未来表现，投资需谨慎。
            </p>
          </div>
        </div>
      </Card>

      {/* 排除对话框 */}
      <Modal
        title="排除基金"
        open={excludeModalVisible}
        onOk={handleExclude}
        onCancel={() => {
          setExcludeModalVisible(false)
          setCurrentFund(null)
          setExcludeReason('')
        }}
        okText="确认排除"
        cancelText="取消"
      >
        <div className="terminal-section-gap">
          <p><strong>基金名称：</strong>{currentFund?.fundName}</p>
          <p><strong>基金代码：</strong>{currentFund?.fundCode}</p>
        </div>
        <div>
          <p className="terminal-field-label">
            <strong style={{ color: 'red' }}>*</strong> 排除原因：
          </p>
          <TextArea
            rows={4}
            placeholder="请填写排除该基金的原因（必填）"
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
