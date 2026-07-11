/**
 * 系统配置页面
 * 管理系统运行参数
 */
import React, { useState, useEffect } from 'react'
import { Alert, Card, Form, Input, Switch, Button, Space, Divider, Modal, message } from 'antd'
import { SaveOutlined, ReloadOutlined, SendOutlined } from '@ant-design/icons'
import TerminalPage from '../components/TerminalPage'
import { systemConfigApi } from '../services/api'

function SystemConfig() {
  const [form] = Form.useForm()
  // 配置保存状态
  const [loading, setLoading] = useState(false)
  // 配置加载状态
  const [loadingData, setLoadingData] = useState(false)
  // 页面加载错误
  const [pageError, setPageError] = useState('')
  // 邮件发送状态
  const [sendingEmail, setSendingEmail] = useState(false)

  /**
   * 加载系统配置
   */
  const loadConfig = async () => {
    try {
      setLoadingData(true)
      setPageError('')
      const [emailResponse, fundRecommendationResponse] = await Promise.all([
        systemConfigApi.getEmailConfig(),
        systemConfigApi.getFundRecommendationConfig(),
      ])

      if (emailResponse.code !== 0) {
        console.error('读取邮件简报设置服务返回失败:', emailResponse)
        setPageError(`邮件简报设置暂时无法读取（错误码：${emailResponse.code}），请稍后重试。`)
        return
      }
      if (fundRecommendationResponse.code !== 0) {
        console.error('读取基金优选设置服务返回失败:', fundRecommendationResponse)
        setPageError(`基金优选设置暂时无法读取（错误码：${fundRecommendationResponse.code}），请稍后重试。`)
        return
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
    } catch (error) {
      console.error('加载配置异常:', error)
      setPageError('服务设置暂时无法读取，请检查网络连接后重试。')
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
    let fundSettingsSaved = false
    try {
      setLoading(true)

      // 拆分不同配置接口需要的字段，避免无关字段进入后端。
      const emailConfigData = {
        emailEnabled: values.emailEnabled ? 'true' : 'false',
        emailRecipients: values.emailRecipients,
        emailHost: values.emailHost,
        emailPort: String(values.emailPort),
        emailUsername: values.emailUsername,
        emailSchedule: values.emailSchedule,
      }
      const emailPassword = values.emailPassword?.trim()
      if (emailPassword) {
        emailConfigData.emailPassword = emailPassword
      }
      const fundRecommendationConfigData = {
        conditionId: values.conditionId.trim(),
      }

      const fundRecommendationResponse = await systemConfigApi.saveFundRecommendationConfig(fundRecommendationConfigData)
      if (fundRecommendationResponse.code !== 0) {
        console.error('保存基金优选设置服务返回失败:', fundRecommendationResponse)
        message.error(`基金优选设置保存失败（错误码：${fundRecommendationResponse.code}）`)
        return
      }
      fundSettingsSaved = true

      const emailResponse = await systemConfigApi.saveEmailConfig(emailConfigData)
      if (emailResponse.code !== 0) {
        console.error('保存邮件简报设置服务返回失败:', emailResponse)
        message.error(`基金优选设置已保存，但邮件简报设置保存失败（错误码：${emailResponse.code}）`)
        return
      }

      message.success('设置已保存，定时发送安排已更新')
    } catch (error) {
      console.error('保存配置失败:', error)
      message.error(fundSettingsSaved
        ? '基金优选设置已保存，但邮件简报设置保存失败，请重试'
        : '基金优选设置保存失败，请重试')
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
        title: '确认保存设置？',
        content: '保存后将更新基金优选筛选来源和邮件简报安排；邮箱授权码留空时不会修改原授权码。',
        okText: '保存设置',
        cancelText: '取消',
        onOk: () => submitSystemConfig(values),
      })
    } catch (error) {
      if (!error?.errorFields) {
        console.error('校验服务设置失败:', error)
        message.error('设置保存失败，请稍后重试')
      }
    }
  }

  /**
   * 立即发送邮件
   */
  const submitSendNow = async () => {
    try {
      setSendingEmail(true)
      message.loading({ content: '正在发送简报...', key: 'sendEmail', duration: 0 })

      const response = await systemConfigApi.sendEmailNow()

      message.destroy('sendEmail')

      if (response.code === 0) {
        message.success('简报已发送，请查看当前收件邮箱')
      } else {
        console.error('立即发送简报服务返回失败:', response)
        message.error(`简报发送失败（错误码：${response.code}）`)
      }
    } catch (error) {
      console.error('发送邮件失败:', error)
      message.destroy('sendEmail')
      message.error('简报发送失败，请稍后重试')
    } finally {
      setSendingEmail(false)
    }
  }

  /**
   * 确认后立即发送邮件
   */
  const handleSendNow = () => {
    Modal.confirm({
      title: '确认立即发送简报？',
      content: '将立即向当前设置的收件人发送一封真实简报。',
      okText: '立即发送简报',
      cancelText: '取消',
      onOk: submitSendNow,
    })
  }

  return (
    <TerminalPage
      title="服务设置"
      subtitle="管理基金筛选来源与邮件简报"
      status={<span>{loadingData ? '正在读取设置' : '服务设置'}</span>}
    >

      <Alert
        type="warning"
        showIcon
        message="此页包含邮件授权码等敏感设置，仅应在受信任环境中使用。"
        className="terminal-section-gap"
      />

      {pageError && (
        <Alert
          type="error"
          showIcon
          message="服务设置加载失败"
          description={pageError}
          action={<Button size="small" onClick={loadConfig} loading={loadingData}>重试</Button>}
          className="terminal-section-gap"
        />
      )}

      <Card title="数据与简报设置">
        <Form
          form={form}
          layout="vertical"
        >
          {/* 基金推荐配置 */}
          <Divider orientation="left">基金优选</Divider>

          <Form.Item
            name="conditionId"
            label="筛选组合编号"
            rules={[{ required: true, message: '请输入筛选组合编号' }]}
            extra="用于选择基金优选数据源中的筛选组合"
          >
            <Input placeholder="请输入筛选组合编号" />
          </Form.Item>

          {/* 邮件配置 */}
          <Divider orientation="left">邮件简报</Divider>

          <Form.Item
            name="emailEnabled"
            label="定时发送简报"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            name="emailRecipients"
            label="简报收件人"
            rules={[{ required: true, message: '请输入简报收件人' }]}
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
            label="简报发送时间"
            rules={[{ required: true, message: '请输入简报发送时间' }]}
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
                保存设置
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
                立即发送简报
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {/* 设置说明 */}
      <Card title="设置说明">
        <div className="terminal-copy-block">
          <h3>基金优选：</h3>
          <ul>
            <li><strong>筛选组合编号：</strong>用于选择基金优选数据源中的筛选组合。</li>
          </ul>

          <h3>邮件简报：</h3>
          <ul>
            <li><strong>定时发送简报：</strong>开启后会按设置的时间向收件人发送分析简报</li>
            <li><strong>简报收件人：</strong>可以填写多个邮箱，使用逗号分隔</li>
            <li><strong>邮件服务器：</strong>填写发件邮箱所需的服务器信息，QQ 邮箱需要使用授权码而非登录密码</li>
            <li><strong>简报发送时间：</strong>使用 HH:MM（24 小时制），多个时间用逗号分隔，如 12:00,14:50；默认周一到周五发送</li>
            <li><strong>立即发送简报：</strong>向当前收件人发送一封真实简报</li>
            <li><strong>如何获取QQ邮箱授权码：</strong>登录QQ邮箱 → 设置 → 账户 → POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务 → 开启服务并生成授权码</li>
          </ul>
        </div>
      </Card>
    </TerminalPage>
  )
}

export default SystemConfig
