package com.fund.analysis.scheduled;

import com.fund.analysis.exception.BusinessException;
import com.fund.analysis.service.FundAnalysisService;
import com.fund.analysis.service.FundPortfolioService;
import com.fund.analysis.service.MarketDataService;
import com.fund.analysis.service.MaStrategyService;
import com.fund.analysis.service.MomentumStrategyService;
import com.fund.analysis.service.RsiAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据刷新定时任务
 * 定时从第三方API获取数据并保存到数据库
 */
@Component
@EnableScheduling
public class DataRefreshTask {
    
    private static final Logger logger = LoggerFactory.getLogger(DataRefreshTask.class);
    
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
    
    @Autowired
    private MomentumStrategyService momentumStrategyService;
    
    /**
     * 每天早上9点执行市场数据刷新
     * 刷新市场概览数据（RSI、风险溢价、5年均线偏离度等）
     */
    @Scheduled(cron = "0 0/5 9-16 * * MON-FRI")
    public void refreshMarketData() {
        runRefreshTask("刷新市场数据", () -> {
            boolean success = marketDataService.refreshMarketOverview();
            if (!success) {
                throw new BusinessException("市场数据刷新返回失败");
            }
            logger.info("市场数据刷新成功");
        });
    }

    /**
     * 每天早上9点到15点之间每1分钟执行一次ETF RSI数据刷新
     */
    @Scheduled(cron = "0 0/2 9-16 * * MON-FRI")
    public void refreshEtfRsi() {
        runRefreshTask("刷新ETF RSI数据", () -> {
            int count = rsiAnalysisService.refreshAllEtfRsi();
            logger.info("ETF RSI数据刷新完成，共刷新 {} 条记录", count);
        });
    }
    
    /**
     * 每天早上9点到15点之间每1分钟执行一次ETF MA策略数据刷新
     */
    @Scheduled(cron = "0 0/2 9-16 * * MON-FRI")
    public void refreshEtfMa() {
        runRefreshTask("刷新ETF MA策略数据", () -> {
            int count = maStrategyService.refreshAllEtfMa();
            logger.info("ETF MA策略数据刷新完成，共刷新 {} 条记录", count);
        });
    }
    
    /**
     * 每周9-15点执行基金推荐数据刷新
     * 刷新基金推荐列表
     */
    @Scheduled(cron = "0 0/5 9-18 ? * MON-FRI")
    public void refreshFundRecommendations() {
        runRefreshTask("刷新基金推荐数据", () -> {
            fundAnalysisService.refreshFundRecommendations();
            logger.info("基金推荐数据刷新完成");
        });
    }
    
    /**
     * 每天早上9点到15点之间每5分钟执行一次基金组合 RSI 数据刷新
     */
    @Scheduled(cron = "0 0/5 9-16 * * MON-FRI")
    public void refreshFundPortfolioRsi() {
        runRefreshTask("刷新基金组合 RSI 数据", () -> {
            boolean success = fundPortfolioService.refreshPortfolioRsi();
            if (!success) {
                throw new BusinessException("基金组合 RSI 数据刷新返回失败");
            }
            sleepForRateLimit(2000);
            boolean historySuccess = fundPortfolioService.refreshPortfolioRsiHistory(100);
            if (!historySuccess) {
                throw new BusinessException("基金组合 RSI 历史数据刷新返回失败");
            }
        });
    }
    
    /**
     * 每天早上9点到15点之间每5分钟执行一次动量策略数据刷新
     */
    @Scheduled(cron = "0 0/5 9-16 * * MON-FRI")
    public void refreshMomentumStrategy() {
        runRefreshTask("刷新动量策略数据", () -> {
            int count = momentumStrategyService.refreshMomentumStrategy();
            logger.info("动量策略数据刷新完成，共生成 {} 条新交易记录", count);
        });
    }
    
    /**
     * 每天下午14:00执行动量策略回测和交易
     * 这是主要的动量策略执行时间点
     */
    @Scheduled(cron = "0 0 14 * * MON-FRI")
    public void executeDailyMomentumStrategy() {
        runRefreshTask("执行每日动量策略", () -> {
            int count = momentumStrategyService.refreshMomentumStrategy();
            logger.info("每日动量策略执行完成，共生成 {} 条新交易记录", count);
        });
    }
    
    /**
     * 每天下午3点30分执行完整的数据刷新（兜底任务）
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void fullDataRefresh() {
        runRefreshTask("执行完整数据刷新", () -> {
            logger.info("正在刷新市场数据...");
            marketDataService.refreshMarketOverview();
            sleepForRateLimit(5000);

            logger.info("正在刷新ETF RSI数据...");
            int rsiCount = rsiAnalysisService.refreshAllEtfRsi();
            logger.info("刷新了 {} 条RSI记录", rsiCount);
            sleepForRateLimit(5000);

            logger.info("正在刷新ETF MA策略数据...");
            int maCount = maStrategyService.refreshAllEtfMa();
            logger.info("刷新了 {} 条MA记录", maCount);
            sleepForRateLimit(5000);

            logger.info("正在刷新基金组合 RSI 数据...");
            if (!fundPortfolioService.refreshPortfolioRsi()) {
                throw new BusinessException("基金组合 RSI 数据刷新返回失败");
            }
            sleepForRateLimit(2000);

            logger.info("正在刷新基金组合 RSI 历史数据...");
            if (!fundPortfolioService.refreshPortfolioRsiHistory(100)) {
                throw new BusinessException("基金组合 RSI 历史数据刷新返回失败");
            }
            sleepForRateLimit(5000);

            logger.info("正在刷新动量策略数据...");
            int momentumCount = momentumStrategyService.refreshMomentumStrategy();
            logger.info("刷新了 {} 条动量策略交易记录", momentumCount);
        });
    }

    /**
     * 执行刷新任务，失败时显式抛出异常
     *
     * @param taskName 任务名称
     * @param task 刷新逻辑
     */
    private void runRefreshTask(String taskName, Runnable task) {
        logger.info("========== 开始{} ==========", taskName);
        try {
            task.run();
            logger.info("========== {}完成 ==========", taskName);
        } catch (RuntimeException e) {
            logger.error("{}失败", taskName, e);
            throw e;
        } catch (Exception e) {
            logger.error("{}失败", taskName, e);
            throw new BusinessException(taskName + "失败", e);
        }
    }

    /**
     * 等待第三方接口限流窗口
     *
     * @param millis 等待毫秒数
     */
    private void sleepForRateLimit(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("刷新任务等待被中断", e);
        }
    }
}
