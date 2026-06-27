package com.fund.analysis.service;

import com.fund.analysis.dto.DashboardDecisionDTO;
import com.fund.analysis.dto.FundPortfolioRsiDTO;
import com.fund.analysis.dto.MaStrategyDTO;
import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.RsiDataDTO;
import com.fund.analysis.entity.FundInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 决策驾驶舱聚合服务
 */
@Service
public class DashboardDecisionService {

    /**
     * 首页日期时间格式
     */
    private static final DateTimeFormatter DASHBOARD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 市场数据服务
     */
    @Autowired
    private MarketDataService marketDataService;

    /**
     * MA策略服务
     */
    @Autowired
    private MaStrategyService maStrategyService;

    /**
     * RSI分析服务
     */
    @Autowired
    private RsiAnalysisService rsiAnalysisService;

    /**
     * 基金推荐服务
     */
    @Autowired
    private FundAnalysisService fundAnalysisService;

    /**
     * 基金黑名单服务
     */
    @Autowired
    private FundBlacklistService fundBlacklistService;

    /**
     * 基金组合服务
     */
    @Autowired
    private FundPortfolioService fundPortfolioService;

    /**
     * 蛋卷指数估值服务
     */
    @Autowired
    private DanjuanIndexValuationService danjuanIndexValuationService;

    /**
     * 获取决策驾驶舱数据
     *
     * @return 决策驾驶舱数据
     */
    public DashboardDecisionDTO getDecisionDashboard() {
        DashboardDecisionDTO result = new DashboardDecisionDTO();
        MarketOverviewDTO overview = loadMarketOverview(result);
        if (overview != null) {
            result.setUpdateTime(overview.getUpdateTime());
            loadPortfolioRsi(result, overview);
            loadEtfOpportunities(result);
            buildDecisionCards(result, overview);
            buildMetrics(result, overview);
            buildTrendPoints(result, overview);
        }
        loadMaSignals(result);
        loadFundRecommendations(result);
        loadIndexValuations(result);
        buildOperations(result);
        updateHealthMessage(result);
        return result;
    }

    /**
     * 加载市场概览
     *
     * @param result 聚合结果
     * @return 市场概览
     */
    private MarketOverviewDTO loadMarketOverview(DashboardDecisionDTO result) {
        try {
            return marketDataService.getCoreMarketOverview();
        } catch (RuntimeException e) {
            addModuleError(result, "市场概览", e.getMessage());
            return null;
        }
    }

    /**
     * 加载组合RSI
     *
     * @param result 聚合结果
     * @param overview 市场概览
     */
    private void loadPortfolioRsi(DashboardDecisionDTO result, MarketOverviewDTO overview) {
        try {
            Map<String, Object> portfolioSummary = fundPortfolioService.getPortfolioRsiSummary();
            FundPortfolioRsiDTO portfolioRsi = new FundPortfolioRsiDTO();
            portfolioRsi.setRsi14(formatPortfolioRsiValue(portfolioSummary.get("rsi14"), "rsi14"));
            portfolioRsi.setRsi90(formatPortfolioRsiValue(portfolioSummary.get("rsi90"), "rsi90"));
            portfolioRsi.setWeeklyRsi14(formatPortfolioRsiValue(portfolioSummary.get("weeklyRsi14"), "weeklyRsi14"));
            overview.setFundPortfolioRsi(portfolioRsi);
        } catch (RuntimeException e) {
            addModuleError(result, "组合RSI", e.getMessage());
        }
    }

    /**
     * 加载ETF机会
     *
     * @param result 聚合结果
     */
    private void loadEtfOpportunities(DashboardDecisionDTO result) {
        try {
            result.getEtfOpportunities().addAll(nonNullList(rsiAnalysisService.getEtfBuySignals()));
        } catch (RuntimeException e) {
            addModuleError(result, "ETF机会", e.getMessage());
        }
    }

