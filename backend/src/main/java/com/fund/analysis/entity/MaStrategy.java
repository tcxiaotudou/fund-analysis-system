package com.fund.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 移动平均线策略实体类
 * 用于存储移动平均线策略分析结果
 */
@Data
@TableName("ma_strategy")
public class MaStrategy implements Serializable {
    
    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * ETF代码
     */
    private String etfCode;
    
    /**
     * ETF名称
     */
    private String etfName;
    
    /**
     * 10日均线
     */
    private BigDecimal ma10;
    
    /**
     * 30日均线
     */
    private BigDecimal ma30;
    
    /**
     * 当前日K线收盘价
     */
    private BigDecimal currentDaily;
    
    /**
     * 是否为买入信号：0-否 1-是
     * 买入条件：10日均线上穿30日均线（金叉）
     */
    private Integer isBuySignal;
    
    /**
     * 是否为卖出信号：0-否 1-是
     * 卖出条件：10日均线下穿30日均线（死叉）
     */
    private Integer isSellSignal;
    
    /**
     * 信号说明
     */
    private String signalDescription;
    
    /**
     * 数据时间
     */
    private Date dataTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
}

