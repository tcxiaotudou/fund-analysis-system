package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.fund.analysis.dto.MomentumPerformanceDTO;
import com.fund.analysis.dto.MomentumTransactionDTO;
import com.fund.analysis.entity.MomentumStrategyPerformance;
import com.fund.analysis.entity.MomentumStrategyTransaction;
import com.fund.analysis.exception.BusinessException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.mapper.MomentumStrategyPerformanceMapper;
import com.fund.analysis.mapper.MomentumStrategyTransactionMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 21日动量策略服务类
 * 提供动量策略交易记录的查询和管理功能
 */
@Service
public class MomentumStrategyService {
    
    private static final Logger logger = LoggerFactory.getLogger(MomentumStrategyService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final int MOMENTUM_PERIOD = 21;
    private static final int MOMENTUM_WARMUP_DAYS = 90;
    private static final int MAX_KLINE_DATA_LEN = 1950;
    private static final BigDecimal DEFAULT_INITIAL_CAPITAL = BigDecimal.valueOf(100000);
    
    @Autowired
    private MomentumStrategyTransactionMapper transactionMapper;

    @Autowired
    private MomentumStrategyPerformanceMapper performanceMapper;

    @Autowired
    private ExternalApiClient externalApiClient;

    private final MomentumPerformanceCalculator performanceCalculator = new MomentumPerformanceCalculator();
    
    /**
     * 获取所有交易记录
     * @return 交易记录列表
     */
    public List<MomentumTransactionDTO> getAllTransactions() {
        List<MomentumStrategyTransaction> transactions = transactionMapper.selectAllOrderByDateDesc();
        return convertToDTOList(transactions);
    }
    
    /**
     * 获取指定日期范围内的交易记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 交易记录列表
     */
    public List<MomentumTransactionDTO> getTransactionsByDateRange(Date startDate, Date endDate) {
        List<MomentumStrategyTransaction> transactions = transactionMapper.selectByDateRange(startDate, endDate);
        return convertToDTOList(transactions);
    }
    
    /**
     * 获取指定ETF的交易记录
     * @param etfCode ETF代码
     * @return 交易记录列表
     */
    public List<MomentumTransactionDTO> getTransactionsByEtfCode(String etfCode) {
        List<MomentumStrategyTransaction> transactions = transactionMapper.selectByEtfCode(etfCode);
        return convertToDTOList(transactions);
    }
    
    /**
     * 批量保存交易记录
     * @param transactions 交易记录列表
     */
    @Transactional
    public void saveTransactions(List<MomentumStrategyTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            logger.warn("No transactions to save");
            return;
        }
        
        for (MomentumStrategyTransaction transaction : transactions) {
            validateTransactionForSave(transaction);
            transactionMapper.insert(transaction);
        }
        
        logger.info("Saved {} momentum strategy transactions", transactions.size());
    }
    
    /**
     * 保存单条交易记录
     * @param transaction 交易记录
     */
    @Transactional
    public void saveTransaction(MomentumStrategyTransaction transaction) {
        validateTransactionForSave(transaction);
        transactionMapper.insert(transaction);
        logger.info("Saved momentum strategy transaction: {} {} {} @ {}",
                transaction.getTransactionDate(), transaction.getTransactionType(),
                transaction.getEtfName(), transaction.getPrice());
    }

    /**
     * 批量保存每日绩效
     * @param performances 每日绩效列表
     */
    @Transactional
    public void savePerformanceRecords(List<MomentumStrategyPerformance> performances) {
        if (performances == null || performances.isEmpty()) {
            logger.warn("No momentum strategy performance records to save");
            return;
        }

        for (MomentumStrategyPerformance performance : performances) {
            validatePerformanceForSave(performance);
            performanceMapper.insert(performance);
        }

        logger.info("Saved {} momentum strategy performance records", performances.size());
    }
    
    /**
     * 删除指定日期之前的数据
     * @param beforeDate 日期
     */
    @Transactional
    public void deleteBeforeDate(Date beforeDate) {
        transactionMapper.deleteBeforeDate(beforeDate);
        logger.info("Deleted momentum strategy transactions before {}", dateFormat.format(beforeDate));
    }

    /**
     * 删除指定日期范围内的数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 删除记录数
     */
    @Transactional
    public int deleteByDateRange(Date startDate, Date endDate) {
        int deleted = transactionMapper.deleteByDateRange(startDate, endDate);
        logger.info("Deleted {} momentum strategy transactions from {} to {}",
                deleted, dateFormat.format(startDate), dateFormat.format(endDate));
        return deleted;
    }

