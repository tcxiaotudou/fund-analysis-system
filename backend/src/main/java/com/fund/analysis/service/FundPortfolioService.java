package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.fund.analysis.entity.FundInfo;
import com.fund.analysis.entity.FundPortfolioRsi;
import com.fund.analysis.entity.FundPortfolioRsiHistory;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.mapper.FundInfoMapper;
import com.fund.analysis.mapper.FundPortfolioRsiMapper;
import com.fund.analysis.mapper.FundPortfolioRsiHistoryMapper;
import com.fund.analysis.utils.RsiCalculator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基金组合服务类
 * 提供持有基金组合的 RSI 计算功能
 */
@Service
public class FundPortfolioService {
    
    private static final Logger logger = LoggerFactory.getLogger(FundPortfolioService.class);
    
    @Autowired
    private FundInfoMapper fundInfoMapper;
    
    @Autowired
    private FundPortfolioRsiMapper fundPortfolioRsiMapper;
    
    @Autowired
    private FundPortfolioRsiHistoryMapper fundPortfolioRsiHistoryMapper;

    @Autowired
    private ExternalApiClient externalApiClient;
    
    /**
     * 获取持有的基金列表
     * @return 持有的基金列表
     */
    public List<FundInfo> getHoldingFunds() {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("is_holding", 1);
        queryMap.put("deleted", 0);
        List<FundInfo> funds = fundInfoMapper.selectByMap(queryMap);
        return funds != null ? funds : new ArrayList<>();
    }
    
    /**
     * 计算持有基金组合的 RSI
     * @param period RSI 周期（14或90）
     * @return RSI 值
     */
    public BigDecimal calculatePortfolioRsi(int period) {
        List<FundInfo> holdingFunds = getHoldingFunds();
        if (holdingFunds.isEmpty()) {
            throw new DataUnavailableException("没有持有基金，无法计算组合RSI");
        }

        PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices = buildAlignedPortfolioPrices(holdingFunds);
        List<BigDecimal> rsiValues = RsiCalculator.calculateRSI(alignedPrices.getPrices(), period);
        if (rsiValues.isEmpty()) {
            throw new DataUnavailableException("组合RSI数据不足，period=" + period);
        }

        return rsiValues.get(rsiValues.size() - 1);
    }
    
    /**
     * 计算持有基金组合的周 RSI
     * @param period RSI 周期（通常是14）
     * @return RSI 值
     */
    public BigDecimal calculatePortfolioWeeklyRsi(int period) {
        List<FundInfo> holdingFunds = getHoldingFunds();
        if (holdingFunds.isEmpty()) {
            throw new DataUnavailableException("没有持有基金，无法计算组合周RSI");
        }

        PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices = buildAlignedPortfolioPrices(holdingFunds);
        List<BigDecimal> weeklyPrices = extractWeeklyPrices(alignedPrices.getPrices());
        List<BigDecimal> rsiValues = RsiCalculator.calculateRSI(weeklyPrices, period);
        if (rsiValues.isEmpty()) {
            throw new DataUnavailableException("组合周RSI数据不足，period=" + period);
        }

        return rsiValues.get(rsiValues.size() - 1);
    }
    
    /**
     * 构建按共同交易日对齐后的组合价格
     *
     * @param holdingFunds 持有基金列表
     * @return 对齐后的组合价格
     */
    private PortfolioPriceAligner.AlignedPortfolioPrices buildAlignedPortfolioPrices(List<FundInfo> holdingFunds) {
        List<BigDecimal> weights = resolvePortfolioWeights(holdingFunds);
        List<PortfolioPriceAligner.FundPriceSeries> seriesList = new ArrayList<>();

        for (FundInfo fund : holdingFunds) {
            seriesList.add(getFundPriceSeries(fund.getFundCode()));
        }

        return PortfolioPriceAligner.align(seriesList, weights);
    }

