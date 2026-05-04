package com.fund.analysis.service;

import com.fund.analysis.entity.MomentumStrategyPerformance;
import com.fund.analysis.exception.DataUnavailableException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MomentumPerformanceCalculatorTest {

    private final MomentumPerformanceCalculator calculator = new MomentumPerformanceCalculator();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    void buildsDailyPerformanceFromCurrentHoldingPrice() throws Exception {
        Map<String, BigDecimal> priceMap = new HashMap<>();
        priceMap.put("sh518880", new BigDecimal("12.000"));

        Map<String, String> etfNames = new HashMap<>();
        etfNames.put("sh518880", "黄金ETF");

        MomentumStrategyPerformance performance = calculator.buildDailyPerformance(
                dateFormat.parse("2025-01-02"),
                new BigDecimal("1000.00"),
                new BigDecimal("900.00"),
                "sh518880",
                10L,
                priceMap,
                etfNames
        );

        assertEquals("2025-01-02", dateFormat.format(performance.getPerformanceDate()));
        assertEquals(0, new BigDecimal("1020.000").compareTo(performance.getTotalValue()));
        assertEquals(0, new BigDecimal("2.0000").compareTo(performance.getReturnRate()));
        assertEquals("sh518880", performance.getHoldingEtfCode());
        assertEquals("黄金ETF", performance.getHoldingEtfName());
        assertEquals(10L, performance.getHoldingQuantity());
        assertEquals(0, new BigDecimal("12.000").compareTo(performance.getCurrentPrice()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(performance.getInitialCapital()));
    }

    @Test
    void throwsWhenHoldingPriceIsMissing() throws Exception {
        DataUnavailableException exception = assertThrows(
                DataUnavailableException.class,
                () -> calculator.buildDailyPerformance(
                        dateFormat.parse("2025-01-02"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("900.00"),
                        "sh518880",
                        10L,
                        new HashMap<>(),
                        new HashMap<>()
                )
        );

        assertEquals("动量策略收益曲线缺少持仓价格: sh518880, 日期=2025-01-02", exception.getMessage());
    }
}
