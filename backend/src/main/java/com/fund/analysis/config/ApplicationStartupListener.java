package com.fund.analysis.config;

import com.fund.analysis.entity.RsiAnalysis;
import com.fund.analysis.entity.StockBondBalance;
import com.fund.analysis.mapper.RsiAnalysisMapper;
import com.fund.analysis.mapper.StockBondBalanceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动检查器
 */
@Component
public class ApplicationStartupListener implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);
    private static final String GUO_ZHENG = "sz399317";

    private final StockBondBalanceMapper stockBondBalanceMapper;
    private final RsiAnalysisMapper rsiAnalysisMapper;

    public ApplicationStartupListener(StockBondBalanceMapper stockBondBalanceMapper,
                                      RsiAnalysisMapper rsiAnalysisMapper) {
        this.stockBondBalanceMapper = stockBondBalanceMapper;
        this.rsiAnalysisMapper = rsiAnalysisMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("========== 应用启动数据检查 ==========");
        if (checkNeedRefresh()) {
            logger.warn("数据库中缺少市场概览数据，请调用 POST /api/admin/refresh-market 或等待定时任务刷新");
        } else {
            logger.info("数据库中已有市场概览基础数据");
        }
        logger.info("========== 应用启动数据检查完成 ==========");
    }

    private boolean checkNeedRefresh() {
        StockBondBalance stockBondBalance = stockBondBalanceMapper.selectLatest();
        if (stockBondBalance == null) {
            logger.info("股债平衡表中没有数据");
            return true;
        }

        RsiAnalysis rsi14 = rsiAnalysisMapper.selectLatestByCodeAndPeriod(GUO_ZHENG, 14);
        RsiAnalysis rsi90 = rsiAnalysisMapper.selectLatestByCodeAndPeriod(GUO_ZHENG, 90);
        if (rsi14 == null || rsi90 == null) {
            logger.info("RSI分析表中没有国证指数数据");
            return true;
        }

        return false;
    }
}
