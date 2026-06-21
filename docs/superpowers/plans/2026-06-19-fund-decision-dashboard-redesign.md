# Fund Decision Dashboard Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将方案 1「决策驾驶舱」落地为可运行的基金/ETF 投资决策首页，并补齐首页需要的数据聚合、操作入口、错误可见性和当前代码中已发现的前端问题。

**Architecture:** 后端新增一个只读决策聚合接口，把市场概览、组合 RSI、RSI/MA 信号、基金推荐、系统状态组合成首页专用 DTO；各子模块失败时以模块级健康信息暴露，不把失败伪装为空数据。前端把首页拆成数据模型、页面容器和小组件，复用现有 Ant Design、Recharts、React Router、Axios，不引入新的 UI 框架。

**Tech Stack:** Java 8, Spring Boot 2.7, MyBatis-Plus, MySQL 8, React 18, React Router 6, Vite 4, Ant Design 5, Recharts, Axios, Node test runner.

---

## Execution Rules

- 执行前创建或切换到 `codex/fund-decision-dashboard` 分支；不要覆盖当前工作区里已经存在的用户改动。
- 遵守根目录 AGENTS 规则：新增或修改的字段、方法、关键代码只写中文注释；优先使用已有 service、mapper、utils 和依赖；不新增静默兜底、模拟成功或吞异常路径。
- 新增测试用于驱动实现和验证行为；最终交付前按用户规则删除本轮新增的临时测试文件，但保留修复后的业务代码。
- 每个任务完成后运行任务内验证命令。命令失败时停止，修根因后重新运行。
- 每个任务完成后做一次小提交。若当前有用户未提交改动，只提交本任务新增/修改的文件。
- 方案 1 视觉参考图：`/Users/fciasth/.codex/generated_images/019ee074-bf44-7061-82bc-dadf498cdc8f/ig_08629a3ddb5ca8c7016a355ec6a3848196959bfd29390e1d39.png`。

## File Structure

- Create `backend/src/main/java/com/fund/analysis/dto/DashboardDecisionDTO.java`: 首页决策中心聚合 DTO，包含数据健康、今日决策、指标卡、操作队列、ETF 机会、基金推荐、图表点。
- Create `backend/src/main/java/com/fund/analysis/service/DashboardDecisionService.java`: 聚合现有服务与 mapper，负责把各模块数据转换成首页 DTO，并记录模块级错误。
- Create `backend/src/main/java/com/fund/analysis/controller/DashboardController.java`: 暴露 `GET /api/dashboard/decision`。
- Modify `backend/src/main/java/com/fund/analysis/service/MarketDataService.java`: 将股债配置解析逻辑抽成可复用方法，供首页 DTO 使用。
- Modify `backend/src/main/java/com/fund/analysis/service/MaStrategyService.java`: 增加读取买入和卖出信号的只读方法，首页需要同时展示卖出风险。
- Modify `backend/src/main/java/com/fund/analysis/controller/AdminController.java`: 让 `/admin/status` 返回更有用的数据更新时间、API/DB 状态字段；仍然明确暴露失败。
- Create temporary `backend/src/test/java/com/fund/analysis/service/DashboardDecisionServiceTest.java`: 驱动首页聚合字段、模块错误、操作队列。
- Modify `frontend/src/services/api.js`: 新增 `dashboardApi`、`adminApi`，复用现有 Axios 错误归一化。
- Create `frontend/src/utils/dashboardDecision.js`: 首页数据归一化、指标颜色、信号分组、操作队列目标。
- Create temporary `frontend/src/utils/dashboardDecision.test.js`: 验证首页模型转换。
- Replace `frontend/src/pages/Dashboard.jsx`: 从旧市场概览页重构为决策驾驶舱页面容器。
- Create `frontend/src/components/dashboard/DecisionSummary.jsx`: 今日决策三栏。
- Create `frontend/src/components/dashboard/MetricStrip.jsx`: RSI、股债、风险溢价、均线偏离指标条。
- Create `frontend/src/components/dashboard/MarketTemperatureChart.jsx`: RSI 趋势图和阈值说明。
- Create `frontend/src/components/dashboard/OperationQueue.jsx`: 执行回测、查看动量、编辑组合、发送日报等操作入口。
- Create `frontend/src/components/dashboard/SignalTables.jsx`: ETF 机会和基金推荐双表。
- Modify `frontend/src/components/MainLayout.jsx`: 调整导航分组与浅色侧栏，贴近方案 1。
- Modify `frontend/src/assets/css/index.css`: 增加决策驾驶舱样式 token 和响应式布局。
- Modify `frontend/src/pages/RsiBacktest.jsx`: 修复参数区 JSX 嵌套错误。
- Modify `frontend/src/pages/MaStrategy.jsx`: 删除重复 `type="primary"` 属性。
- Modify `frontend/src/pages/EtfManagement.jsx`: 删除重复 `rules` 属性。
- Modify `frontend/package.json`: 临时把 `src/utils/dashboardDecision.test.js` 纳入现有测试脚本。

---

### Task 1: Create Branch And Capture Baseline

**Files:**
- Modify: none

- [ ] **Step 1: Create the execution branch**

Run:

```bash
cd /Users/fciasth/project/trade/fund
git switch -c codex/fund-decision-dashboard
```

Expected: current branch is `codex/fund-decision-dashboard`.

- [ ] **Step 2: Record current dirty files**

Run:

```bash
git status --short
```

Expected: existing user changes may include `backend/src/main/java/com/fund/analysis/service/FundAnalysisService.java` and `docs/`. Do not revert them.

- [ ] **Step 3: Run baseline verification**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
cd /Users/fciasth/project/trade/fund/frontend
npm test
npm run build
```

Expected: tests and build either pass or expose real existing failures. If they fail, record the exact failing file and line before Task 2.

- [ ] **Step 4: Do not commit baseline-only state**

Run:

```bash
git status --short
```

Expected: no new staged files. Leave the tree unchanged.

---

### Task 2: Add Backend Dashboard Decision Contract

**Files:**
- Create: `backend/src/main/java/com/fund/analysis/dto/DashboardDecisionDTO.java`
- Create temporary test: `backend/src/test/java/com/fund/analysis/service/DashboardDecisionServiceTest.java`

- [ ] **Step 1: Write the temporary failing DTO/service contract test**

Create `backend/src/test/java/com/fund/analysis/service/DashboardDecisionServiceTest.java`:

```java
package com.fund.analysis.service;

