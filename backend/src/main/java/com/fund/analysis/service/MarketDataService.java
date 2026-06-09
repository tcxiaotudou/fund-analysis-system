package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.fund.analysis.dto.FundPortfolioRsiDTO;
import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.RsiDataDTO;
import com.fund.analysis.entity.StockBondBalance;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.mapper.StockBondBalanceMapper;
import com.fund.analysis.mapper.SystemConfigMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 市场数据服务类
 * 提供市场整体数据和分析
 */
@Service
public class MarketDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // 国证指数代码
    private static final String GUO_ZHENG = "sz399317";
    // 30年国债代码
    private static final String GUO_ZHAI = "sh511090";
    
    @Autowired
    private RsiAnalysisService rsiAnalysisService;
    
    @Autowired
    private MaStrategyService maStrategyService;
    
    @Autowired
    private StockBondBalanceMapper stockBondBalanceMapper;
    
    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private ExternalApiClient externalApiClient;

    /**
     * 短事务执行器
     */
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    /**
     * 获取市场概览数据（从数据库读取）
     * @return 市场概览DTO
     */
    public MarketOverviewDTO getMarketOverview() {
        MarketOverviewDTO overview = new MarketOverviewDTO();
        
        // 从数据库获取国证指数的RSI
        RsiDataDTO rsi14 = rsiAnalysisService.getLatestRsi(GUO_ZHENG, 14);
        RsiDataDTO rsi90 = rsiAnalysisService.getLatestRsi(GUO_ZHENG, 90);
        
        if (rsi14 != null) {
            overview.setRsi14(String.format("%.2f", rsi14.getCurrentRsi()));
        }
        if (rsi90 != null) {
            overview.setRsi90(String.format("%.2f", rsi90.getCurrentRsi()));
            
            // 计算股债平衡建议
            String balanceSuggestion = calculateStockBondBalance(rsi90.getCurrentRsi());
            overview.setBalanceSuggestion(balanceSuggestion);
        }
        
        // 从数据库获取国债RSI
        RsiDataDTO bondRsi14 = rsiAnalysisService.getLatestRsi(GUO_ZHAI, 14);
        if (bondRsi14 != null) {
            overview.setBondRsi14(String.format("%.2f", bondRsi14.getCurrentRsi()));
        }
        
        // 从数据库获取最新的股债平衡数据
        StockBondBalance balance = stockBondBalanceMapper.selectLatest();
        if (balance != null) {
            overview.setRiskPremium(balance.getRiskPremium());
            overview.setMa5yDeviation(balance.getMa5yDeviation());
        }
        
        // 获取养老基金组合RSI
        FundPortfolioRsiDTO portfolioRsi = getFundPortfolioRsi();
        overview.setFundPortfolioRsi(portfolioRsi);
        
        // 获取ETF买入机会
        overview.setEtfOpportunities(rsiAnalysisService.getEtfBuySignals());
        
        // 获取MA策略买入信号
        overview.setMaSignals(maStrategyService.getMaBuySignals());
        
        // 设置更新时间（使用最新的股债平衡数据时间或当前时间）
        if (balance != null && balance.getDataTime() != null) {
            overview.setUpdateTime(dateFormat.format(balance.getDataTime()));
        } else {
            overview.setUpdateTime(dateFormat.format(new Date()));
        }
        
        return overview;
    }
    
    /**
     * 刷新市场概览数据（定时任务使用）
     * @return 是否成功
     */
    public boolean refreshMarketOverview() {
        // 刷新国证指数的RSI
        rsiAnalysisService.refreshRsi(GUO_ZHENG, 14);
        RsiDataDTO rsi90 = rsiAnalysisService.refreshRsi(GUO_ZHENG, 90);

        // 刷新国债RSI
        rsiAnalysisService.refreshRsi(GUO_ZHAI, 14);

        String riskPremium = refreshStock300RiskPremium();
        String ma5yDeviation = refreshMa5yDeviation();

        if (rsi90 == null || rsi90.getCurrentRsi() == null) {
            throw new DataUnavailableException("国证指数90日RSI数据不足，无法刷新市场概览");
        }

        String balanceSuggestion = calculateStockBondBalance(rsi90.getCurrentRsi());
        StockBondBalance balance = new StockBondBalance();
        balance.setRsi90(rsi90.getCurrentRsi());

        // 从建议中提取股债比例
        String[] parts = balanceSuggestion.replace("股", " ").replace("债", "").split(" ");
        if (parts.length == 2) {
            balance.setStockRatio(Integer.parseInt(parts[0]) * 10);
            balance.setBondRatio(Integer.parseInt(parts[1]) * 10);
        }

        balance.setSuggestion(balanceSuggestion);
        balance.setRiskPremium(riskPremium);
        balance.setMa5yDeviation(ma5yDeviation);
        balance.setDataTime(new Date());
        balance.setCreateTime(new Date());

        transactionTemplate.executeWithoutResult(status -> {
            stockBondBalanceMapper.insert(balance);
            stockBondBalanceMapper.deleteOldData(1);
        });

        logger.info("Market overview data refreshed successfully");
        return true;
    }
    
    /**
     * 计算股债平衡建议
     * @param rsi90 90日RSI值
     * @return 股债配置建议
     */
    private String calculateStockBondBalance(BigDecimal rsi90) {
        double rsiValue = rsi90.doubleValue();
        
        if (rsiValue < 30) {
            return "10股0债";
        } else if (rsiValue >= 30 && rsiValue < 35) {
            return "9股1债";
        } else if (rsiValue >= 35 && rsiValue < 43) {
            return "8股2债";
        } else if (rsiValue >= 43 && rsiValue < 47.5) {
            return "7股3债";
        } else if (rsiValue >= 47.5 && rsiValue < 52) {
            return "6股4债";
        } else if (rsiValue >= 52 && rsiValue < 56.5) {
            return "5股5债";
        } else if (rsiValue >= 56.5 && rsiValue < 61) {
            return "4股6债";
        } else if (rsiValue >= 61 && rsiValue < 65.5) {
            return "3股7债";
        } else {
            return "2股8债";
        }
    }
    
    /**
     * 刷新沪深300风险溢价（从第三方API获取）
     * @return 风险溢价
     */
    public String refreshStock300RiskPremium() {
        String url = "https://api.jiucaishuo.com/gz/gz/fed";
        Map<String, Object> payload = new HashMap<>();
        payload.put("gu_code", "000300.SH");
        payload.put("year", 5);
        payload.put("type", "h5");

        JsonObject jsonObject = externalApiClient.postJsonElement(url, payload).getAsJsonObject();
        if (!"success".equals(jsonObject.get("message").getAsString())) {
            throw new ExternalApiException("沪深300风险溢价接口返回失败: " + jsonObject);
        }

        double percent = jsonObject.getAsJsonObject("data")
                .getAsJsonObject("new")
                .get("percent").getAsDouble();

        return String.format("%.2f%%", percent);
    }
    
    /**
     * 刷新5年均线偏离度（从第三方API计算）
     * @return 5年均线偏离度
     */
    public String refreshMa5yDeviation() {
        try {

            // 延迟避免请求过快
            Thread.sleep(5000);

            String url = "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol=sz399317&scale=240&ma=no&datalen=1800";
            JsonArray jsonArray = externalApiClient.getJson(url).getAsJsonArray();
            if (jsonArray.size() == 0) {
                throw new DataUnavailableException("5年均线偏离度接口返回空数据");
            }
            
            // 提取收盘价
            BigDecimal[] prices = new BigDecimal[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject obj = jsonArray.get(i).getAsJsonObject();
                prices[i] = new BigDecimal(obj.get("close").getAsString());
            }
            
            // 计算1250日均线（5年均线）
            int period = 1250;
            if (prices.length < period) {
                throw new DataUnavailableException("5年均线偏离度数据不足，当前数据量: " + prices.length);
            }
            
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = prices.length - period; i < prices.length; i++) {
                sum = sum.add(prices[i]);
            }
            BigDecimal sma = sum.divide(BigDecimal.valueOf(period), 2, BigDecimal.ROUND_HALF_UP);
            
            // 计算偏离度
            BigDecimal currentPrice = prices[prices.length - 1];
            BigDecimal deviation = currentPrice.subtract(sma)
                    .divide(sma, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            return String.format("%.2f%%", deviation);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("计算5年均线偏离度被中断", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new ExternalApiException("计算5年均线偏离度失败", e);
        }
    }
    
    /**
     * 获取养老基金组合RSI
     * 通过读取系统配置中的代表性ETF代码，查询其RSI数据作为基金组合的RSI
     * @return 基金组合RSI
     */
    private FundPortfolioRsiDTO getFundPortfolioRsi() {
        FundPortfolioRsiDTO dto = new FundPortfolioRsiDTO();

        // 从系统配置中读取养老基金组合对应的ETF代码
        String etfCode = systemConfigMapper.selectValueByKey("portfolio.fund.etf_code");
        if (etfCode == null || etfCode.isEmpty()) {
            etfCode = GUO_ZHENG;
        }

        RsiDataDTO rsi14 = rsiAnalysisService.getLatestRsi(etfCode, 14);
        if (rsi14 != null && rsi14.getCurrentRsi() != null) {
            dto.setRsi14(String.format("%.2f", rsi14.getCurrentRsi()));
        }

        RsiDataDTO rsi90 = rsiAnalysisService.getLatestRsi(etfCode, 90);
        if (rsi90 != null && rsi90.getCurrentRsi() != null) {
            dto.setRsi90(String.format("%.2f", rsi90.getCurrentRsi()));
        }

        logger.debug("Fund portfolio RSI - Code: {}, RSI14: {}, RSI90: {}",
                etfCode, dto.getRsi14(), dto.getRsi90());

        return dto;
    }
    
}
