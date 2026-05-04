package com.fund.analysis.service;

import com.fund.analysis.entity.MomentumStrategyPerformance;
import com.fund.analysis.exception.DataUnavailableException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 动量策略每日绩效计算器
 */
class MomentumPerformanceCalculator {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    MomentumStrategyPerformance buildDailyPerformance(Date date,
                                                       BigDecimal initialCapital,
                                                       BigDecimal availableCapital,
                                                       String currentHolding,
                                                       long currentQuantity,
                                                       Map<String, BigDecimal> priceMap,
                                                       Map<String, String> etfNames) {
        BigDecimal totalValue = availableCapital;
        BigDecimal currentPrice = null;

        if (currentHolding != null && currentQuantity > 0) {
            currentPrice = priceMap.get(currentHolding);
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DataUnavailableException("动量策略收益曲线缺少持仓价格: "
                        + currentHolding + ", 日期=" + dateFormat.format(date));
            }

            BigDecimal holdingValue = currentPrice.multiply(BigDecimal.valueOf(currentQuantity));
            totalValue = totalValue.add(holdingValue);
        }

        MomentumStrategyPerformance performance = new MomentumStrategyPerformance();
        performance.setPerformanceDate(date);
        performance.setTotalValue(totalValue);
        performance.setInitialCapital(initialCapital);

        BigDecimal returnRate = totalValue.subtract(initialCapital)
                .divide(initialCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        performance.setReturnRate(returnRate);

        if (currentHolding != null && currentQuantity > 0) {
            performance.setHoldingEtfCode(currentHolding);
            performance.setHoldingEtfName(etfNames.get(currentHolding));
            performance.setHoldingQuantity(currentQuantity);
            performance.setCurrentPrice(currentPrice);
        }

        return performance;
    }
}
