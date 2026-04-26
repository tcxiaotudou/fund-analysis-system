package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.fund.analysis.dto.RsiDataDTO;
import com.fund.analysis.entity.EtfInfo;
import com.fund.analysis.entity.RsiAnalysis;
import com.fund.analysis.exception.BusinessException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.mapper.EtfInfoMapper;
import com.fund.analysis.mapper.RsiAnalysisMapper;
import com.fund.analysis.utils.RsiCalculator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RSI分析服务类
 * 提供RSI技术指标的计算和分析功能
 */
@Service
public class RsiAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(RsiAnalysisService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private RsiAnalysisMapper rsiAnalysisMapper;
    
    @Autowired
    private EtfInfoMapper etfInfoMapper;

    @Autowired
    private ExternalApiClient externalApiClient;

    
    /**
     * 获取指定标的的最新RSI数据（从数据库读取）
     * @param code 标的代码
     * @param period RSI周期
     * @return RSI数据
     */
    public RsiDataDTO getLatestRsi(String code, int period) {
        RsiAnalysis entity = rsiAnalysisMapper.selectLatestByCodeAndPeriod(code, period);
        if (entity == null) {
            logger.warn("No RSI data found for code: {}, period: {}", code, period);
            return null;
        }
        return convertToDTO(entity);
    }
    
    /**
     * 刷新指定标的的RSI数据（从第三方API计算并保存）
     * @param code 标的代码
     * @param period RSI周期
     * @return RSI数据
     */
    @Transactional
    public RsiDataDTO refreshRsi(String code, int period) {
        List<BigDecimal> prices = getPrices(code, period);
        if (prices.isEmpty()) {
            throw new DataUnavailableException("未获取到价格数据: " + code);
        }

        List<BigDecimal> rsiList = RsiCalculator.calculateRSI(prices, period);
        if (rsiList.isEmpty()) {
            throw new DataUnavailableException("RSI数据不足: " + code + ", period=" + period);
        }

        List<BigDecimal> validRsiList = new ArrayList<>();
        for (int i = period; i < rsiList.size(); i++) {
            validRsiList.add(rsiList.get(i));
        }

        if (validRsiList.isEmpty()) {
            throw new DataUnavailableException("有效RSI数据为空: " + code + ", period=" + period);
        }

        RsiDataDTO rsiData = analyzeRsi(code, period, validRsiList);
        saveRsiAnalysis(rsiData);
        rsiAnalysisMapper.deleteOldData(code, period, 1);

        return rsiData;
    }
    
    /**
     * 分析RSI数据
     * @param code 标的代码
     * @param period RSI周期
     * @param rsiList RSI值列表
     * @return RSI数据DTO
     */
    private RsiDataDTO analyzeRsi(String code, int period, List<BigDecimal> rsiList) {
        BigDecimal currentRsi = rsiList.get(rsiList.size() - 1);
        BigDecimal highRsi = BigDecimal.ZERO;
        BigDecimal lowRsi = new BigDecimal("100");
        
        int highIndex = 0;
        int rsi70Days = 0;
        int rsi65Days = 0;
        int rsi60Days = 0;
        int rsi55Days = 0;
        
        // 统计RSI数据
        for (int i = 0; i < rsiList.size(); i++) {
            BigDecimal rsi = rsiList.get(i);
            if (rsi.compareTo(highRsi) > 0) {
                highRsi = rsi;
                highIndex = i;
            }
            if (rsi.compareTo(lowRsi) < 0) {
                lowRsi = rsi;
            }
            if (rsi.compareTo(new BigDecimal("70")) >= 0) rsi70Days++;
            if (rsi.compareTo(new BigDecimal("65")) >= 0) rsi65Days++;
            if (rsi.compareTo(new BigDecimal("60")) >= 0) rsi60Days++;
            if (rsi.compareTo(new BigDecimal("55")) >= 0) rsi55Days++;
        }
        
        // 计算从最高点到当前位置的最低值
        BigDecimal high2NowLow = new BigDecimal("100");
        for (int i = highIndex; i < rsiList.size(); i++) {
            if (rsiList.get(i).compareTo(high2NowLow) < 0) {
                high2NowLow = rsiList.get(i);
            }
        }
        
        // 计算距离最低点的天数
        int daysFromLow = 0;
        for (BigDecimal rsi : rsiList) {
            if (rsi.compareTo(lowRsi) >= 0 && rsi.compareTo(currentRsi) < 0) {
                daysFromLow++;
            }
        }
        
        // 判断买入信号
        boolean isBuySignal = (currentRsi.compareTo(new BigDecimal("30")) <= 0) ||
                (highRsi.compareTo(new BigDecimal("70")) >= 0 && 
                 high2NowLow.compareTo(new BigDecimal("43")) <= 0 && 
                 high2NowLow.compareTo(new BigDecimal("38")) >= 0 && 
                 currentRsi.compareTo(new BigDecimal("43")) <= 0);
        
        // 构建消息
        String message = String.format("数据%d天, 70以上有%d天, 65以上有%d天, 60以上有%d天, 55以上有%d天, 当前与最低点之间有%d天",
                rsiList.size(), rsi70Days, rsi65Days, rsi60Days, rsi55Days, daysFromLow);
        
        // 查询ETF名称
        String name = code;
        EtfInfo etfInfo = etfInfoMapper.selectByCode(code);
        if (etfInfo != null) {
            name = etfInfo.getEtfName();
        }
        
        // 构建DTO
        RsiDataDTO dto = new RsiDataDTO();
        dto.setCode(code);
        dto.setName(name);
        dto.setPeriod(period);
        dto.setCurrentRsi(currentRsi);
        dto.setHighRsi(highRsi);
        dto.setLowRsi(lowRsi);
        
        BigDecimal range = highRsi.subtract(lowRsi);
        BigDecimal twoThirds = highRsi.subtract(range.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
        BigDecimal oneThirds = highRsi.subtract(range.multiply(new BigDecimal("2")).divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
        
        String interval = String.format("(%.2f, %.2f, %.2f, %.2f)", 
                highRsi, twoThirds, oneThirds, lowRsi);
        dto.setInterval(interval);
        dto.setIsBuySignal(isBuySignal);
        dto.setMessage(message);
        dto.setDataTime(dateFormat.format(new Date()));
        
        return dto;
    }
    
    /**
     * 获取价格数据
     * @param code 标的代码
     * @param period RSI周期
     * @return 价格列表
     */
    private List<BigDecimal> getPrices(String code, int period) {
        try {
            // 延迟避免请求过快
            Thread.sleep(5000);
            
            int dataLen = 201;
            if (period > dataLen / 3) {
                dataLen = period * 11;
            }
            
            String url = String.format(
                    "http://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol=%s&scale=120&ma=no&datalen=%d",
                    code, dataLen);

            JsonArray jsonArray = externalApiClient.getJson(url).getAsJsonArray();
            List<BigDecimal> prices = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement element = jsonArray.get(i);
                String dateStr = element.getAsJsonObject().get("day").getAsString();
                String closeStr = element.getAsJsonObject().get("close").getAsString();
                
                // 只取15:00:00的数据（或最后一条）
                if (i != jsonArray.size() - 1 && !dateStr.contains("15:00:00")) {
                    continue;
                }
                
                prices.add(new BigDecimal(closeStr));
            }
            
            return prices;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取RSI价格数据被中断: " + code, e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new ExternalApiException("获取RSI价格数据失败: " + code, e);
        }
    }
    
    /**
     * 获取所有ETF的RSI分析结果（有买入信号的，从数据库读取）
     * @return RSI分析结果列表
     */
    public List<RsiDataDTO> getEtfBuySignals() {
        List<RsiDataDTO> signals = new ArrayList<>();
        List<RsiAnalysis> buySignals = rsiAnalysisMapper.selectBuySignals();
        
        for (RsiAnalysis entity : buySignals) {
            signals.add(convertToDTO(entity));
        }
        
        return signals;
    }
    
    /**
     * 刷新所有ETF的RSI数据（定时任务使用）
     * @return 刷新的记录数
     */
    @Transactional
    public int refreshAllEtfRsi() {
        List<EtfInfo> etfList = etfInfoMapper.selectEnabledEtfs();
        int count = 0;
        
        for (EtfInfo etf : etfList) {
            RsiDataDTO rsi14 = refreshRsi(etf.getEtfCode(), 14);
            count++;
            logger.info("Refreshed RSI14 for {}: {}", etf.getEtfName(), rsi14.getCurrentRsi());

            if (etf.getEtfCode().equals("sz399317") || etf.getEtfCode().equals("sh511090")) {
                RsiDataDTO rsi90 = refreshRsi(etf.getEtfCode(), 90);
                count++;
                logger.info("Refreshed RSI90 for {}: {}", etf.getEtfName(), rsi90.getCurrentRsi());
            }
        }
        
        return count;
    }
    
    /**
     * 保存RSI分析结果到数据库
     * @param rsiData RSI数据DTO
     */
    public void saveRsiAnalysis(RsiDataDTO rsiData) {
        RsiAnalysis entity = new RsiAnalysis();
        entity.setCode(rsiData.getCode());
        entity.setName(rsiData.getName());
        entity.setPeriod(rsiData.getPeriod());
        entity.setCurrentRsi(rsiData.getCurrentRsi());
        entity.setHighRsi(rsiData.getHighRsi());
        entity.setLowRsi(rsiData.getLowRsi());
        entity.setIsBuySignal(rsiData.getIsBuySignal() ? 1 : 0);
        entity.setMessage(rsiData.getMessage());
        entity.setDataTime(new Date());
        entity.setCreateTime(new Date());
        
        rsiAnalysisMapper.insert(entity);
    }
    
    /**
     * 将Entity转换为DTO
     * @param entity RSI分析实体
     * @return RSI数据DTO
     */
    private RsiDataDTO convertToDTO(RsiAnalysis entity) {
        RsiDataDTO dto = new RsiDataDTO();
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setPeriod(entity.getPeriod());
        dto.setCurrentRsi(entity.getCurrentRsi());
        dto.setHighRsi(entity.getHighRsi());
        dto.setLowRsi(entity.getLowRsi());
        dto.setIsBuySignal(entity.getIsBuySignal() == 1);
        dto.setMessage(entity.getMessage());
        
        // 计算区间
        if (entity.getHighRsi() != null && entity.getLowRsi() != null) {
            BigDecimal range = entity.getHighRsi().subtract(entity.getLowRsi());
            BigDecimal twoThirds = entity.getHighRsi().subtract(range.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
            BigDecimal oneThirds = entity.getHighRsi().subtract(range.multiply(new BigDecimal("2")).divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
            
            String interval = String.format("(%.2f, %.2f, %.2f, %.2f)", 
                    entity.getHighRsi(), twoThirds, oneThirds, entity.getLowRsi());
            dto.setInterval(interval);
        }
        
        if (entity.getDataTime() != null) {
            dto.setDataTime(dateFormat.format(entity.getDataTime()));
        }
        
        return dto;
    }
}