import com.fund.analysis.dto.DashboardDecisionDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DashboardDecisionServiceTest {

    @Test
    void dashboardDecisionDtoKeepsVisibleModuleHealthAndOperations() {
        DashboardDecisionDTO dto = new DashboardDecisionDTO();
        dto.setDataStatus(new DashboardDecisionDTO.DataStatusDTO());
        dto.getDataStatus().setStatus("partial");
        dto.getDataStatus().setMessage("组合 RSI 加载失败");

        DashboardDecisionDTO.OperationDTO operation = new DashboardDecisionDTO.OperationDTO();
        operation.setKey("rsi-backtest");
        operation.setTitle("执行RSI回测");
        operation.setTargetPath("/rsi-backtest");
        operation.setDanger(false);
        dto.getOperations().add(operation);

        assertEquals("partial", dto.getDataStatus().getStatus());
        assertEquals("组合 RSI 加载失败", dto.getDataStatus().getMessage());
        assertEquals("执行RSI回测", dto.getOperations().get(0).getTitle());
        assertFalse(dto.getOperations().get(0).getDanger());
    }
}
```

- [ ] **Step 2: Run the temporary test and verify it fails**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn -Dtest=DashboardDecisionServiceTest test
```

Expected: compilation fails because `DashboardDecisionDTO` does not exist.

- [ ] **Step 3: Create the DTO with Chinese field comments**

Create `backend/src/main/java/com/fund/analysis/dto/DashboardDecisionDTO.java`:

```java
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
     * ETF 机会列表
     */
    private List<RsiDataDTO> etfOpportunities = new ArrayList<>();

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
         * 状态，normal 表示正常，partial 表示部分失败，error 表示不可用
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
         * 14日 RSI
         */
        private BigDecimal rsi14;

        /**
         * 90日 RSI
         */
        private BigDecimal rsi90;

        /**
         * 组合 RSI
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
         * API 动作键
         */
        private String action;

        /**
         * 是否为风险操作
         */
        private boolean danger;
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
         * 14日 RSI，当前接口无值时保持 null
         */
        private BigDecimal rsi14;

        /**
         * 推荐条件 ID
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
```

- [ ] **Step 4: Run the DTO contract test**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn -Dtest=DashboardDecisionServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit DTO contract**

Run:

```bash
git add backend/src/main/java/com/fund/analysis/dto/DashboardDecisionDTO.java backend/src/test/java/com/fund/analysis/service/DashboardDecisionServiceTest.java
git commit -m "feat: add dashboard decision dto contract"
```

Expected: commit succeeds with only these files.

---

### Task 3: Implement Backend Aggregation Endpoint

**Files:**
- Create: `backend/src/main/java/com/fund/analysis/service/DashboardDecisionService.java`
- Create: `backend/src/main/java/com/fund/analysis/controller/DashboardController.java`
- Modify: `backend/src/main/java/com/fund/analysis/service/MaStrategyService.java`
- Modify: `backend/src/main/java/com/fund/analysis/service/MarketDataService.java`
- Modify temporary test: `backend/src/test/java/com/fund/analysis/service/DashboardDecisionServiceTest.java`

- [ ] **Step 1: Extend the temporary test for aggregation behavior**

Replace `DashboardDecisionServiceTest.java` with:

```java
package com.fund.analysis.service;

import com.fund.analysis.dto.DashboardDecisionDTO;
import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.RsiDataDTO;
import com.fund.analysis.entity.FundInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardDecisionServiceTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private FundAnalysisService fundAnalysisService;

    @Mock
    private FundBlacklistService fundBlacklistService;

    @InjectMocks
    private DashboardDecisionService dashboardDecisionService;

    @Test
    void buildsDecisionDashboardFromExistingModules() {
        MarketOverviewDTO overview = new MarketOverviewDTO();
        overview.setRsi14("56.32");
        overview.setRsi90("58.71");
        overview.setBalanceSuggestion("6股4债");
        overview.setRiskPremium("4.21%");
        overview.setMa5yDeviation("「2026-06-19」 -3.62%");
        overview.setUpdateTime("2026-06-19 10:25:36");

        RsiDataDTO buySignal = new RsiDataDTO();
        buySignal.setCode("510300.SH");
        buySignal.setName("沪深300ETF");
        buySignal.setCurrentRsi(new BigDecimal("41.25"));
        buySignal.setIsBuySignal(true);
        overview.setEtfOpportunities(Collections.singletonList(buySignal));

        FundInfo fund = new FundInfo();
        fund.setFundCode("005827");
        fund.setFundName("易方达蓝筹精选混合");
        fund.setIsHolding(1);

        when(marketDataService.getMarketOverview()).thenReturn(overview);
        when(fundAnalysisService.getFundRecommendations()).thenReturn(Collections.singletonList(fund));
        when(fundBlacklistService.isBlacklisted("005827")).thenReturn(false);

        DashboardDecisionDTO result = dashboardDecisionService.getDecisionDashboard();

        assertEquals("normal", result.getDataStatus().getStatus());
        assertEquals("2026-06-19 10:25:36", result.getUpdateTime());
        assertEquals(3, result.getDecisions().size());
        assertEquals("5", result.getDecisions().get(0).getValue());
        assertEquals("6 / 4", result.getMetrics().get(3).getValue());
        assertEquals("沪深300ETF", result.getEtfOpportunities().get(0).getName());
        assertEquals("已持有", result.getFundRecommendations().get(0).getTag());
        assertEquals(4, result.getOperations().size());
    }

    @Test
    void exposesModuleErrorWhenFundRecommendationsFail() {
        MarketOverviewDTO overview = new MarketOverviewDTO();
        overview.setRsi14("42.00");
        overview.setRsi90("51.00");
        when(marketDataService.getMarketOverview()).thenReturn(overview);
        when(fundAnalysisService.getFundRecommendations()).thenThrow(new IllegalStateException("推荐接口异常"));

        DashboardDecisionDTO result = dashboardDecisionService.getDecisionDashboard();

        assertEquals("partial", result.getDataStatus().getStatus());
        assertFalse(result.getDataStatus().getModuleErrors().isEmpty());
        assertEquals("基金推荐", result.getDataStatus().getModuleErrors().get(0).getModule());
        assertTrue(result.getDataStatus().getMessage().contains("部分数据加载失败"));
    }
}
```