    /**
     * 删除指定日期范围内的每日绩效
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 删除记录数
     */
    @Transactional
    public int deletePerformanceByDateRange(Date startDate, Date endDate) {
        int deleted = performanceMapper.deleteByDateRange(startDate, endDate);
        logger.info("Deleted {} momentum strategy performance records from {} to {}",
                deleted, dateFormat.format(startDate), dateFormat.format(endDate));
        return deleted;
    }
    
    /**
     * 执行21日动量策略回测
     * @param startDate 回测开始日期
     * @param endDate 回测结束日期
     * @param initialCapital 初始资金
     * @return 交易记录列表
     */
    public List<MomentumStrategyTransaction> runBacktest(Date startDate, Date endDate, BigDecimal initialCapital) {
        return runBacktestWithPerformance(startDate, endDate, initialCapital).getTransactions();
    }

    public BacktestResult runBacktestWithPerformance(Date startDate, Date endDate, BigDecimal initialCapital) {
        return runBacktest(startDate, endDate, initialCapital, null);
    }

    /**
     * 执行21日动量策略回测
     * @param startDate 回测开始日期
     * @param endDate 回测结束日期
     * @param initialCapital 初始资金
     * @param initialState 起始持仓状态，null表示空仓开始
     * @return 交易记录列表
     */
    private BacktestResult runBacktest(Date startDate, Date endDate, BigDecimal initialCapital,
                                       BacktestState initialState) {
        logger.info("开始执行回测: {} 到 {}, 初始资金: {}", 
                dateFormat.format(startDate), dateFormat.format(endDate), initialCapital);
        
        // 定义可投资的ETF（需要加上市场前缀：sh-上海，sz-深圳）
        Map<String, String> etfList = buildMomentumEtfList();
        
        List<MomentumStrategyTransaction> transactions = new ArrayList<>();
        List<MomentumStrategyPerformance> performances = new ArrayList<>();
        String currentHolding = null; // 当前持有的ETF代码
        long currentQuantity = 0;
        BigDecimal availableCapital = initialCapital;
        BigDecimal recordInitialCapital = initialCapital;

        if (initialState != null) {
            currentHolding = initialState.getCurrentHolding();
            currentQuantity = initialState.getCurrentQuantity();
            availableCapital = initialState.getAvailableCapital();
            recordInitialCapital = initialState.getInitialCapital();
            logger.info("使用历史状态继续回测: 持仓={}, 数量={}, 可用资金={}, 原始初始资金={}",
                    currentHolding, currentQuantity, availableCapital, recordInitialCapital);
        }
        
        // 获取所有ETF的历史数据
        Map<String, List<PriceData>> etfHistories = new HashMap<>();
        for (Map.Entry<String, String> entry : etfList.entrySet()) {
            String code = entry.getKey();
            String name = entry.getValue();
            List<PriceData> history = getHistoricalPrices(code, startDate, endDate, MOMENTUM_WARMUP_DAYS);
            if (history != null && !history.isEmpty()) {
                etfHistories.put(code, history);
                logger.info("获取{}({})历史数据: {}条", name, code, history.size());
            }
        }
        
        if (etfHistories.isEmpty()) {
            logger.warn("没有获取到任何ETF的历史数据");
            return new BacktestResult(transactions, performances);
        }
        
        // 找到所有交易日期（所有ETF的交易日期的并集）
        List<Date> tradingDates = getTradingDates(etfHistories, startDate, endDate);
        logger.info("回测期间共有{}个交易日", tradingDates.size());
        
        // 按日期遍历，每天检查是否需要调仓
        for (Date date : tradingDates) {
            // 计算当天各ETF的21个交易日动量
            Map<String, BigDecimal> momentumMap = new HashMap<>();
            Map<String, BigDecimal> priceMap = new HashMap<>();
            
            for (Map.Entry<String, List<PriceData>> entry : etfHistories.entrySet()) {
                String code = entry.getKey();
                List<PriceData> history = entry.getValue();
                
                // 找到当天或之前最近的价格数据
                int currentIndex = findLatestPriceIndex(history, date);
                if (currentIndex < 0) {
                    continue;
                }

                BigDecimal currentPrice = history.get(currentIndex).getPrice();
                priceMap.put(code, currentPrice);
                
                if (currentIndex < MOMENTUM_PERIOD) {
                    continue;
                }

                BigDecimal price21DaysAgo = history.get(currentIndex - MOMENTUM_PERIOD).getPrice();
                if (currentPrice != null && price21DaysAgo != null && 
                    price21DaysAgo.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal momentum = currentPrice.divide(price21DaysAgo, 4, RoundingMode.HALF_UP)
                            .subtract(BigDecimal.ONE);
                    momentumMap.put(code, momentum);
                }
            }
            
            // 如果无法计算动量，跳过这一天
            if (momentumMap.isEmpty()) {
                continue;
            }
            
            // 找到动量最强的ETF
            String bestETF = null;
            BigDecimal bestMomentum = null;
            for (Map.Entry<String, BigDecimal> entry : momentumMap.entrySet()) {
                if (bestMomentum == null || entry.getValue().compareTo(bestMomentum) > 0) {
                    bestMomentum = entry.getValue();
                    bestETF = entry.getKey();
                }
            }
            
            // 如果动量最强的ETF与当前持有的不同，需要调仓
            if (bestETF != null && bestMomentum != null && !bestETF.equals(currentHolding)) {
                // 卖出当前持有的ETF
                if (currentHolding != null && currentQuantity > 0) {
                    BigDecimal sellPrice = priceMap.get(currentHolding);
                    if (sellPrice == null || sellPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new DataUnavailableException("动量策略无法获取当前持仓卖出价格: "
                                + currentHolding + ", 日期=" + dateFormat.format(date));
                    }

                    String etfName = etfList.get(currentHolding);

                    MomentumStrategyTransaction sellTxn = new MomentumStrategyTransaction();
                    sellTxn.setTransactionDate(date);
                    sellTxn.setEtfCode(currentHolding);
                    sellTxn.setEtfName(etfName);
                    sellTxn.setTransactionType("sell");
                    sellTxn.setQuantity(-currentQuantity);
                    sellTxn.setPrice(sellPrice);
                    sellTxn.setInitialCapital(recordInitialCapital);
                    transactions.add(sellTxn);

                    BigDecimal proceeds = BigDecimal.valueOf(currentQuantity).multiply(sellPrice);
                    availableCapital = availableCapital.add(proceeds);
                    logger.info("{}: 卖出 {} {}股 @ {}, 获得资金 {}, 总资金 {}",
                            dateFormat.format(date), etfName, currentQuantity, sellPrice, proceeds, availableCapital);
                    currentHolding = null;
                    currentQuantity = 0;
                }
                
                // 买入动量最强的ETF
                BigDecimal buyPrice = priceMap.get(bestETF);
                if (buyPrice != null && buyPrice.compareTo(BigDecimal.ZERO) > 0 && 
                    availableCapital.compareTo(BigDecimal.ZERO) > 0) {
                    String etfName = etfList.get(bestETF);
                    
                    // 计算可买入数量
                    long newQuantity = availableCapital.divide(buyPrice, 0, RoundingMode.DOWN).longValue();
                    
                    if (newQuantity > 0) {
                        MomentumStrategyTransaction buyTxn = new MomentumStrategyTransaction();
                        buyTxn.setTransactionDate(date);
                        buyTxn.setEtfCode(bestETF);
                        buyTxn.setEtfName(etfName);
                        buyTxn.setTransactionType("buy");
                        buyTxn.setQuantity(newQuantity);
                        buyTxn.setPrice(buyPrice);
                        buyTxn.setMomentum21(bestMomentum);
                        buyTxn.setInitialCapital(recordInitialCapital);
                        transactions.add(buyTxn);
                        
                        currentHolding = bestETF;
                        currentQuantity = newQuantity;
                        availableCapital = availableCapital.subtract(
                                BigDecimal.valueOf(newQuantity).multiply(buyPrice));
                        
                        logger.info("{}: 买入 {} {}股 @ {}, 21日动量={}%, 剩余资金 {}",
                                dateFormat.format(date), etfName, newQuantity, buyPrice, 
                                bestMomentum.multiply(BigDecimal.valueOf(100)), availableCapital);
                    }
                }
            }

            MomentumStrategyPerformance performance = performanceCalculator.buildDailyPerformance(
                    date,
                    recordInitialCapital,
                    availableCapital,
                    currentHolding,
                    currentQuantity,
                    priceMap,
                    etfList
            );
            performances.add(performance);
        }
        
        logger.info("回测完成，共生成{}条交易记录，{}条每日绩效", transactions.size(), performances.size());
        return new BacktestResult(transactions, performances);
    }

