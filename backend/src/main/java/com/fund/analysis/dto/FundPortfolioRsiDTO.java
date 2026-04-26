package com.fund.analysis.dto;

import lombok.Data;

/**
 * 基金组合RSI数据传输对象
 * 用于展示养老基金组合的RSI指标
 */
@Data
public class FundPortfolioRsiDTO {
    
    /**
     * 14日RSI
     */
    private String rsi14;
    
    /**
     * 90日RSI
     */
    private String rsi90;
    
    /**
     * 14周RSI
     */
    private String weeklyRsi14;
}

