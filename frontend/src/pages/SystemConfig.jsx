/**
 * 系统配置页面
 * 管理系统运行参数
 */
import React, { useState, useEffect } from 'react'
import { Card, Form, Input, Switch, Button, Space, Divider, Modal, message } from 'antd'
import { SaveOutlined, ReloadOutlined, SendOutlined } from '@ant-design/icons'
import TerminalPage from '../components/TerminalPage'
import { systemConfigApi } from '../services/api'

function SystemConfig() {
  const [form] = Form.useForm()
  // 配置保存状态
  const [loading, setLoading] = useState(false)
  // 配置加载状态
  const [loadingData, setLoadingData] = useState(false)
  // 邮件发送状态
  const [sendingEmail, setSendingEmail] = useState(false)

  /**
   * 加载系统配置
   */
  const loadConfig = async () => {
    try {
      setLoadingData(true)
      const [emailResponse, fundRecommendationResponse] = await Promise.all([
        systemConfigApi.getEmailConfig(),
        systemConfigApi.getFundRecommendationConfig(),
      ])

      if (emailResponse.code !== 0) {
        throw new Error(emailResponse.message || '加载邮件配置失败')
      }
      if (fundRecommendationResponse.code !== 0) {
        throw new Error(fundRecommendationResponse.message || '加载基金推荐配置失败')
      }

      const emailConfig = emailResponse.data || {}
      const fundRecommendationConfig = fundRecommendationResponse.data || {}

      // 邮箱授权码不回填，避免保存时误覆盖原授权码。
      form.setFieldsValue({
        conditionId: fundRecommendationConfig.conditionId || '',
        emailEnabled: emailConfig.emailEnabled === 'true' || emailConfig.emailEnabled === '1' || emailConfig.emailEnabled === true,
        emailRecipients: emailConfig.emailRecipients || '',
        emailHost: emailConfig.emailHost || 'smtp.qq.com',
        emailPort: emailConfig.emailPort || 587,
        emailUsername: emailConfig.emailUsername || '',
        emailPassword: '',
        emailSchedule: emailConfig.emailSchedule || '12:00,14:50',
      })
      message.success('配置加载成功')
    } catch (error) {
      console.error('加载配置异常:', error)
      message.error('加载配置失败: ' + (error.normalizedMessage || error.message || '网络错误'))
    } finally {
      setLoadingData(false)
    }
  }

  /**
   * 组件加载时获取配置
   */
  useEffect(() => {
    loadConfig()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  /**
   * 提交系统配置
   */
  const submitSystemConfig = async (values) => {
    try {
      setLoading(true)

      // 拆分不同配置接口需要的字段，避免无关字段进入后端。
      const emailConfigData = {
        emailEnabled: values.emailEnabled ? 'true' : 'false',
        emailRecipients: values.emailRecipients,
        emailHost: values.emailHost,
        emailPort: String(values.emailPort),
        emailUsername: values.emailUsername,
        emailPassword: values.emailPassword || '',
        emailSchedule: values.emailSchedule,
      }
      const fundRecommendationConfigData = {
        conditionId: values.conditionId.trim(),
      }

      const [emailResponse, fundRecommendationResponse] = await Promise.all([
        systemConfigApi.saveEmailConfig(emailConfigData),
        systemConfigApi.saveFundRecommendationConfig(fundRecommendationConfigData),
      ])

      if (emailResponse.code !== 0) {
        throw new Error(emailResponse.message || '邮件配置保存失败')
      }
      if (fundRecommendationResponse.code !== 0) {
        throw new Error(fundRecommendationResponse.message || '组合Id保存失败')
      }

      message.success('配置保存成功，定时任务已更新')
    } catch (error) {
      console.error('保存配置失败:', error)
      message.error('保存失败: ' + (error.normalizedMessage || error.message))
    } finally {
      setLoading(false)
    }
  }

  /**
   * 保存系统配置
   */
  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      Modal.confirm({
        title: '确认保存系统配置？',
        content: '保存后会立即更新邮件定时任务；组合Id会用于基金推荐数据刷新；邮箱授权码留空时不会修改原授权码。',
        okText: '保存配置',
        cancelText: '取消',
        onOk: () => submitSystemConfig(values),
      })
    } catch (error) {
      if (!error?.errorFields) {
        message.error('保存失败: ' + (error.normalizedMessage || error.message))
      }
    }
  }

  /**
   * 立即发送邮件
   */
  const submitSendNow = async () => {
    try {
      setSendingEmail(true)
      message.loading({ content: '正在发送邮件...', key: 'sendEmail', duration: 0 })

      const response = await systemConfigApi.sendEmailNow()

      message.destroy('sendEmail')

      if (response.code === 0) {
        message.success('邮件发送成功！请查收邮箱')
      } else {
        message.error(response.message || '邮件发送失败')
      }
    } catch (error) {
      console.error('发送邮件失败:', error)
      message.destroy('sendEmail')
      message.error('发送失败: ' + (error.normalizedMessage || error.message))
    } finally {
      setSendingEmail(false)
    }
  }

  /**
   * 确认后立即发送邮件
   */
  const handleSendNow = () => {
    Modal.confirm({
      title: '确认立即发送邮件？',
      content: '该操作会立即向当前配置的收件人发送真实日报。',
      okText: '立即发送',
      cancelText: '取消',
      onOk: submitSendNow,
    })
  }

  return (
    <TerminalPage
      title="系统配置"
      subtitle="组合Id、邮件日报、SMTP 和定时发送参数"
      status={<span>{loadingData ? '配置加载中' : '配置控制台'}</span>}
    >

      <Card title="系统参数配置">
        <Form
          form={form}
          layout="vertical"
        >
          {/* 基金推荐配置 */}
          <Divider orientation="left">基金推荐配置</Divider>

          <Form.Item
            name="conditionId"
            label="组合Id"
            rules={[{ required: true, message: '请输入组合Id' }]}
            extra="对应第三方基金推荐接口 condition_id，用于指定基金推荐筛选组合。"
          >
            <Input placeholder="例如：2374632" />
          </Form.Item>

          {/* 邮件配置 */}
          <Divider orientation="left">邮件配置</Divider>

          <Form.Item
            name="emailEnabled"
            label="启用邮件发送"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            name="emailRecipients"
            label="邮件接收人"
            rules={[{ required: true, message: '请输入邮件接收人' }]}
            extra="多个邮箱用逗号分隔"
          >
            <Input placeholder="example@qq.com,example2@qq.com" />
          </Form.Item>

          <Form.Item
            name="emailHost"
            label="SMTP服务器"
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            name="emailPort"
            label="SMTP端口"
            rules={[{ required: true }]}
          >
            <Input type="number" />
          </Form.Item>

          <Form.Item
            name="emailUsername"
            label="发件邮箱"
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            name="emailPassword"
            label="邮箱授权码"
            extra="首次配置需要填写；已有授权码时留空表示不修改。QQ邮箱需要使用授权码，而非登录密码"
          >
            <Input.Password placeholder="请输入邮箱授权码" />
          </Form.Item>

          <Form.Item
            name="emailSchedule"
            label="邮件发送时间"
            rules={[{ required: true, message: '请输入邮件发送时间' }]}
            extra="多个时间用逗号分隔，格式：HH:MM（24小时制），默认周一到周五发送"
          >
            <Input placeholder="12:00,14:50" />
          </Form.Item>

          {/* 保存按钮 */}
          <Form.Item>
            <Space className="terminal-toolbar" wrap>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                onClick={handleSave}
                loading={loading}
              >
                保存配置
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={loadConfig}
                loading={loadingData}
              >
                重新加载
              </Button>
              <Button
                type="default"
                icon={<SendOutlined />}
                onClick={handleSendNow}
                loading={sendingEmail}
                className="terminal-success-button"
              >
                立即发送邮件
              </Button>
              <Button onClick={() => form.resetFields()}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {/* 配置说明 */}
      <Card title="配置说明">
        <div className="terminal-copy-block">
          <h3>基金推荐配置：</h3>
          <ul>
            <li><strong>组合Id：</strong>用于第三方基金推荐接口的筛选条件，对应原 condition_id。</li>
          </ul>

          <h3>邮件配置：</h3>
          <ul>
            <li><strong>启用邮件发送：</strong>开启后系统会定期发送分析报告到指定邮箱</li>
            <li><strong>邮件接收人：</strong>可以配置多个邮箱接收报告，多个邮箱用逗号分隔</li>
            <li><strong>SMTP配置：</strong>配置邮件服务器信息，QQ邮箱需要使用授权码而非登录密码</li>
            <li><strong>邮件发送时间：</strong>配置每天发送邮件的时间点，格式为 HH:MM（24小时制），多个时间用逗号分隔，如：12:00,14:50。保存后会自动更新定时任务（周一到周五发送）</li>
            <li><strong>立即发送邮件：</strong>点击"立即发送邮件"按钮可以手动触发一次邮件发送，用于测试或立即查看报告</li>
            <li><strong>如何获取QQ邮箱授权码：</strong>登录QQ邮箱 → 设置 → 账户 → POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务 → 开启服务并生成授权码</li>
          </ul>
        </div>
      </Card>
    </TerminalPage>
  )
}

export default SystemConfig
