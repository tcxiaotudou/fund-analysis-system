package com.fund.analysis.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 双均线策略回测结果DTO
 */
@Data
public class MaStrategyBacktestDTO {
    
    /**
     * ETF代码
     */
    private String etfCode;
    
    /**
     * ETF名称
     */
    private String etfName;
    
    /**
     * 回测开始日期
     */
    private String startDate;
    
    /**
     * 回测结束日期
     */
    private String endDate;
    
    /**
     * 初始资金
     */
    private BigDecimal initialCapital;
    
    /**
     * 最终资金
     */
    private BigDecimal finalCapital;
    
    /**
     * 总收益率（百分比）
     */
    private BigDecimal totalReturnRate;
    
    /**
     * 年化收益率（百分比）
     */
    private BigDecimal annualizedReturnRate;
    
    /**
     * 交易次数
     */
    private Integer tradeCount;
    
    /**
     * 买入次数
     */
    private Integer buyCount;
    
    /**
     * 卖出次数
     */
    private Integer sellCount;
    
    /**
     * 交易记录列表
     */
    private List<BacktestTransaction> transactions;
    
    /**
     * 每日净值数据（用于图表展示）
     */
    private List<DailyValue> dailyValues;
    
    /**
     * 交易记录内部类
     */
    @Data
    public static class BacktestTransaction {
        /**
         * 交易日期
         */
        private String date;
        
        /**
         * 交易类型：BUY-买入，SELL-卖出
         */
        private String type;
        
        /**
         * 交易价格
         */
        private BigDecimal price;
        
        /**
         * 交易数量
         */
        private Long quantity;
        
        /**
         * 交易金额
         */
        private BigDecimal amount;
        
        /**
         * 交易后总资产
         */
        private BigDecimal totalValue;
        
        /**
         * 信号说明
         */
        private String signalDescription;
    }
    
    /**
     * 每日净值内部类
     */
    @Data
    public static class DailyValue {
        /**
         * 日期
         */
        private String date;
        
        /**
         * 当日价格
         */
        private BigDecimal price;
        
        /**
         * 当日资产总值
         */
        private BigDecimal totalValue;
        
        /**
         * 累计收益率（百分比）
         */
        private BigDecimal returnRate;
    }
}

