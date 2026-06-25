package com.fund.analysis.service;

import com.fund.analysis.client.ExternalApiClient;
import com.fund.analysis.entity.FundInfo;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.ExternalApiException;
import com.fund.analysis.mapper.FundInfoMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * 基金分析服务类
 * 提供基金数据获取、筛选和推荐功能
 */
@Service
public class FundAnalysisService {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(FundAnalysisService.class);

    /**
     * 基金信息数据访问对象
     */
    @Autowired
    private FundInfoMapper fundInfoMapper;

    /**
     * 基金黑名单服务
     */
    @Autowired
    private FundBlacklistService fundBlacklistService;

    /**
     * 第三方接口客户端
     */
    @Autowired
    private ExternalApiClient externalApiClient;

    /**
     * 系统配置服务
     */
    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 短事务执行器
     */
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    /**
     * 获取基金推荐列表（从数据库读取）
     * @return 基金列表
     */
    public List<FundInfo> getFundRecommendations() {
        List<FundInfo> fundList = fundInfoMapper.selectLatestRecommendations(12);
        if (fundList == null || fundList.isEmpty()) {
            logger.warn("No fund recommendations found in database");
            return new ArrayList<>();
        }
        return fundList;
    }
    
    /**
     * 刷新基金推荐列表（从第三方API获取并保存）
     * @return 基金列表
     */
    public List<FundInfo> refreshFundRecommendations() {
        String url = "https://api.jiucaishuo.com/v2/fundchoose/result2";
        Map<String, String> payload = new HashMap<>();
        payload.put("condition_id", systemConfigService.getFundRecommendationConditionId());

        JsonObject jsonObject = externalApiClient.postJsonElement(url, payload).getAsJsonObject();
        if (jsonObject.get("code").getAsInt() != 0) {
            throw new ExternalApiException("基金推荐接口返回失败: " + jsonObject);
        }

        JsonObject dataObject = jsonObject.getAsJsonObject("data");
        FundRecommendationColumnIndexes columnIndexes = resolveFundRecommendationColumnIndexes(dataObject.getAsJsonArray("position_th"));
        JsonArray fundArray = dataObject.getAsJsonArray("position_table_data");
        List<FundInfo> fundList = new ArrayList<>();

        // 先获取所有基金信息
        for (JsonElement element : fundArray) {
            JsonObject fund = element.getAsJsonObject();
            String fundCode = fund.get("code").getAsString();
            String fundName = fund.get("name").getAsString();

            // 黑名单是用户显式配置，不属于降级逻辑
            if (fundBlacklistService.isBlacklisted(fundCode)) {
                logger.info("Fund {} - {} is blacklisted, skipping", fundCode, fundName);
                continue;
            }

            FundInfo fundInfo = getFundDetails(fundCode, fundName, fund.getAsJsonArray("list"), columnIndexes);
            if (fundInfo.getFiveYearReturn().intValue() < 10) {
                continue;
            }
            fundList.add(fundInfo);
        }

        fundList.sort(Comparator.comparing(FundInfo::getCalmarRank));

        // 按基金经理去重，保留每个基金经理收益率最高的基金
        Set<String> managerSet = new HashSet<>();
        List<FundInfo> uniqueFunds = new ArrayList<>();
        for (FundInfo fund : fundList) {
            if (!managerSet.contains(fund.getManagerName())) {
                managerSet.add(fund.getManagerName());
                uniqueFunds.add(fund);
            }
        }

        List<FundInfo> topFunds = uniqueFunds.size() > 12 ? uniqueFunds.subList(0, 12) : uniqueFunds;
        // 同一轮刷新使用同一个数据时间，查询推荐列表时以它作为批次边界。
        Date refreshTime = new Date();
        for (FundInfo fund : topFunds) {
            fund.setDataTime(refreshTime);
            fund.setUpdateTime(refreshTime);
        }

        transactionTemplate.executeWithoutResult(status -> {
            fundInfoMapper.deleteOldData(30);
            for (FundInfo fund : topFunds) {
                saveFundInfo(fund);
            }
        });

        logger.info("Refreshed {} fund recommendations", topFunds.size());
        return topFunds;
    }
    
