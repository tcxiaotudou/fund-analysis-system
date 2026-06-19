package com.fund.analysis.controller;

import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.Result;
import com.fund.analysis.exception.BusinessException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.service.FundAnalysisService;
import com.fund.analysis.service.FundPortfolioService;
import com.fund.analysis.service.MaStrategyService;
import com.fund.analysis.service.MarketDataService;
import com.fund.analysis.service.RsiAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private RsiAnalysisService rsiAnalysisService;

    @Autowired
    private MaStrategyService maStrategyService;

    @Autowired
    private FundAnalysisService fundAnalysisService;

    @Autowired
    private FundPortfolioService fundPortfolioService;

    /**
     * 数据源
     */
    @Autowired
    private DataSource dataSource;

    @PostMapping("/refresh-all")
    public Result<Map<String, Object>> refreshAll() {
        Map<String, Object> data = new HashMap<>();

        data.put("marketDataRefreshed", marketDataService.refreshMarketOverview());
        pauseForExternalApiInterval(5000);

        data.put("rsiRecordsRefreshed", rsiAnalysisService.refreshAllEtfRsi());
        pauseForExternalApiInterval(5000);

        data.put("maRecordsRefreshed", maStrategyService.refreshAllEtfMa());
        pauseForExternalApiInterval(5000);

        fundAnalysisService.refreshFundRecommendations();
        data.put("fundDataRefreshed", true);
        pauseForExternalApiInterval(5000);

        boolean portfolioSuccess = fundPortfolioService.refreshPortfolioRsi();
        if (!portfolioSuccess) {
            throw new DataUnavailableException("基金组合RSI数据刷新失败（可能没有持有基金）");
        }
        data.put("portfolioRsiRefreshed", true);

        return Result.success("数据刷新成功", data);
    }

    @PostMapping("/refresh-market")
    public Result<Void> refreshMarket() {
        marketDataService.refreshMarketOverview();
        return Result.success("市场数据刷新成功", null);
    }

    @PostMapping("/refresh-rsi")
    public Result<Map<String, Object>> refreshRsi() {
        Map<String, Object> rsiData = new HashMap<>();
        rsiData.put("recordsRefreshed", rsiAnalysisService.refreshAllEtfRsi());
        return Result.success("RSI数据刷新成功", rsiData);
    }

    @PostMapping("/refresh-ma")
    public Result<Map<String, Object>> refreshMa() {
        Map<String, Object> maData = new HashMap<>();
        maData.put("recordsRefreshed", maStrategyService.refreshAllEtfMa());
        return Result.success("MA策略数据刷新成功", maData);
    }

    @PostMapping("/refresh-fund")
    public Result<Void> refreshFund() {
        fundAnalysisService.refreshFundRecommendations();
        return Result.success("基金推荐数据刷新成功", null);
    }

    @PostMapping("/refresh-portfolio-rsi")
    public Result<Void> refreshPortfolioRsi() {
        if (!fundPortfolioService.refreshPortfolioRsi()) {
            throw new DataUnavailableException("基金组合RSI数据刷新失败（可能没有持有基金）");
        }
        return Result.success("基金组合RSI数据刷新成功", null);
    }

    @PostMapping("/refresh-portfolio-rsi-history")
    public Result<Void> refreshPortfolioRsiHistory() {
        if (!fundPortfolioService.refreshPortfolioRsiHistory(100)) {
            throw new DataUnavailableException("基金组合RSI历史数据刷新失败（可能没有持有基金）");
        }
        return Result.success("基金组合RSI历史数据刷新成功", null);
    }

    /**
     * 获取系统状态
     *
     * @return 系统状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("apiStatus", "running");
        statusData.put("version", "1.0.0");
        putDatabaseStatus(statusData);
        putMarketDataStatus(statusData);
        statusData.put("status", resolveSystemStatus(statusData));
        String message = "degraded".equals(statusData.get("status")) ? "系统部分异常" : "系统运行正常";
        return Result.success(message, statusData);
    }

    /**
     * 写入数据库状态
     *
     * @param statusData 状态数据
     */
    private void putDatabaseStatus(Map<String, Object> statusData) {
        try (Connection connection = dataSource.getConnection()) {
            statusData.put("databaseStatus", connection.isValid(2) ? "running" : "error");
        } catch (SQLException e) {
            statusData.put("databaseStatus", "error");
            statusData.put("databaseMessage", e.getMessage());
        }
    }

    /**
     * 写入市场数据状态
     *
     * @param statusData 状态数据
     */
    private void putMarketDataStatus(Map<String, Object> statusData) {
        try {
            MarketOverviewDTO overview = marketDataService.getCoreMarketOverview();
            statusData.put("marketDataStatus", "running");
            statusData.put("marketDataUpdateTime", overview.getUpdateTime());
        } catch (RuntimeException e) {
            statusData.put("marketDataStatus", "error");
            statusData.put("marketDataMessage", e.getMessage());
        }
    }

    /**
     * 解析系统总体状态
     *
     * @param statusData 状态数据
     * @return 系统总体状态
     */
    private String resolveSystemStatus(Map<String, Object> statusData) {
        if ("error".equals(statusData.get("databaseStatus")) || "error".equals(statusData.get("marketDataStatus"))) {
            return "degraded";
        }
        return "running";
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
