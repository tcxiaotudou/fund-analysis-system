package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fund.analysis.entity.FundInfo;
import com.fund.analysis.entity.FundPortfolioRsi;
import com.fund.analysis.entity.FundPortfolioRsiHistory;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.exception.BadRequestException;
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
import org.springframework.transaction.support.TransactionTemplate;

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
     * 短事务执行器
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

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
        return calculatePortfolioRsi(alignedPrices, period);
    }

    private BigDecimal calculatePortfolioRsi(
            PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices,
            int period) {
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
        return calculatePortfolioWeeklyRsi(alignedPrices, period);
    }

    private BigDecimal calculatePortfolioWeeklyRsi(
            PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices,
            int period) {
        List<BigDecimal> weeklyPrices =
                PortfolioPriceAligner.extractWeeklyClosingPrices(alignedPrices);
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
        List<BigDecimal> positiveWeights = new ArrayList<>();

        for (int index = 0; index < holdingFunds.size(); index++) {
            BigDecimal weight = weights.get(index);
            if (weight.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            FundInfo fund = holdingFunds.get(index);
            seriesList.add(getFundPriceSeries(fund.getFundCode()));
            positiveWeights.add(weight);
        }

        return PortfolioPriceAligner.align(seriesList, positiveWeights);
    }

    /**
     * 解析并校验组合权重
     *
     * @param holdingFunds 持有基金列表
     * @return 权重列表
     */
    private List<BigDecimal> resolvePortfolioWeights(List<FundInfo> holdingFunds) {
        List<BigDecimal> weights = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (FundInfo fund : holdingFunds) {
            BigDecimal weight = fund.getPortfolioWeight();
            if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0) {
                throw new DataUnavailableException(
                        "持仓基金权重未配置或无效: " + fund.getFundCode());
            }
            weights.add(weight);
            totalWeight = totalWeight.add(weight);
        }
        if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
            throw new DataUnavailableException(
                    "持仓基金权重总和必须等于100，当前为: " + totalWeight);
        }
        return weights;
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
    public boolean refreshPortfolioRsi() {
        logger.info("开始刷新基金组合 RSI 数据");

        List<FundInfo> holdingFunds = getHoldingFunds();
        if (holdingFunds.isEmpty()) {
            logger.warn("No holding funds found, skip refreshing portfolio RSI");
            return false;
        }

        PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices =
                buildAlignedPortfolioPrices(holdingFunds);
        BigDecimal rsi14 = calculatePortfolioRsi(alignedPrices, 14);
        BigDecimal rsi90 = calculatePortfolioRsi(alignedPrices, 90);
        BigDecimal weeklyRsi14 = calculatePortfolioWeeklyRsi(alignedPrices, 14);

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

        transactionTemplate.executeWithoutResult(status -> {
            fundPortfolioRsiMapper.insert(portfolioRsi);
            fundPortfolioRsiMapper.deleteOldData(10);
        });

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
        validateWeightInput(fundCode, weight);
        List<FundInfo> holdingFunds = getHoldingFundsForUpdate();
        FundInfo fund = holdingFunds.stream()
                .filter(item -> fundCode.equals(item.getFundCode()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("基金不在当前持仓中: " + fundCode));

        BigDecimal projectedTotal = BigDecimal.ZERO;
        for (FundInfo holdingFund : holdingFunds) {
            BigDecimal currentWeight = fundCode.equals(holdingFund.getFundCode())
                    ? weight
                    : holdingFund.getPortfolioWeight();
            if (currentWeight == null) {
                throw new BadRequestException("持仓基金存在未配置权重: " + holdingFund.getFundCode());
            }
            projectedTotal = projectedTotal.add(currentWeight);
        }
        if (projectedTotal.compareTo(new BigDecimal("100")) != 0) {
            throw new BadRequestException("权重更新后总和必须等于 100%，当前为: "
                    + projectedTotal + "%");
        }

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
        if (weights == null || weights.isEmpty()) {
            throw new BadRequestException("权重配置不能为空");
        }
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            validateWeightInput(entry.getKey(), entry.getValue());
            totalWeight = totalWeight.add(entry.getValue());
        }
        if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
            throw new BadRequestException("权重总和必须等于 100%，当前为: " + totalWeight + "%");
        }

        List<FundInfo> holdingFunds = getHoldingFundsForUpdate();
        Set<String> holdingFundCodes = holdingFunds.stream()
                .map(FundInfo::getFundCode)
                .collect(Collectors.toSet());
        if (!holdingFundCodes.equals(weights.keySet())) {
            throw new BadRequestException("批量权重必须完整覆盖当前全部持仓基金");
        }

        for (FundInfo fund : holdingFunds) {
            fund.setPortfolioWeight(weights.get(fund.getFundCode()));
            if (fundInfoMapper.updateById(fund) != 1) {
                throw new DataUnavailableException("权重更新失败: " + fund.getFundCode());
            }
        }

        logger.info("Successfully updated weights for {} funds", weights.size());
        return true;
    }

    private List<FundInfo> getHoldingFundsForUpdate() {
        QueryWrapper<FundInfo> query = new QueryWrapper<>();
        query.eq("is_holding", 1)
                .eq("deleted", 0)
                .last("FOR UPDATE");
        List<FundInfo> holdingFunds = fundInfoMapper.selectList(query);
        if (holdingFunds == null || holdingFunds.isEmpty()) {
            throw new BadRequestException("当前没有持仓基金");
        }
        return holdingFunds;
    }

    private void validateWeightInput(String fundCode, BigDecimal weight) {
        if (fundCode == null || fundCode.trim().isEmpty()) {
            throw new BadRequestException("基金代码不能为空");
        }
        if (weight == null
                || weight.compareTo(BigDecimal.ZERO) < 0
                || weight.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("权重必须在 0-100 之间");
        }
        if (weight.scale() > 2) {
            throw new BadRequestException("权重最多保留2位小数");
        }
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
        List<String> rsiDates = PortfolioPriceAligner.alignDatesToRsiValues(dates, rsiValues.size());

        String fundCodes = holdingFunds.stream()
                .map(FundInfo::getFundCode)
                .collect(Collectors.joining(","));

        List<FundPortfolioRsiHistory> historyList = new ArrayList<>();
        Date now = new Date();

        int startIndex = Math.max(0, rsiValues.size() - days);
        for (int i = startIndex; i < rsiValues.size(); i++) {
            String dataDate = rsiDates.get(i);
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

        transactionTemplate.executeWithoutResult(status -> {
            fundPortfolioRsiHistoryMapper.deleteAll();
            logger.info("已清空旧的 RSI 历史数据，准备重新计算");
            if (!historyList.isEmpty()) {
                fundPortfolioRsiHistoryMapper.batchInsert(historyList);
                logger.info("成功插入 {} 条基金组合 RSI 历史数据", historyList.size());
            } else {
                logger.info("无历史数据可插入");
            }
        });

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
        JsonArray cumulativePriceArray = null;
        for (int index = 1; index < listArray.size(); index++) {
            JsonArray candidate = listArray.get(index).getAsJsonArray();
            if (!candidate.isEmpty()
                    && candidate.get(0).isJsonObject()
                    && candidate.get(0).getAsJsonObject().has("name")
                    && "累计净值".equals(
                            candidate.get(0).getAsJsonObject().get("name").getAsString())) {
                cumulativePriceArray = candidate;
                break;
            }
        }
        if (cumulativePriceArray == null) {
            throw new DataUnavailableException("基金历史数据缺少累计净值序列: " + fundCode);
        }
        if (dateArray.size() != cumulativePriceArray.size()) {
            throw new DataUnavailableException("基金净值日期与累计净值数量不一致: " + fundCode);
        }

        Map<String, BigDecimal> pricesByDate = new LinkedHashMap<>();
        for (int i = 1; i < cumulativePriceArray.size(); i++) {
            JsonObject dateItem = dateArray.get(i).getAsJsonObject();
            JsonObject priceItem = cumulativePriceArray.get(i).getAsJsonObject();
            pricesByDate.put(dateItem.get("name").getAsString(), new BigDecimal(priceItem.get("name").getAsString()));
        }

        return new PortfolioPriceAligner.FundPriceSeries(fundCode, pricesByDate);
    }
}
