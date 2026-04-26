package com.fund.analysis.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsiCalculatorTest {

    @Test
    void returnsEmptyWhenDataIsInsufficient() {
        List<BigDecimal> values = RsiCalculator.calculateRSI(
                Arrays.asList(BigDecimal.ONE, BigDecimal.TEN), 14);

        assertTrue(values.isEmpty());
    }

    @Test
    void calculatesHighRsiForContinuousRise() {
        List<BigDecimal> values = RsiCalculator.calculateRSI(Arrays.asList(
                BigDecimal.ONE,
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(6)
        ), 3);

        assertEquals(5, values.size());
        assertTrue(values.get(values.size() - 1).compareTo(BigDecimal.valueOf(99)) > 0);
    }

    @Test
    void calculatesLowRsiForContinuousDrop() {
        List<BigDecimal> values = RsiCalculator.calculateRSI(Arrays.asList(
                BigDecimal.valueOf(6),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(2),
                BigDecimal.ONE
        ), 3);

        assertEquals(5, values.size());
        assertTrue(values.get(values.size() - 1).compareTo(BigDecimal.ONE) < 0);
    }

    @Test
    void calculatesZeroRsiForFlatPrices() {
        List<BigDecimal> values = RsiCalculator.calculateRSI(
                Collections.nCopies(6, BigDecimal.TEN), 3);

        assertEquals(BigDecimal.valueOf(0.00).setScale(2), values.get(values.size() - 1).setScale(2));
    }
}
