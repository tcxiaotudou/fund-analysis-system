package com.fund.analysis.controller;

import com.fund.analysis.dto.BackgroundRefreshStatusDTO;
import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.Result;
import com.fund.analysis.service.AdminRefreshService;
import com.fund.analysis.service.BackgroundRefreshService;
import com.fund.analysis.service.MarketDataService;
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

    /**
     * 管理端刷新服务
     */
    @Autowired
    private AdminRefreshService adminRefreshService;

    /**
     * 后台刷新服务
     */
    @Autowired
    private BackgroundRefreshService backgroundRefreshService;

    /**
     * 市场数据服务
     */
    @Autowired
    private MarketDataService marketDataService;

    /**
     * 数据源
     */
    @Autowired
    private DataSource dataSource;

    /**
     * 同步刷新全部分析数据
     *
     * @return 刷新结果摘要
     */
    @PostMapping("/refresh-all")
    public Result<Map<String, Object>> refreshAll() {
        return Result.success("数据刷新成功", adminRefreshService.refreshAll());
    }

    /**
     * 启动后台全量刷新
     *
     * @return 后台刷新状态
     */
    @PostMapping("/refresh-all/background")
    public Result<BackgroundRefreshStatusDTO> startBackgroundRefresh() {
        return Result.success("后台刷新已启动", backgroundRefreshService.startFullRefresh());
    }

    /**
     * 获取后台全量刷新状态
     *
     * @return 后台刷新状态
     */
    @GetMapping("/refresh-all/status")
    public Result<BackgroundRefreshStatusDTO> getBackgroundRefreshStatus() {
        return Result.success("获取成功", backgroundRefreshService.getStatus());
    }

    /**
     * 刷新市场概览数据
     *
     * @return 刷新结果
     */
    @PostMapping("/refresh-market")
    public Result<Void> refreshMarket() {
        adminRefreshService.refreshMarket();
        return Result.success("市场数据刷新成功", null);
    }

    /**
     * 刷新 ETF RSI 数据
     *
     * @return 刷新结果摘要
     */
    @PostMapping("/refresh-rsi")
    public Result<Map<String, Object>> refreshRsi() {
        return Result.success("RSI数据刷新成功", adminRefreshService.refreshRsi());
    }

    /**
     * 刷新 ETF MA 策略数据
     *
     * @return 刷新结果摘要
     */
    @PostMapping("/refresh-ma")
    public Result<Map<String, Object>> refreshMa() {
        return Result.success("MA策略数据刷新成功", adminRefreshService.refreshMa());
    }

    /**
     * 刷新基金推荐数据
     *
     * @return 刷新结果
     */
    @PostMapping("/refresh-fund")
    public Result<Void> refreshFund() {
        adminRefreshService.refreshFund();
        return Result.success("基金推荐数据刷新成功", null);
    }

    /**
     * 刷新基金组合 RSI 数据
     *
     * @return 刷新结果摘要
     */
    @PostMapping("/refresh-portfolio-rsi")
    public Result<Map<String, Object>> refreshPortfolioRsi() {
        return Result.success("基金组合RSI数据刷新成功", adminRefreshService.refreshPortfolioRsi());
    }

    /**
     * 刷新基金组合 RSI 历史数据
     *
     * @return 刷新结果
     */
    @PostMapping("/refresh-portfolio-rsi-history")
    public Result<Void> refreshPortfolioRsiHistory() {
        adminRefreshService.refreshPortfolioRsiHistory();
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

}
