/**
 * 基金推荐页面
 * 展示优质基金推荐列表
 */
import React, { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Tooltip, Modal, Input, message } from 'antd'
import { ReloadOutlined, StarOutlined, StarFilled, StopOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { fundApi } from '../services/api'

const { TextArea } = Input

function FundRecommendation() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [blacklist, setBlacklist] = useState([])
  const [excludeModalVisible, setExcludeModalVisible] = useState(false)
  const [currentFund, setCurrentFund] = useState(null)
  const [excludeReason, setExcludeReason] = useState('')
  const [holdings, setHoldings] = useState(new Set())

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
    } catch (error) {
      console.error('加载基金推荐数据失败:', error)
    } finally {
      setLoading(false)
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
        if (!val) return '-'
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
        if (!val) return '-'
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
        if (!val) return '-'
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
        if (!val) return '-'
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
            <Space size="small" direction="vertical" style={{ width: '100%' }}>
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
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h1 className="page-title" style={{ margin: 0 }}>🏆 基金推荐</h1>
        {data.length > 0 && data[0]?.dataTime && (
          <span style={{ color: '#999', fontSize: '14px' }}>
            数据更新时间: {new Date(data[0].dataTime).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit', 
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit'
            })}
          </span>
        )}
      </div>

      <Card>
        {/* 操作栏 */}
        <Space style={{ marginBottom: 16 }}>
          <Button 
            type="primary" 
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            刷新数据
          </Button>
        </Space>

        {/* 数据表格 */}
        <Table
          columns={columns}
          dataSource={data}
          rowKey="fundCode"
          loading={loading}
          pagination={false}
          scroll={{ x: 1600 }}
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
      <Card title="📖 筛选标准说明" style={{ marginTop: 16 }}>
        <div style={{ lineHeight: '2' }}>
          <h3>本推荐列表的筛选标准：</h3>
          <ul>
            <li><strong>数据来源：</strong>第三方基金数据API</li>
            <li><strong>基金经理去重：</strong>同一基金经理只保留一只基金</li>
            <li><strong>排序依据：</strong>按<span style={{ color: '#cf1322', fontWeight: 'bold' }}>卡玛比率排名</span>从优到劣排序（排名越小越好）</li>
            <li><strong>数量限制：</strong>展示前12只基金</li>
          </ul>

          <h3 style={{ marginTop: '20px' }}>关键指标说明：</h3>
          <ul>
            <li><strong>卡玛比率（Calmar Ratio）：</strong>收益与最大回撤的比值，衡量单位回撤下能获得多少收益，数值越大越好。排名越小表示卡玛比率越高。</li>
            <li><strong>近5年年化收益率：</strong>过去5年的年化收益率，反映基金的长期收益能力</li>
            <li><strong>今年收益率：</strong>今年以来的累计收益率，反映基金的短期表现</li>
            <li><strong>赎回费率：</strong>不同持有时长对应的赎回费率</li>
          </ul>

          <div style={{ 
            background: '#e6f7ff', 
            border: '1px solid #91d5ff',
            borderRadius: '4px',
            padding: '12px',
            marginTop: '12px'
          }}>
            <strong>💡 为什么使用卡玛比率排序？</strong>
            <p style={{ margin: '8px 0 0 0' }}>
              卡玛比率综合考虑了收益和风险（最大回撤），比单纯的收益率排序更能反映基金的长期投资价值。
              卡玛比率高的基金意味着在控制回撤的前提下获得了较好的收益，更适合长期持有。
            </p>
          </div>

          <div style={{ 
            background: '#fff7e6', 
            border: '1px solid #ffd591',
            borderRadius: '4px',
            padding: '12px',
            marginTop: '12px'
          }}>
            <strong>⚠️ 免责声明：</strong>
            <p style={{ margin: '8px 0 0 0' }}>
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
        <div style={{ marginBottom: 16 }}>
          <p><strong>基金名称：</strong>{currentFund?.fundName}</p>
          <p><strong>基金代码：</strong>{currentFund?.fundCode}</p>
        </div>
        <div>
          <p style={{ marginBottom: 8 }}>
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

      <style jsx>{`
        .blacklisted-row {
          background-color: #f5f5f5 !important;
          opacity: 0.6;
        }
        .blacklisted-row:hover {
          background-color: #e8e8e8 !important;
        }
        .holding-row {
          background-color: #e6f7ff !important;
          border-left: 3px solid #1890ff;
        }
        .holding-row:hover {
          background-color: #d9f0ff !important;
        }
      `}</style>
    </div>
  )
}

export default FundRecommendation
