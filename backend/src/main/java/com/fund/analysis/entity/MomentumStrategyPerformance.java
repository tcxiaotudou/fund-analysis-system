package com.fund.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 动量策略每日绩效实体类
 */
@Data
@TableName("momentum_strategy_performance")
public class MomentumStrategyPerformance implements Serializable {

    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 绩效日期
     */
    private Date performanceDate;

    /**
     * 资产总值
     */
    private BigDecimal totalValue;

    /**
     * 累计收益率（百分比）
     */
    private BigDecimal returnRate;

    /**
     * 持仓ETF代码
     */
    private String holdingEtfCode;

    /**
     * 持仓ETF名称
     */
    private String holdingEtfName;

    /**
     * 持仓数量
     */
    private Long holdingQuantity;

    /**
     * 当前ETF价格
     */
    private BigDecimal currentPrice;

    /**
     * 本轮回测初始资金
     */
    private BigDecimal initialCapital;

    /**
     * 创建时间
     */
    private Date createTime;
}
