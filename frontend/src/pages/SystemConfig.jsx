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
  const [loading, setLoading] = useState(false)
  const [loadingData, setLoadingData] = useState(false)
  const [sendingEmail, setSendingEmail] = useState(false)

  /**
   * 加载配置
   */
  const loadConfig = async () => {
    try {
      setLoadingData(true)
      const response = await systemConfigApi.getEmailConfig()
      
      if (response.code === 0) {
        const config = response.data || {}
        
        // 将配置填充到表单，密码字段保持空（用于提示用户"不修改则留空"）
        const formValues = {
          emailEnabled: config.emailEnabled === 'true' || config.emailEnabled === '1' || config.emailEnabled === true,
          emailRecipients: config.emailRecipients || '',
          emailHost: config.emailHost || 'smtp.qq.com',
          emailPort: config.emailPort || 587,
          emailUsername: config.emailUsername || '',
          emailPassword: config.emailPassword,
          emailSchedule: config.emailSchedule || '12:00,14:50'
        }
        form.setFieldsValue(formValues)
        message.success('配置加载成功')
      } else {
        message.error(response.message || '加载配置失败')
      }
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
   * 提交邮件配置
   */
  const submitEmailConfig = async (values) => {
    try {
      setLoading(true)
      
      // 转换emailEnabled为字符串
      const configData = {
        ...values,
        emailEnabled: values.emailEnabled ? 'true' : 'false',
        emailPort: String(values.emailPort)
      }
      
      const response = await systemConfigApi.saveEmailConfig(configData)
      
      if (response.code === 0) {
        message.success('配置保存成功，定时任务已更新')
      } else {
        message.error(response.message || '保存失败')
      }
    } catch (error) {
      console.error('保存配置失败:', error)
      message.error('保存失败: ' + (error.normalizedMessage || error.message))
    } finally {
      setLoading(false)
    }
  }

  /**
   * 保存配置
   */
  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      Modal.confirm({
        title: '确认保存邮件配置？',
        content: '保存后会立即更新邮件定时任务；邮箱授权码留空时不会修改原授权码。',
        okText: '保存配置',
        cancelText: '取消',
        onOk: () => submitEmailConfig(values),
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
      subtitle="邮件日报、SMTP 和定时发送参数"
      status={<span>{loadingData ? '配置加载中' : '配置控制台'}</span>}
    >

      <Card title="邮件发送配置">
        <Form
          form={form}
          layout="vertical"
        >
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
        <div style={{ lineHeight: '2' }}>
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