    /**
     * 加载MA买卖信号
     *
     * @param result 聚合结果
     */
    private void loadMaSignals(DashboardDecisionDTO result) {
        try {
            result.getMaSignals().addAll(nonNullList(maStrategyService.getMaActionSignals()));
        } catch (RuntimeException e) {
            addModuleError(result, "MA信号", e.getMessage());
        }
    }

    /**
     * 加载基金推荐
     *
     * @param result 聚合结果
     */
    private void loadFundRecommendations(DashboardDecisionDTO result) {
        try {
            for (FundInfo fund : nonNullList(fundAnalysisService.getFundRecommendations())) {
                DashboardDecisionDTO.FundRecommendationDTO item = new DashboardDecisionDTO.FundRecommendationDTO();
                item.setFundCode(fund.getFundCode());
                item.setFundName(fund.getFundName());
                item.setConditionId("");
                item.setDataTime(formatFundDataTime(fund.getDataTime()));
                item.setIsHolding(fund.getIsHolding());
                item.setBlacklisted(fundBlacklistService.isBlacklisted(fund.getFundCode()));
                item.setTag(resolveFundTag(item));
                result.getFundRecommendations().add(item);
            }
        } catch (RuntimeException e) {
            addModuleError(result, "基金推荐", e.getMessage());
        }
    }

    /**
     * 加载指数估值
     *
     * @param result 聚合结果
     */
    private void loadIndexValuations(DashboardDecisionDTO result) {
        try {
            result.getIndexValuations().add(danjuanIndexValuationService.getCachedNasdaq100Valuation());
        } catch (RuntimeException e) {
            addModuleError(result, "指数估值", e.getMessage());
        }
    }

    /**
     * 构建今日决策卡片
     *
     * @param result 聚合结果
     * @param overview 市场概览
     */
    private void buildDecisionCards(DashboardDecisionDTO result, MarketOverviewDTO overview) {
        int buyCount = result.getEtfOpportunities().size();
        result.getDecisions().add(createDecision("buy", "买入机会", String.valueOf(buyCount), "ETF信号",
                "关注RSI与MA共振标的", buyCount > 0 ? "success" : "info"));
        result.getDecisions().add(createDecision("rebalance", "再平衡建议",
                marketDataService.formatStockBondRatio(overview.getBalanceSuggestion()), "股 / 债",
                "根据90日RSI调整配置", "warning"));
        result.getDecisions().add(createDecision("risk", "风险提示", resolveRiskText(overview.getRsi90()),
                "市场温度", "结合风险溢价与均线偏离度控制仓位", resolveRiskLevel(overview.getRsi90())));
    }

    /**
     * 构建核心指标
     *
     * @param result 聚合结果
     * @param overview 市场概览
     */
    private void buildMetrics(DashboardDecisionDTO result, MarketOverviewDTO overview) {
        result.getMetrics().add(createMetric("rsi14", "14日RSI", overview.getRsi14(), "短期温度",
                "flat", resolveRiskLevel(overview.getRsi14())));
        result.getMetrics().add(createMetric("rsi90", "90日RSI", overview.getRsi90(), "中期温度",
                "flat", resolveRiskLevel(overview.getRsi90())));
        String portfolioRsi = overview.getFundPortfolioRsi() == null ? null : overview.getFundPortfolioRsi().getRsi14();
        result.getMetrics().add(createMetric("portfolioRsi", "组合RSI", portfolioRsi, "持仓组合",
                "flat", portfolioRsi == null ? "neutral" : resolveRiskLevel(portfolioRsi)));
        result.getMetrics().add(createMetric("balance", "股债配置",
                marketDataService.formatStockBondRatio(overview.getBalanceSuggestion()), "股 / 债", "flat", "neutral"));
        result.getMetrics().add(createMetric("riskPremium", "风险溢价", valueOrDash(overview.getRiskPremium()),
                "沪深300", "flat", "info"));
        result.getMetrics().add(createMetric("ma5yDeviation", "5年均线偏离度", valueOrDash(overview.getMa5yDeviation()),
                "1250日均线", "flat", "info"));
    }