    /**
     * 获取基金详细信息
     * @param fundCode 基金代码
     * @param fundName 基金名称
     * @param list 基金数据列表
     * @param columnIndexes 基金推荐列索引
     * @return 基金信息
     */
    private FundInfo getFundDetails(String fundCode, String fundName, JsonArray list, FundRecommendationColumnIndexes columnIndexes) {
        FundInfo fundInfo = new FundInfo();
        fundInfo.setFundCode(fundCode);
        fundInfo.setFundName(fundName);

        String managerName = getRequiredFundField(fundCode, fundName, list, columnIndexes.managerNameIndex, "基金经理");
        String managerYears = getRequiredFundField(fundCode, fundName, list, columnIndexes.managerYearsIndex, "任职年限");
        String scale = getRequiredFundField(fundCode, fundName, list, columnIndexes.scaleIndex, "基金规模");
        String yearToDateReturn = getRequiredFundField(fundCode, fundName, list, columnIndexes.yearToDateReturnIndex, "收益率（今年以来）");

        String sharpeStr = getRequiredFundField(fundCode, fundName, list, columnIndexes.sharpeRankIndex, "夏普率排序");
        Integer sharpeRank = parseRankValue(fundCode, fundName, sharpeStr, "夏普率排序");

        String calmarStr = getRequiredFundField(fundCode, fundName, list, columnIndexes.calmarRankIndex, "卡玛比率排序");
        Integer calmarRank = parseRankValue(fundCode, fundName, calmarStr, "卡玛比率排序");

        fundInfo.setManagerName(managerName);
        fundInfo.setManagerYears(managerYears);
        fundInfo.setScale(scale);
        fundInfo.setYearToDateReturn(yearToDateReturn);
        fundInfo.setSharpeRank(sharpeRank);
        fundInfo.setCalmarRank(calmarRank);
        fundInfo.setFiveYearReturn(get5YearReturn(fundCode));
        fundInfo.setRedemptionFee(getRedemptionFee(fundCode));
        fundInfo.setDataTime(new Date());
        fundInfo.setCreateTime(new Date());
        fundInfo.setUpdateTime(new Date());
        fundInfo.setIsCustom(0);

        return fundInfo;
    }

    /**
     * 解析基金推荐表头列索引
     * @param headerArray 表头数组
     * @return 基金推荐列索引
     */
    private FundRecommendationColumnIndexes resolveFundRecommendationColumnIndexes(JsonArray headerArray) {
        if (headerArray == null || headerArray.size() == 0) {
            throw new ExternalApiException("基金推荐接口返回数据缺少表头 position_th");
        }

        FundRecommendationColumnIndexes indexes = new FundRecommendationColumnIndexes();
        indexes.managerNameIndex = findFundListIndex(headerArray, "基金经理", null);
        indexes.managerYearsIndex = findFundListIndex(headerArray, "任职年限", null);
        indexes.scaleIndex = findFundListIndex(headerArray, "基金规模", null);
        indexes.yearToDateReturnIndex = findFundListIndex(headerArray, "收益率", "今年以来");
        indexes.sharpeRankIndex = findFundListIndex(headerArray, "夏普率排序", null);
        indexes.calmarRankIndex = findFundListIndex(headerArray, "卡玛比率排序", null);
        return indexes;
    }

    /**
     * 按表头名称查找基金行数据索引
     * @param headerArray 表头数组
     * @param headerName 表头名称
     * @param headerDesc 表头描述
     * @return 基金行数据索引
     */
    private int findFundListIndex(JsonArray headerArray, String headerName, String headerDesc) {
        for (int i = 0; i < headerArray.size(); i++) {
            JsonObject header = headerArray.get(i).getAsJsonObject();
            String name = getJsonString(header, "name");
            String desc = getJsonString(header, "desc");
            if (headerName.equals(name) && (headerDesc == null || headerDesc.equals(desc))) {
                // 表头第一列是基金名称，行数据 list 从第二列开始，所以索引需要减一。
                return i - 1;
            }
        }
        throw new BadRequestException("当前 condition_id 缺少基金推荐必需列: " + headerName + (headerDesc == null ? "" : "（" + headerDesc + "）"));
    }

