package com.fund.analysis.service;

import com.fund.analysis.dto.MomentumPerformanceDTO;
import com.fund.analysis.entity.MomentumStrategyPerformance;
import com.fund.analysis.mapper.MomentumStrategyPerformanceMapper;
import com.fund.analysis.mapper.MomentumStrategyTransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomentumStrategyServiceTest {

    @Mock
    private MomentumStrategyTransactionMapper transactionMapper;

    @Mock
    private MomentumStrategyPerformanceMapper performanceMapper;

    private MomentumStrategyService momentumStrategyService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeEach
    void setUp() {
        momentumStrategyService = new MomentumStrategyService();
        ReflectionTestUtils.setField(momentumStrategyService, "transactionMapper", transactionMapper);
        ReflectionTestUtils.setField(momentumStrategyService, "performanceMapper", performanceMapper);
    }

    @Test
    void calculatePerformanceByDateRangeReadsPersistedDailyPerformance() throws Exception {
        Date startDate = dateFormat.parse("2025-01-01");
        Date endDate = dateFormat.parse("2025-01-02");

        MomentumStrategyPerformance performance = new MomentumStrategyPerformance();
        performance.setPerformanceDate(dateFormat.parse("2025-01-02"));
        performance.setTotalValue(new BigDecimal("1020.000"));
        performance.setReturnRate(new BigDecimal("2.0000"));
        performance.setHoldingEtfCode("sh518880");
        performance.setHoldingEtfName("黄金ETF");
        performance.setHoldingQuantity(10L);
        performance.setCurrentPrice(new BigDecimal("12.000"));
        performance.setInitialCapital(new BigDecimal("1000.00"));

        when(performanceMapper.selectByDateRange(startDate, endDate))
                .thenReturn(Collections.singletonList(performance));

        List<MomentumPerformanceDTO> result = momentumStrategyService.calculatePerformanceByDateRange(startDate, endDate);

        assertEquals(1, result.size());
        MomentumPerformanceDTO dto = result.get(0);
        assertEquals("2025-01-02", dto.getDate());
        assertEquals(0, new BigDecimal("1020.000").compareTo(dto.getTotalValue()));
        assertEquals(0, new BigDecimal("2.0000").compareTo(dto.getReturnRate()));
        assertEquals("sh518880", dto.getHoldingEtfCode());
        assertEquals("黄金ETF", dto.getHoldingEtfName());
        assertEquals(10L, dto.getHoldingQuantity());
        assertEquals(0, new BigDecimal("12.000").compareTo(dto.getCurrentPrice()));
        verify(transactionMapper, never()).selectByDateRange(startDate, endDate);
    }
}