    /**
     * 构建市场温度趋势点
     *
     * @param result 聚合结果
     * @param overview 市场概览
     */
    private void buildTrendPoints(DashboardDecisionDTO result, MarketOverviewDTO overview) {
        DashboardDecisionDTO.TrendPointDTO point = new DashboardDecisionDTO.TrendPointDTO();
        point.setDate(resolveTrendDate(overview.getUpdateTime()));
        point.setRsi14(parseBigDecimal(overview.getRsi14()));
        point.setRsi90(parseBigDecimal(overview.getRsi90()));
        point.setPortfolioRsi(overview.getFundPortfolioRsi() == null
                ? null
                : parseBigDecimal(overview.getFundPortfolioRsi().getRsi14()));
        result.getTrendPoints().add(point);
    }

    /**
     * 构建操作队列
     *
     * @param result 聚合结果
     */
    private void buildOperations(DashboardDecisionDTO result) {
        result.getOperations().add(createOperation("rsi-backtest", "执行RSI回测",
                "基于当前RSI阈值做区间分析", "/rsi-backtest", null, false));
        result.getOperations().add(createOperation("momentum", "查看动量轮动",
                "查看21日动量最新结果", "/momentum-strategy", null, false));
        result.getOperations().add(createOperation("portfolio-weight", "编辑组合权重",
                "调整基金组合配置比例", "/fund-portfolio", null, false));
        result.getOperations().add(createOperation("send-email", "发送日报",
                "发送今日投资分析日报", null, "sendEmailNow", true));
    }

    /**
     * 新增模块错误
     *
     * @param result 聚合结果
     * @param module 模块名称
     * @param message 错误消息
     */
    private void addModuleError(DashboardDecisionDTO result, String module, String message) {
        DashboardDecisionDTO.ModuleErrorDTO error = new DashboardDecisionDTO.ModuleErrorDTO();
        error.setModule(module);
        error.setMessage(message == null || message.trim().isEmpty() ? "未知错误" : message);
        result.getDataStatus().getModuleErrors().add(error);
    }

    /**
     * 更新健康状态说明
     *
     * @param result 聚合结果
     */
    private void updateHealthMessage(DashboardDecisionDTO result) {
        if (result.getDataStatus().getModuleErrors().isEmpty()) {
            result.getDataStatus().setStatus("normal");
            result.getDataStatus().setMessage("数据正常");
        } else if (result.getDecisions().isEmpty() && result.getMetrics().isEmpty()) {
            result.getDataStatus().setStatus("error");
            result.getDataStatus().setMessage("首页核心数据不可用");
        } else {
            result.getDataStatus().setStatus("partial");
            result.getDataStatus().setMessage("部分数据加载失败");
        }
    }

    /**
     * 创建今日决策卡片
     *
     * @param key 卡片键
     * @param title 标题
     * @param value 主值
     * @param subtitle 副标题
     * @param description 说明
     * @param level 级别
     * @return 今日决策卡片
     */
    private DashboardDecisionDTO.DecisionCardDTO createDecision(String key, String title, String value,
                                                                String subtitle, String description, String level) {
        DashboardDecisionDTO.DecisionCardDTO card = new DashboardDecisionDTO.DecisionCardDTO();
        card.setKey(key);
        card.setTitle(title);
        card.setValue(valueOrDash(value));
        card.setSubtitle(subtitle);
        card.setDescription(description);
        card.setLevel(level);
        return card;
    }

    /**
     * 创建指标卡片
     *
     * @param key 指标键
     * @param label 指标名称
     * @param value 指标值
     * @param helper 补充说明
     * @param trend 趋势方向
     * @param level 状态级别
     * @return 指标卡片
     */
    private DashboardDecisionDTO.MetricDTO createMetric(String key, String label, String value,
                                                        String helper, String trend, String level) {
        DashboardDecisionDTO.MetricDTO metric = new DashboardDecisionDTO.MetricDTO();
        metric.setKey(key);
        metric.setLabel(label);
        metric.setValue(valueOrDash(value));
        metric.setHelper(helper);
        metric.setTrend(trend);
        metric.setLevel(level);
        return metric;
    }