    /**
     * 获取基金推荐字段值
     * @param fundCode 基金代码
     * @param fundName 基金名称
     * @param list 基金数据列表
     * @param index 字段索引
     * @param fieldName 字段名称
     * @return 字段值
     */
    private String getRequiredFundField(String fundCode, String fundName, JsonArray list, int index, String fieldName) {
        if (list == null) {
            throw new ExternalApiException("基金推荐接口返回数据缺少 list: " + fundCode + " - " + fundName);
        }
        if (index < 0 || index >= list.size()) {
            throw new ExternalApiException("基金推荐接口返回行数据与表头不一致: " + fundCode + " - " + fundName
                    + ", 缺少字段 " + fieldName + ", index=" + index + ", size=" + list.size());
        }

        JsonObject field = list.get(index).getAsJsonObject();
        String value = getJsonString(field, "val");
        if (value == null || value.trim().isEmpty()) {
            throw new ExternalApiException("基金推荐接口返回字段为空: " + fundCode + " - " + fundName + ", 字段=" + fieldName);
        }
        return value.trim();
    }

    /**
     * 解析排名字段
     * @param fundCode 基金代码
     * @param fundName 基金名称
     * @param rankValue 排名字段值
     * @param fieldName 字段名称
     * @return 排名数字
     */
    private Integer parseRankValue(String fundCode, String fundName, String rankValue, String fieldName) {
        try {
            return Integer.parseInt(rankValue.split("/")[0].trim());
        } catch (NumberFormatException e) {
            throw new ExternalApiException("基金推荐接口返回排名格式错误: " + fundCode + " - " + fundName
                    + ", 字段=" + fieldName + ", 值=" + rankValue, e);
        }
    }

