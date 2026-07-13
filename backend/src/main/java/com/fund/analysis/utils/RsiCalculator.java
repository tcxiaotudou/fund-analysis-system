package com.fund.analysis.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * RSI计算工具类
 * 实现相对强弱指标（RSI）的计算
 */
public class RsiCalculator {

    private static final int CALCULATION_SCALE = 10;
    private static final int RESULT_SCALE = 2;
    
    /**
     * 计算RSI指标
     * @param prices 价格序列
     * @param period RSI周期
     * @return RSI值序列
     */
    public static List<BigDecimal> calculateRSI(List<BigDecimal> prices, int period) {
        List<BigDecimal> rsiList = new ArrayList<>();

        if (period <= 0) {
            throw new IllegalArgumentException("RSI周期必须大于0");
        }
        if (prices == null || prices.size() <= period) {
            return rsiList;
        }

        BigDecimal totalGain = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        for (int i = 1; i <= period; i++) {
            BigDecimal change = change(prices, i);
            if (change.signum() > 0) {
                totalGain = totalGain.add(change);
            } else {
                totalLoss = totalLoss.add(change.abs());
            }
        }

        BigDecimal periodValue = BigDecimal.valueOf(period);
        BigDecimal avgGain = totalGain.divide(
                periodValue, CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal avgLoss = totalLoss.divide(
                periodValue, CALCULATION_SCALE, RoundingMode.HALF_UP);
        rsiList.add(calculateValue(avgGain, avgLoss));

        for (int i = period + 1; i < prices.size(); i++) {
            BigDecimal change = change(prices, i);
            BigDecimal gain = change.signum() > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.signum() < 0 ? change.abs() : BigDecimal.ZERO;
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gain)
                    .divide(periodValue, CALCULATION_SCALE, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(loss)
                    .divide(periodValue, CALCULATION_SCALE, RoundingMode.HALF_UP);
            rsiList.add(calculateValue(avgGain, avgLoss));
        }

        return rsiList;
    }

    private static BigDecimal change(List<BigDecimal> prices, int index) {
        BigDecimal current = prices.get(index);
        BigDecimal previous = prices.get(index - 1);
        if (current == null || previous == null) {
            throw new IllegalArgumentException("价格序列不能包含空值");
        }
        return current.subtract(previous);
    }

    private static BigDecimal calculateValue(BigDecimal avgGain, BigDecimal avgLoss) {
        if (avgGain.signum() == 0 && avgLoss.signum() == 0) {
            return BigDecimal.valueOf(50).setScale(RESULT_SCALE);
        }
        if (avgLoss.signum() == 0) {
            return BigDecimal.valueOf(100).setScale(RESULT_SCALE);
        }
        if (avgGain.signum() == 0) {
            return BigDecimal.ZERO.setScale(RESULT_SCALE);
        }
        BigDecimal relativeStrength = avgGain.divide(
                avgLoss, CALCULATION_SCALE, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(100).divide(
                        BigDecimal.ONE.add(relativeStrength),
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP))
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }
}