    /**
     * 解析组合权重，权重总和不等于100时使用等权重
     *
     * @param holdingFunds 持有基金列表
     * @return 权重列表
     */
    private List<BigDecimal> resolvePortfolioWeights(List<FundInfo> holdingFunds) {
        BigDecimal totalWeight = holdingFunds.stream()
                .map(f -> f.getPortfolioWeight() != null ? f.getPortfolioWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean useEqualWeight = totalWeight.compareTo(new BigDecimal("100")) != 0;
        if (useEqualWeight) {
            logger.info("Total weight is {}, using equal weight", totalWeight);
        }

        List<BigDecimal> weights = new ArrayList<>();
        int fundCount = holdingFunds.size();
        for (FundInfo fund : holdingFunds) {
            BigDecimal weight = useEqualWeight
                    ? BigDecimal.valueOf(100.0 / fundCount)
                    : fund.getPortfolioWeight() != null ? fund.getPortfolioWeight() : BigDecimal.ZERO;
            weights.add(weight);
        }
        return weights;
    }

    /**
     * 从日价格中提取周价格
     *
     * @param dailyWeightedPrices 日组合价格
     * @return 周组合价格
     */
    private List<BigDecimal> extractWeeklyPrices(List<BigDecimal> dailyWeightedPrices) {
        List<BigDecimal> weeklyPrices = new ArrayList<>();
        for (int i = 4; i < dailyWeightedPrices.size(); i += 5) {
            weeklyPrices.add(dailyWeightedPrices.get(i));
        }

        int remainder = dailyWeightedPrices.size() % 5;
        if (remainder != 0 && !dailyWeightedPrices.isEmpty()) {
            weeklyPrices.add(dailyWeightedPrices.get(dailyWeightedPrices.size() - 1));
        }
        return weeklyPrices;
    }
    
    /**
     * 获取基金组合 RSI 汇总数据（从数据库读取）
     * @return RSI 汇总数据
     */
    public Map<String, Object> getPortfolioRsiSummary() {
        Map<String, Object> result = new HashMap<>();

        FundPortfolioRsi portfolioRsi = fundPortfolioRsiMapper.selectLatest();
        if (portfolioRsi != null) {
            result.put("rsi14", portfolioRsi.getRsi14());
            result.put("rsi90", portfolioRsi.getRsi90());
            result.put("weeklyRsi14", portfolioRsi.getWeeklyRsi14());
            result.put("fundCount", portfolioRsi.getFundCount());
            result.put("fundCodes", portfolioRsi.getFundCodes());
            result.put("updateTime", portfolioRsi.getDataTime());
        } else {
            result.put("rsi14", null);
            result.put("rsi90", null);
            result.put("weeklyRsi14", null);
            result.put("fundCount", 0);
            result.put("fundCodes", "");
            result.put("updateTime", null);
        }

        return result;
    }
    
    /**
     * 刷新基金组合 RSI 数据
     * 计算持有基金组合的 RSI 并保存到数据库
     * @return 是否成功
     */
    @Transactional
    public boolean refreshPortfolioRsi() {
        logger.info("开始刷新基金组合 RSI 数据");

        List<FundInfo> holdingFunds = getHoldingFunds();
        if (holdingFunds.isEmpty()) {
            logger.warn("No holding funds found, skip refreshing portfolio RSI");
            return false;
        }

        BigDecimal rsi14 = calculatePortfolioRsi(14);
        BigDecimal rsi90 = calculatePortfolioRsi(90);
        BigDecimal weeklyRsi14 = calculatePortfolioWeeklyRsi(14);

        String fundCodes = holdingFunds.stream()
                .map(FundInfo::getFundCode)
                .collect(Collectors.joining(","));

        FundPortfolioRsi portfolioRsi = new FundPortfolioRsi();
        portfolioRsi.setRsi14(rsi14);
        portfolioRsi.setRsi90(rsi90);
        portfolioRsi.setWeeklyRsi14(weeklyRsi14);
        portfolioRsi.setFundCount(holdingFunds.size());
        portfolioRsi.setFundCodes(fundCodes);
        portfolioRsi.setDataTime(new Date());
        portfolioRsi.setCreateTime(new Date());

        fundPortfolioRsiMapper.insert(portfolioRsi);
        fundPortfolioRsiMapper.deleteOldData(10);

        logger.info("基金组合 RSI 数据刷新完成 - RSI14: {}, RSI90: {}, WeeklyRSI14: {}, 基金数量: {}",
                rsi14, rsi90, weeklyRsi14, holdingFunds.size());
        return true;
    }
    
    /**
     * 更新单个基金的权重
     * @param fundCode 基金代码
     * @param weight 权重值
     * @return 是否成功
     */
    @Transactional
    public boolean updateFundWeight(String fundCode, BigDecimal weight) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("fund_code", fundCode);
        queryMap.put("deleted", 0);
        List<FundInfo> funds = fundInfoMapper.selectByMap(queryMap);

        if (funds.isEmpty()) {
            logger.error("Fund not found: {}", fundCode);
            return false;
        }

        FundInfo fund = funds.get(0);
        fund.setPortfolioWeight(weight);
        int count = fundInfoMapper.updateById(fund);

        logger.info("Updated weight for fund {} to {}, affected rows: {}", fundCode, weight, count);
        return count > 0;
    }
    
    /**
     * 批量更新基金权重
     * @param weights 权重映射 {fundCode: weight}
     * @return 是否成功
     */
    @Transactional
    public boolean updateFundWeights(Map<String, BigDecimal> weights) {
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            boolean success = updateFundWeight(entry.getKey(), entry.getValue());
            if (!success) {
                throw new DataUnavailableException("Failed to update weight for fund: " + entry.getKey());
            }
        }

