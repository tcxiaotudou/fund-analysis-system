package com.fund.analysis.service;

import com.fund.analysis.exception.DataUnavailableException;

import java.math.BigDecimal;
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

        Set<String> commonDates = null;
        for (FundPriceSeries series : seriesList) {
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
        for (String date : dates) {
            BigDecimal weightedPrice = BigDecimal.ZERO;
            for (int i = 0; i < seriesList.size(); i++) {
                BigDecimal price = seriesList.get(i).getPricesByDate().get(date);
                BigDecimal weight = weights.get(i);
                weightedPrice = weightedPrice.add(price.multiply(weight));
            }
            prices.add(weightedPrice);
        }

        return new AlignedPortfolioPrices(dates, prices);
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