    private Map<String, String> buildMomentumEtfList() {
        Map<String, String> etfList = new HashMap<>();
        etfList.put("sh518880", "黄金ETF");  // 上海市场
        etfList.put("sh513100", "纳指ETF");  // 上海市场
        etfList.put("sz159915", "创业板ETF"); // 深圳市场
        return etfList;
    }

    private int findLatestPriceIndex(List<PriceData> history, Date date) {
        for (int i = history.size() - 1; i >= 0; i--) {
            PriceData data = history.get(i);
            if (!data.getDate().after(date)) {
                return i;
            }
        }
        return -1;
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
     * 回测结果
     */
    public static class BacktestResult {
        private final List<MomentumStrategyTransaction> transactions;
        private final List<MomentumStrategyPerformance> performances;

        private BacktestResult(List<MomentumStrategyTransaction> transactions,
                               List<MomentumStrategyPerformance> performances) {
            this.transactions = transactions;
            this.performances = performances;
        }

        public List<MomentumStrategyTransaction> getTransactions() {
            return transactions;
        }

        public List<MomentumStrategyPerformance> getPerformances() {
            return performances;
        }
    }

    /**
     * 回测起始状态
     */
    private static class BacktestState {
        private final String currentHolding;
        private final long currentQuantity;
        private final BigDecimal availableCapital;
        private final BigDecimal initialCapital;

        private BacktestState(String currentHolding, long currentQuantity,
                              BigDecimal availableCapital, BigDecimal initialCapital) {
            this.currentHolding = currentHolding;
            this.currentQuantity = currentQuantity;
            this.availableCapital = availableCapital;
            this.initialCapital = initialCapital;
        }

        public String getCurrentHolding() {
            return currentHolding;
        }

        public long getCurrentQuantity() {
            return currentQuantity;
        }

        public BigDecimal getAvailableCapital() {
            return availableCapital;
        }

        public BigDecimal getInitialCapital() {
            return initialCapital;
        }
    }
    
    /**
     * 获取历史价格数据
     */
    private List<PriceData> getHistoricalPrices(String code, Date startDate, Date endDate, int extraDays) {
        try {
            Date extendedStartDate = new Date(startDate.getTime() - extraDays * 24L * 60 * 60 * 1000);
            int dataLen = calculateKlineDataLen(extendedStartDate, endDate, MAX_KLINE_DATA_LEN);
            
            // 延迟避免请求过快
            Thread.sleep(2000);
            
            // 使用scale=240表示日K线，参考MaStrategyService的实现
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
                
                // 检查必要的字段是否存在
                if (!obj.has("day") || !obj.has("close")) {
                    continue;
                }
                
                String dateStr = obj.get("day").getAsString();
                String closeStr = obj.get("close").getAsString();
                
                try {
                    // 解析日期（格式可能是 "2025-11-24 15:00:00" 或 "2025-11-24"）
                    Date date;
                    if (dateStr != null && dateStr.length() >= 10) {
                        date = dateParser.parse(dateStr.substring(0, 10));
                    } else {
                        logger.debug("跳过无效日期: {}", dateStr);
                        continue;
                    }
                    
                    // 包含回测开始日期之前的数据，用于计算21个交易日动量
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
     * 获取所有交易日期（所有ETF的交易日期的并集）
     */
    private List<Date> getTradingDates(Map<String, List<PriceData>> etfHistories, Date startDate, Date endDate) {
        Set<String> dateSet = new HashSet<>();
        
        for (List<PriceData> history : etfHistories.values()) {
            for (PriceData data : history) {
                Date date = data.getDate();
                if (!date.before(startDate) && !date.after(endDate)) {
                    dateSet.add(dateFormat.format(date));
                }
            }
        }
        
        List<Date> dates = new ArrayList<>();
        for (String dateStr : dateSet) {
            try {
                dates.add(dateFormat.parse(dateStr));
            } catch (ParseException e) {
                logger.warn("解析日期失败: {}", dateStr);
            }
        }
        
        // 排序
        dates.sort(Date::compareTo);
        
        return dates;
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
    
    /**
     * 将实体列表转换为DTO列表
     */
    private List<MomentumTransactionDTO> convertToDTOList(List<MomentumStrategyTransaction> transactions) {
        return transactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将实体转换为DTO
     */
    private MomentumTransactionDTO convertToDTO(MomentumStrategyTransaction transaction) {
        MomentumTransactionDTO dto = new MomentumTransactionDTO();
        dto.setDate(dateFormat.format(transaction.getTransactionDate()));
        dto.setCode(transaction.getEtfCode());
        dto.setName(transaction.getEtfName());
        dto.setType(transaction.getTransactionType());
        dto.setQuantity(transaction.getQuantity());
        dto.setPrice(transaction.getPrice());
        dto.setMomentum21(transaction.getMomentum21());
        return dto;
    }
    
    /**
     * 计算收益曲线数据
     * 根据已落库的每日绩效返回资产总值和收益率
     * @return 收益曲线数据列表
     */
    public List<MomentumPerformanceDTO> calculatePerformance() {
        List<MomentumStrategyPerformance> performances = performanceMapper.selectAllOrderByDateAsc();
        return convertPerformanceToDTOList(performances);
    }

    public List<MomentumPerformanceDTO> calculatePerformanceByDateRange(Date startDate, Date endDate) {
        List<MomentumStrategyPerformance> performances = performanceMapper.selectByDateRange(startDate, endDate);
        return convertPerformanceToDTOList(performances);
    }

    private List<MomentumPerformanceDTO> convertPerformanceToDTOList(List<MomentumStrategyPerformance> performances) {
        validatePerformanceInitialCapital(performances);
        return performances.stream()
                .map(this::convertPerformanceToDTO)
                .collect(Collectors.toList());
    }

    private MomentumPerformanceDTO convertPerformanceToDTO(MomentumStrategyPerformance performance) {
        MomentumPerformanceDTO dto = new MomentumPerformanceDTO();
        dto.setDate(dateFormat.format(performance.getPerformanceDate()));
        dto.setTotalValue(performance.getTotalValue());
        dto.setReturnRate(performance.getReturnRate());
        dto.setHoldingEtfCode(performance.getHoldingEtfCode());
        dto.setHoldingEtfName(performance.getHoldingEtfName());
        dto.setHoldingQuantity(performance.getHoldingQuantity());
        dto.setCurrentPrice(performance.getCurrentPrice());
        return dto;
    }

    private void validatePerformanceInitialCapital(List<MomentumStrategyPerformance> performances) {
        if (performances == null || performances.isEmpty()) {
            return;
        }

        BigDecimal initialCapital = performances.get(0).getInitialCapital();
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DataUnavailableException("动量策略每日绩效缺少初始资金，请重新执行回测生成新记录");
        }

        for (MomentumStrategyPerformance performance : performances) {
            BigDecimal performanceInitialCapital = performance.getInitialCapital();
            if (performanceInitialCapital == null || performanceInitialCapital.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DataUnavailableException("动量策略每日绩效缺少初始资金，请重新执行回测生成新记录");
            }
            if (performanceInitialCapital.compareTo(initialCapital) != 0) {
                throw new DataUnavailableException("动量策略每日绩效初始资金不一致，请清理旧记录后重新回测");
            }
        }
    }

    private void sortTransactionsForSimulation(List<MomentumStrategyTransaction> transactions) {
        transactions.sort(this::compareTransactionsForSimulation);
    }

    private int compareTransactionsForSimulation(MomentumStrategyTransaction left, MomentumStrategyTransaction right) {
        int dateCompare = left.getTransactionDate().compareTo(right.getTransactionDate());
        if (dateCompare != 0) {
            return dateCompare;
        }

        int typeCompare = Integer.compare(getTransactionTypeOrder(left), getTransactionTypeOrder(right));
        if (typeCompare != 0) {
            return typeCompare;
        }

        Date leftCreateTime = left.getCreateTime();
        Date rightCreateTime = right.getCreateTime();
        if (leftCreateTime != null && rightCreateTime != null) {
            int createTimeCompare = leftCreateTime.compareTo(rightCreateTime);
            if (createTimeCompare != 0) {
                return createTimeCompare;
            }
        } else if (leftCreateTime != null) {
            return -1;
        } else if (rightCreateTime != null) {
            return 1;
        }

        Long leftId = left.getId();
        Long rightId = right.getId();
        if (leftId == null && rightId == null) {
            return 0;
        }
        if (leftId == null) {
            return 1;
        }
        if (rightId == null) {
            return -1;
        }
        return leftId.compareTo(rightId);
    }

    private int getTransactionTypeOrder(MomentumStrategyTransaction transaction) {
        if ("sell".equals(transaction.getTransactionType())) {
            return 0;
        }
        if ("buy".equals(transaction.getTransactionType())) {
            return 1;
        }
        throw new DataUnavailableException("未知动量策略交易类型: " + transaction.getTransactionType());
    }

    private BigDecimal requireInitialCapital(MomentumStrategyTransaction transaction) {
        BigDecimal initialCapital = transaction.getInitialCapital();
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DataUnavailableException("动量策略交易记录缺少初始资金，请重新执行回测生成新记录");
        }
        return initialCapital;
    }

    private void validateTransactionForSave(MomentumStrategyTransaction transaction) {
        if (transaction == null) {
            throw new BusinessException("动量策略交易记录不能为空");
        }
        requireInitialCapital(transaction);
    }

    private void validatePerformanceForSave(MomentumStrategyPerformance performance) {
        if (performance == null) {
            throw new BusinessException("动量策略每日绩效不能为空");
        }
        if (performance.getPerformanceDate() == null) {
            throw new BusinessException("动量策略每日绩效日期不能为空");
        }
        BigDecimal initialCapital = performance.getInitialCapital();
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DataUnavailableException("动量策略每日绩效缺少初始资金，请重新执行回测生成新记录");
        }
        if (performance.getTotalValue() == null) {
            throw new DataUnavailableException("动量策略每日绩效缺少资产总值，请重新执行回测生成新记录");
        }
        if (performance.getHoldingEtfCode() != null && performance.getCurrentPrice() == null) {
            throw new DataUnavailableException("动量策略每日绩效缺少持仓价格: "
                    + performance.getHoldingEtfCode()
                    + ", 日期=" + dateFormat.format(performance.getPerformanceDate()));
        }
    }
    
    /**
     * 从最新每日绩效中恢复最新状态（持仓和资金）
     * @return 最新持仓状态，如果没有每日绩效则返回交易记录恢复的状态
     */
    private BacktestState restoreLatestState() {
        MomentumStrategyPerformance latestPerformance = performanceMapper.selectLatestPerformance();
        if (latestPerformance != null) {
            return restoreStateFromPerformance(latestPerformance);
        }

        List<MomentumStrategyTransaction> allTransactions = transactionMapper.selectAllOrderByDateDesc();
        return restoreStateFromTransactions(allTransactions);
    }

    /**
     * 恢复指定日期之前的策略状态，用于重算最新交易日
     */
    private BacktestState restoreStateBefore(Date beforeDate) {
        MomentumStrategyPerformance previousPerformance = performanceMapper.selectLatestPerformanceBefore(beforeDate);
        if (previousPerformance != null) {
            return restoreStateFromPerformance(previousPerformance);
        }

        List<MomentumStrategyTransaction> previousTransactions = transactionMapper.selectBeforeDate(beforeDate);
        return restoreStateFromTransactions(previousTransactions);
    }

    /**
     * 根据交易记录恢复持仓和现金状态
     */
    private BacktestState restoreStateFromTransactions(List<MomentumStrategyTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return null;
        }

        sortTransactionsForSimulation(transactions);
        BigDecimal initialCapital = requireInitialCapital(transactions.get(0));

        String currentHolding = null;
        long currentQuantity = 0;
        BigDecimal availableCapital = initialCapital;

        // 交易记录按时间顺序回放，得到区间末尾状态
        for (MomentumStrategyTransaction txn : transactions) {
            BigDecimal txnInitialCapital = requireInitialCapital(txn);
            if (txnInitialCapital.compareTo(initialCapital) != 0) {
                throw new DataUnavailableException("动量策略交易记录初始资金不一致，请清理旧记录后重新回测");
            }

            if ("sell".equals(txn.getTransactionType())) {
                BigDecimal proceeds = txn.getPrice().multiply(BigDecimal.valueOf(Math.abs(txn.getQuantity())));
                availableCapital = availableCapital.add(proceeds);
                currentHolding = null;
                currentQuantity = 0;
            } else if ("buy".equals(txn.getTransactionType())) {
                BigDecimal cost = txn.getPrice().multiply(BigDecimal.valueOf(txn.getQuantity()));
                availableCapital = availableCapital.subtract(cost);
                currentHolding = txn.getEtfCode();
                currentQuantity = txn.getQuantity();
            }
        }

        return new BacktestState(currentHolding, currentQuantity, availableCapital, initialCapital);
    }

    private BacktestState restoreStateFromPerformance(MomentumStrategyPerformance performance) {
        BigDecimal initialCapital = performance.getInitialCapital();
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DataUnavailableException("动量策略每日绩效缺少初始资金，请重新执行回测生成新记录");
        }

        BigDecimal totalValue = performance.getTotalValue();
        if (totalValue == null) {
            throw new DataUnavailableException("动量策略每日绩效缺少资产总值，请重新执行回测生成新记录");
        }

        String currentHolding = performance.getHoldingEtfCode();
        Long holdingQuantity = performance.getHoldingQuantity();
        BigDecimal availableCapital = totalValue;

        if (currentHolding != null && holdingQuantity != null && holdingQuantity > 0) {
            BigDecimal currentPrice = performance.getCurrentPrice();
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DataUnavailableException("动量策略每日绩效缺少持仓价格: "
                        + currentHolding + ", 日期=" + dateFormat.format(performance.getPerformanceDate()));
            }
            BigDecimal holdingValue = currentPrice.multiply(BigDecimal.valueOf(holdingQuantity));
            availableCapital = totalValue.subtract(holdingValue);
            return new BacktestState(currentHolding, holdingQuantity, availableCapital, initialCapital);
        }

        return new BacktestState(null, 0, availableCapital, initialCapital);
    }

