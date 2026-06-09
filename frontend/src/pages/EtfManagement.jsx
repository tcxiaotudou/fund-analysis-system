/**
 * ETF管理页面
 * 管理监控的ETF列表，支持增删改查
 */
import React, { useState, useEffect } from 'react'
import { Card, Table, Button, Space, Modal, Form, Input, Select, InputNumber, message, Tag } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons'
import { etfApi } from '../services/api'

const { Option } = Select

function EtfManagement() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRecord, setEditingRecord] = useState(null)
  const [form] = Form.useForm()

  /**
   * 加载ETF列表数据
   */
  const loadData = async () => {
    try {
      setLoading(true)
      const response = await etfApi.getList()
      
      if (response.code === 0) {
        setData(response.data || [])
      }
    } catch (error) {
      console.error('加载ETF列表失败:', error)
      message.error('加载数据失败')
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

      const response = editingRecord
        ? await etfApi.update({ ...values, id: editingRecord.id })
        : await etfApi.add(values)

      if (response.code !== 0) {
        message.error(response.message || '保存失败')
        return
      }

      message.success(editingRecord ? '更新成功' : '添加成功')
      setModalVisible(false)
      await loadData()
    } catch (error) {
      console.error('保存失败:', error)
      message.error('保存失败')
    }
  }

  /**
   * 删除ETF
   */
  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除 ${record.etfName} 吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await etfApi.delete(record.id)
          if (response.code !== 0) {
            message.error(response.message || '删除失败')
            return
          }
          message.success('删除成功')
          await loadData()
        } catch (error) {
          message.error('删除失败')
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
      title: 'RSI买入阈值',
      dataIndex: 'rsiBuyThreshold',
      key: 'rsiBuyThreshold',
      render: (val) => val || 30,
    },
    {
      title: 'RSI卖出阈值',
      dataIndex: 'rsiSellThreshold',
      key: 'rsiSellThreshold',
      render: (val) => val || 70,
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
            编辑
          </Button>
          <Button 
            type="link" 
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h1 className="page-title">📊 ETF监控管理</h1>

      <Card>
        {/* 操作栏 */}
        <Space style={{ marginBottom: 16 }}>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={() => openModal()}
          >
            添加ETF
          </Button>
          <Button 
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            刷新
          </Button>
        </Space>

        {/* 数据表格 */}
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showTotal: (total) => `共 ${total} 个ETF`,
          }}
        />
      </Card>

      {/* 添加/编辑对话框 */}
      <Modal
        title={editingRecord ? '编辑ETF' : '添加ETF'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            enabled: 1,
            rsiBuyThreshold: 30,
            rsiSellThreshold: 70,
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
            name="rsiBuyThreshold"
            label="RSI买入阈值"
          >
            <InputNumber min={0} max={100} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="rsiSellThreshold"
            label="RSI卖出阈值"
          >
            <InputNumber min={0} max={100} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="remark"
            label="备注"
          >
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default EtfManagement
