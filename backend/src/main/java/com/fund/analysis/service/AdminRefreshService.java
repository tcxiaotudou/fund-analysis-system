package com.fund.analysis.service;

import com.fund.analysis.exception.BusinessException;
import com.fund.analysis.exception.DataUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端数据刷新服务
 */
@Service
public class AdminRefreshService {

    /**
     * 市场数据服务
     */
    @Autowired
    private MarketDataService marketDataService;

    /**
     * RSI分析服务
     */
    @Autowired
    private RsiAnalysisService rsiAnalysisService;

    /**
     * MA策略服务
     */
    @Autowired
    private MaStrategyService maStrategyService;

    /**
     * 基金推荐服务
     */
    @Autowired
    private FundAnalysisService fundAnalysisService;

    /**
     * 基金组合服务
     */
    @Autowired
    private FundPortfolioService fundPortfolioService;

    /**
     * 蛋卷指数估值服务
     */
    @Autowired
    private DanjuanIndexValuationService danjuanIndexValuationService;

    /**
     * 刷新全部分析数据
     *
     * @return 刷新结果摘要
     */
    public Map<String, Object> refreshAll() {
        Map<String, Object> data = new HashMap<>();

        data.put("marketDataRefreshed", refreshMarketOverview());
        pauseForExternalApiInterval(5000);

        data.put("indexValuationRefreshed", refreshIndexValuation());
        pauseForExternalApiInterval(5000);

        data.put("rsiRecordsRefreshed", refreshRsi().get("recordsRefreshed"));
        pauseForExternalApiInterval(5000);

        data.put("maRecordsRefreshed", refreshMa().get("recordsRefreshed"));
        pauseForExternalApiInterval(5000);

        refreshFund();
        data.put("fundDataRefreshed", true);
        pauseForExternalApiInterval(5000);

        data.putAll(refreshPortfolioRsi());

        return data;
    }

    /**
     * 刷新市场概览数据
     *
     * @return 是否刷新成功
     */
    public boolean refreshMarket() {
        boolean refreshed = refreshMarketOverview();
        refreshIndexValuation();
        return refreshed;
    }

    /**
     * 刷新市场概览基础数据
     *
     * @return 是否刷新成功
     */
    private boolean refreshMarketOverview() {
        return marketDataService.refreshMarketOverview();
    }

    /**
     * 刷新指数估值缓存
     *
     * @return 是否刷新成功
     */
    public boolean refreshIndexValuation() {
        danjuanIndexValuationService.refreshNasdaq100Valuation();
        return true;
    }

    /**
     * 刷新 ETF RSI 数据
     *
     * @return 刷新结果摘要
     */
    public Map<String, Object> refreshRsi() {
        Map<String, Object> rsiData = new HashMap<>();
        rsiData.put("recordsRefreshed", rsiAnalysisService.refreshAllEtfRsi());
        return rsiData;
    }

    /**
     * 刷新 ETF MA 策略数据
     *
     * @return 刷新结果摘要
     */
    public Map<String, Object> refreshMa() {
        Map<String, Object> maData = new HashMap<>();
        maData.put("recordsRefreshed", maStrategyService.refreshAllEtfMa());
        return maData;
    }

    /**
     * 刷新基金推荐数据
     */
    public void refreshFund() {
        fundAnalysisService.refreshFundRecommendations();
    }

    /**
     * 刷新基金组合 RSI 和历史数据
     *
     * @return 刷新结果摘要
     */
    public Map<String, Object> refreshPortfolioRsi() {
        if (!fundPortfolioService.refreshPortfolioRsi()) {
            throw new DataUnavailableException("基金组合RSI数据刷新失败（可能没有持有基金）");
        }
        if (!fundPortfolioService.refreshPortfolioRsiHistory(100)) {
            throw new DataUnavailableException("基金组合RSI历史数据刷新失败（可能没有持有基金）");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("portfolioRsiRefreshed", true);
        data.put("portfolioRsiHistoryRefreshed", true);
        return data;
    }

    /**
     * 刷新基金组合 RSI 历史数据
     */
    public void refreshPortfolioRsiHistory() {
        if (!fundPortfolioService.refreshPortfolioRsiHistory(100)) {
            throw new DataUnavailableException("基金组合RSI历史数据刷新失败（可能没有持有基金）");
        }
    }

    /**
     * 等待外部接口调用间隔
     *
     * @param millis 等待毫秒数
     */
    private void pauseForExternalApiInterval(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("数据刷新等待被中断", e);
        }
    }
}
