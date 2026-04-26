package com.fund.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置实体类
 * 用于存储系统运行时的各种配置参数
 */
@Data
@TableName("system_config")
public class SystemConfig implements Serializable {
    
    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 配置键（唯一标识）
     */
    private String configKey;
    
    /**
     * 配置值
     */
    private String configValue;
    
    /**
     * 配置分组：email-邮件配置 analysis-分析配置 threshold-阈值配置
     */
    private String configGroup;
    
    /**
     * 配置名称
     */
    private String configName;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 是否启用：0-否 1-是
     */
    private Integer enabled;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}

