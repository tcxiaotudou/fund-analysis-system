package com.fund.analysis.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 决策驾驶舱数据传输对象
 */
@Data
public class DashboardDecisionDTO {

    /**
     * 数据健康状态
     */
    private DataStatusDTO dataStatus = new DataStatusDTO();

    /**
     * 今日决策卡片
     */
    private List<DecisionCardDTO> decisions = new ArrayList<>();

    /**
     * 核心指标卡片
     */
    private List<MetricDTO> metrics = new ArrayList<>();

    /**
     * 市场温度图表点
     */
    private List<TrendPointDTO> trendPoints = new ArrayList<>();

    /**
     * 操作队列
     */
    private List<OperationDTO> operations = new ArrayList<>();

    /**
     * ETF机会列表
     */
    private List<RsiDataDTO> etfOpportunities = new ArrayList<>();

    /**
     * MA买卖信号列表
     */
    private List<MaStrategyDTO> maSignals = new ArrayList<>();

    /**
     * 基金推荐列表
     */
    private List<FundRecommendationDTO> fundRecommendations = new ArrayList<>();

    /**
     * 数据更新时间
     */
    private String updateTime;

    /**
     * 数据健康状态
     */
    @Data
    public static class DataStatusDTO {

        /**
         * 状态，normal表示正常，partial表示部分失败，error表示不可用
         */
        private String status = "normal";

        /**
         * 状态说明
         */
        private String message = "数据正常";

        /**
         * 模块级错误
         */
        private List<ModuleErrorDTO> moduleErrors = new ArrayList<>();
    }

    /**
     * 模块错误
     */
    @Data
    public static class ModuleErrorDTO {

        /**
         * 模块名称
         */
        private String module;

        /**
         * 错误消息
         */
        private String message;
    }

    /**
     * 今日决策卡片
     */
    @Data
    public static class DecisionCardDTO {

        /**
         * 卡片键
         */
        private String key;

        /**
         * 标题
         */
        private String title;

        /**
         * 主值
         */
        private String value;

        /**
         * 副标题
         */
        private String subtitle;

        /**
         * 说明
         */
        private String description;

        /**
         * 级别，success、warning、danger、info
         */
        private String level;
    }

    /**
     * 核心指标
     */
    @Data
    public static class MetricDTO {

        /**
         * 指标键
         */
        private String key;

        /**
         * 指标名称
         */
        private String label;

        /**
         * 指标值
         */
        private String value;

        /**
         * 指标补充说明
         */
        private String helper;

        /**
         * 趋势方向，up、down、flat
         */
        private String trend;

        /**
         * 状态级别，success、warning、danger、info、neutral
         */
        private String level;
    }

    /**
     * 趋势图表点
     */
    @Data
    public static class TrendPointDTO {

        /**
         * 日期标签
         */
        private String date;

        /**
         * 14日RSI
         */
        private BigDecimal rsi14;

        /**
         * 90日RSI
         */
        private BigDecimal rsi90;

        /**
         * 组合RSI
         */
        private BigDecimal portfolioRsi;
    }

    /**
     * 操作入口
     */
    @Data
    public static class OperationDTO {

        /**
         * 操作键
         */
        private String key;

        /**
         * 操作标题
         */
        private String title;

        /**
         * 操作说明
         */
        private String description;

        /**
         * 前端目标路由
         */
        private String targetPath;

        /**
         * API动作键
         */
        private String action;

        /**
         * 是否为风险操作
         */
        private boolean danger;

        /**
         * 获取是否为风险操作
         *
         * @return 是否为风险操作
         */
        public boolean getDanger() {
            return danger;
        }
    }

    /**
     * 基金推荐行
     */
    @Data
    public static class FundRecommendationDTO {

        /**
         * 基金代码
         */
        private String fundCode;

        /**
         * 基金名称
         */
        private String fundName;

        /**
         * 14日RSI，当前接口无值时保持null
         */
        private BigDecimal rsi14;

        /**
         * 推荐条件ID
         */
        private String conditionId;

        /**
         * 标签
         */
        private String tag;

        /**
         * 是否持有
         */
        private Integer isHolding;

        /**
         * 是否排除
         */
        private boolean blacklisted;
    }
}
