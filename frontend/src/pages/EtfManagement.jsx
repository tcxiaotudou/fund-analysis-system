/**
 * ETF管理页面
 * 管理监控的ETF列表，支持增删改查
 */
import React, { useState, useEffect } from 'react'
import { Alert, Card, Table, Button, Space, Modal, Form, Input, Select, message, Tag } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons'
import TerminalPage from '../components/TerminalPage'
import { etfApi } from '../services/api'

const { Option } = Select

function EtfManagement() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRecord, setEditingRecord] = useState(null)
  const [pageError, setPageError] = useState(null)
  const [form] = Form.useForm()

  /**
   * 加载ETF列表数据
   */
  const loadData = async () => {
    try {
      setLoading(true)
      setPageError(null)
      const response = await etfApi.getList()

      if (response.code !== 0) {
        console.error('关注标的服务返回失败:', response)
        setPageError(response.code == null
          ? '关注标的列表暂时无法加载，请稍后重试'
          : `关注标的列表暂时无法加载（错误码：${response.code}），请稍后重试`)
        return
      }

      setData(response.data || [])
    } catch (error) {
      console.error('加载关注标的失败:', error)
      setPageError('关注标的列表暂时无法加载，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  /**
   * 打开添加/编辑对话框
   */
  const openModal = (record = null) => {
    setEditingRecord(record)
    if (record) {
      form.setFieldsValue(record)
    } else {
      form.resetFields()
    }
    setModalVisible(true)
  }

  /**
   * 提交表单
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const payload = { ...values }
      delete payload.rsiBuyThreshold
      delete payload.rsiSellThreshold

      const response = editingRecord
        ? await etfApi.update({
          ...payload,
          id: editingRecord.id,
          rsiBuyThreshold: editingRecord.rsiBuyThreshold,
          rsiSellThreshold: editingRecord.rsiSellThreshold,
        })
        : await etfApi.add(payload)

      if (response.code !== 0) {
        console.error('保存关注标的服务返回失败:', response)
        message.error(`${editingRecord ? '编辑' : '添加'}标的失败（错误码：${response.code}）`)
        return
      }

      message.success(editingRecord ? '标的已更新' : '已添加标的')
      setModalVisible(false)
      await loadData()
    } catch (error) {
      console.error('保存失败:', error)
      message.error('保存失败')
    }
  }

  /**
   * 移除标的
   */
  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认移除标的',
      content: `确定要从关注清单中移除 ${record.etfName} 吗？`,
      okText: '移除标的',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await etfApi.delete(record.id)
          if (response.code !== 0) {
            console.error('移除关注标的服务返回失败:', response)
            message.error(`移除标的失败（错误码：${response.code}）`)
            return
          }
          message.success('已移除标的')
          await loadData()
        } catch (error) {
          console.error('移除关注标的失败:', error)
          message.error('移除失败')
        }
      },
    })
  }

  /**
   * ETF分类选项
   */
  const categoryOptions = [
    { value: 1, label: '指数' },
    { value: 2, label: '行业' },
    { value: 3, label: '主题' },
    { value: 4, label: '债券' },
    { value: 5, label: '商品' },
    { value: 6, label: '海外' },
  ]

  /**
   * 表格列配置
   */
  const columns = [
    {
      title: 'ETF名称',
      dataIndex: 'etfName',
      key: 'etfName',
    },
    {
      title: 'ETF代码',
      dataIndex: 'etfCode',
      key: 'etfCode',
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      render: (val) => {
        const category = categoryOptions.find(c => c.value === val)
        return category ? category.label : '-'
      },
      filters: categoryOptions.map(c => ({ text: c.label, value: c.value })),
      onFilter: (value, record) => record.category === value,
    },
    {
      title: '启用状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (val) => (
        val === 1 ? 
          <Tag color="success">启用</Tag> : 
          <Tag color="default">禁用</Tag>
      ),
      filters: [
        { text: '启用', value: 1 },
        { text: '禁用', value: 0 },
      ],
      onFilter: (value, record) => record.enabled === value,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button 
            type="link" 
            size="small"
            icon={<EditOutlined />}
            onClick={() => openModal(record)}
          >
            编辑标的
          </Button>
          <Button 
            type="link" 
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            移除标的
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <TerminalPage
      title="关注标的"
      subtitle="维护纳入 RSI 与趋势观察的 ETF"
      status={<span>{pageError ? '数据不可用' : `共 ${data.length} 个标的`}</span>}
    >

      <Card title="关注清单">
        {/* 操作栏 */}
        <Space className="terminal-toolbar" wrap>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={() => openModal()}
          >
            添加标的
          </Button>
          <Button 
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            更新列表
          </Button>
        </Space>

        {/* 数据表格 */}
        {pageError ? (
          <Alert
            type="error"
            showIcon
            message="关注标的加载失败"
            description={pageError}
            action={(
              <Button size="small" onClick={loadData} loading={loading}>
                重试
              </Button>
            )}
          />
        ) : (
          <Table
            columns={columns}
            dataSource={data}
            rowKey="id"
            loading={loading}
            scroll={{ x: 900 }}
            pagination={{
              pageSize: 10,
              showTotal: (total) => `共 ${total} 个标的`,
            }}
            locale={{ emptyText: loading ? '数据加载中...' : '暂无关注标的' }}
          />
        )}
      </Card>

      {/* 添加/编辑对话框 */}
      <Modal
        title={editingRecord ? '编辑标的' : '添加标的'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        okText={editingRecord ? '保存修改' : '添加标的'}
        cancelText="取消"
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            enabled: 1,
            category: 2,
          }}
        >
          <Form.Item
            name="etfName"
            label="ETF名称"
            rules={[{ required: true, message: '请输入ETF名称' }]}
          >
            <Input placeholder="例如：中证银行" />
          </Form.Item>

          <Form.Item
            name="etfCode"
            label="ETF代码"
            rules={[{ required: true, message: '请输入ETF代码' }]}
          >
            <Input placeholder="例如：sz399986" />
          </Form.Item>

          <Form.Item
            name="category"
            label="分类"
            rules={[{ required: true, message: '请选择分类' }]}
          >
            <Select>
              {categoryOptions.map(option => (
                <Option key={option.value} value={option.value}>
                  {option.label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="enabled"
            label="启用状态"
            rules={[{ required: true }]}
          >
            <Select>
              <Option value={1}>启用</Option>
              <Option value={0}>禁用</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="remark"
            label="备注"
          >
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>
    </TerminalPage>
  )
}

export default EtfManagement
