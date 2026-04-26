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
import com.google.gson.JsonObject;
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

        int extraDays = rsiPeriod + 30;
        List<PriceData> allPrices = getHistoricalPrices(etfCode, startDate, endDate, extraDays);
        if (allPrices.size() < rsiPeriod + 10) {
            throw new DataUnavailableException("RSI回测历史数据不足: " + etfCode + ", 数据量=" + allPrices.size());
        }

        List<BigDecimal> allCloses = new ArrayList<>();
        for (PriceData pd : allPrices) {
            allCloses.add(pd.getPrice());
        }
        List<BigDecimal> rsiValues = RsiCalculator.calculateRSI(allCloses, rsiPeriod);

        int backtestStartIdx = 0;
        for (int i = 0; i < allPrices.size(); i++) {
            if (!allPrices.get(i).getDate().before(startDate)) {
                backtestStartIdx = i;
                break;
            }
        }

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

        for (int i = Math.max(backtestStartIdx, rsiPeriod); i < allPrices.size(); i++) {
            PriceData current = allPrices.get(i);
            if (current.getDate().after(endDate)) break;

            BigDecimal price = current.getPrice();
            int rsiIdx = i - 1;
            if (rsiIdx < 0 || rsiIdx >= rsiValues.size()) continue;
            BigDecimal rsi = rsiValues.get(rsiIdx);
            if (rsi.compareTo(BigDecimal.ZERO) == 0) continue;

            boolean isBuySignal = rsi.compareTo(rsiBuyThreshold) <= 0;
            boolean isSellSignal = rsi.compareTo(rsiSellThreshold) >= 0;

            BigDecimal actualBuyAmount = fixedAmountPerTrade.min(cash);

            if (isBuySignal && actualBuyAmount.compareTo(price) >= 0) {
                long quantity = actualBuyAmount.divide(price, 0, RoundingMode.DOWN).longValue();
                if (quantity > 0) {
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
                    totalCost = totalCost.add(amount);
                    holdingQuantity += quantity;
                    cash = cash.subtract(amount);
                    totalInvested = totalInvested.add(amount);

                    BigDecimal avgCost = totalCost.divide(BigDecimal.valueOf(holdingQuantity), 4, RoundingMode.HALF_UP);
                    BigDecimal totalValue = cash.add(price.multiply(BigDecimal.valueOf(holdingQuantity)));

                    RsiBacktestDTO.BacktestTransaction tx = new RsiBacktestDTO.BacktestTransaction();
                    tx.setDate(dateOnlyFormat.format(current.getDate()));
                    tx.setType("BUY");
                    tx.setPrice(price);
                    tx.setQuantity(quantity);
                    tx.setAmount(amount);
                    tx.setTotalValue(totalValue);
                    tx.setRsiValue(rsi);
                    tx.setProfit(BigDecimal.ZERO);
                    tx.setHoldingQuantityAfter(holdingQuantity);
                    tx.setAvgCostAfter(avgCost);
                    tx.setCashAfter(cash);
                    tx.setSignalDescription("RSI(" + rsiPeriod + ")=" + rsi.setScale(2, RoundingMode.HALF_UP)
                            + " ≤ " + rsiBuyThreshold + "，定额买入" + amount.setScale(2, RoundingMode.HALF_UP));
                    transactions.add(tx);
                    buyCount++;
                }
            } else if (isSellSignal && holdingQuantity > 0) {
                long sellQuantity = fixedAmountPerTrade.divide(price, 0, RoundingMode.DOWN).longValue();
                sellQuantity = Math.min(sellQuantity, holdingQuantity);

                if (sellQuantity > 0) {
                    BigDecimal avgCost = totalCost.divide(BigDecimal.valueOf(holdingQuantity), 4, RoundingMode.HALF_UP);
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(sellQuantity));
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
                    BigDecimal totalValue = cash.add(price.multiply(BigDecimal.valueOf(holdingQuantity)));

                    RsiBacktestDTO.BacktestTransaction tx = new RsiBacktestDTO.BacktestTransaction();
                    tx.setDate(dateOnlyFormat.format(current.getDate()));
                    tx.setType("SELL");
                    tx.setPrice(price);
                    tx.setQuantity(sellQuantity);
                    tx.setAmount(amount);
                    tx.setTotalValue(totalValue);
                    tx.setRsiValue(rsi);
                    tx.setProfit(profit);
                    tx.setHoldingQuantityAfter(holdingQuantity);
                    tx.setAvgCostAfter(newAvgCost);
                    tx.setCashAfter(cash);
                    tx.setSignalDescription("RSI(" + rsiPeriod + ")=" + rsi.setScale(2, RoundingMode.HALF_UP)
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
                totalValue = totalValue.add(price.multiply(BigDecimal.valueOf(holdingQuantity)));
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
            dv.setPrice(price);
            dv.setTotalValue(totalValue);
            dv.setRsiValue(rsi);
            BigDecimal returnRate = totalValue.subtract(initialCapital)
                    .divide(initialCapital, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dv.setReturnRate(returnRate);
            dailyValues.add(dv);
        }

        BigDecimal finalCapital = cash;
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
        private final BigDecimal price;

        PriceData(Date date, BigDecimal price) {
            this.date = date;
            this.price = price;
        }

        Date getDate() { return date; }
        BigDecimal getPrice() { return price; }
    }

    private List<PriceData> getHistoricalPrices(String code, Date startDate, Date endDate, int extraDays) {
        try {
            long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24) + extraDays + 50;
            int dataLen = (int) Math.min(days, 1950);

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
            if (jsonArray.size() == 0) {
                throw new DataUnavailableException("获取" + code + "历史数据失败: 数据为空");
            }

            List<PriceData> prices = new ArrayList<>();
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");

            for (JsonElement element : jsonArray) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("day") || !obj.has("close")) continue;

                String dateStr = obj.get("day").getAsString();
                String closeStr = obj.get("close").getAsString();

                try {
                    if (dateStr == null || dateStr.length() < 10) continue;
                    Date date = dateParser.parse(dateStr.substring(0, 10));

                    Date extendedStart = new Date(startDate.getTime() - (long) extraDays * 24 * 60 * 60 * 1000);
                    if (!date.before(extendedStart) && !date.after(endDate)) {
                        prices.add(new PriceData(date, new BigDecimal(closeStr)));
                    }
                } catch (ParseException e) {
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
}