- [ ] **Step 2: Run the aggregation test and verify it fails**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn -Dtest=DashboardDecisionServiceTest test
```

Expected: compilation fails because `DashboardDecisionService` does not exist.

- [ ] **Step 3: Add read helpers to existing services**

In `MarketDataService.java`, add this public method below `calculateStockBondBalance`:

```java
    /**
     * 将股债建议转换成首页展示格式
     *
     * @param suggestion 股债建议文本
     * @return 首页展示格式
     */
    public String formatStockBondRatio(String suggestion) {
        if (suggestion == null || suggestion.trim().isEmpty()) {
            return "-";
        }
        String normalized = suggestion.replace("股", " ").replace("债", "").trim();
        String[] parts = normalized.split("\\s+");
        if (parts.length != 2) {
            return suggestion;
        }
        return parts[0] + " / " + parts[1];
    }
```

In `MaStrategyService.java`, add this method below `getMaSellSignals`:

```java
    /**
     * 获取买入和卖出 MA 信号
     *
     * @return 买入和卖出 MA 信号列表
     */
    public List<MaStrategyDTO> getMaActionSignals() {
        List<MaStrategyDTO> signals = new ArrayList<>();
        signals.addAll(getMaBuySignals());
        signals.addAll(getMaSellSignals());
        return signals;
    }
```

- [ ] **Step 4: Create the dashboard aggregation service**

Create `backend/src/main/java/com/fund/analysis/service/DashboardDecisionService.java`:

```java
package com.fund.analysis.service;

import com.fund.analysis.dto.DashboardDecisionDTO;
import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.RsiDataDTO;
import com.fund.analysis.entity.FundInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 决策驾驶舱聚合服务
 */
@Service
public class DashboardDecisionService {

