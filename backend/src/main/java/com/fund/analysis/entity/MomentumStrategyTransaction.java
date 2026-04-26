package com.fund.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 21日动量策略交易记录实体类
 * 用于存储动量策略的回测和实盘交易记录
 */
@Data
@TableName("momentum_strategy_transaction")
public class MomentumStrategyTransaction implements Serializable {
    
    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 交易日期
     */
    private Date transactionDate;
    
    /**
     * ETF代码
     */
    private String etfCode;
    
    /**
     * ETF名称
     */
    private String etfName;
    
    /**
     * 交易类型：buy-买入 sell-卖出
     */
    private String transactionType;
    
    /**
     * 交易数量（买入为正，卖出为负）
     */
    private Long quantity;
    
    /**
     * 交易价格
     */
    private BigDecimal price;
    
    /**
     * 21日动量值（用于记录交易时的动量）
     */
    @TableField("momentum_21")
    private BigDecimal momentum21;

    /**
     * 本轮回测初始资金
     */
    private BigDecimal initialCapital;
    
    /**
     * 创建时间
     */
    private Date createTime;
}
