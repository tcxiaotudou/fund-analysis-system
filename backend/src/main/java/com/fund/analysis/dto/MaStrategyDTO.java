package com.fund.analysis.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 移动平均线策略数据传输对象
 * 用于前后端之间传输MA策略结果
 */
@Data
public class MaStrategyDTO {
    
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
     * 是否为买入信号
     */
    private Boolean isBuySignal;
    
    /**
     * 是否为卖出信号
     */
    private Boolean isSellSignal;
    
    /**
     * 信号说明
     */
    private String signalDescription;
    
    /**
     * 数据时间（格式化字符串）
     */
    private String dataTime;
}