    /**
     * 市场数据服务
     */
    @Autowired
    private MarketDataService marketDataService;

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
     * 获取决策驾驶舱数据
     *
     * @return 决策驾驶舱数据
     */
    public DashboardDecisionDTO getDecisionDashboard() {
        DashboardDecisionDTO result = new DashboardDecisionDTO();
        MarketOverviewDTO overview = loadMarketOverview(result);
        if (overview != null) {
            result.setUpdateTime(overview.getUpdateTime());
            result.getEtfOpportunities().addAll(overview.getEtfOpportunities() == null
                    ? java.util.Collections.emptyList()
                    : overview.getEtfOpportunities());
            buildDecisionCards(result, overview);
            buildMetrics(result, overview);
            buildOperations(result);
        }
        loadFundRecommendations(result);
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
            return marketDataService.getMarketOverview();
        } catch (RuntimeException e) {
            addModuleError(result, "市场概览", e.getMessage());
            return null;
        }
    }

    /**
     * 加载基金推荐
     *
     * @param result 聚合结果
     */
    private void loadFundRecommendations(DashboardDecisionDTO result) {
        try {
            List<FundInfo> funds = fundAnalysisService.getFundRecommendations();
            for (FundInfo fund : funds) {
                DashboardDecisionDTO.FundRecommendationDTO item = new DashboardDecisionDTO.FundRecommendationDTO();
                item.setFundCode(fund.getFundCode());
                item.setFundName(fund.getFundName());
                item.setConditionId("");
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
     * 构建今日决策卡片
     *
     * @param result 聚合结果
     * @param overview 市场概览
     */
    private void buildDecisionCards(DashboardDecisionDTO result, MarketOverviewDTO overview) {
        int buyCount = overview.getEtfOpportunities() == null ? 0 : overview.getEtfOpportunities().size();
        result.getDecisions().add(createDecision("buy", "买入机会", String.valueOf(buyCount), "ETF信号", "关注 RSI 与 MA 共振标的", buyCount > 0 ? "success" : "info"));
        result.getDecisions().add(createDecision("rebalance", "再平衡建议", marketDataService.formatStockBondRatio(overview.getBalanceSuggestion()), "股 / 债", "根据90日RSI调整配置", "warning"));
        result.getDecisions().add(createDecision("risk", "风险提示", resolveRiskText(overview.getRsi90()), "市场温度", "结合风险溢价与均线偏离度控制仓位", resolveRiskLevel(overview.getRsi90())));
    }

    /**
     * 构建核心指标
     *
     * @param result 聚合结果
     * @param overview 市场概览
     */
    private void buildMetrics(DashboardDecisionDTO result, MarketOverviewDTO overview) {
        result.getMetrics().add(createMetric("rsi14", "14日RSI", overview.getRsi14(), "短期温度", "flat", resolveRiskLevel(overview.getRsi14())));
        result.getMetrics().add(createMetric("rsi90", "90日RSI", overview.getRsi90(), "中期温度", "flat", resolveRiskLevel(overview.getRsi90())));
        String portfolioRsi = overview.getFundPortfolioRsi() == null ? "-" : overview.getFundPortfolioRsi().getRsi14();
        result.getMetrics().add(createMetric("portfolioRsi", "组合RSI", portfolioRsi, "持仓组合", "flat", resolveRiskLevel(portfolioRsi)));
        result.getMetrics().add(createMetric("balance", "股债配置", marketDataService.formatStockBondRatio(overview.getBalanceSuggestion()), "股 / 债", "flat", "neutral"));
        result.getMetrics().add(createMetric("riskPremium", "风险溢价", valueOrDash(overview.getRiskPremium()), "沪深300", "flat", "info"));
        result.getMetrics().add(createMetric("ma5yDeviation", "5年均线偏离度", valueOrDash(overview.getMa5yDeviation()), "1250日均线", "flat", "info"));
    }

    /**
     * 构建操作队列
     *
     * @param result 聚合结果
     */
    private void buildOperations(DashboardDecisionDTO result) {
        result.getOperations().add(createOperation("rsi-backtest", "执行RSI回测", "基于当前 RSI 阈值做区间分析", "/rsi-backtest", null, false));
        result.getOperations().add(createOperation("momentum", "查看动量轮动", "查看 21 日动量最新结果", "/momentum-strategy", null, false));
        result.getOperations().add(createOperation("portfolio-weight", "编辑组合权重", "调整基金组合配置比例", "/fund-portfolio", null, false));
        result.getOperations().add(createOperation("send-email", "发送日报", "发送今日投资分析日报", null, "sendEmailNow", false));
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
     */
    private DashboardDecisionDTO.DecisionCardDTO createDecision(String key, String title, String value, String subtitle, String description, String level) {
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
     */
    private DashboardDecisionDTO.MetricDTO createMetric(String key, String label, String value, String helper, String trend, String level) {
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
     */
    private DashboardDecisionDTO.OperationDTO createOperation(String key, String title, String description, String targetPath, String action, boolean danger) {
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
     * 解析风险文本
     */
    private String resolveRiskText(String value) {
        Double number = parseDouble(value);
        if (number == null) {
            return "未知";
        }
        if (number >= 70) {
            return "偏高";
        }
        if (number <= 30) {
            return "偏低";
        }
        return "中性";
    }

    /**
     * 解析风险级别
     */
    private String resolveRiskLevel(String value) {
        Double number = parseDouble(value);
        if (number == null) {
            return "neutral";
        }
        if (number >= 70) {
            return "danger";
        }
        if (number >= 57) {
            return "warning";
        }
        if (number <= 30) {
            return "success";
        }
        return "info";
    }

    /**
     * 解析数字
     */
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace("%", "").replace("「", "").replace("」", "").trim().split("\\s+")[0]);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 空值转横线
     */
    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
```

- [ ] **Step 5: Create the dashboard controller**

Create `backend/src/main/java/com/fund/analysis/controller/DashboardController.java`:

```java
package com.fund.analysis.controller;

import com.fund.analysis.dto.DashboardDecisionDTO;
import com.fund.analysis.dto.Result;
import com.fund.analysis.service.DashboardDecisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 决策驾驶舱控制器
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    /**
     * 决策驾驶舱服务
     */
    @Autowired
    private DashboardDecisionService dashboardDecisionService;

    /**
     * 获取决策驾驶舱数据
     *
     * @return 决策驾驶舱数据
     */
    @GetMapping("/decision")
    public Result<DashboardDecisionDTO> getDecisionDashboard() {
        return Result.success(dashboardDecisionService.getDecisionDashboard());
    }
}
```

- [ ] **Step 6: Run backend tests**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn -Dtest=DashboardDecisionServiceTest test
mvn test
```

Expected: PASS.

- [ ] **Step 7: Commit backend aggregation**

Run:

```bash
git add backend/src/main/java/com/fund/analysis/dto/DashboardDecisionDTO.java backend/src/main/java/com/fund/analysis/service/DashboardDecisionService.java backend/src/main/java/com/fund/analysis/controller/DashboardController.java backend/src/main/java/com/fund/analysis/service/MarketDataService.java backend/src/main/java/com/fund/analysis/service/MaStrategyService.java backend/src/test/java/com/fund/analysis/service/DashboardDecisionServiceTest.java
git commit -m "feat: add decision dashboard aggregation api"
```

Expected: commit succeeds.

---

### Task 4: Add Frontend Dashboard Data Model

**Files:**
- Create: `frontend/src/utils/dashboardDecision.js`
- Create temporary test: `frontend/src/utils/dashboardDecision.test.js`
- Modify: `frontend/package.json`
- Modify: `frontend/src/services/api.js`

- [ ] **Step 1: Write the temporary failing frontend model test**

Create `frontend/src/utils/dashboardDecision.test.js`:

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getDashboardStatusColor,
  getOperationRoute,
  normalizeDashboardDecision,
} from './dashboardDecision.js'

test('首页数据模型保留决策卡片、指标和表格', () => {
  const result = normalizeDashboardDecision({
    dataStatus: { status: 'normal', message: '数据正常', moduleErrors: [] },
    decisions: [{ key: 'buy', title: '买入机会', value: '5', level: 'success' }],
    metrics: [{ key: 'rsi14', label: '14日RSI', value: '56.32', level: 'info' }],
    operations: [{ key: 'rsi-backtest', targetPath: '/rsi-backtest' }],
    etfOpportunities: [{ code: '510300.SH', name: '沪深300ETF', currentRsi: 41.25 }],
    fundRecommendations: [{ fundCode: '005827', fundName: '易方达蓝筹精选混合', tag: '已持有' }],
  })

  assert.equal(result.dataStatus.status, 'normal')
  assert.equal(result.decisions[0].value, '5')
  assert.equal(result.metrics[0].label, '14日RSI')
  assert.equal(result.etfOpportunities[0].name, '沪深300ETF')
  assert.equal(result.fundRecommendations[0].tag, '已持有')
})

test('状态颜色和操作路由明确可见', () => {
  assert.equal(getDashboardStatusColor('normal'), 'success')
  assert.equal(getDashboardStatusColor('partial'), 'warning')
  assert.equal(getDashboardStatusColor('error'), 'error')
  assert.equal(getOperationRoute({ targetPath: '/fund-portfolio' }), '/fund-portfolio')
  assert.equal(getOperationRoute({ action: 'sendEmailNow' }), null)
})
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
cd /Users/fciasth/project/trade/fund/frontend
node --test src/utils/dashboardDecision.test.js
```

Expected: FAIL because `dashboardDecision.js` does not exist.

- [ ] **Step 3: Create dashboard model utility**

Create `frontend/src/utils/dashboardDecision.js`:

```javascript
// 首页默认数据，避免页面在请求前访问空对象。
export const EMPTY_DASHBOARD_DECISION = {
  dataStatus: { status: 'normal', message: '数据加载中', moduleErrors: [] },
  decisions: [],
  metrics: [],
  trendPoints: [],
  operations: [],
  etfOpportunities: [],
  fundRecommendations: [],
  updateTime: '',
}

// 归一化首页聚合数据。
export function normalizeDashboardDecision(data) {
  const source = data || {}
  return {
    dataStatus: {
      status: source.dataStatus?.status || 'error',
      message: source.dataStatus?.message || '首页数据不可用',
      moduleErrors: Array.isArray(source.dataStatus?.moduleErrors) ? source.dataStatus.moduleErrors : [],
    },
    decisions: Array.isArray(source.decisions) ? source.decisions : [],
    metrics: Array.isArray(source.metrics) ? source.metrics : [],
    trendPoints: Array.isArray(source.trendPoints) ? source.trendPoints : [],
    operations: Array.isArray(source.operations) ? source.operations : [],
    etfOpportunities: Array.isArray(source.etfOpportunities) ? source.etfOpportunities : [],
    fundRecommendations: Array.isArray(source.fundRecommendations) ? source.fundRecommendations : [],
    updateTime: source.updateTime || '',
  }
}

// 获取数据状态展示颜色。
export function getDashboardStatusColor(status) {
  if (status === 'normal') return 'success'
  if (status === 'partial') return 'warning'
  if (status === 'error') return 'error'
  return 'default'
}

// 获取风险级别颜色。
export function getLevelColor(level) {
  if (level === 'success') return '#0f9f6e'
  if (level === 'warning') return '#d97706'
  if (level === 'danger') return '#dc2626'
  if (level === 'info') return '#2563eb'
  return '#475569'
}

// 获取操作路由，API 动作没有路由。
export function getOperationRoute(operation) {
  return operation?.targetPath || null
}
```

- [ ] **Step 4: Add frontend API wrappers**

In `frontend/src/services/api.js`, add before `marketApi`:

```javascript
/**
 * 决策驾驶舱相关API
 */
export const dashboardApi = {
  // 获取首页决策聚合数据
  getDecision: () => api.get('/dashboard/decision'),
}

/**
 * 管理动作相关API
 */
export const adminApi = {
  // 刷新全部分析数据
  refreshAll: () => api.post('/admin/refresh-all'),

  // 获取系统状态
  getStatus: () => api.get('/admin/status'),
}
```

- [ ] **Step 5: Run frontend model tests**

Run:

```bash
cd /Users/fciasth/project/trade/fund/frontend
node --test src/utils/dashboardDecision.test.js
npm test
```

Expected: PASS.

- [ ] **Step 6: Commit frontend data layer**

Run:

```bash
git add frontend/src/utils/dashboardDecision.js frontend/src/utils/dashboardDecision.test.js frontend/src/services/api.js
git commit -m "feat: add dashboard decision frontend model"
```

Expected: commit succeeds.

---

### Task 5: Build Decision Dashboard Components

**Files:**
- Create: `frontend/src/components/dashboard/DecisionSummary.jsx`
- Create: `frontend/src/components/dashboard/MetricStrip.jsx`
- Create: `frontend/src/components/dashboard/MarketTemperatureChart.jsx`
- Create: `frontend/src/components/dashboard/OperationQueue.jsx`
- Create: `frontend/src/components/dashboard/SignalTables.jsx`
- Replace: `frontend/src/pages/Dashboard.jsx`

- [ ] **Step 1: Create `DecisionSummary.jsx`**

Create `frontend/src/components/dashboard/DecisionSummary.jsx`:

```jsx
import React from 'react'
import { InfoCircleOutlined } from '@ant-design/icons'
import { getLevelColor } from '../../utils/dashboardDecision'

// 今日决策摘要区。
function DecisionSummary({ decisions }) {
  return (
    <section className="dashboard-section">
      <div className="dashboard-section-title">今日决策</div>
      <div className="decision-grid">
        {decisions.map(decision => (
          <article className={`decision-card decision-card-${decision.level}`} key={decision.key}>
            <div className="decision-card-header">
              <span>{decision.title}</span>
              <InfoCircleOutlined />
            </div>
            <div className="decision-card-value" style={{ color: getLevelColor(decision.level) }}>
              {decision.value}
            </div>
            <div className="decision-card-subtitle">{decision.subtitle}</div>
            <div className="decision-card-desc">{decision.description}</div>
          </article>
        ))}
      </div>
    </section>
  )
}

export default DecisionSummary
```

- [ ] **Step 2: Create `MetricStrip.jsx`**

Create `frontend/src/components/dashboard/MetricStrip.jsx`:

```jsx
import React from 'react'
import { ArrowDownOutlined, ArrowRightOutlined, ArrowUpOutlined } from '@ant-design/icons'
import { getLevelColor } from '../../utils/dashboardDecision'

// 核心指标横条。
function MetricStrip({ metrics }) {
  const renderTrendIcon = (trend) => {
    if (trend === 'up') return <ArrowUpOutlined />
    if (trend === 'down') return <ArrowDownOutlined />
    return <ArrowRightOutlined />
  }

  return (
    <section className="metric-strip">
      {metrics.map(metric => (
        <article className="metric-item" key={metric.key}>
          <div className="metric-label">{metric.label}</div>
          <div className="metric-value" style={{ color: getLevelColor(metric.level) }}>{metric.value}</div>
          <div className="metric-helper">
            {renderTrendIcon(metric.trend)}
            <span>{metric.helper}</span>
          </div>
        </article>
      ))}
    </section>
  )
}

export default MetricStrip
```

- [ ] **Step 3: Create `MarketTemperatureChart.jsx`**

Create `frontend/src/components/dashboard/MarketTemperatureChart.jsx`:

```jsx
import React from 'react'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

// 市场温度趋势图。
function MarketTemperatureChart({ data }) {
  return (
    <section className="dashboard-panel market-chart-panel">
      <div className="dashboard-panel-header">
        <div>
          <h2>市场温度（RSI趋势）</h2>
          <p>阈值说明：&gt;70 超买，57-70 偏热，43-57 中性，30-43 偏冷，&lt;30 超卖</p>
        </div>
      </div>
      <div className="market-chart-body">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
            <XAxis dataKey="date" tick={{ fontSize: 12 }} />
            <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} />
            <Tooltip />
            <Legend />
            <ReferenceLine y={70} stroke="#dc2626" strokeDasharray="4 4" label="70" />
            <ReferenceLine y={57} stroke="#d97706" strokeDasharray="4 4" label="57" />
            <ReferenceLine y={43} stroke="#2563eb" strokeDasharray="4 4" label="43" />
            <ReferenceLine y={30} stroke="#0f9f6e" strokeDasharray="4 4" label="30" />
            <Line type="monotone" dataKey="rsi14" name="14日RSI" stroke="#2563eb" dot={false} strokeWidth={2} />
            <Line type="monotone" dataKey="rsi90" name="90日RSI" stroke="#0f9f6e" dot={false} strokeWidth={2} />
            <Line type="monotone" dataKey="portfolioRsi" name="组合RSI" stroke="#f59e0b" dot={false} strokeWidth={2} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </section>
  )
}

export default MarketTemperatureChart
```

- [ ] **Step 4: Create `OperationQueue.jsx`**

Create `frontend/src/components/dashboard/OperationQueue.jsx`:

```jsx
import React from 'react'
import { Button, Space } from 'antd'
import { MailOutlined, RightOutlined } from '@ant-design/icons'

// 操作队列。
function OperationQueue({ operations, onRunOperation }) {
  return (
    <section className="dashboard-panel operation-panel">
      <div className="dashboard-panel-header">
        <h2>操作队列</h2>
      </div>
      <Space direction="vertical" size={10} style={{ width: '100%' }}>
        {operations.map(operation => (
          <button
            className="operation-row"
            key={operation.key}
            type="button"
            onClick={() => onRunOperation(operation)}
          >
            <span className="operation-icon">{operation.action === 'sendEmailNow' ? <MailOutlined /> : <RightOutlined />}</span>
            <span className="operation-copy">
              <strong>{operation.title}</strong>
              <small>{operation.description}</small>
            </span>
            <RightOutlined />
          </button>
        ))}
      </Space>
      <Button block className="operation-all-button">全部操作</Button>
    </section>
  )
}

export default OperationQueue
```

- [ ] **Step 5: Create `SignalTables.jsx`**

Create `frontend/src/components/dashboard/SignalTables.jsx`:

```jsx
import React from 'react'
import { Button, Space, Table, Tag } from 'antd'
import { StarFilled, StarOutlined } from '@ant-design/icons'
import { formatNumber } from '../../utils/formatters'

// ETF 与基金推荐双表。
function SignalTables({ etfOpportunities, fundRecommendations }) {
  const etfColumns = [
    { title: 'ETF代码', dataIndex: 'code', key: 'code', width: 120 },
    { title: 'ETF名称', dataIndex: 'name', key: 'name', width: 160 },
    {
      title: '14日RSI',
      dataIndex: 'currentRsi',
      key: 'currentRsi',
      width: 100,
      render: value => formatNumber(value, 2),
    },
    { title: '区间', dataIndex: 'interval', key: 'interval', width: 160, ellipsis: true },
    {
      title: '操作建议',
      key: 'signal',
      width: 110,
      render: (_, record) => <Tag color={record.isBuySignal ? 'green' : 'default'}>{record.isBuySignal ? '关注买入' : '观望'}</Tag>,
    },
  ]

  const fundColumns = [
    { title: '基金代码', dataIndex: 'fundCode', key: 'fundCode', width: 120 },
    { title: '基金名称', dataIndex: 'fundName', key: 'fundName', ellipsis: true },
    { title: '推荐条件ID', dataIndex: 'conditionId', key: 'conditionId', width: 130 },
    {
      title: '标签',
      dataIndex: 'tag',
      key: 'tag',
      width: 100,
      render: tag => <Tag color={tag === '已持有' ? 'blue' : tag === '已排除' ? 'red' : 'green'}>{tag || '推荐'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_, record) => (
        <Button type="text" icon={record.isHolding === 1 ? <StarFilled /> : <StarOutlined />} />
      ),
    },
  ]

  return (
    <section className="dashboard-table-grid">
      <div className="dashboard-panel">
        <div className="dashboard-panel-header">
          <h2>ETF机会</h2>
          <Space><Button type="link">更多</Button></Space>
        </div>
        <Table columns={etfColumns} dataSource={etfOpportunities} rowKey="code" pagination={false} size="small" scroll={{ x: 680 }} />
      </div>
      <div className="dashboard-panel">
        <div className="dashboard-panel-header">
          <h2>基金推荐</h2>
          <Space><Button type="link">更多</Button></Space>
        </div>
        <Table columns={fundColumns} dataSource={fundRecommendations} rowKey="fundCode" pagination={false} size="small" scroll={{ x: 680 }} />
      </div>
    </section>
  )
}

export default SignalTables
```

- [ ] **Step 6: Replace `Dashboard.jsx` with the decision dashboard container**

Replace `frontend/src/pages/Dashboard.jsx`:

```jsx
import React, { useEffect, useState } from 'react'
import { Alert, Button, Space, Spin, Tag, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import DecisionSummary from '../components/dashboard/DecisionSummary'
import MarketTemperatureChart from '../components/dashboard/MarketTemperatureChart'
import MetricStrip from '../components/dashboard/MetricStrip'
import OperationQueue from '../components/dashboard/OperationQueue'
import SignalTables from '../components/dashboard/SignalTables'
import { adminApi, dashboardApi, systemConfigApi } from '../services/api'
import {
  EMPTY_DASHBOARD_DECISION,
  getDashboardStatusColor,
  getOperationRoute,
  normalizeDashboardDecision,
} from '../utils/dashboardDecision'

// 决策驾驶舱页面。
function Dashboard() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [dashboard, setDashboard] = useState(EMPTY_DASHBOARD_DECISION)
  const [pageError, setPageError] = useState(null)

  const loadDashboard = async () => {
    try {
      setLoading(true)
      setPageError(null)
      const response = await dashboardApi.getDecision()
      if (response.code !== 0) {
        throw new Error(response.message || '首页数据加载失败')
      }
      setDashboard(normalizeDashboardDecision(response.data))
    } catch (error) {
      setPageError(error.normalizedMessage || error.message || '首页数据加载失败')
    } finally {
      setLoading(false)
    }
  }

  const handleRefreshAll = async () => {
    try {
      setRefreshing(true)
      const response = await adminApi.refreshAll()
      if (response.code !== 0) {
        throw new Error(response.message || '刷新数据失败')
      }
      message.success('数据刷新完成')
      await loadDashboard()
    } catch (error) {
      message.error(error.normalizedMessage || error.message || '刷新数据失败')
    } finally {
      setRefreshing(false)
    }
  }

  const handleRunOperation = async (operation) => {
    const route = getOperationRoute(operation)
    if (route) {
      navigate(route)
      return
    }
    if (operation.action === 'sendEmailNow') {
      const response = await systemConfigApi.sendEmailNow()
      if (response.code === 0) {
        message.success('日报发送成功')
        return
      }
      message.error(response.message || '日报发送失败')
    }
  }

  useEffect(() => {
    loadDashboard()
  }, [])

  if (loading) {
    return <div className="loading-container"><Spin size="large" /></div>
  }

  if (pageError) {
    return (
      <Alert
        message="首页数据加载失败"
        description={pageError}
        type="error"
        showIcon
        action={<Button onClick={loadDashboard}>重试</Button>}
      />
    )
  }

  return (
    <div className="decision-dashboard">
      <header className="dashboard-topbar">
        <div>
          <h1>基金和ETF投资策略分析系统</h1>
          <Space size={12}>
            <Tag color={getDashboardStatusColor(dashboard.dataStatus.status)}>数据状态：{dashboard.dataStatus.message}</Tag>
            {dashboard.updateTime && <span className="dashboard-update-time">最后更新：{dashboard.updateTime}</span>}
          </Space>
        </div>
        <Button icon={<ReloadOutlined />} onClick={handleRefreshAll} loading={refreshing}>刷新数据</Button>
      </header>

      {dashboard.dataStatus.moduleErrors.length > 0 && (
        <Alert
          type="warning"
          showIcon
          className="dashboard-alert"
          message="部分模块加载失败"
          description={dashboard.dataStatus.moduleErrors.map(error => `${error.module}：${error.message}`).join('；')}
        />
      )}

      <DecisionSummary decisions={dashboard.decisions} />
      <MetricStrip metrics={dashboard.metrics} />
      <div className="dashboard-main-grid">
        <MarketTemperatureChart data={dashboard.trendPoints} />
        <OperationQueue operations={dashboard.operations} onRunOperation={handleRunOperation} />
      </div>
      <SignalTables etfOpportunities={dashboard.etfOpportunities} fundRecommendations={dashboard.fundRecommendations} />
    </div>
  )
}

export default Dashboard
```

- [ ] **Step 7: Run frontend build**

Run:

```bash
cd /Users/fciasth/project/trade/fund/frontend
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit dashboard components**

Run:

```bash
git add frontend/src/components/dashboard frontend/src/pages/Dashboard.jsx
git commit -m "feat: build decision dashboard page"
```

Expected: commit succeeds.

---

### Task 6: Apply Option 1 Visual System And Navigation

**Files:**
- Modify: `frontend/src/components/MainLayout.jsx`
- Modify: `frontend/src/assets/css/index.css`

- [ ] **Step 1: Update sidebar grouping and light theme**

In `frontend/src/components/MainLayout.jsx`, replace `menuItems` with grouped labels:

```jsx
const menuItems = [
  {
    type: 'group',
    label: '决策中心',
    children: [
      { key: '/', icon: <DashboardOutlined />, label: '市场概览' },
      { key: '/rsi-analysis', icon: <LineChartOutlined />, label: 'RSI分析' },
      { key: '/rsi-backtest', icon: <ExperimentOutlined />, label: 'RSI回测' },
    ],
  },
  {
    type: 'group',
    label: '策略分析',
    children: [
      { key: '/ma-strategy', icon: <RiseOutlined />, label: 'MA策略' },
      { key: '/momentum-strategy', icon: <ThunderboltOutlined />, label: '动量策略' },
    ],
  },
  {
    type: 'group',
    label: '组合与推荐',
    children: [
      { key: '/fund-recommendation', icon: <FundOutlined />, label: '基金推荐' },
      { key: '/fund-portfolio', icon: <WalletOutlined />, label: '基金组合' },
    ],
  },
  {
    type: 'group',
    label: '基础配置',
    children: [
      { key: '/etf-management', icon: <AppstoreOutlined />, label: 'ETF管理' },
      { key: '/system-config', icon: <SettingOutlined />, label: '系统配置' },
    ],
  },
]
```

Set the `Sider` and `Menu` theme to light:

```jsx
<Sider
  className="app-sider"
  collapsible
  collapsed={collapsed}
  onCollapse={setCollapsed}
  breakpoint="md"
  collapsedWidth={isMobile ? 0 : 80}
>
```

```jsx
<Menu
  theme="light"
  mode="inline"
  selectedKeys={[location.pathname]}
  items={menuItems}
  onClick={handleMenuClick}
/>
```

- [ ] **Step 2: Add dashboard CSS**

Append to `frontend/src/assets/css/index.css`:

```css
.app-sider {
  background: #ffffff !important;
  border-right: 1px solid #e2e8f0;
}

.decision-dashboard {
  color: #0f172a;
}

.dashboard-topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-topbar h1 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
}

.dashboard-update-time {
  color: #64748b;
  font-size: 13px;
}

.dashboard-alert {
  margin-bottom: 16px;
}

.dashboard-section,
.dashboard-panel,
.metric-strip {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.dashboard-section {
  padding: 16px;
  margin-bottom: 14px;
}

.dashboard-section-title,
.dashboard-panel h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.decision-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 12px;
}

.decision-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  background: #ffffff;
}

.decision-card-success {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.decision-card-warning {
  background: #fff7ed;
  border-color: #fed7aa;
}

.decision-card-danger {
  background: #fef2f2;
  border-color: #fecaca;
}

.decision-card-header {
  display: flex;
  justify-content: space-between;
  color: #334155;
  font-weight: 600;
}

.decision-card-value {
  margin-top: 14px;
  font-size: 34px;
  font-weight: 800;
}

.decision-card-subtitle,
.decision-card-desc,
.metric-helper {
  color: #64748b;
  font-size: 13px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0;
  margin-bottom: 14px;
}

.metric-item {
  padding: 16px;
  border-right: 1px solid #e2e8f0;
}

.metric-item:last-child {
  border-right: 0;
}

.metric-label {
  color: #334155;
  font-size: 13px;
}

.metric-value {
  margin: 8px 0 4px;
  font-size: 22px;
  font-weight: 800;
}

.dashboard-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 14px;
  margin-bottom: 14px;
}

.dashboard-panel {
  padding: 16px;
}

.dashboard-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.dashboard-panel-header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.market-chart-body {
  min-height: 300px;
}

.operation-row {
  width: 100%;
  min-height: 58px;
  display: grid;
  grid-template-columns: 38px 1fr 16px;
  align-items: center;
  gap: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  color: #0f172a;
  cursor: pointer;
  text-align: left;
  padding: 10px 12px;
}

.operation-row:hover {
  border-color: #2563eb;
  background: #eff6ff;
}

.operation-icon {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  background: #eff6ff;
  border-radius: 8px;
}

.operation-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.operation-copy small {
  color: #64748b;
}

.operation-all-button {
  margin-top: 10px;
}

.dashboard-table-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1180px) {
  .metric-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-main-grid,
  .dashboard-table-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-topbar,
  .decision-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .metric-strip {
    grid-template-columns: 1fr;
  }

  .metric-item {
    border-right: 0;
    border-bottom: 1px solid #e2e8f0;
  }
}
```

- [ ] **Step 3: Run frontend build**

Run:

```bash
cd /Users/fciasth/project/trade/fund/frontend
npm run build
```

Expected: PASS and no overlapping layout in generated assets.

- [ ] **Step 4: Commit visual system**

Run:

```bash
git add frontend/src/components/MainLayout.jsx frontend/src/assets/css/index.css
git commit -m "style: apply decision dashboard visual system"
```

Expected: commit succeeds.

---

### Task 7: Fix Existing Frontend Issues Found During Review

**Files:**
- Modify: `frontend/src/pages/RsiBacktest.jsx`
- Modify: `frontend/src/pages/MaStrategy.jsx`
- Modify: `frontend/src/pages/EtfManagement.jsx`

- [ ] **Step 1: Fix malformed JSX in RSI backtest parameter row**

In `frontend/src/pages/RsiBacktest.jsx`, replace the second threshold row with:

```jsx
          <Row gutter={16}>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <div style={{ marginBottom: 8 }}>买入阈值(RSI≤)：</div>
                <InputNumber style={{ width: '100%' }} value={buyThreshold} onChange={setBuyThreshold} min={1} max={99} />
              </div>
            </Col>
            <Col xs={24} sm={12} lg={4}>
              <div>
                <div style={{ marginBottom: 8 }}>卖出阈值(RSI≥)：</div>
                <InputNumber style={{ width: '100%' }} value={sellThreshold} onChange={setSellThreshold} min={1} max={99} />
              </div>
            </Col>
          </Row>
```

- [ ] **Step 2: Remove duplicate button prop in MA strategy**

In `frontend/src/pages/MaStrategy.jsx`, replace:

```jsx
          <Button
            type="primary"
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleRunBacktest}
            loading={backtestLoading}
            size="large"
          >
```

with:

```jsx
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleRunBacktest}
            loading={backtestLoading}
            size="large"
          >
```

- [ ] **Step 3: Remove duplicate validation prop in ETF management**

In `frontend/src/pages/EtfManagement.jsx`, replace the ETF name form item opening with:

```jsx
          <Form.Item
            name="etfName"
            label="ETF名称"
            rules={[{ required: true, message: '请输入ETF名称' }]}
          >
```

- [ ] **Step 4: Run build to verify JSX fixes**

Run:

```bash
cd /Users/fciasth/project/trade/fund/frontend
npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit bug fixes**

Run:

```bash
git add frontend/src/pages/RsiBacktest.jsx frontend/src/pages/MaStrategy.jsx frontend/src/pages/EtfManagement.jsx
git commit -m "fix: clean up existing frontend jsx issues"
```

Expected: commit succeeds.

---

### Task 8: Verify Browser Rendering And Operations

**Files:**
- Modify: none unless visual defects are found

- [ ] **Step 1: Start backend dependencies if available**

Run:

```bash
cd /Users/fciasth/project/trade/fund
docker compose up -d mysql redis
```

Expected: MySQL and Redis start if Docker is available. If Docker CLI is missing, record the environment gap and continue with frontend-only rendering.

- [ ] **Step 2: Start backend when database is available**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn spring-boot:run
```

Expected: backend starts on `http://localhost:8080/api`. If local database credentials are unavailable, record the exact error and continue with frontend error-state QA.

- [ ] **Step 3: Start frontend**

Run:

```bash
cd /Users/fciasth/project/trade/fund/frontend
npm run dev -- --host 127.0.0.1
```

Expected: Vite starts on `http://127.0.0.1:3000/`.

- [ ] **Step 4: Browser QA desktop**

Open `http://127.0.0.1:3000/` at 1440x1024 and verify:

```text
首页显示今日决策三张卡片、六个指标、市场温度图、操作队列、ETF机会表、基金推荐表。
数据健康状态显示 normal、partial 或 error。
刷新数据按钮会调用真实 /api/admin/refresh-all。
执行RSI回测、查看动量轮动、编辑组合权重会跳转到对应路由。
发送日报会调用真实 /api/system-config/email/send-now。
```

Expected: no overlapping text, no clipped cards, no console syntax errors.

- [ ] **Step 5: Browser QA mobile**

Open `http://127.0.0.1:3000/` at 390x844 and verify:

```text
导航可折叠，指标改为单列或三列响应式布局，图表不挤压操作队列，表格可横向滚动。
```

Expected: no incoherent overlap.

- [ ] **Step 6: Stop dev sessions**

Stop any `mvn spring-boot:run` and `npm run dev` sessions with `Ctrl+C`.

Expected: no required long-running sessions remain.

---

### Task 9: Final Verification And Remove Temporary Tests

**Files:**
- Delete temporary: `backend/src/test/java/com/fund/analysis/service/DashboardDecisionServiceTest.java`
- Delete temporary: `frontend/src/utils/dashboardDecision.test.js`

- [ ] **Step 1: Run complete verification before removing temporary tests**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
cd /Users/fciasth/project/trade/fund/frontend
npm test
npm run build
```

Expected: PASS.

- [ ] **Step 2: Delete this task's temporary tests**

Run:

```bash
rm backend/src/test/java/com/fund/analysis/service/DashboardDecisionServiceTest.java
rm frontend/src/utils/dashboardDecision.test.js
```

Expected: temporary tests are removed per user delivery rule.

- [ ] **Step 3: Run final retained verification**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
cd /Users/fciasth/project/trade/fund/frontend
npm test
npm run build
```

Expected: PASS using retained project tests and production build.

- [ ] **Step 4: Commit final cleanup**

Run:

```bash
git add -A
git commit -m "chore: finalize decision dashboard verification"
```

Expected: commit succeeds.

---

## Self-Review

- Spec coverage: the plan covers the selected option 1 visual direction, backend data aggregation, missing operations for refresh and send-email actions, explicit module errors, navigation/style changes, current frontend JSX defects, browser QA, and final verification.
- Placeholder scan: no task relies on deferred labels, later-fill wording, or unspecified error handling. Each code-changing step names exact files and concrete code.
- Type consistency: backend DTO names used by service/controller match `DashboardDecisionDTO`; frontend utilities match imports in `Dashboard.jsx`; operation actions use `sendEmailNow` consistently.
- Scope check: this is one cohesive subsystem because all changes serve the new decision dashboard. Existing RSI、MA、动量、基金、组合 pages are touched only for bug cleanup or navigation targets.
