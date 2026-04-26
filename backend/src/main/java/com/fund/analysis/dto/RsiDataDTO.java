package com.fund.analysis.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * RSI数据传输对象
 * 用于前后端之间传输RSI计算结果
 */
@Data
public class RsiDataDTO {
    
    /**
     * 标的代码
     */
    private String code;
    
    /**
     * 标的名称
     */
    private String name;
    
    /**
     * RSI周期
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
     * RSI区间（格式化字符串）
     */
    private String interval;
    
    /**
     * 是否为买入信号
     */
    private Boolean isBuySignal;
    
    /**
     * 分析消息
     */
    private String message;
    
    /**
     * 数据时间（格式化字符串）
     */
    private String dataTime;
}

