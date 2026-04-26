package com.fund.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * RSI分析记录实体类
 * 用于存储RSI技术指标分析结果
 */
@Data
@TableName("rsi_analysis")
public class RsiAnalysis implements Serializable {
    
    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 标的代码（ETF代码或基金代码）
     */
    private String code;
    
    /**
     * 标的名称
     */
    private String name;
    
    /**
     * RSI周期（天）
     */
    private Integer period;
    
    /**
     * 当前RSI值
     */
    private BigDecimal currentRsi;
    
    /**
     * RSI最高值
     */
    private BigDecimal highRsi;
    
    /**
     * RSI最低值
     */
    private BigDecimal lowRsi;
    
    /**
     * RSI区间上限（2/3位置）
     */
    private BigDecimal twoThirdsRsi;
    
    /**
     * RSI区间下限（1/3位置）
     */
    private BigDecimal oneThirdsRsi;
    
    /**
     * 最高点到当前位置的最低值
     */
    private BigDecimal high2NowLow;
    
    /**
     * 当前RSI距离最低点的天数
     */
    private Integer daysFromLow;
    
    /**
     * RSI>=70的天数
     */
    private Integer rsi70Days;
    
    /**
     * RSI>=65的天数
     */
    private Integer rsi65Days;
    
    /**
     * RSI>=60的天数
     */
    private Integer rsi60Days;
    
    /**
     * RSI>=55的天数
     */
    private Integer rsi55Days;
    
    /**
     * 分析数据总天数
     */
    private Integer totalDays;
    
    /**
     * 是否为买入信号：0-否 1-是
     */
    private Integer isBuySignal;
    
    /**
     * 分析消息
     */
    private String message;
    
    /**
     * 数据时间
     */
    private Date dataTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
}