    private Date resolveLatestStrategyDate() {
        Date latestPerformanceDate = performanceMapper.selectLatestPerformanceDate();
        if (latestPerformanceDate != null) {
            return latestPerformanceDate;
        }
        return transactionMapper.selectLatestTransactionDate();
    }

    /**
     * 替换重算区间内的绩效和交易记录
     */
    private int replaceRefreshedStrategyRange(Date startDate, Date endDate,
                                              List<MomentumStrategyTransaction> newTransactions,
                                              List<MomentumStrategyPerformance> newPerformances) {
        if (newPerformances == null || newPerformances.isEmpty()) {
            logger.info("增量回测未生成每日绩效");
            return 0;
        }

        deletePerformanceByDateRange(startDate, endDate);
        savePerformanceRecords(newPerformances);

        deleteByDateRange(startDate, endDate);
        if (newTransactions == null || newTransactions.isEmpty()) {
            logger.info("重算区间未生成新的交易记录，已清理区间内旧交易记录");
            return 0;
        }

        saveTransactions(newTransactions);
        logger.info("动量策略数据刷新完成，共生成 {} 条新交易记录", newTransactions.size());
        return newTransactions.size();
    }
    
    /**
     * 刷新动量策略数据（定时任务使用）
     * 从最新每日绩效日期开始重算，刷新收益曲线并重新判断调仓
     * @return 新生成的交易记录数
     */
    @Transactional
    public synchronized int refreshMomentumStrategy() {
        logger.info("========== 开始刷新动量策略数据 ==========");

        Date latestDate = resolveLatestStrategyDate();
        Date startDate;
        Date endDate = new Date();
        BigDecimal initialCapital = DEFAULT_INITIAL_CAPITAL;
        BacktestState state = null;

        if (latestDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.add(Calendar.MONTH, -3);
            startDate = cal.getTime();
            logger.info("没有历史交易记录，从 {} 开始回测", dateFormat.format(startDate));
        } else {
            startDate = latestDate;
            logger.info("从最新策略日期 {} 开始重算收益曲线和交易信号",
                    dateFormat.format(startDate));

            state = restoreStateBefore(startDate);
            if (state != null) {
                initialCapital = state.getInitialCapital();
                logger.info("从重算起始日前恢复资金状态: 持仓={}, 数量={}, 可用资金={}, 初始资金={}",
                        state.getCurrentHolding(), state.getCurrentQuantity(),
                        state.getAvailableCapital(), state.getInitialCapital());
            }
        }

        if (!startDate.before(endDate)) {
            logger.info("数据已是最新，无需刷新");
            return 0;
        }

        BacktestResult result = runBacktest(startDate, endDate, initialCapital, state);
        List<MomentumStrategyTransaction> newTransactions = result.getTransactions();
        List<MomentumStrategyPerformance> newPerformances = result.getPerformances();
        return replaceRefreshedStrategyRange(startDate, endDate, newTransactions, newPerformances);
    }
    
}
