package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.fund.analysis.dto.MaStrategyBacktestDTO;
import com.fund.analysis.dto.MaStrategyDTO;
import com.fund.analysis.entity.EtfInfo;
import com.fund.analysis.entity.MaStrategy;
import com.fund.analysis.exception.BusinessException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.mapper.EtfInfoMapper;
import com.fund.analysis.mapper.MaStrategyMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 移动平均线策略服务类
 * 实现基于10日均线和30日均线的双均线交易策略
 */
@Service
public class MaStrategyService {
    
    private static final Logger logger = LoggerFactory.getLogger(MaStrategyService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final int MA_BACKTEST_WARMUP_DAYS = 90;
    private static final int MAX_KLINE_DATA_LEN = 1950;
    
    @Autowired
    private MaStrategyMapper maStrategyMapper;
    
    @Autowired
    private EtfInfoMapper etfInfoMapper;

    @Autowired
    private ExternalApiClient externalApiClient;

    /**
     * 短事务执行器
     */
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    /**
     * 获取指定ETF的最新MA策略数据（从数据库读取）
     * @param code ETF代码
     * @return MA策略数据
     */
    public MaStrategyDTO getLatestMaStrategy(String code) {
        MaStrategy entity = maStrategyMapper.selectLatestByCode(code);
        if (entity == null) {
            logger.warn("No MA strategy data found for code: {}", code);
            return null;
        }
        return convertToDTO(entity);
    }
    
    /**
     * 刷新移动平均线策略（从第三方API计算并保存）
     * @param code ETF代码
     * @return MA策略数据
     */
    public MaStrategyDTO refreshMaStrategy(String code) {
        // 获取足够的历史数据用于计算均线和判断金叉死叉
        List<BigDecimal> prices = getPricesForMA(code, 240, 60);
        if (prices.size() < 40) {
            throw new DataUnavailableException("MA策略价格数据不足: " + code + ", 当前数据量=" + prices.size());
        }

        BigDecimal currentPrice = prices.get(prices.size() - 1);
        BigDecimal ma10Current = calculateMAFromList(prices, 10, prices.size() - 1);
        BigDecimal ma10Previous = calculateMAFromList(prices, 10, prices.size() - 2);
        BigDecimal ma30Current = calculateMAFromList(prices, 30, prices.size() - 1);
        BigDecimal ma30Previous = calculateMAFromList(prices, 30, prices.size() - 2);

        boolean isBuySignal = false;
        boolean isSellSignal = false;
        StringBuilder signalDesc = new StringBuilder();

        boolean isGoldenCross = ma10Previous != null && ma30Previous != null &&
                ma10Previous.compareTo(ma30Previous) <= 0 &&
                ma10Current.compareTo(ma30Current) > 0;

        boolean isDeadCross = ma10Previous != null && ma30Previous != null &&
                ma10Previous.compareTo(ma30Previous) >= 0 &&
                ma10Current.compareTo(ma30Current) < 0;

        if (isGoldenCross) {
            isBuySignal = true;
            signalDesc.append("10日均线上穿30日均线(金叉)");
        }

        if (isDeadCross) {
            isSellSignal = true;
            signalDesc.append("10日均线下穿30日均线(死叉)");
        }

        if (!isBuySignal && !isSellSignal) {
            signalDesc.append("暂无明确信号");
        }

        String name = code;
        EtfInfo etfInfo = etfInfoMapper.selectByCode(code);
        if (etfInfo != null) {
            name = etfInfo.getEtfName();
        }

        MaStrategyDTO dto = new MaStrategyDTO();
        dto.setEtfCode(code);
        dto.setEtfName(name);
        dto.setMa10(ma10Current);
        dto.setMa30(ma30Current);
        dto.setCurrentDaily(currentPrice);
        dto.setIsBuySignal(isBuySignal);
        dto.setIsSellSignal(isSellSignal);
        dto.setSignalDescription(signalDesc.toString());
        dto.setDataTime(dateFormat.format(new Date()));

        transactionTemplate.executeWithoutResult(status -> saveMaStrategy(dto));
        return dto;
    }
    
    /**
     * 从价格列表计算移动平均线
     * @param prices 价格列表
     * @param period 均线周期
     * @param endIndex 结束位置（包含）
     * @return 均线值
     */
    private BigDecimal calculateMAFromList(List<BigDecimal> prices, int period, int endIndex) {
        if (prices == null || endIndex < period - 1) {
            return null;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum = sum.add(prices.get(i));
        }
        
        return sum.divide(BigDecimal.valueOf(period), 3, RoundingMode.HALF_UP);
    }
    
    /**
     * 获取用于移动平均线计算的价格数据
     * @param code 标的代码
     * @param scale K线类型
     * @param dataLen 数据长度
     * @return 价格列表
     */
    private List<BigDecimal> getPricesForMA(String code, int scale, int dataLen) {
        try {
            // 延迟避免请求过快
            Thread.sleep(5000);
            
            String url = String.format(
                    "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol=%s&scale=%d&ma=no&datalen=%d",
                    code, scale, dataLen);

            JsonArray jsonArray = externalApiClient.getJson(url).getAsJsonArray();
            List<BigDecimal> prices = new ArrayList<>();
            
            for (JsonElement element : jsonArray) {
                String closeStr = element.getAsJsonObject().get("close").getAsString();
                prices.add(new BigDecimal(closeStr));
            }
            
            return prices;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取MA价格数据被中断: " + code, e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new ExternalApiException("获取MA价格数据失败: " + code, e);
        }
    }
    
    /**
     * 获取所有ETF的MA策略买入信号（从数据库读取）
     * @return MA策略结果列表
     */
    public List<MaStrategyDTO> getMaBuySignals() {
        List<MaStrategyDTO> signals = new ArrayList<>();
        List<MaStrategy> buySignals = maStrategyMapper.selectBuySignals();
        
        for (MaStrategy entity : buySignals) {
            signals.add(convertToDTO(entity));
        }
        
        return signals;
    }
    
    /**
     * 获取所有ETF的MA策略卖出信号（从数据库读取）
     * @return MA策略结果列表
     */
    public List<MaStrategyDTO> getMaSellSignals() {
        List<MaStrategyDTO> signals = new ArrayList<>();
        List<MaStrategy> sellSignals = maStrategyMapper.selectSellSignals();
        
        for (MaStrategy entity : sellSignals) {
            signals.add(convertToDTO(entity));
        }
        
        return signals;
    }

    /**
     * 获取所有ETF最新MA策略记录
     * @return MA策略结果列表
     */
    public List<MaStrategyDTO> getLatestAllMaStrategies() {
        List<MaStrategyDTO> strategies = new ArrayList<>();
        List<MaStrategy> latestStrategies = maStrategyMapper.selectLatestAll();

        for (MaStrategy entity : latestStrategies) {
            strategies.add(convertToDTO(entity));
        }

        return strategies;
    }
    
    /**
     * 刷新所有ETF的MA策略数据（定时任务使用）
     * @return 刷新的记录数
     */
    public int refreshAllEtfMa() {
        List<EtfInfo> etfList = etfInfoMapper.selectEnabledEtfs();
        int count = 0;
        
        for (EtfInfo etf : etfList) {
            MaStrategyDTO maData = refreshMaStrategy(etf.getEtfCode());
            count++;
            logger.info("Refreshed MA strategy for {}: buy_signal={}",
                    etf.getEtfName(), maData.getIsBuySignal());
        }
        
        return count;
    }
    
    /**
     * 保存MA策略结果到数据库
     * @param maData MA策略数据DTO
     */
    public void saveMaStrategy(MaStrategyDTO maData) {
        MaStrategy entity = new MaStrategy();
        entity.setEtfCode(maData.getEtfCode());
        entity.setEtfName(maData.getEtfName());
        entity.setMa10(maData.getMa10());
        entity.setMa30(maData.getMa30());
        entity.setCurrentDaily(maData.getCurrentDaily());
        entity.setIsBuySignal(maData.getIsBuySignal() ? 1 : 0);
        entity.setIsSellSignal(maData.getIsSellSignal() ? 1 : 0);
        entity.setSignalDescription(maData.getSignalDescription());
        entity.setDataTime(new Date());
        entity.setCreateTime(new Date());
        
        maStrategyMapper.insert(entity);
        
        // 清理旧数据，每个ETF只保留最新的1条记录
        maStrategyMapper.deleteOldData(maData.getEtfCode(), 1);
    }
    
    /**
     * 将Entity转换为DTO
     * @param entity MA策略实体
     * @return MA策略数据DTO
     */
    private MaStrategyDTO convertToDTO(MaStrategy entity) {
        MaStrategyDTO dto = new MaStrategyDTO();
        dto.setEtfCode(entity.getEtfCode());
        dto.setEtfName(entity.getEtfName());
        dto.setMa10(entity.getMa10());
        dto.setMa30(entity.getMa30());
        dto.setCurrentDaily(entity.getCurrentDaily());
        dto.setIsBuySignal(entity.getIsBuySignal() == 1);
        dto.setIsSellSignal(entity.getIsSellSignal() == 1);
        dto.setSignalDescription(entity.getSignalDescription());
        
        if (entity.getDataTime() != null) {
            dto.setDataTime(dateFormat.format(entity.getDataTime()));
        }
        
        return dto;
    }
    
    /**
     * 执行双均线策略回测
     * @param etfCode ETF代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param initialCapital 初始资金
     * @return 回测结果
     */
    public MaStrategyBacktestDTO runBacktest(String etfCode, Date startDate, Date endDate, BigDecimal initialCapital) {
        logger.info("开始执行MA策略回测: ETF={}, 开始日期={}, 结束日期={}, 初始资金={}", 
                etfCode, dateOnlyFormat.format(startDate), dateOnlyFormat.format(endDate), initialCapital);
        
        MaStrategyBacktestDTO result = new MaStrategyBacktestDTO();
        result.setEtfCode(etfCode);
        result.setStartDate(dateOnlyFormat.format(startDate));
        result.setEndDate(dateOnlyFormat.format(endDate));
        result.setInitialCapital(initialCapital);
        
        // 查询ETF名称
        String etfName = etfCode;
        EtfInfo etfInfo = etfInfoMapper.selectByCode(etfCode);
        if (etfInfo != null) {
            etfName = etfInfo.getEtfName();
        }
        result.setEtfName(etfName);
        
        // 获取历史价格数据（需要多取一些数据用于计算均线）
        List<PriceData> allPrices = getHistoricalPrices(etfCode, startDate, endDate, MA_BACKTEST_WARMUP_DAYS);
        if (allPrices.size() < 40) {
            throw new DataUnavailableException("MA回测历史数据不足: " + etfCode + ", 数据量=" + allPrices.size());
        }

        int backtestStartIndex = -1;
        int backtestDataCount = 0;
        for (int i = 0; i < allPrices.size(); i++) {
            Date date = allPrices.get(i).getDate();
            if (!date.before(startDate) && !date.after(endDate)) {
                if (backtestStartIndex < 0) {
                    backtestStartIndex = i;
                }
                backtestDataCount++;
            }
        }

        if (backtestStartIndex < 0 || backtestDataCount == 0) {
            throw new DataUnavailableException("MA回测日期范围内没有可用数据: " + etfCode);
        }

        if (backtestDataCount < 30) {
            throw new DataUnavailableException("MA回测日期范围内数据不足: " + etfCode + ", 数据量=" + backtestDataCount);
        }
        
        // 回测逻辑
        List<MaStrategyBacktestDTO.BacktestTransaction> transactions = new ArrayList<>();
        List<MaStrategyBacktestDTO.DailyValue> dailyValues = new ArrayList<>();
        
        BigDecimal currentCapital = initialCapital;
        Long holdingQuantity = 0L; // 持有数量
        
        int buyCount = 0;
        int sellCount = 0;
        
        // 遍历每一天进行回测
        for (int i = Math.max(backtestStartIndex, 30); i < allPrices.size(); i++) { // 从第30天开始，确保有足够数据计算30日均线
            PriceData currentPriceData = allPrices.get(i);
            Date currentDate = currentPriceData.getDate();
            if (currentDate.after(endDate)) {
                break;
            }
            BigDecimal currentPrice = currentPriceData.getPrice();
            
            // 获取用于计算均线的价格列表（包含当前日期及之前的数据）
            List<BigDecimal> priceList = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                priceList.add(allPrices.get(j).getPrice());
            }
            
            // 计算10日和30日均线（当前和前一天）
            BigDecimal ma10Current = calculateMAFromList(priceList, 10, priceList.size() - 1);
            BigDecimal ma10Previous = priceList.size() > 10 ?
                    calculateMAFromList(priceList, 10, priceList.size() - 2) : null;
            BigDecimal ma30Current = calculateMAFromList(priceList, 30, priceList.size() - 1);
            BigDecimal ma30Previous = priceList.size() > 30 ? 
                    calculateMAFromList(priceList, 30, priceList.size() - 2) : null;
            
            if (ma10Current == null || ma30Current == null || ma10Previous == null || ma30Previous == null) {
                // 数据不足，跳过
                continue;
            }
            
            // 判断买卖信号（仅基于金叉和死叉）
            boolean isBuySignal = false;
            boolean isSellSignal = false;
            StringBuilder signalDesc = new StringBuilder();
            
            // 判断10日均线上穿30日均线（金叉）
            // 前一天：MA10 <= MA30，当前天：MA10 > MA30
            boolean isGoldenCross = ma10Previous.compareTo(ma30Previous) <= 0 && 
                    ma10Current.compareTo(ma30Current) > 0;
            
            // 判断10日均线下穿30日均线（死叉）
            // 前一天：MA10 >= MA30，当前天：MA10 < MA30
            boolean isDeadCross = ma10Previous.compareTo(ma30Previous) >= 0 && 
                    ma10Current.compareTo(ma30Current) < 0;
            
            // 买入信号：10日均线上穿30日均线（金叉）
            if (isGoldenCross) {
                isBuySignal = true;
                signalDesc.append("10日均线上穿30日均线(金叉)");
            }
            
            // 卖出信号：10日均线下穿30日均线（死叉）
            if (isDeadCross) {
                isSellSignal = true;
                signalDesc.append("10日均线下穿30日均线(死叉)");
            }
            
            // 执行交易逻辑
            if (isBuySignal && holdingQuantity == 0) {
                // 买入：全部资金买入
                long quantity = currentCapital.divide(currentPrice, 0, RoundingMode.DOWN).longValue();
                if (quantity > 0) {
                    BigDecimal amount = currentPrice.multiply(BigDecimal.valueOf(quantity));
                    holdingQuantity = quantity;
                    currentCapital = currentCapital.subtract(amount);
                    
                    MaStrategyBacktestDTO.BacktestTransaction transaction = new MaStrategyBacktestDTO.BacktestTransaction();
                    transaction.setDate(dateOnlyFormat.format(currentDate));
                    transaction.setType("BUY");
                    transaction.setPrice(currentPrice);
                    transaction.setQuantity(quantity);
                    transaction.setAmount(amount);
                    transaction.setTotalValue(currentCapital.add(amount));
                    transaction.setSignalDescription(signalDesc.toString());
                    transactions.add(transaction);
                    
                    buyCount++;
                    logger.debug("买入: 日期={}, 价格={}, 数量={}, 金额={}", 
                            dateOnlyFormat.format(currentDate), currentPrice, quantity, amount);
                }
            } else if (isSellSignal && holdingQuantity > 0) {
                // 卖出：全部卖出
                BigDecimal amount = currentPrice.multiply(BigDecimal.valueOf(holdingQuantity));
                currentCapital = currentCapital.add(amount);
                
                MaStrategyBacktestDTO.BacktestTransaction transaction = new MaStrategyBacktestDTO.BacktestTransaction();
                transaction.setDate(dateOnlyFormat.format(currentDate));
                transaction.setType("SELL");
                transaction.setPrice(currentPrice);
                transaction.setQuantity(holdingQuantity);
                transaction.setAmount(amount);
                transaction.setTotalValue(currentCapital);
                transaction.setSignalDescription(signalDesc.toString());
                transactions.add(transaction);
                
                sellCount++;
                holdingQuantity = 0L;
                
                logger.debug("卖出: 日期={}, 价格={}, 数量={}, 金额={}", 
                        dateOnlyFormat.format(currentDate), currentPrice, transaction.getQuantity(), amount);
            }
            
            // 计算当日资产总值
            BigDecimal totalValue = currentCapital;
            if (holdingQuantity > 0) {
                totalValue = totalValue.add(currentPrice.multiply(BigDecimal.valueOf(holdingQuantity)));
            }
            
            // 记录每日净值
            MaStrategyBacktestDTO.DailyValue dailyValue = new MaStrategyBacktestDTO.DailyValue();
            dailyValue.setDate(dateOnlyFormat.format(currentDate));
            dailyValue.setPrice(currentPrice);
            dailyValue.setTotalValue(totalValue);
            BigDecimal returnRate = totalValue.subtract(initialCapital)
                    .divide(initialCapital, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dailyValue.setReturnRate(returnRate);
            dailyValues.add(dailyValue);
        }
        
        // 计算最终资产（如果最后还有持仓，按最后一天价格计算）
        BigDecimal finalCapital = currentCapital;
        if (holdingQuantity > 0 && !allPrices.isEmpty()) {
            BigDecimal lastPrice = allPrices.get(allPrices.size() - 1).getPrice();
            finalCapital = finalCapital.add(lastPrice.multiply(BigDecimal.valueOf(holdingQuantity)));
        }
        
        result.setFinalCapital(finalCapital);
        result.setTransactions(transactions);
        result.setDailyValues(dailyValues);
        result.setBuyCount(buyCount);
        result.setSellCount(sellCount);
        result.setTradeCount(buyCount + sellCount);
        
        // 计算总收益率
        BigDecimal totalReturnRate = finalCapital.subtract(initialCapital)
                .divide(initialCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        result.setTotalReturnRate(totalReturnRate);
        
        // 计算年化收益率
        long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
        if (days > 0) {
            double years = days / 365.0;
            double returnRate = totalReturnRate.doubleValue() / 100.0;
            double annualizedReturn = (Math.pow(1 + returnRate, 1.0 / years) - 1) * 100;
            result.setAnnualizedReturnRate(BigDecimal.valueOf(annualizedReturn)
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            result.setAnnualizedReturnRate(BigDecimal.ZERO);
        }
        
        logger.info("回测完成: ETF={}, 总收益率={}%, 年化收益率={}%, 交易次数={}", 
                etfCode, totalReturnRate, result.getAnnualizedReturnRate(), result.getTradeCount());
        
        return result;
    }
    
    /**
     * 价格数据内部类
     */
    private static class PriceData {
        private Date date;
        private BigDecimal price;
        
        public PriceData(Date date, BigDecimal price) {
            this.date = date;
            this.price = price;
        }
        
        public Date getDate() {
            return date;
        }
        
        public BigDecimal getPrice() {
            return price;
        }
    }
    
    /**
     * 获取历史价格数据（包含日期）
     * @param code ETF代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param extraDays 额外获取的天数（用于计算均线）
     * @return 价格数据列表
     */
    private List<PriceData> getHistoricalPrices(String code, Date startDate, Date endDate, int extraDays) {
        try {
            Date extendedStartDate = new Date(startDate.getTime() - extraDays * 24L * 60 * 60 * 1000);
            int dataLen = calculateKlineDataLen(extendedStartDate, endDate, MAX_KLINE_DATA_LEN);
            
            // 延迟避免请求过快
            Thread.sleep(2000);
            
            String url = String.format(
                    "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol=%s&scale=240&ma=no&datalen=%d",
                    code, dataLen);
            
            logger.debug("请求ETF历史数据: {}", url);

            JsonElement jsonElement = externalApiClient.getJson(url);
            if (!jsonElement.isJsonArray()) {
                throw new ExternalApiException("获取" + code + "历史数据失败: 响应不是JSON数组");
            }
            
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            if (jsonArray == null || jsonArray.size() == 0) {
                throw new DataUnavailableException("获取" + code + "历史数据失败: 数据为空");
            }
            
            List<PriceData> prices = new ArrayList<>();
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");
            
            for (JsonElement element : jsonArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                
                JsonObject obj = element.getAsJsonObject();
                
                if (!obj.has("day") || !obj.has("close")) {
                    continue;
                }
                
                String dateStr = obj.get("day").getAsString();
                String closeStr = obj.get("close").getAsString();
                
                try {
                    Date date;
                    if (dateStr != null && dateStr.length() >= 10) {
                        date = dateParser.parse(dateStr.substring(0, 10));
                    } else {
                        continue;
                    }
                    
                    if (!date.before(extendedStartDate) && !date.after(endDate)) {
                        BigDecimal price = new BigDecimal(closeStr);
                        prices.add(new PriceData(date, price));
                    }
                } catch (ParseException e) {
                    logger.debug("解析日期失败: {}, 跳过", dateStr);
                    continue;
                } catch (Exception e) {
                    logger.debug("解析价格失败: {}, 跳过", closeStr);
                    continue;
                }
            }
            
            // 按日期排序
            prices.sort(Comparator.comparing(PriceData::getDate));
            
            logger.info("获取{}历史数据成功: {}条记录", code, prices.size());
            return prices;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取" + code + "历史数据被中断", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new ExternalApiException("获取" + code + "历史数据失败", e);
        }
    }

    /**
     * 新浪K线接口按“最近N条”返回数据，历史回测需要把结束日至今天之间的数据量也算进去。
     */
    private int calculateKlineDataLen(Date extendedStartDate, Date endDate, int maxDataLen) {
        Date latestPossibleDate = new Date();
        if (endDate.after(latestPossibleDate)) {
            latestPossibleDate = endDate;
        }

        long days = (latestPossibleDate.getTime() - extendedStartDate.getTime()) / (1000 * 60 * 60 * 24) + 10;
        return (int) Math.min(Math.max(days, 1), maxDataLen);
    }
}
