package com.fund.analysis.dto;

import lombok.Data;

import java.util.List;

/**
 * 市场概览数据传输对象
 * 用于展示市场整体情况
 */
@Data
public class MarketOverviewDTO {
    
    /**
     * 14日RSI
     */
    private String rsi14;
    
    /**
     * 90日RSI
     */
    private String rsi90;
    
    /**
     * 30年国债14日RSI
     */
    private String bondRsi14;
    
    /**
     * 沪深300风险溢价
     */
    private String riskPremium;
    
    /**
     * 股债平衡建议
     */
    private String balanceSuggestion;
    
    /**
     * 5年均线偏离度
     */
    private String ma5yDeviation;
    
    /**
     * 养老基金组合RSI
     */
    private FundPortfolioRsiDTO fundPortfolioRsi;
    
    /**
     * ETF买入机会列表
     */
    private List<RsiDataDTO> etfOpportunities;
    
    /**
     * MA策略买入信号列表
     */
    private List<MaStrategyDTO> maSignals;
    
    /**
     * 数据更新时间
     */
    private String updateTime;
}

