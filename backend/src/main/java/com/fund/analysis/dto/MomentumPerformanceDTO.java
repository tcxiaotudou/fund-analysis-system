package com.fund.analysis.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 动量策略收益曲线数据传输对象
 */
@Data
public class MomentumPerformanceDTO {
    
    /**
     * 日期（格式化字符串）
     */
    private String date;
    
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
}
