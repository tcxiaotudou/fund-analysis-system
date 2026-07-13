package com.fund.analysis.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        assertEquals(3, values.size());
        assertEquals(new BigDecimal("100.00"), values.get(values.size() - 1));
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

        assertEquals(3, values.size());
        assertEquals(new BigDecimal("0.00"), values.get(values.size() - 1));
    }

    @Test
    void calculatesNeutralRsiForFlatPrices() {
        List<BigDecimal> values = RsiCalculator.calculateRSI(
                Collections.nCopies(6, BigDecimal.TEN), 3);

        assertEquals(new BigDecimal("50.00"), values.get(values.size() - 1));
    }

    @Test
    void matchesWilderReferenceValue() {
        List<BigDecimal> prices = Arrays.asList(
                "44.34", "44.09", "44.15", "43.61", "44.33",
                "44.83", "45.10", "45.42", "45.84", "46.08",
                "45.89", "46.03", "45.61", "46.28", "46.28"
        ).stream().map(BigDecimal::new).collect(Collectors.toList());

        List<BigDecimal> values = RsiCalculator.calculateRSI(prices, 14);

        assertEquals(Collections.singletonList(new BigDecimal("70.46")), values);
    }

    @Test
    void remainsInvariantWhenPricesAreScaled() {
        List<BigDecimal> lowPrices = java.util.stream.IntStream.range(0, 31)
                .mapToObj(i -> new BigDecimal(i % 2 == 0 ? "1.0000" : "1.0001"))
                .collect(Collectors.toList());
        List<BigDecimal> scaledPrices = lowPrices.stream()
                .map(price -> price.multiply(BigDecimal.valueOf(10000)))
                .collect(Collectors.toList());

        List<BigDecimal> lowRsi = RsiCalculator.calculateRSI(lowPrices, 14);
        List<BigDecimal> scaledRsi = RsiCalculator.calculateRSI(scaledPrices, 14);

        assertEquals(lowRsi, scaledRsi);
    }

    @Test
    void rejectsInvalidPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> RsiCalculator.calculateRSI(Collections.singletonList(BigDecimal.ONE), 0));
    }
}