    /**
     * 创建操作入口
     *
     * @param key 操作键
     * @param title 操作标题
     * @param description 操作说明
     * @param targetPath 前端目标路由
     * @param action API动作键
     * @param danger 是否为风险操作
     * @return 操作入口
     */
    private DashboardDecisionDTO.OperationDTO createOperation(String key, String title, String description,
                                                              String targetPath, String action, boolean danger) {
        DashboardDecisionDTO.OperationDTO operation = new DashboardDecisionDTO.OperationDTO();
        operation.setKey(key);
        operation.setTitle(title);
        operation.setDescription(description);
        operation.setTargetPath(targetPath);
        operation.setAction(action);
        operation.setDanger(danger);
        return operation;
    }

    /**
     * 解析基金标签
     *
     * @param fund 基金推荐行
     * @return 基金标签
     */
    private String resolveFundTag(DashboardDecisionDTO.FundRecommendationDTO fund) {
        if (fund.isBlacklisted()) {
            return "已排除";
        }
        if (fund.getIsHolding() != null && fund.getIsHolding() == 1) {
            return "已持有";
        }
        return "推荐";
    }

    /**
     * 格式化基金数据时间
     *
     * @param dataTime 基金数据时间
     * @return 页面展示时间
     */
    private String formatFundDataTime(Date dataTime) {
        if (dataTime == null) {
            return null;
        }
        return DASHBOARD_TIME_FORMATTER.format(dataTime.toInstant().atZone(ZoneId.systemDefault()));
    }

    /**
     * 解析风险文本
     *
     * @param value RSI文本
     * @return 风险文本
     */
    private String resolveRiskText(String value) {
        BigDecimal number = parseBigDecimal(value);
        if (number == null) {
            return "未知";
        }
        if (number.compareTo(new BigDecimal("70")) >= 0) {
            return "偏高";
        }
        if (number.compareTo(new BigDecimal("30")) <= 0) {
            return "偏低";
        }
        return "中性";
    }

    /**
     * 解析风险级别
     *
     * @param value RSI文本
     * @return 风险级别
     */
    private String resolveRiskLevel(String value) {
        BigDecimal number = parseBigDecimal(value);
        if (number == null) {
            return "neutral";
        }
        if (number.compareTo(new BigDecimal("70")) >= 0) {
            return "danger";
        }
        if (number.compareTo(new BigDecimal("57")) >= 0) {
            return "warning";
        }
        if (number.compareTo(new BigDecimal("30")) <= 0) {
            return "success";
        }
        return "info";
    }

    /**
     * 解析趋势日期
     *
     * @param updateTime 更新时间
     * @return 趋势日期
     */
    private String resolveTrendDate(String updateTime) {
        if (updateTime == null || updateTime.trim().isEmpty()) {
            return "";
        }
        return updateTime.trim().split("\\s+")[0];
    }

    /**
     * 格式化组合 RSI 数值
     *
     * @param value RSI原始值
     * @param fieldName 字段名
     * @return 两位小数字符串
     */
    private String formatPortfolioRsiValue(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue())
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();
        }
        if (value instanceof String) {
            return new BigDecimal(((String) value).trim())
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();
        }
        throw new IllegalArgumentException("组合RSI字段类型不支持: " + fieldName);
    }

    /**
     * 解析数字
     *
     * @param value 数字文本
     * @return 数字
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(value.replace("%", "").trim());
    }

    /**
     * 空值转横线
     *
     * @param value 文本值
     * @return 展示文本
     */
    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    /**
     * 转成非空列表
     *
     * @param list 原始列表
     * @param <T> 元素类型
     * @return 非空列表
     */
    private <T> List<T> nonNullList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
