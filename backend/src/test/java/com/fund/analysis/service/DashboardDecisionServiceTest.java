package com.fund.analysis.service;

import com.fund.analysis.dto.DashboardDecisionDTO;
import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.RsiDataDTO;
import com.fund.analysis.entity.FundInfo;
import com.fund.analysis.exception.DataUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardDecisionServiceTest {

    /**
     * 市场数据服务
     */
    @Mock
    private MarketDataService marketDataService;

    /**
     * 基金推荐服务
     */
    @Mock
    private FundAnalysisService fundAnalysisService;

    /**
     * MA策略服务
     */
    @Mock
    private MaStrategyService maStrategyService;

    /**
     * RSI分析服务
     */
    @Mock
    private RsiAnalysisService rsiAnalysisService;

    /**
     * 基金黑名单服务
     */
    @Mock
    private FundBlacklistService fundBlacklistService;

    /**
     * 决策驾驶舱聚合服务
     */
    @InjectMocks
    private DashboardDecisionService dashboardDecisionService;

    /**
     * 验证聚合服务从现有模块组装首页数据
     */
    @Test
    void buildsDecisionDashboardFromExistingModules() {
        MarketOverviewDTO overview = new MarketOverviewDTO();
        overview.setRsi14("56.32");
        overview.setRsi90("58.71");
        overview.setBalanceSuggestion("6股4债");
        overview.setRiskPremium("4.21%");
        overview.setMa5yDeviation("「2026-06-19」 -3.62%");
        overview.setUpdateTime("2026-06-19 10:25:36");

        RsiDataDTO buySignal = new RsiDataDTO();
        buySignal.setCode("510300.SH");
        buySignal.setName("沪深300ETF");
        buySignal.setCurrentRsi(new BigDecimal("41.25"));
        buySignal.setIsBuySignal(true);

        FundInfo fund = new FundInfo();
        fund.setFundCode("005827");
        fund.setFundName("易方达蓝筹精选混合");
        fund.setIsHolding(1);

        when(marketDataService.getCoreMarketOverview()).thenReturn(overview);
        when(marketDataService.formatStockBondRatio("6股4债")).thenReturn("6 / 4");
        when(rsiAnalysisService.getEtfBuySignals()).thenReturn(Collections.singletonList(buySignal));
        when(maStrategyService.getMaActionSignals()).thenReturn(Collections.emptyList());
        when(fundAnalysisService.getFundRecommendations()).thenReturn(Collections.singletonList(fund));
        when(fundBlacklistService.isBlacklisted("005827")).thenReturn(false);

        DashboardDecisionDTO result = dashboardDecisionService.getDecisionDashboard();

        assertEquals("normal", result.getDataStatus().getStatus());
        assertEquals("2026-06-19 10:25:36", result.getUpdateTime());
        assertEquals(3, result.getDecisions().size());
        assertEquals("1", result.getDecisions().get(0).getValue());
        assertEquals("6 / 4", result.getMetrics().get(3).getValue());
        assertEquals("沪深300ETF", result.getEtfOpportunities().get(0).getName());
        assertEquals("已持有", result.getFundRecommendations().get(0).getTag());
        assertEquals(4, result.getOperations().size());
        assertEquals(1, result.getTrendPoints().size());
    }

    /**
     * 验证基金推荐失败时暴露模块级错误
     */
    @Test
    void exposesModuleErrorWhenFundRecommendationsFail() {
        MarketOverviewDTO overview = new MarketOverviewDTO();
        overview.setRsi14("42.00");
        overview.setRsi90("51.00");
        when(marketDataService.getCoreMarketOverview()).thenReturn(overview);
        when(maStrategyService.getMaActionSignals()).thenReturn(Collections.emptyList());
        when(fundAnalysisService.getFundRecommendations()).thenThrow(new IllegalStateException("推荐接口异常"));

        DashboardDecisionDTO result = dashboardDecisionService.getDecisionDashboard();

        assertEquals("partial", result.getDataStatus().getStatus());
        assertFalse(result.getDataStatus().getModuleErrors().isEmpty());
        assertEquals("基金推荐", result.getDataStatus().getModuleErrors().get(0).getModule());
        assertTrue(result.getDataStatus().getMessage().contains("部分数据加载失败"));
    }

    /**
     * 验证核心市场概览失败时首页不可伪装为正常
     */
    @Test
    void exposesErrorWhenCoreMarketOverviewFails() {
        when(marketDataService.getCoreMarketOverview()).thenThrow(new DataUnavailableException("国证指数90日RSI缺失"));
        when(maStrategyService.getMaActionSignals()).thenReturn(Collections.emptyList());
        when(fundAnalysisService.getFundRecommendations()).thenReturn(Collections.emptyList());

        DashboardDecisionDTO result = dashboardDecisionService.getDecisionDashboard();

        assertEquals("error", result.getDataStatus().getStatus());
        assertEquals("市场概览", result.getDataStatus().getModuleErrors().get(0).getModule());
        assertTrue(result.getDataStatus().getMessage().contains("首页核心数据不可用"));
    }

    /**
     * 验证MA信号失败不会清空核心市场概览
     */
    @Test
    void exposesMaModuleErrorWithoutClearingMarketOverview() {
        MarketOverviewDTO overview = new MarketOverviewDTO();
        overview.setRsi14("42.00");
        overview.setRsi90("51.00");
        overview.setBalanceSuggestion("6股4债");
        when(marketDataService.getCoreMarketOverview()).thenReturn(overview);
        when(marketDataService.formatStockBondRatio("6股4债")).thenReturn("6 / 4");
        when(maStrategyService.getMaActionSignals()).thenThrow(new IllegalStateException("MA查询失败"));
        when(fundAnalysisService.getFundRecommendations()).thenReturn(Collections.emptyList());

        DashboardDecisionDTO result = dashboardDecisionService.getDecisionDashboard();

        assertEquals("partial", result.getDataStatus().getStatus());
        assertEquals("MA信号", result.getDataStatus().getModuleErrors().get(0).getModule());
        assertFalse(result.getMetrics().isEmpty());
        assertEquals("6 / 4", result.getMetrics().get(3).getValue());
    }

    /**
     * 验证核心市场概览要求90日RSI存在
     */
    @Test
    void coreMarketOverviewRequiresRsi90() {
        MarketDataService service = new MarketDataService();
        ReflectionTestUtils.setField(service, "rsiAnalysisService", rsiAnalysisService);

        RsiDataDTO rsi14 = new RsiDataDTO();
        rsi14.setCurrentRsi(new BigDecimal("42.00"));
        when(rsiAnalysisService.getLatestRsi("sz399317", 14)).thenReturn(rsi14);
        when(rsiAnalysisService.getLatestRsi("sz399317", 90)).thenReturn(null);

        assertThrows(DataUnavailableException.class, service::getCoreMarketOverview);
    }
}
