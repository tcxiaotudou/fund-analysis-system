package com.fund.analysis.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RsiBacktestDTO {

    private String etfCode;
    private String etfName;
    private String startDate;
    private String endDate;
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private BigDecimal totalReturnRate;
    private BigDecimal annualizedReturnRate;
    private Integer tradeCount;
    private Integer buyCount;
    private Integer sellCount;
    private Integer rsiPeriod;
    private BigDecimal rsiBuyThreshold;
    private BigDecimal rsiSellThreshold;
    private BigDecimal maxDrawdown;
    private BigDecimal winRate;
    private BigDecimal fixedAmountPerTrade;
    private BigDecimal totalInvested;
    private Long finalHoldingQuantity;
    private BigDecimal averageCost;

    private List<BacktestTransaction> transactions;
    private List<DailyValue> dailyValues;

    @Data
    public static class BacktestTransaction {
        private String date;
        private String type;
        private BigDecimal price;
        private Long quantity;
        private BigDecimal amount;
        private BigDecimal totalValue;
        private BigDecimal rsiValue;
        private BigDecimal profit;
        private String signalDescription;
        private Long holdingQuantityAfter;
        private BigDecimal avgCostAfter;
        private BigDecimal cashAfter;
    }

    @Data
    public static class DailyValue {
        private String date;
        private BigDecimal price;
        private BigDecimal totalValue;
        private BigDecimal returnRate;
        private BigDecimal rsiValue;
    }
}
