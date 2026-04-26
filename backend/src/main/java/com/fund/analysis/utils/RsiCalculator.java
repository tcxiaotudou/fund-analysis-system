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
    
    /**
     * 计算RSI指标
     * @param prices 价格序列
     * @param period RSI周期
     * @return RSI值序列
     */
    public static List<BigDecimal> calculateRSI(List<BigDecimal> prices, int period) {
        List<BigDecimal> rsiList = new ArrayList<>();
        
        if (prices == null || prices.size() < period + 1) {
            return rsiList;
        }
        
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();
        
        // 计算每日涨跌幅
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }
        
        // 填充前期空值
        for (int i = 0; i < period - 1; i++) {
            rsiList.add(BigDecimal.ZERO);
        }
        
        // 计算第一个RSI值
        BigDecimal avgGain = sumList(gains.subList(0, period))
                .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        BigDecimal avgLoss = sumList(losses.subList(0, period))
                .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        
        BigDecimal rs = avgGain.divide(avgLoss.add(BigDecimal.valueOf(0.0001)), 4, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP));
        rsiList.add(rsi);
        
        // 计算后续的RSI值
        for (int i = period; i < gains.size(); i++) {
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gains.get(i))
                    .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(losses.get(i))
                    .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            
            rs = avgGain.divide(avgLoss.add(BigDecimal.valueOf(0.0001)), 4, RoundingMode.HALF_UP);
            rsi = BigDecimal.valueOf(100)
                    .subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP));
            rsiList.add(rsi);
        }
        
        return rsiList;
    }
    
    /**
     * 计算列表元素总和
     * @param list BigDecimal列表
     * @return 总和
     */
    private static BigDecimal sumList(List<BigDecimal> list) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : list) {
            sum = sum.add(value);
        }
        return sum;
    }
}

