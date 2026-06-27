package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.fund.analysis.dto.DashboardDecisionDTO;
import com.fund.analysis.entity.IndexValuation;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.mapper.IndexValuationMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 蛋卷指数估值服务
 */
@Service
public class DanjuanIndexValuationService {

    /**
     * 纳指100估值接口
     */
    private static final String NASDAQ_100_VALUATION_URL = "https://danjuanfunds.com/djapi/index_eva/detail/NDX";

    /**
     * 纳指100估值详情页
     */
    private static final String NASDAQ_100_REFERER = "https://danjuanfunds.com/dj-valuation-table-detail/NDX";

    /**
     * 浏览器用户代理
     */
    private static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    /**
     * 外部接口客户端
     */
    private final ExternalApiClient externalApiClient;

    /**
     * 指数估值缓存数据访问
     */
    private final IndexValuationMapper indexValuationMapper;

    /**
     * 创建蛋卷指数估值服务
     *
     * @param externalApiClient 外部接口客户端
     * @param indexValuationMapper 指数估值缓存数据访问
     */
    public DanjuanIndexValuationService(ExternalApiClient externalApiClient, IndexValuationMapper indexValuationMapper) {
        this.externalApiClient = externalApiClient;
        this.indexValuationMapper = indexValuationMapper;
    }

    /**
     * 刷新纳指100估值缓存
     *
     * @return 纳指100估值
     */
    @Transactional
    public DashboardDecisionDTO.IndexValuationDTO refreshNasdaq100Valuation() {
        JsonObject root = externalApiClient.getJson(NASDAQ_100_VALUATION_URL, buildDanjuanHeaders()).getAsJsonObject();
        if (!root.has("result_code") || root.get("result_code").getAsInt() != 0) {
            throw new ExternalApiException("蛋卷纳指100估值接口返回失败: " + root);
        }
        JsonObject data = requireObject(root, "data");
        IndexValuation valuation = parseValuation(data);
        indexValuationMapper.insert(valuation);
        indexValuationMapper.deleteOldData(valuation.getIndexCode(), 1);
        return toDashboardDTO(valuation);
    }

    /**
     * 获取已缓存的纳指100估值
     *
     * @return 纳指100估值
     */
    public DashboardDecisionDTO.IndexValuationDTO getCachedNasdaq100Valuation() {
        IndexValuation valuation = indexValuationMapper.selectLatestByIndexCode("NDX");
        if (valuation == null) {
            throw new DataUnavailableException("纳指100估值缓存不存在，请先刷新指数估值");
        }
        return toDashboardDTO(valuation);
    }

    /**
     * 解析指数估值缓存
     *
     * @param data 蛋卷接口数据
     * @return 指数估值缓存
     */
    private IndexValuation parseValuation(JsonObject data) {
        IndexValuation valuation = new IndexValuation();
        valuation.setIndexCode(requireString(data, "index_code"));
        valuation.setName(requireString(data, "name"));
        valuation.setHistoryLowText("比过去" + formatRatioPercent(data, "pe_over_history") + "的时间低");
        applyValuationType(valuation, requireString(data, "eva_type"));
        valuation.setPeDate(requireString(data, "date"));
        valuation.setPe(formatDecimal(data, "pe"));
        valuation.setPePercentile(formatRatioPercent(data, "pe_percentile"));
        valuation.setDataTime(new Date());
        valuation.setCreateTime(new Date());
        return valuation;
    }

    /**
     * 转换首页估值DTO
     *
     * @param valuation 指数估值缓存
     * @return 首页估值DTO
     */
    private DashboardDecisionDTO.IndexValuationDTO toDashboardDTO(IndexValuation valuation) {
        DashboardDecisionDTO.IndexValuationDTO dto = new DashboardDecisionDTO.IndexValuationDTO();
        dto.setIndexCode(valuation.getIndexCode());
        dto.setName(valuation.getName());
        dto.setHistoryLowText(valuation.getHistoryLowText());
        dto.setValuationLabel(valuation.getValuationLabel());
        dto.setLevel(valuation.getLevel());
        dto.setPeDate(valuation.getPeDate());
        dto.setPe(valuation.getPe());
        dto.setPePercentile(valuation.getPePercentile());
        return dto;
    }

    /**
     * 构建蛋卷请求头
     *
     * @return 请求头
     */
    private Map<String, String> buildDanjuanHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", BROWSER_USER_AGENT);
        headers.put("Referer", NASDAQ_100_REFERER);
        headers.put("Accept", "application/json,text/plain,*/*");
        return headers;
    }

    /**
     * 应用估值状态
     *
     * @param valuation 估值缓存
     * @param valuationType 估值状态
     */
    private void applyValuationType(IndexValuation valuation, String valuationType) {
        if ("high".equals(valuationType)) {
            valuation.setValuationLabel("估值偏高");
            valuation.setLevel("warning");
            return;
        }
        if ("low".equals(valuationType)) {
            valuation.setValuationLabel("估值偏低");
            valuation.setLevel("success");
            return;
        }
        if ("normal".equals(valuationType) || "middle".equals(valuationType)) {
            valuation.setValuationLabel("估值适中");
            valuation.setLevel("info");
            return;
        }
        valuation.setValuationLabel("估值状态未知");
        valuation.setLevel("neutral");
    }

    /**
     * 读取必需对象字段
     *
     * @param root JSON对象
     * @param field 字段名
     * @return 字段对象
     */
    private JsonObject requireObject(JsonObject root, String field) {
        JsonElement element = requireElement(root, field);
        if (!element.isJsonObject()) {
            throw new ExternalApiException("蛋卷纳指100估值接口字段不是对象: " + field);
        }
        return element.getAsJsonObject();
    }

    /**
     * 读取必需文本字段
     *
     * @param root JSON对象
     * @param field 字段名
     * @return 字段文本
     */
    private String requireString(JsonObject root, String field) {
        return requireElement(root, field).getAsString();
    }

    /**
     * 格式化小数
     *
     * @param root JSON对象
     * @param field 字段名
     * @return 两位小数字符串
     */
    private String formatDecimal(JsonObject root, String field) {
        return readDecimal(root, field).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 格式化比例百分比
     *
     * @param root JSON对象
     * @param field 字段名
     * @return 百分比字符串
     */
    private String formatRatioPercent(JsonObject root, String field) {
        return readDecimal(root, field).multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    /**
     * 读取必需数字字段
     *
     * @param root JSON对象
     * @param field 字段名
     * @return 数字值
     */
    private BigDecimal readDecimal(JsonObject root, String field) {
        return new BigDecimal(requireElement(root, field).getAsString());
    }

    /**
     * 读取必需字段
     *
     * @param root JSON对象
     * @param field 字段名
     * @return 字段值
     */
    private JsonElement requireElement(JsonObject root, String field) {
        if (!root.has(field) || root.get(field).isJsonNull()) {
            throw new ExternalApiException("蛋卷纳指100估值接口缺少字段: " + field);
        }
        return root.get(field);
    }
}
