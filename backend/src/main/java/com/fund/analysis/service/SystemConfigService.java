package com.fund.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fund.analysis.config.DynamicScheduleConfig;
import com.fund.analysis.entity.SystemConfig;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.mapper.SystemConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务类
 * 提供系统配置的保存、读取和管理功能
 */
@Service
public class SystemConfigService {

    /**
     * 基金推荐条件ID配置键
     */
    public static final String FUND_RECOMMENDATION_CONDITION_ID_KEY = "fund_recommendation_condition_id";

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);

    /**
     * 系统配置数据访问对象
     */
    @Autowired
    private SystemConfigMapper systemConfigMapper;

    /**
     * 动态定时任务配置
     */
    @Autowired(required = false)
    private DynamicScheduleConfig dynamicScheduleConfig;

    /**
     * 获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    public String getConfigValue(String configKey) {
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", configKey);
        wrapper.eq("enabled", 1);
        
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 获取配置值（带默认值）
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 保存或更新配置
     *
     * @param configKey   配置键
     * @param configValue 配置值
     * @param configGroup 配置分组
     * @param configName  配置名称
     */
    @Transactional
    public void saveOrUpdateConfig(String configKey, String configValue, String configGroup, String configName) {
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", configKey);
        
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        
        if (config != null) {
            // 更新现有配置
            config.setConfigValue(configValue);
            config.setUpdateTime(new Date());
            systemConfigMapper.updateById(config);
        } else {
            // 创建新配置
            config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigGroup(configGroup);
            config.setConfigName(configName);
            config.setEnabled(1);
            config.setCreateTime(new Date());
            config.setUpdateTime(new Date());
            systemConfigMapper.insert(config);
        }
        
        logger.info("保存配置成功: {} = {}", configKey, configValue);
    }

    /**
     * 批量保存邮件配置
     *
     * @param configMap 配置映射
     */
    @Transactional
    public void saveEmailConfigs(Map<String, String> configMap) {
        // 邮件启用状态
        if (configMap.containsKey("emailEnabled")) {
            saveOrUpdateConfig("email_enabled", configMap.get("emailEnabled"), "email", "邮件发送启用");
        }
        
        // 邮件接收人
        if (configMap.containsKey("emailRecipients")) {
            saveOrUpdateConfig("email_recipients", configMap.get("emailRecipients"), "email", "邮件接收人");
        }
        
        // SMTP服务器
        if (configMap.containsKey("emailHost")) {
            saveOrUpdateConfig("email_host", configMap.get("emailHost"), "email", "SMTP服务器");
        }
        
        // SMTP端口
        if (configMap.containsKey("emailPort")) {
            saveOrUpdateConfig("email_port", configMap.get("emailPort"), "email", "SMTP端口");
        }
        
        // 发件邮箱
        if (configMap.containsKey("emailUsername")) {
            saveOrUpdateConfig("email_username", configMap.get("emailUsername"), "email", "发件邮箱");
        }
        
        // 邮箱密码/授权码
        if (configMap.containsKey("emailPassword") && !configMap.get("emailPassword").isEmpty()) {
            saveOrUpdateConfig("email_password", configMap.get("emailPassword"), "email", "邮箱授权码");
        }
        
        // 邮件发送时间配置
        if (configMap.containsKey("emailSchedule")) {
            saveOrUpdateConfig("email_schedule", configMap.get("emailSchedule"), "email", "邮件发送时间");
        }
        
        logger.info("批量保存邮件配置成功");
        
        // 重新加载定时任务
        if (dynamicScheduleConfig != null) {
            dynamicScheduleConfig.reloadTasks();
            logger.info("已重新加载邮件发送定时任务");
        }
    }

    /**
     * 批量保存基金推荐配置
     *
     * @param configMap 配置映射
     */
    @Transactional
    public void saveFundRecommendationConfigs(Map<String, String> configMap) {
        String conditionId = configMap.get("conditionId");
        if (conditionId == null || conditionId.trim().isEmpty()) {
            throw new BadRequestException("基金推荐 condition_id 不能为空");
        }

        saveOrUpdateConfig(
                FUND_RECOMMENDATION_CONDITION_ID_KEY,
                conditionId.trim(),
                "fund",
                "基金推荐条件ID"
        );
        logger.info("批量保存基金推荐配置成功");
    }

    /**
     * 获取所有基金推荐配置
     *
     * @return 配置映射
     */
    public Map<String, String> getFundRecommendationConfigs() {
        Map<String, String> configMap = new HashMap<>();
        String conditionId = getConfigValue(FUND_RECOMMENDATION_CONDITION_ID_KEY);
        configMap.put("conditionId", conditionId == null ? "" : conditionId);
        return configMap;
    }

    /**
     * 获取基金推荐条件ID
     *
     * @return 基金推荐条件ID
     */
    public String getFundRecommendationConditionId() {
        String conditionId = getConfigValue(FUND_RECOMMENDATION_CONDITION_ID_KEY);
        if (conditionId == null || conditionId.trim().isEmpty()) {
            throw new BadRequestException("未配置基金推荐 condition_id，请先在基金推荐页面保存配置");
        }
        return conditionId.trim();
    }

    /**
     * 获取所有邮件配置
     *
     * @return 配置映射
     */
    public Map<String, String> getEmailConfigs() {
        logger.info("开始从数据库读取邮件配置...");
        Map<String, String> configMap = new HashMap<>();
        
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_group", "email");
        wrapper.eq("enabled", 1);
        
        List<SystemConfig> configs = systemConfigMapper.selectList(wrapper);
        logger.info("从数据库查询到 {} 条邮件配置记录", configs.size());
        
        for (SystemConfig config : configs) {
            String key = config.getConfigKey();
            String value = config.getConfigValue();
            logger.debug("读取配置: {} = {}", key, key.equals("email_password") ? "******" : value);
            
            // 转换为前端使用的key格式（驼峰命名）
            if (key.equals("email_enabled")) {
                configMap.put("emailEnabled", value);
            } else if (key.equals("email_recipients")) {
                configMap.put("emailRecipients", value);
            } else if (key.equals("email_host")) {
                configMap.put("emailHost", value);
            } else if (key.equals("email_port")) {
                configMap.put("emailPort", value);
            } else if (key.equals("email_username")) {
                configMap.put("emailUsername", value);
            } else if (key.equals("email_password")) {
                configMap.put("emailPassword", value);
            } else if (key.equals("email_schedule")) {
                configMap.put("emailSchedule", value);
            }
        }
        
        logger.info("成功返回 {} 条邮件配置", configMap.size());
        return configMap;
    }

    /**
     * 判断邮件发送功能是否启用
     *
     * @return true-启用，false-未启用
     */
    public boolean isEmailEnabled() {
        String value = getConfigValue("email_enabled", "false");
        return "true".equals(value) || "1".equals(value);
    }

    /**
     * 获取邮件发送时间配置
     * 默认返回：周一到周五 12:00 和 14:50
     *
     * @return 时间配置字符串，格式：12:00,14:50
     */
    public String getEmailSchedule() {
        return getConfigValue("email_schedule", "12:00,14:50");
    }

    /**
     * 删除配置
     *
     * @param configKey 配置键
     */
    @Transactional
    public void deleteConfig(String configKey) {
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", configKey);
        systemConfigMapper.delete(wrapper);
        
        logger.info("删除配置: {}", configKey);
    }
}
