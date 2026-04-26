package com.fund.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 股债平衡策略实体类
 * 用于存储股债配置建议
 */
@Data
@TableName("stock_bond_balance")
public class StockBondBalance implements Serializable {
    
    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 90日RSI值
     */
    private BigDecimal rsi90;
    
    /**
     * 股票配置比例（%）
     */
    private Integer stockRatio;
    
    /**
     * 债券配置比例（%）
     */
    private Integer bondRatio;
    
    /**
     * 配置建议描述
     */
    private String suggestion;
    
    /**
     * 沪深300风险溢价（%）
     */
    private String riskPremium;
    
    /**
     * 5年均线偏离度（%）
     */
    private String ma5yDeviation;
    
    /**
     * 数据时间
     */
    private Date dataTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
}

