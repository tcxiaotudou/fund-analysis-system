package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.fund.analysis.dto.RsiBacktestDTO;
import com.fund.analysis.entity.EtfInfo;
import com.fund.analysis.exception.BusinessException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.mapper.EtfInfoMapper;
import com.fund.analysis.utils.RsiCalculator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class RsiBacktestService {

    private static final Logger logger = LoggerFactory.getLogger(RsiBacktestService.class);
    private static final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final int KLINE_DATA_BUFFER = 10;
    private static final int MAX_KLINE_DATA_LEN = 1950;
    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    @Autowired
    private EtfInfoMapper etfInfoMapper;

    @Autowired
    private ExternalApiClient externalApiClient;

    public RsiBacktestDTO runBacktest(String etfCode, Date startDate, Date endDate,
                                      BigDecimal initialCapital, int rsiPeriod,
                                      BigDecimal rsiBuyThreshold, BigDecimal rsiSellThreshold,
                                      BigDecimal fixedAmountPerTrade) {
        logger.info("开始RSI策略回测(定额模式): ETF={}, RSI周期={}, 买入阈值={}, 卖出阈值={}, 初始资金={}, 每笔金额={}",
                etfCode, rsiPeriod, rsiBuyThreshold, rsiSellThreshold, initialCapital, fixedAmountPerTrade);

        RsiBacktestDTO result = new RsiBacktestDTO();
        result.setEtfCode(etfCode);
        result.setStartDate(dateOnlyFormat.format(startDate));
        result.setEndDate(dateOnlyFormat.format(endDate));
        result.setInitialCapital(initialCapital);
        result.setRsiPeriod(rsiPeriod);
        result.setRsiBuyThreshold(rsiBuyThreshold);
        result.setRsiSellThreshold(rsiSellThreshold);
        result.setFixedAmountPerTrade(fixedAmountPerTrade);

        String etfName = etfCode;
        EtfInfo etfInfo = etfInfoMapper.selectByCode(etfCode);
        if (etfInfo != null) {
            etfName = etfInfo.getEtfName();
        }
        result.setEtfName(etfName);

        List<PriceData> allPrices = getHistoricalPrices(etfCode, startDate, endDate, rsiPeriod);

        int lookbackCount = 0;
        int backtestStartIdx = -1;
        int backtestDataCount = 0;
        for (int i = 0; i < allPrices.size(); i++) {
            Date date = allPrices.get(i).getDate();
            if (date.before(startDate)) {
                lookbackCount++;
            } else if (!date.after(endDate)) {
                if (backtestStartIdx < 0) {
                    backtestStartIdx = i;
                }
                backtestDataCount++;
            }
        }

        if (lookbackCount < rsiPeriod) {
            throw new DataUnavailableException("RSI回测开始日前历史数据不足: " + etfCode
                    + ", 需要=" + rsiPeriod + ", 实际=" + lookbackCount);
        }
        if (backtestStartIdx < 0 || backtestDataCount == 0) {
            throw new DataUnavailableException("RSI回测日期范围内没有可用数据: " + etfCode);
        }

        List<BigDecimal> allCloses = new ArrayList<>();
        for (PriceData pd : allPrices) {
            allCloses.add(pd.getClose());
        }
        List<BigDecimal> rsiValues = RsiCalculator.calculateRSI(allCloses, rsiPeriod);

        List<RsiBacktestDTO.BacktestTransaction> transactions = new ArrayList<>();
        List<RsiBacktestDTO.DailyValue> dailyValues = new ArrayList<>();

        BigDecimal cash = initialCapital;
        long holdingQuantity = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        int buyCount = 0;
        int sellCount = 0;
        BigDecimal peakValue = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        int winTrades = 0;
        int totalSellTrades = 0;
        BigDecimal lastClose = null;

        for (int i = backtestStartIdx; i < allPrices.size(); i++) {
            PriceData current = allPrices.get(i);
            if (current.getDate().after(endDate)) break;

            BigDecimal executionPrice = current.getOpen();
            BigDecimal valuationPrice = current.getClose();
            lastClose = valuationPrice;

            int currentRsiIdx = i - rsiPeriod;
            BigDecimal currentRsi = rsiValues.get(currentRsiIdx);

            BigDecimal signalRsi = null;
            if (i > backtestStartIdx) {
                signalRsi = rsiValues.get(i - 1 - rsiPeriod);
            }

            boolean isBuySignal = signalRsi != null && signalRsi.compareTo(rsiBuyThreshold) <= 0;
            boolean isSellSignal = signalRsi != null && signalRsi.compareTo(rsiSellThreshold) >= 0;

            BigDecimal actualBuyAmount = fixedAmountPerTrade.min(cash);

            if (isBuySignal && actualBuyAmount.compareTo(executionPrice) >= 0) {
                long quantity = actualBuyAmount.divide(executionPrice, 0, RoundingMode.DOWN).longValue();
                if (quantity > 0) {
                    BigDecimal amount = executionPrice.multiply(BigDecimal.valueOf(quantity));
                    totalCost = totalCost.add(amount);
                    holdingQuantity += quantity;
                    cash = cash.subtract(amount);
                    totalInvested = totalInvested.add(amount);

                    BigDecimal avgCost = totalCost.divide(BigDecimal.valueOf(holdingQuantity), 4, RoundingMode.HALF_UP);
                    BigDecimal totalValue = cash.add(executionPrice.multiply(BigDecimal.valueOf(holdingQuantity)));

                    RsiBacktestDTO.BacktestTransaction tx = new RsiBacktestDTO.BacktestTransaction();
                    tx.setDate(dateOnlyFormat.format(current.getDate()));
                    tx.setType("BUY");
                    tx.setPrice(executionPrice);
                    tx.setQuantity(quantity);
                    tx.setAmount(amount);
                    tx.setTotalValue(totalValue);
                    tx.setRsiValue(signalRsi);
                    tx.setProfit(BigDecimal.ZERO);
                    tx.setHoldingQuantityAfter(holdingQuantity);
                    tx.setAvgCostAfter(avgCost);
                    tx.setCashAfter(cash);
                    tx.setSignalDescription("RSI(" + rsiPeriod + ")=" + signalRsi.setScale(2, RoundingMode.HALF_UP)
                            + " ≤ " + rsiBuyThreshold + "，定额买入" + amount.setScale(2, RoundingMode.HALF_UP));
                    transactions.add(tx);
                    buyCount++;
                }
            } else if (isSellSignal && holdingQuantity > 0) {
                long sellQuantity = fixedAmountPerTrade.divide(executionPrice, 0, RoundingMode.DOWN).longValue();
                sellQuantity = Math.min(sellQuantity, holdingQuantity);

                if (sellQuantity > 0) {
                    BigDecimal avgCost = totalCost.divide(BigDecimal.valueOf(holdingQuantity), 4, RoundingMode.HALF_UP);
                    BigDecimal amount = executionPrice.multiply(BigDecimal.valueOf(sellQuantity));
                    BigDecimal costOfSold = avgCost.multiply(BigDecimal.valueOf(sellQuantity));
                    BigDecimal profit = amount.subtract(costOfSold);

                    totalCost = totalCost.subtract(costOfSold);
                    holdingQuantity -= sellQuantity;
                    cash = cash.add(amount);

                    if (holdingQuantity == 0) {
                        totalCost = BigDecimal.ZERO;
                    }

                    BigDecimal newAvgCost = holdingQuantity > 0
                            ? totalCost.divide(BigDecimal.valueOf(holdingQuantity), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal totalValue = cash.add(executionPrice.multiply(BigDecimal.valueOf(holdingQuantity)));

                    RsiBacktestDTO.BacktestTransaction tx = new RsiBacktestDTO.BacktestTransaction();
                    tx.setDate(dateOnlyFormat.format(current.getDate()));
                    tx.setType("SELL");
                    tx.setPrice(executionPrice);
                    tx.setQuantity(sellQuantity);
                    tx.setAmount(amount);
                    tx.setTotalValue(totalValue);
                    tx.setRsiValue(signalRsi);
                    tx.setProfit(profit);
                    tx.setHoldingQuantityAfter(holdingQuantity);
                    tx.setAvgCostAfter(newAvgCost);
                    tx.setCashAfter(cash);
                    tx.setSignalDescription("RSI(" + rsiPeriod + ")=" + signalRsi.setScale(2, RoundingMode.HALF_UP)
                            + " ≥ " + rsiSellThreshold + "，定额卖出" + amount.setScale(2, RoundingMode.HALF_UP));
                    transactions.add(tx);

                    sellCount++;
                    totalSellTrades++;
                    if (profit.compareTo(BigDecimal.ZERO) > 0) {
                        winTrades++;
                    }
                }
            }

            BigDecimal totalValue = cash;
            if (holdingQuantity > 0) {
                totalValue = totalValue.add(valuationPrice.multiply(BigDecimal.valueOf(holdingQuantity)));
            }

            if (totalValue.compareTo(peakValue) > 0) {
                peakValue = totalValue;
            }
            BigDecimal drawdown = peakValue.subtract(totalValue)
                    .divide(peakValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }

            RsiBacktestDTO.DailyValue dv = new RsiBacktestDTO.DailyValue();
            dv.setDate(dateOnlyFormat.format(current.getDate()));
            dv.setPrice(valuationPrice);
            dv.setTotalValue(totalValue);
            dv.setRsiValue(currentRsi);
            BigDecimal returnRate = totalValue.subtract(initialCapital)
                    .divide(initialCapital, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dv.setReturnRate(returnRate);
            dailyValues.add(dv);
        }

        BigDecimal finalCapital = cash;
        if (holdingQuantity > 0) {
            finalCapital = finalCapital.add(lastClose.multiply(BigDecimal.valueOf(holdingQuantity)));
        }

        result.setFinalCapital(finalCapital);
        result.setTransactions(transactions);
        result.setDailyValues(dailyValues);
        result.setBuyCount(buyCount);
        result.setSellCount(sellCount);
        result.setTradeCount(buyCount + sellCount);
        result.setMaxDrawdown(maxDrawdown);
        result.setTotalInvested(totalInvested);
        result.setFinalHoldingQuantity(holdingQuantity);
        result.setAverageCost(holdingQuantity > 0
                ? totalCost.divide(BigDecimal.valueOf(holdingQuantity), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        result.setWinRate(totalSellTrades > 0
                ? BigDecimal.valueOf(winTrades).divide(BigDecimal.valueOf(totalSellTrades), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO);

        BigDecimal totalReturnRate = finalCapital.subtract(initialCapital)
                .divide(initialCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        result.setTotalReturnRate(totalReturnRate);

        long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
        if (days > 0) {
            double years = days / 365.0;
            double returnRateDecimal = totalReturnRate.doubleValue() / 100.0;
            double annualized = (Math.pow(1 + returnRateDecimal, 1.0 / years) - 1) * 100;
            result.setAnnualizedReturnRate(BigDecimal.valueOf(annualized).setScale(2, RoundingMode.HALF_UP));
        } else {
            result.setAnnualizedReturnRate(BigDecimal.ZERO);
        }

        logger.info("RSI回测完成(定额模式): ETF={}, 总收益率={}%, 年化={}%, 交易={}, 胜率={}%, 最大回撤={}%, 剩余持仓={}",
                etfCode, totalReturnRate, result.getAnnualizedReturnRate(),
                result.getTradeCount(), result.getWinRate(), maxDrawdown, holdingQuantity);

        return result;
    }

    private static class PriceData {
        private final Date date;
        private final BigDecimal open;
        private final BigDecimal close;

        PriceData(Date date, BigDecimal open, BigDecimal close) {
            this.date = date;
            this.open = open;
            this.close = close;
        }

        Date getDate() { return date; }
        BigDecimal getOpen() { return open; }
        BigDecimal getClose() { return close; }
    }

    private List<PriceData> getHistoricalPrices(String code, Date startDate, Date endDate, int rsiPeriod) {
        try {
            Date latestDate = endDate.after(new Date()) ? endDate : new Date();
            long rangeDays = Math.max(0, (latestDate.getTime() - startDate.getTime()) / MILLIS_PER_DAY);
            long requestedDataLen = rangeDays + rsiPeriod + KLINE_DATA_BUFFER;
            int dataLen = (int) Math.min(Math.max(requestedDataLen, 1), MAX_KLINE_DATA_LEN);

            Thread.sleep(2000);

            boolean index = isIndexCode(code);
            String adjustment = index ? "" : "qfq";
            String url = String.format(
                    "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=%s,day,,,%d,%s",
                    code, dataLen, adjustment);

            logger.debug("请求ETF历史数据: {}", url);

            JsonElement jsonElement = externalApiClient.getJson(url);

            if (!jsonElement.isJsonObject()
                    || !jsonElement.getAsJsonObject().has("code")
                    || jsonElement.getAsJsonObject().get("code").getAsInt() != 0) {
                throw new ExternalApiException("获取" + code + "历史数据失败: " + jsonElement);
            }

            JsonElement data = jsonElement.getAsJsonObject().get("data");
            if (data == null || !data.isJsonObject()) {
                throw new ExternalApiException("获取" + code + "历史数据失败: 响应缺少data");
            }

            JsonElement codeData = data.getAsJsonObject().get(code);
            if (codeData == null || !codeData.isJsonObject()) {
                throw new DataUnavailableException("获取" + code + "历史数据失败: 缺少标的数据");
            }
            String seriesName = index || !codeData.getAsJsonObject().has("qfqday")
                    ? "day"
                    : "qfqday";
            if (!codeData.getAsJsonObject().has(seriesName)
                    || !codeData.getAsJsonObject().get(seriesName).isJsonArray()) {
                throw new DataUnavailableException(
                        "获取" + code + "历史数据失败: 缺少"
                                + (index ? "指数原始" : "ETF前复权") + "序列");
            }

            JsonArray jsonArray = codeData.getAsJsonObject().getAsJsonArray(seriesName);
            if (jsonArray.size() == 0) {
                throw new DataUnavailableException("获取" + code + "历史数据失败: 数据为空");
            }

            List<PriceData> prices = new ArrayList<>();
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");

            for (JsonElement element : jsonArray) {
                if (!element.isJsonArray() || element.getAsJsonArray().size() < 3) continue;
                JsonArray bar = element.getAsJsonArray();

                String dateStr = bar.get(0).getAsString();
                String openStr = bar.get(1).getAsString();
                String closeStr = bar.get(2).getAsString();

                try {
                    if (dateStr == null || dateStr.length() < 10) continue;
                    Date date = dateParser.parse(dateStr.substring(0, 10));

                    if (!date.after(endDate)) {
                        prices.add(new PriceData(date, new BigDecimal(openStr), new BigDecimal(closeStr)));
                    }
                } catch (ParseException | NumberFormatException e) {
                    continue;
                }
            }

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

    private boolean isIndexCode(String code) {
        return code != null && (code.startsWith("sh000") || code.startsWith("sz399"));
    }
}
