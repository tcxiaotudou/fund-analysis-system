package com.fund.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ETF信息实体类
 * 用于存储ETF的基本信息和监控配置
 */
@Data
@TableName("etf_info")
public class EtfInfo implements Serializable {
    
    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * ETF名称，例如：中证银行
     */
    private String etfName;
    
    /**
     * ETF代码，例如：sz399986
     */
    private String etfCode;
    
    /**
     * ETF分类：1-指数 2-行业 3-主题 4-债券 5-商品 6-海外
     */
    private Integer category;
    
    /**
     * 是否启用监控：0-否 1-是
     */
    private Integer enabled;
    
    /**
     * RSI买入阈值（低于此值发出买入信号）
     */
    private Double rsiBuyThreshold;
    
    /**
     * RSI卖出阈值（高于此值发出卖出信号）
     */
    private Double rsiSellThreshold;
    
    /**
     * 备注说明
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer deleted;
}