    /**
     * 获取 JSON 字符串字段
     * @param jsonObject JSON对象
     * @param memberName 字段名称
     * @return 字符串字段值
     */
    private String getJsonString(JsonObject jsonObject, String memberName) {
        JsonElement value = jsonObject.get(memberName);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    /**
     * 基金推荐列索引
     */
    private static class FundRecommendationColumnIndexes {

        /**
         * 基金经理索引
         */
        private int managerNameIndex;

        /**
         * 任职年限索引
         */
        private int managerYearsIndex;

        /**
         * 基金规模索引
         */
        private int scaleIndex;

        /**
         * 今年以来收益率索引
         */
        private int yearToDateReturnIndex;

        /**
         * 夏普率排名索引
         */
        private int sharpeRankIndex;

        /**
         * 卡玛比率排名索引
         */
        private int calmarRankIndex;
    }
    
    /**
     * 获取5年年化收益率
     * @param fundCode 基金代码
     * @return 5年年化收益率
     */
    private BigDecimal get5YearReturn(String fundCode) {
        String url = "https://api.jiucaishuo.com/fundetail/details/earn-line";
        Map<String, Object> payload = new HashMap<>();
        payload.put("fund_code", fundCode);
        payload.put("type", "h5");
        payload.put("date", 60);
        payload.put("version", "2.5.6");

        JsonObject jsonObject = externalApiClient.postJsonElement(url, payload).getAsJsonObject();
        if (jsonObject.get("code").getAsInt() != 0) {
            throw new ExternalApiException("获取基金5年收益率失败: " + fundCode + ", response=" + jsonObject);
        }

        double yearIncome = jsonObject.getAsJsonObject("data").get("year_income").getAsDouble();
        return BigDecimal.valueOf(yearIncome);
    }
    
    /**
     * 获取基金赎回费率
     * @param fundCode 基金代码
     * @return 赎回费率信息
     */
    private String getRedemptionFee(String fundCode) {
        String url = "https://api.jiucaishuo.com/v2/fund-lists/fundrate";
        Map<String, String> payload = new HashMap<>();
        payload.put("code", fundCode);
        payload.put("type", "h5");
        payload.put("version", "2.5.7");

        JsonObject jsonObject = externalApiClient.postJsonElement(url, payload).getAsJsonObject();
        if (jsonObject.get("code").getAsInt() != 0) {
            throw new ExternalApiException("获取基金赎回费率失败: " + fundCode + ", response=" + jsonObject);
        }

        JsonArray shArray = jsonObject.getAsJsonObject("data").getAsJsonArray("sh");
        if (shArray.size() == 0) {
            return "无赎回费率信息";
        }

        List<String> rateInfo = new ArrayList<>();
        for (JsonElement element : shArray) {
            JsonObject sh = element.getAsJsonObject();
            String time = sh.get("time").getAsString();
            String rate = sh.get("rate").getAsString();
            if (!time.isEmpty() && !rate.isEmpty()) {
                rateInfo.add(time + ": " + rate);
            }
        }

        return String.join("; ", rateInfo);
    }
    
    /**
     * 保存基金信息到数据库（智能更新）
     * 如果基金已存在，更新基金信息但保留用户自定义字段（is_holding, portfolio_weight, remark等）
     * @param fundInfo 基金信息
     */
    public void saveFundInfo(FundInfo fundInfo) {
        // 查询基金是否已存在
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("fund_code", fundInfo.getFundCode());
        queryMap.put("deleted", 0);
        List<FundInfo> existingFunds = fundInfoMapper.selectByMap(queryMap);

        if (existingFunds != null && !existingFunds.isEmpty()) {
            // 基金已存在，保留用户自定义字段
            FundInfo existing = existingFunds.get(0);

            // 保留用户自定义字段
            fundInfo.setId(existing.getId());
            fundInfo.setIsHolding(existing.getIsHolding());
            fundInfo.setIsCustom(existing.getIsCustom());
            fundInfo.setPortfolioWeight(existing.getPortfolioWeight());
            fundInfo.setRemark(existing.getRemark());

            // 更新其他字段
            fundInfoMapper.updateById(fundInfo);
            logger.debug("Updated existing fund: {}", fundInfo.getFundCode());
        } else {
            // 基金不存在，插入新记录
            fundInfoMapper.insert(fundInfo);
            logger.debug("Inserted new fund: {}", fundInfo.getFundCode());
        }
    }
    
    /**
     * 更新基金持有状态
     * @param fundCode 基金代码
     * @param isHolding 是否持有：0-否 1-是
     * @return 更新成功返回true，基金不存在返回false
     */
    @Transactional
    public boolean updateHoldingStatus(String fundCode, Integer isHolding) {
        // 查询基金信息
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("fund_code", fundCode);
        List<FundInfo> funds = fundInfoMapper.selectByMap(queryMap);

        if (funds == null || funds.isEmpty()) {
            logger.warn("Fund not found: {}", fundCode);
            return false;
        }

        // 更新所有匹配的记录（通常只有一条）
        for (FundInfo fund : funds) {
            fund.setIsHolding(isHolding);
            fund.setUpdateTime(new Date());
            fundInfoMapper.updateById(fund);
        }

        logger.info("Updated holding status for fund: {} to {}", fundCode, isHolding);
        return true;
    }
    
    /**
     * 手动添加基金到持有列表
     * @param fundCode 基金代码
     * @param fundName 基金名称
     */
    @Transactional
    public void addCustomHoldingFund(String fundCode, String fundName) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("fund_code", fundCode);
        List<FundInfo> existingFunds = fundInfoMapper.selectByMap(queryMap);

        if (existingFunds != null && !existingFunds.isEmpty()) {
            // 如果基金已存在，只更新持有状态
            for (FundInfo fund : existingFunds) {
                fund.setIsHolding(1);
                fund.setUpdateTime(new Date());
                fundInfoMapper.updateById(fund);
            }
            logger.info("Updated existing fund to holding: {}", fundCode);
            return;
        }

        FundInfo newFund = new FundInfo();
        newFund.setFundCode(fundCode);
        newFund.setFundName(fundName);
        newFund.setIsHolding(1);
        newFund.setIsCustom(1);
        newFund.setDataTime(new Date());
        newFund.setCreateTime(new Date());
        newFund.setUpdateTime(new Date());
        newFund.setDeleted(0);
        newFund.setFiveYearReturn(get5YearReturn(fundCode));
        newFund.setRedemptionFee(getRedemptionFee(fundCode));

        fundInfoMapper.insert(newFund);
        logger.info("Added new holding fund: {} - {}", fundCode, fundName);
    }
}