        logger.info("Successfully updated weights for {} funds", weights.size());
        return true;
    }
    
    /**
     * 获取组合最近N个交易日的14日RSI历史数据（从数据库读取）
     * @param days 交易日数量
     * @return RSI历史数据列表，每个元素包含日期和RSI值
     */
    public List<Map<String, Object>> getPortfolioRsiHistory(int days) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<FundPortfolioRsiHistory> historyList = fundPortfolioRsiHistoryMapper.selectRecentDays(days);
        if (historyList == null || historyList.isEmpty()) {
            logger.warn("No portfolio RSI history data found in database");
            return result;
        }

        Collections.reverse(historyList);
        for (FundPortfolioRsiHistory history : historyList) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", history.getDataDate());
            dataPoint.put("rsi", history.getRsi14());
            result.add(dataPoint);
        }

        logger.info("Successfully retrieved {} days of RSI history from database", result.size());
        return result;
    }
    
    /**
     * 刷新基金组合RSI历史数据
     * 计算最近N个交易日的14日RSI历史数据并保存到数据库
     * @param days 交易日数量，默认100天
     * @return 是否成功
     */
    @Transactional
    public boolean refreshPortfolioRsiHistory(int days) {
        logger.info("开始刷新基金组合 RSI 历史数据，计算最近 {} 个交易日", days);

        List<FundInfo> holdingFunds = getHoldingFunds();
        if (holdingFunds.isEmpty()) {
            logger.warn("No holding funds found, skip refreshing portfolio RSI history");
            return false;
        }

        int fundCount = holdingFunds.size();
        PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices = buildAlignedPortfolioPrices(holdingFunds);
        List<BigDecimal> dailyWeightedPrices = alignedPrices.getPrices();
        List<String> dates = alignedPrices.getDates();

        List<BigDecimal> rsiValues = RsiCalculator.calculateRSI(dailyWeightedPrices, 14);
        if (rsiValues.isEmpty()) {
            throw new DataUnavailableException("组合RSI历史数据不足");
        }

        String fundCodes = holdingFunds.stream()
                .map(FundInfo::getFundCode)
                .collect(Collectors.joining(","));

        fundPortfolioRsiHistoryMapper.deleteAll();
        logger.info("已清空旧的 RSI 历史数据，准备重新计算");

        List<FundPortfolioRsiHistory> historyList = new ArrayList<>();
        Date now = new Date();

        int startIndex = Math.max(0, rsiValues.size() - days);
        for (int i = startIndex; i < rsiValues.size(); i++) {
            String dataDate = dates != null && i < dates.size() ? dates.get(i) : "";
            BigDecimal rsi14 = rsiValues.get(i);

            FundPortfolioRsiHistory history = new FundPortfolioRsiHistory();
            history.setRsi14(rsi14);
            history.setFundCount(fundCount);
            history.setFundCodes(fundCodes);
            history.setDataDate(dataDate);

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(dataDate);
                history.setDataTime(date);
            } catch (Exception e) {
                history.setDataTime(now);
            }

            history.setCreateTime(now);
            historyList.add(history);
        }

        if (!historyList.isEmpty()) {
            fundPortfolioRsiHistoryMapper.batchInsert(historyList);
            logger.info("成功插入 {} 条基金组合 RSI 历史数据", historyList.size());
        } else {
            logger.info("无历史数据可插入");
        }

        logger.info("基金组合 RSI 历史数据刷新完成");
        return true;
    }
    
    /**
     * 获取基金的历史净值日期价格序列
     *
     * @param fundCode 基金代码
     * @return 基金日期价格序列
     */
    private PortfolioPriceAligner.FundPriceSeries getFundPriceSeries(String fundCode) {
        String url = "https://apiv2.jiucaishuo.com/funddetail/changepercent/achieve";
        Map<String, Object> payload = new HashMap<>();
        payload.put("fund_code", fundCode);
        payload.put("tags_id", 4);
        payload.put("limit", 200);
        payload.put("type", "h5");
        payload.put("version", "2.5.6");

        JsonObject jsonObject = externalApiClient.postJsonElement(url, payload).getAsJsonObject();
        if (jsonObject.get("code").getAsInt() != 0) {
            throw new ExternalApiException("获取基金净值历史失败: " + fundCode + ", response=" + jsonObject);
        }

        JsonArray listArray = jsonObject.getAsJsonObject("data").getAsJsonArray("list");
        if (listArray.size() < 3) {
            throw new DataUnavailableException("基金净值历史数据不足: " + fundCode);
        }

        JsonArray dateArray = listArray.get(0).getAsJsonArray();
        JsonArray priceArray = listArray.get(1).getAsJsonArray();
        if (dateArray.size() != priceArray.size()) {
            throw new DataUnavailableException("基金净值日期与价格数量不一致: " + fundCode);
        }

        Map<String, BigDecimal> pricesByDate = new LinkedHashMap<>();
        for (int i = 1; i < priceArray.size(); i++) {
            JsonObject dateItem = dateArray.get(i).getAsJsonObject();
            JsonObject priceItem = priceArray.get(i).getAsJsonObject();
            pricesByDate.put(dateItem.get("name").getAsString(), new BigDecimal(priceItem.get("name").getAsString()));
        }

        return new PortfolioPriceAligner.FundPriceSeries(fundCode, pricesByDate);
    }
}
