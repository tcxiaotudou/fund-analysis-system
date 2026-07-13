package com.fund.analysis.service;

import com.fund.analysis.exception.DataUnavailableException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 基金组合价格日期对齐工具
 */
class PortfolioPriceAligner {

    /**
     * 按共同交易日对齐基金价格并计算组合加权价格
     *
     * @param seriesList 基金价格序列列表
     * @param weights 权重列表
     * @return 对齐后的组合价格
     */
    static AlignedPortfolioPrices align(List<FundPriceSeries> seriesList, List<BigDecimal> weights) {
        if (seriesList == null || seriesList.isEmpty()) {
            throw new DataUnavailableException("基金价格序列为空，无法计算组合价格");
        }
        if (weights == null || weights.size() != seriesList.size()) {
            throw new DataUnavailableException("基金权重数量与价格序列数量不一致");
        }
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (BigDecimal weight : weights) {
            if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0) {
                throw new DataUnavailableException("基金权重不能为负数或空值");
            }
            totalWeight = totalWeight.add(weight);
        }
        if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
            throw new DataUnavailableException("基金组合权重总和必须等于100");
        }

        Set<String> commonDates = null;
        for (int index = 0; index < seriesList.size(); index++) {
            if (weights.get(index).compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            FundPriceSeries series = seriesList.get(index);
            if (series == null) {
                throw new DataUnavailableException("正权重基金价格序列为空");
            }
            if (series.getPricesByDate().isEmpty()) {
                throw new DataUnavailableException("基金净值数据为空: " + series.getFundCode());
            }
            Set<String> fundDates = new TreeSet<>(series.getPricesByDate().keySet());
            if (commonDates == null) {
                commonDates = fundDates;
            } else {
                commonDates.retainAll(fundDates);
            }
        }

        if (commonDates == null || commonDates.isEmpty()) {
            throw new DataUnavailableException("持有基金没有共同交易日，无法计算组合RSI");
        }

        List<String> dates = new ArrayList<>(commonDates);
        Collections.sort(dates);

        List<BigDecimal> prices = new ArrayList<>();
        prices.add(new BigDecimal("100"));
        validatePositivePrices(seriesList, weights, dates.get(0));
        for (int dateIndex = 1; dateIndex < dates.size(); dateIndex++) {
            String previousDate = dates.get(dateIndex - 1);
            String currentDate = dates.get(dateIndex);
            BigDecimal weightedReturn = BigDecimal.ZERO;
            for (int i = 0; i < seriesList.size(); i++) {
                BigDecimal weight = weights.get(i);
                if (weight.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                FundPriceSeries series = seriesList.get(i);
                BigDecimal previousPrice = requirePositivePrice(series, previousDate);
                BigDecimal currentPrice = requirePositivePrice(series, currentDate);
                BigDecimal fundReturn = currentPrice.divide(
                                previousPrice, 12, RoundingMode.HALF_UP)
                        .subtract(BigDecimal.ONE);
                weightedReturn = weightedReturn.add(
                        fundReturn.multiply(weight.movePointLeft(2)));
            }
            prices.add(prices.get(dateIndex - 1).multiply(
                    BigDecimal.ONE.add(weightedReturn)));
        }

        return new AlignedPortfolioPrices(dates, prices);
    }

    private static void validatePositivePrices(
            List<FundPriceSeries> seriesList,
            List<BigDecimal> weights,
            String date) {
        for (int index = 0; index < seriesList.size(); index++) {
            if (weights.get(index).compareTo(BigDecimal.ZERO) > 0) {
                requirePositivePrice(seriesList.get(index), date);
            }
        }
    }

    private static BigDecimal requirePositivePrice(FundPriceSeries series, String date) {
        BigDecimal price = series.getPricesByDate().get(date);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DataUnavailableException("基金净值必须大于0: " + series.getFundCode());
        }
        return price;
    }

    /**
     * 将 RSI 结果按尾部价格日期对齐。
     *
     * @param dates 价格日期列表
     * @param rsiValueCount RSI 结果数量
     * @return 与 RSI 结果一一对应的日期列表
     */
    static List<String> alignDatesToRsiValues(List<String> dates, int rsiValueCount) {
        if (dates == null || dates.isEmpty()) {
            throw new DataUnavailableException("价格日期为空，无法对齐组合RSI日期");
        }
        if (rsiValueCount < 0 || rsiValueCount > dates.size()) {
            throw new DataUnavailableException("组合RSI数量与价格日期数量不一致");
        }
        if (rsiValueCount == 0) {
            return new ArrayList<>();
        }

        int startIndex = dates.size() - rsiValueCount;
        return new ArrayList<>(dates.subList(startIndex, dates.size()));
    }

    /**
     * 按 ISO 自然周提取每周最后一个有效交易日的组合价格。
     * 最后一周尚未结束时，使用当前已有的最后一个交易日。
     *
     * @param alignedPrices 已按日期升序对齐的组合价格
     * @return 周收盘价格序列
     */
    static List<BigDecimal> extractWeeklyClosingPrices(
            AlignedPortfolioPrices alignedPrices) {
        if (alignedPrices == null
                || alignedPrices.getDates().size() != alignedPrices.getPrices().size()) {
            throw new DataUnavailableException("组合价格与日期数量不一致");
        }
        Map<String, BigDecimal> weeklyCloses = new LinkedHashMap<>();
        WeekFields isoWeek = WeekFields.ISO;
        for (int i = 0; i < alignedPrices.getDates().size(); i++) {
            String rawDate = alignedPrices.getDates().get(i);
            try {
                LocalDate date = LocalDate.parse(rawDate);
                String weekKey = date.get(isoWeek.weekBasedYear())
                        + "-" + date.get(isoWeek.weekOfWeekBasedYear());
                weeklyCloses.put(weekKey, alignedPrices.getPrices().get(i));
            } catch (DateTimeParseException exception) {
                throw new DataUnavailableException("基金净值日期格式无效: " + rawDate);
            }
        }
        return new ArrayList<>(weeklyCloses.values());
    }

    /**
     * 单只基金的日期价格序列
     */
    static class FundPriceSeries {

        /**
         * 基金代码
         */
        private final String fundCode;

        /**
         * 日期到净值的映射
         */
        private final Map<String, BigDecimal> pricesByDate;

        /**
         * 创建基金价格序列
         *
         * @param fundCode 基金代码
         * @param pricesByDate 日期价格映射
         */
        FundPriceSeries(String fundCode, Map<String, BigDecimal> pricesByDate) {
            this.fundCode = fundCode;
            this.pricesByDate = pricesByDate == null ? new HashMap<>() : new LinkedHashMap<>(pricesByDate);
        }

        /**
         * 获取基金代码
         *
         * @return 基金代码
         */
        String getFundCode() {
            return fundCode;
        }

        /**
         * 获取日期价格映射
         *
         * @return 日期价格映射
         */
        Map<String, BigDecimal> getPricesByDate() {
            return pricesByDate;
        }
    }

    /**
     * 对齐后的组合价格
     */
    static class AlignedPortfolioPrices {

        /**
         * 对齐后的日期列表
         */
        private final List<String> dates;

        /**
         * 对齐后的组合价格列表
         */
        private final List<BigDecimal> prices;

        /**
         * 创建对齐价格结果
         *
         * @param dates 日期列表
         * @param prices 组合价格列表
         */
        AlignedPortfolioPrices(List<String> dates, List<BigDecimal> prices) {
            this.dates = new ArrayList<>(dates);
            this.prices = new ArrayList<>(prices);
        }

        /**
         * 获取日期列表
         *
         * @return 日期列表
         */
        List<String> getDates() {
            return dates;
        }

        /**
         * 获取组合价格列表
         *
         * @return 组合价格列表
         */
        List<BigDecimal> getPrices() {
            return prices;
        }
    }
}
