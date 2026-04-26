package com.fund.analysis.scheduled;

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
        logger.info("========== 开始刷新市场数据 ==========");
        
        try {
            boolean success = marketDataService.refreshMarketOverview();
            if (success) {
                logger.info("市场数据刷新成功");
            } else {
                logger.warn("市场数据刷新失败");
            }
        } catch (Exception e) {
            logger.error("市场数据刷新异常", e);
        }
        
        logger.info("========== 市场数据刷新完成 ==========");
    }

    /**
     * 每天早上9点到15点之间每1分钟执行一次ETF RSI数据刷新
     */
    @Scheduled(cron = "0 0/2 9-16 * * MON-FRI")
    public void refreshEtfRsi() {
        logger.info("========== 开始刷新ETF RSI数据 ==========");
        
        try {
            int count = rsiAnalysisService.refreshAllEtfRsi();
            logger.info("ETF RSI数据刷新完成，共刷新 {} 条记录", count);
        } catch (Exception e) {
            logger.error("ETF RSI数据刷新异常", e);
        }
        
        logger.info("========== ETF RSI数据刷新完成 ==========");
    }
    
    /**
     * 每天早上9点到15点之间每1分钟执行一次ETF MA策略数据刷新
     */
    @Scheduled(cron = "0 0/2 9-16 * * MON-FRI")
    public void refreshEtfMa() {
        logger.info("========== 开始刷新ETF MA策略数据 ==========");
        
        try {
            int count = maStrategyService.refreshAllEtfMa();
            logger.info("ETF MA策略数据刷新完成，共刷新 {} 条记录", count);
        } catch (Exception e) {
            logger.error("ETF MA策略数据刷新异常", e);
        }
        
        logger.info("========== ETF MA策略数据刷新完成 ==========");
    }
    
    /**
     * 每周9-15点执行基金推荐数据刷新
     * 刷新基金推荐列表
     */
    @Scheduled(cron = "0 0/5 9-18 ? * MON-FRI")
    public void refreshFundRecommendations() {
        logger.info("========== 开始刷新基金推荐数据 ==========");
        
        try {
            fundAnalysisService.refreshFundRecommendations();
            logger.info("基金推荐数据刷新完成");
        } catch (Exception e) {
            logger.error("基金推荐数据刷新异常", e);
        }
        
        logger.info("========== 基金推荐数据刷新完成 ==========");
    }
    
    /**
     * 每天早上9点到15点之间每5分钟执行一次基金组合 RSI 数据刷新
     */
    @Scheduled(cron = "0 0/5 9-16 * * MON-FRI")
    public void refreshFundPortfolioRsi() {
        logger.info("========== 开始刷新基金组合 RSI 数据 ==========");
        
        try {
            // 刷新当前RSI数据
            boolean success = fundPortfolioService.refreshPortfolioRsi();
            if (success) {
                logger.info("基金组合 RSI 数据刷新成功");
            } else {
                logger.warn("基金组合 RSI 数据刷新失败");
            }
            
            // 等待2秒避免API限流
            Thread.sleep(2000);
            
            // 刷新RSI历史数据（最近100天）
            boolean historySuccess = fundPortfolioService.refreshPortfolioRsiHistory(100);
            if (historySuccess) {
                logger.info("基金组合 RSI 历史数据刷新成功");
            } else {
                logger.warn("基金组合 RSI 历史数据刷新失败");
            }
        } catch (Exception e) {
            logger.error("基金组合 RSI 数据刷新异常", e);
        }
        
        logger.info("========== 基金组合 RSI 数据刷新完成 ==========");
    }
    
    /**
     * 每天早上9点到15点之间每5分钟执行一次动量策略数据刷新
     */
    @Scheduled(cron = "0 0/5 9-16 * * MON-FRI")
    public void refreshMomentumStrategy() {
        logger.info("========== 开始刷新动量策略数据 ==========");
        
        try {
            int count = momentumStrategyService.refreshMomentumStrategy();
            logger.info("动量策略数据刷新完成，共生成 {} 条新交易记录", count);
        } catch (Exception e) {
            logger.error("动量策略数据刷新异常", e);
        }
        
        logger.info("========== 动量策略数据刷新完成 ==========");
    }
    
    /**
     * 每天下午14:00执行动量策略回测和交易
     * 这是主要的动量策略执行时间点
     */
    @Scheduled(cron = "0 0 14 * * MON-FRI")
    public void executeDailyMomentumStrategy() {
        logger.info("========== 开始执行每日动量策略 ==========");
        
        try {
            int count = momentumStrategyService.refreshMomentumStrategy();
            if (count > 0) {
                logger.info("每日动量策略执行完成，共生成 {} 条新交易记录", count);
            } else {
                logger.info("每日动量策略执行完成，无新交易记录生成");
            }
        } catch (Exception e) {
            logger.error("每日动量策略执行异常", e);
        }
        
        logger.info("========== 每日动量策略执行完成 ==========");
    }
    
    /**
     * 每天下午3点30分执行完整的数据刷新（兜底任务）
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void fullDataRefresh() {
        logger.info("========== 开始执行完整数据刷新 ==========");
        
        try {
            // 刷新市场数据
            logger.info("正在刷新市场数据...");
            marketDataService.refreshMarketOverview();
            
            // 等待5秒避免API限流
            Thread.sleep(5000);
            
            // 刷新ETF RSI数据
            logger.info("正在刷新ETF RSI数据...");
            int rsiCount = rsiAnalysisService.refreshAllEtfRsi();
            logger.info("刷新了 {} 条RSI记录", rsiCount);
            
            // 等待5秒避免API限流
            Thread.sleep(5000);
            
            // 刷新ETF MA策略数据
            logger.info("正在刷新ETF MA策略数据...");
            int maCount = maStrategyService.refreshAllEtfMa();
            logger.info("刷新了 {} 条MA记录", maCount);
            
            // 等待5秒避免API限流
            Thread.sleep(5000);
            
            // 刷新基金组合RSI数据
            logger.info("正在刷新基金组合 RSI 数据...");
            boolean portfolioSuccess = fundPortfolioService.refreshPortfolioRsi();
            if (portfolioSuccess) {
                logger.info("基金组合 RSI 数据刷新成功");
            } else {
                logger.warn("基金组合 RSI 数据刷新失败");
            }
            
            // 等待2秒避免API限流
            Thread.sleep(2000);
            
            // 刷新基金组合RSI历史数据
            logger.info("正在刷新基金组合 RSI 历史数据...");
            boolean historySuccess = fundPortfolioService.refreshPortfolioRsiHistory(100);
            if (historySuccess) {
                logger.info("基金组合 RSI 历史数据刷新成功");
            } else {
                logger.warn("基金组合 RSI 历史数据刷新失败");
            }
            
            // 等待5秒避免API限流
            Thread.sleep(5000);
            
            // 刷新动量策略数据
            logger.info("正在刷新动量策略数据...");
            int momentumCount = momentumStrategyService.refreshMomentumStrategy();
            logger.info("刷新了 {} 条动量策略交易记录", momentumCount);
            
            logger.info("完整数据刷新完成");
            
        } catch (Exception e) {
            logger.error("完整数据刷新异常", e);
        }
        
        logger.info("========== 完整数据刷新完成 ==========");
    }
}

