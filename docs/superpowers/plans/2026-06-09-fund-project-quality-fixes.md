# Fund Project Quality Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复本轮审计发现的前端、后端和设计层面的非安全问题，让关键投资指标可信、失败可见、接口边界清晰、页面体验稳定。

**Architecture:** 先处理后端可靠性和数据正确性，再收紧接口校验，最后修复前端状态、响应式布局和构建体验。外部 HTTP、耗时计算和数据库写入分离；组合 RSI 使用按日期对齐后的价格序列；前端对错误使用显式 UI 状态，不再静默显示为正常空数据。

**Tech Stack:** Java 8, Spring Boot 2.7, MyBatis-Plus, MySQL 8, React 18, Vite 4, Ant Design 5, Recharts, Node test runner.

---

## Execution Rules

- 执行前必须先用 `superpowers:using-git-worktrees` 或创建 `codex/fund-quality-fixes` 分支，不能直接在 `master` 上改业务代码。
- 遵守根目录 AGENTS 规则：新增或修改的字段、方法、关键代码只写中文注释；不新增静默兜底、模拟成功、吞异常路径。
- 本计划使用临时测试驱动修复；按 AGENTS 要求，最终交付前删除本轮新增的临时测试用例，保留项目原有测试。
- 每个任务结束后运行该任务列出的验证命令。若验证失败，停止执行并定位根因。
- 每个任务完成后提交一次小提交，提交信息使用中文或英文均可，但需要准确描述本任务变化。

## Files And Responsibilities

- `backend/src/main/java/com/fund/analysis/client/ExternalApiClient.java`: 统一第三方 HTTP 超时和请求配置。
- `backend/src/main/java/com/fund/analysis/service/PortfolioPriceAligner.java`: 新增基金价格按日期对齐和加权聚合逻辑。
- `backend/src/main/java/com/fund/analysis/service/FundPortfolioService.java`: 使用对齐后的组合价格计算当前 RSI、周 RSI、历史 RSI。
- `backend/src/main/java/com/fund/analysis/service/*Service.java`: 将外部请求和长耗时计算移出数据库事务，只把写库动作放入短事务。
- `backend/src/main/java/com/fund/analysis/scheduled/DataRefreshTask.java`: 定时任务失败时显式抛出，避免日志显示“完成”掩盖失败。
- `backend/src/main/java/com/fund/analysis/controller/*Controller.java`: 补齐请求参数校验、写入结果校验、回测资金校验。
- `backend/src/main/resources/mapper/FundInfoMapper.xml`: 基金推荐读取最新有效数据，不再被 1 天窗口误清空。
- `frontend/src/services/api.js`: 统一错误对象，减少页面重复拼接错误消息。
- `frontend/src/App.jsx`: 页面级懒加载，降低首屏 bundle。
- `frontend/src/components/MainLayout.jsx`: 修复移动端固定侧栏挤压问题。
- `frontend/src/pages/*.jsx`: 显式错误提示、响应式表单、数值空值语义、URL 参数状态同步。
- `frontend/src/utils/formatters.js`: 新增前端通用数值、日期、金额格式化。
- `frontend/package.json`: 增加前端测试脚本和 ESM 类型声明。
- `README.md`: 修正响应码约定、验证命令、已知运行前提。

---

### Task 1: Create Execution Branch And Record Baseline

**Files:**
- Modify: none

- [ ] **Step 1: Create isolated branch**

Run:

```bash
git switch -c codex/fund-quality-fixes
```

Expected: current branch becomes `codex/fund-quality-fixes`.

- [ ] **Step 2: Confirm existing user change remains**

Run:

```bash
git status --short
```

Expected: `backend/src/main/java/com/fund/analysis/service/FundAnalysisService.java` may still be modified from the user's prior work. Do not revert it.

- [ ] **Step 3: Run baseline verification**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
cd /Users/fciasth/project/trade/fund/frontend
npm run build
node --test src/utils/*.test.js
```

Expected: backend tests pass, frontend build passes, frontend utility tests pass. The Vite chunk-size warning can remain before Task 8.

- [ ] **Step 4: Commit branch marker only if there are no staged changes**

Run:

```bash
git status --short
```

Expected: no new files are staged. Do not commit baseline-only state.

---

### Task 2: Add Explicit HTTP Timeouts

**Files:**
- Modify: `backend/src/main/java/com/fund/analysis/client/ExternalApiClient.java`
- Temporarily modify then restore: `backend/src/test/java/com/fund/analysis/client/ExternalApiClientTest.java`

- [ ] **Step 1: Add a temporary failing timeout test**

Add this method to `ExternalApiClientTest`:

```java
@Test
void timesOutWhenResponseBodyIsTooSlow() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/slow", exchange -> {
        try {
            Thread.sleep(500);
            byte[] body = "{\"code\":0}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            exchange.close();
        }
    });
    server.start();

    try {
        ExternalApiClient client = new ExternalApiClient(100, 100, 100);
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        assertThrows(ExternalApiException.class, () -> client.get(baseUrl + "/slow"));
    } finally {
        server.stop(0);
    }
}
```

Ensure these imports exist in the same test file:

```java
import java.nio.charset.StandardCharsets;
```

- [ ] **Step 2: Run the temporary test and verify it fails**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn -Dtest=ExternalApiClientTest#timesOutWhenResponseBodyIsTooSlow test
```

Expected: compilation fails because `ExternalApiClient(int,int,int)` does not exist.

- [ ] **Step 3: Implement request timeout configuration**

Add this import to `ExternalApiClient.java`:

```java
import org.apache.http.client.config.RequestConfig;
```

Replace the field section and constructors with:

```java
    /**
     * 默认连接超时时间，单位毫秒
     */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;

    /**
     * 默认连接池取连接超时时间，单位毫秒
     */
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = 5000;

    /**
     * 默认响应读取超时时间，单位毫秒
     */
    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 30000;

    /**
     * JSON 序列化工具
     */
    private final Gson gson = new Gson();

    /**
     * HTTP 请求配置
     */
    private final RequestConfig requestConfig;

    /**
     * 使用默认超时配置创建客户端
     */
    public ExternalApiClient() {
        this(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS, DEFAULT_SOCKET_TIMEOUT_MS);
    }

    /**
     * 使用指定超时配置创建客户端
     *
     * @param connectTimeoutMs 连接超时时间
     * @param connectionRequestTimeoutMs 取连接超时时间
     * @param socketTimeoutMs 响应读取超时时间
     */
    ExternalApiClient(int connectTimeoutMs, int connectionRequestTimeoutMs, int socketTimeoutMs) {
        this.requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(connectionRequestTimeoutMs)
                .setSocketTimeout(socketTimeoutMs)
                .build();
    }
```

Update `get` and `postJson` request creation:

```java
    public String get(String url) {
        HttpGet request = new HttpGet(url);
        request.setConfig(requestConfig);
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
             CloseableHttpResponse response = client.execute(request)) {
            return readResponse(url, response);
        } catch (IOException e) {
            throw new ExternalApiException("第三方 GET 请求失败: " + url, e);
        }
    }

    public String postJson(String url, Object body) {
        HttpPost request = new HttpPost(url);
        request.setConfig(requestConfig);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "*/*");
        if (body != null) {
            request.setEntity(new StringEntity(gson.toJson(body), StandardCharsets.UTF_8));
        }

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
             CloseableHttpResponse response = client.execute(request)) {
            return readResponse(url, response);
        } catch (IOException e) {
            throw new ExternalApiException("第三方 POST 请求失败: " + url, e);
        }
    }
```

- [ ] **Step 4: Verify timeout behavior**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn -Dtest=ExternalApiClientTest#timesOutWhenResponseBodyIsTooSlow test
```

Expected: the temporary timeout test passes.

- [ ] **Step 5: Delete the temporary timeout test**

Remove only `timesOutWhenResponseBodyIsTooSlow` and the now-unused `StandardCharsets` import from `ExternalApiClientTest.java`.

- [ ] **Step 6: Run existing backend tests**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
```

Expected: all existing backend tests pass.

- [ ] **Step 7: Commit**

Run:

```bash
git add backend/src/main/java/com/fund/analysis/client/ExternalApiClient.java
git commit -m "fix: add explicit external api timeouts"
```

---

### Task 3: Align Fund Portfolio RSI By Date

**Files:**
- Create: `backend/src/main/java/com/fund/analysis/service/PortfolioPriceAligner.java`
- Modify: `backend/src/main/java/com/fund/analysis/service/FundPortfolioService.java`
- Temporarily create then delete: `backend/src/test/java/com/fund/analysis/service/PortfolioPriceAlignerTest.java`

- [ ] **Step 1: Write temporary aligner tests**

Create `backend/src/test/java/com/fund/analysis/service/PortfolioPriceAlignerTest.java`:

```java
package com.fund.analysis.service;

import com.fund.analysis.exception.DataUnavailableException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortfolioPriceAlignerTest {

    @Test
    void alignsPricesByCommonDateBeforeWeighting() {
        Map<String, BigDecimal> firstPrices = new LinkedHashMap<>();
        firstPrices.put("2026-01-01", new BigDecimal("1.00"));
        firstPrices.put("2026-01-02", new BigDecimal("2.00"));
        firstPrices.put("2026-01-03", new BigDecimal("3.00"));

        Map<String, BigDecimal> secondPrices = new LinkedHashMap<>();
        secondPrices.put("2026-01-02", new BigDecimal("10.00"));
        secondPrices.put("2026-01-03", new BigDecimal("20.00"));
        secondPrices.put("2026-01-04", new BigDecimal("30.00"));

        List<PortfolioPriceAligner.FundPriceSeries> seriesList = Arrays.asList(
                new PortfolioPriceAligner.FundPriceSeries("000001", firstPrices),
                new PortfolioPriceAligner.FundPriceSeries("000002", secondPrices)
        );
        List<BigDecimal> weights = Arrays.asList(new BigDecimal("60"), new BigDecimal("40"));

        PortfolioPriceAligner.AlignedPortfolioPrices aligned = PortfolioPriceAligner.align(seriesList, weights);

        assertEquals(Arrays.asList("2026-01-02", "2026-01-03"), aligned.getDates());
        assertEquals(0, new BigDecimal("520.00").compareTo(aligned.getPrices().get(0)));
        assertEquals(0, new BigDecimal("980.00").compareTo(aligned.getPrices().get(1)));
    }

    @Test
    void throwsWhenNoCommonDateExists() {
        Map<String, BigDecimal> firstPrices = new LinkedHashMap<>();
        firstPrices.put("2026-01-01", new BigDecimal("1.00"));

        Map<String, BigDecimal> secondPrices = new LinkedHashMap<>();
        secondPrices.put("2026-01-02", new BigDecimal("10.00"));

        List<PortfolioPriceAligner.FundPriceSeries> seriesList = Arrays.asList(
                new PortfolioPriceAligner.FundPriceSeries("000001", firstPrices),
                new PortfolioPriceAligner.FundPriceSeries("000002", secondPrices)
        );

        assertThrows(DataUnavailableException.class, () -> PortfolioPriceAligner.align(
                seriesList,
                Arrays.asList(new BigDecimal("50"), new BigDecimal("50"))
        ));
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn -Dtest=PortfolioPriceAlignerTest test
```

Expected: compilation fails because `PortfolioPriceAligner` does not exist.

- [ ] **Step 3: Create the date aligner**

Create `backend/src/main/java/com/fund/analysis/service/PortfolioPriceAligner.java`:

```java
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
```

- [ ] **Step 4: Run temporary aligner tests**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn -Dtest=PortfolioPriceAlignerTest test
```

Expected: tests pass.

- [ ] **Step 5: Update `FundPortfolioService` to use aligned prices**

In `FundPortfolioService`, replace the old `calculatePortfolioRsi` body with:

```java
    public BigDecimal calculatePortfolioRsi(int period) {
        List<FundInfo> holdingFunds = getHoldingFunds();
        if (holdingFunds.isEmpty()) {
            throw new DataUnavailableException("没有持有基金，无法计算组合RSI");
        }

        PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices = buildAlignedPortfolioPrices(holdingFunds);
        List<BigDecimal> rsiValues = RsiCalculator.calculateRSI(alignedPrices.getPrices(), period);
        if (rsiValues.isEmpty()) {
            throw new DataUnavailableException("组合RSI数据不足，period=" + period);
        }

        return rsiValues.get(rsiValues.size() - 1);
    }
```

Replace the old `calculatePortfolioWeeklyRsi` body with:

```java
    public BigDecimal calculatePortfolioWeeklyRsi(int period) {
        List<FundInfo> holdingFunds = getHoldingFunds();
        if (holdingFunds.isEmpty()) {
            throw new DataUnavailableException("没有持有基金，无法计算组合周RSI");
        }

        PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices = buildAlignedPortfolioPrices(holdingFunds);
        List<BigDecimal> weeklyPrices = extractWeeklyPrices(alignedPrices.getPrices());
        List<BigDecimal> rsiValues = RsiCalculator.calculateRSI(weeklyPrices, period);
        if (rsiValues.isEmpty()) {
            throw new DataUnavailableException("组合周RSI数据不足，period=" + period);
        }

        return rsiValues.get(rsiValues.size() - 1);
    }
```

Add these helper methods to `FundPortfolioService`:

```java
    /**
     * 构建按共同交易日对齐后的组合价格
     *
     * @param holdingFunds 持有基金列表
     * @return 对齐后的组合价格
     */
    private PortfolioPriceAligner.AlignedPortfolioPrices buildAlignedPortfolioPrices(List<FundInfo> holdingFunds) {
        List<BigDecimal> weights = resolvePortfolioWeights(holdingFunds);
        List<PortfolioPriceAligner.FundPriceSeries> seriesList = new ArrayList<>();

        for (FundInfo fund : holdingFunds) {
            seriesList.add(getFundPriceSeries(fund.getFundCode()));
        }

        return PortfolioPriceAligner.align(seriesList, weights);
    }

    /**
     * 解析组合权重，权重总和不等于100时使用等权重
     *
     * @param holdingFunds 持有基金列表
     * @return 权重列表
     */
    private List<BigDecimal> resolvePortfolioWeights(List<FundInfo> holdingFunds) {
        BigDecimal totalWeight = holdingFunds.stream()
                .map(f -> f.getPortfolioWeight() != null ? f.getPortfolioWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean useEqualWeight = totalWeight.compareTo(new BigDecimal("100")) != 0;
        if (useEqualWeight) {
            logger.info("Total weight is {}, using equal weight", totalWeight);
        }

        List<BigDecimal> weights = new ArrayList<>();
        int fundCount = holdingFunds.size();
        for (FundInfo fund : holdingFunds) {
            BigDecimal weight = useEqualWeight
                    ? BigDecimal.valueOf(100.0 / fundCount)
                    : fund.getPortfolioWeight() != null ? fund.getPortfolioWeight() : BigDecimal.ZERO;
            weights.add(weight);
        }
        return weights;
    }

    /**
     * 从日价格中提取周价格
     *
     * @param dailyWeightedPrices 日组合价格
     * @return 周组合价格
     */
    private List<BigDecimal> extractWeeklyPrices(List<BigDecimal> dailyWeightedPrices) {
        List<BigDecimal> weeklyPrices = new ArrayList<>();
        for (int i = 4; i < dailyWeightedPrices.size(); i += 5) {
            weeklyPrices.add(dailyWeightedPrices.get(i));
        }

        int remainder = dailyWeightedPrices.size() % 5;
        if (remainder != 0 && !dailyWeightedPrices.isEmpty()) {
            weeklyPrices.add(dailyWeightedPrices.get(dailyWeightedPrices.size() - 1));
        }
        return weeklyPrices;
    }
```

Replace `getFundPricesWithDates` with:

```java
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
        JsonArray priceArray = listArray.get(1).getAsJsonArray();
        if (dateArray.size() != priceArray.size()) {
            throw new DataUnavailableException("基金净值日期与价格数量不一致: " + fundCode);
        }

        Map<String, BigDecimal> pricesByDate = new LinkedHashMap<>();
        for (int i = 1; i < priceArray.size(); i++) {
            JsonObject dateItem = dateArray.get(i).getAsJsonObject();
            JsonObject priceItem = priceArray.get(i).getAsJsonObject();
            pricesByDate.put(dateItem.get("name").getAsString(), new BigDecimal(priceItem.get("name").getAsString()));
        }

        return new PortfolioPriceAligner.FundPriceSeries(fundCode, pricesByDate);
    }
```

Update `refreshPortfolioRsiHistory` to call `buildAlignedPortfolioPrices(holdingFunds)` once, then use:

```java
        PortfolioPriceAligner.AlignedPortfolioPrices alignedPrices = buildAlignedPortfolioPrices(holdingFunds);
        List<BigDecimal> dailyWeightedPrices = alignedPrices.getPrices();
        List<String> dates = alignedPrices.getDates();

        List<BigDecimal> rsiValues = RsiCalculator.calculateRSI(dailyWeightedPrices, 14);
        if (rsiValues.isEmpty()) {
            throw new DataUnavailableException("组合RSI历史数据不足");
        }
```

In the history loop, keep:

```java
            String dataDate = dates != null && i < dates.size() ? dates.get(i) : "";
```

Remove unused imports caused by deleting the old map-returning helper.

- [ ] **Step 6: Verify backend compile and tests**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
```

Expected: all backend tests pass.

- [ ] **Step 7: Delete temporary aligner test**

Run:

```bash
rm backend/src/test/java/com/fund/analysis/service/PortfolioPriceAlignerTest.java
cd backend
mvn test
```

Expected: original backend tests still pass.

- [ ] **Step 8: Commit**

Run:

```bash
git add backend/src/main/java/com/fund/analysis/service/PortfolioPriceAligner.java backend/src/main/java/com/fund/analysis/service/FundPortfolioService.java
git commit -m "fix: align portfolio rsi prices by date"
```

---

### Task 4: Keep Database Transactions Short

**Files:**
- Modify: `backend/src/main/java/com/fund/analysis/service/FundAnalysisService.java`
- Modify: `backend/src/main/java/com/fund/analysis/service/FundPortfolioService.java`
- Modify: `backend/src/main/java/com/fund/analysis/service/MarketDataService.java`
- Modify: `backend/src/main/java/com/fund/analysis/service/MaStrategyService.java`
- Modify: `backend/src/main/java/com/fund/analysis/service/MomentumStrategyService.java`
- Modify: `backend/src/main/java/com/fund/analysis/service/RsiAnalysisService.java`
- Modify: `backend/src/main/java/com/fund/analysis/controller/MomentumBacktestController.java`

- [ ] **Step 1: Add `TransactionTemplate` imports and fields**

Add this import to each listed service file:

```java
import org.springframework.transaction.support.TransactionTemplate;
```

Add this field to each listed service class:

```java
    /**
     * 短事务执行器
     */
    @Autowired
    private TransactionTemplate transactionTemplate;
```

- [ ] **Step 2: Remove long-running transaction annotations**

Remove `@Transactional` from methods that perform third-party HTTP calls or full backtests:

```text
FundAnalysisService.refreshFundRecommendations
FundPortfolioService.refreshPortfolioRsi
FundPortfolioService.refreshPortfolioRsiHistory
MarketDataService.refreshMarketOverview
MaStrategyService.refreshMaStrategy
MaStrategyService.refreshAllEtfMa
MomentumStrategyService.refreshMomentumStrategy
RsiAnalysisService.refreshRsi
RsiAnalysisService.refreshAllEtfRsi
MomentumBacktestController.runBacktest
```

- [ ] **Step 3: Wrap only database writes in `FundAnalysisService`**

Replace the write block at the end of `refreshFundRecommendations`:

```java
        transactionTemplate.executeWithoutResult(status -> {
            fundInfoMapper.deleteOldData(30);
            for (FundInfo fund : topFunds) {
                saveFundInfo(fund);
            }
        });
```

- [ ] **Step 4: Wrap only database writes in `RsiAnalysisService`**

Replace the save/delete block in `refreshRsi`:

```java
        transactionTemplate.executeWithoutResult(status -> {
            saveRsiAnalysis(rsiData);
            rsiAnalysisMapper.deleteOldData(code, period, 1);
        });
```

- [ ] **Step 5: Wrap only database writes in `MaStrategyService`**

Remove `saveMaStrategy(dto);` from `refreshMaStrategy` and replace it with:

```java
        transactionTemplate.executeWithoutResult(status -> saveMaStrategy(dto));
```

- [ ] **Step 6: Wrap only database writes in `MarketDataService`**

Replace the insert/delete block in `refreshMarketOverview`:

```java
        transactionTemplate.executeWithoutResult(status -> {
            stockBondBalanceMapper.insert(balance);
            stockBondBalanceMapper.deleteOldData(1);
        });
```

- [ ] **Step 7: Wrap only database writes in `FundPortfolioService`**

Replace the insert/delete block in `refreshPortfolioRsi`:

```java
        transactionTemplate.executeWithoutResult(status -> {
            fundPortfolioRsiMapper.insert(portfolioRsi);
            fundPortfolioRsiMapper.deleteOldData(10);
        });
```

Replace the delete/insert block in `refreshPortfolioRsiHistory`:

```java
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
```

- [ ] **Step 8: Wrap momentum range replacement in one short transaction**

In `MomentumStrategyService`, replace the body of `replaceRefreshedStrategyRange` with:

```java
        if (newPerformances == null || newPerformances.isEmpty()) {
            logger.info("增量回测未生成每日绩效");
            return 0;
        }

        return transactionTemplate.execute(status -> {
            deletePerformanceByDateRange(startDate, endDate);
            savePerformanceRecords(newPerformances);

            deleteByDateRange(startDate, endDate);
            if (newTransactions == null || newTransactions.isEmpty()) {
                logger.info("重算区间未生成新的交易记录，已清理区间内旧交易记录");
                return 0;
            }

            saveTransactions(newTransactions);
            logger.info("动量策略数据刷新完成，共生成 {} 条新交易记录", newTransactions.size());
            return newTransactions.size();
        });
```

- [ ] **Step 9: Remove unused transaction imports from controller**

Remove this import from `MomentumBacktestController`:

```java
import org.springframework.transaction.annotation.Transactional;
```

- [ ] **Step 10: Verify backend tests**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
```

Expected: all backend tests pass.

- [ ] **Step 11: Commit**

Run:

```bash
git add backend/src/main/java/com/fund/analysis/service backend/src/main/java/com/fund/analysis/controller/MomentumBacktestController.java
git commit -m "fix: keep refresh database transactions short"
```

---

### Task 5: Make Scheduled Refresh Failures Visible

**Files:**
- Modify: `backend/src/main/java/com/fund/analysis/scheduled/DataRefreshTask.java`
- Modify: `backend/src/main/java/com/fund/analysis/config/DynamicScheduleConfig.java`

- [ ] **Step 1: Add visible task wrapper**

Add this import to `DataRefreshTask.java`:

```java
import com.fund.analysis.exception.BusinessException;
```

Add this helper method to `DataRefreshTask`:

```java
    /**
     * 执行刷新任务，失败时显式抛出异常
     *
     * @param taskName 任务名称
     * @param task 刷新逻辑
     */
    private void runRefreshTask(String taskName, Runnable task) {
        logger.info("========== 开始{} ==========", taskName);
        try {
            task.run();
            logger.info("========== {}完成 ==========", taskName);
        } catch (RuntimeException e) {
            logger.error("{}失败", taskName, e);
            throw e;
        } catch (Exception e) {
            logger.error("{}失败", taskName, e);
            throw new BusinessException(taskName + "失败", e);
        }
    }
```

- [ ] **Step 2: Replace scheduled method bodies**

Replace `refreshMarketData` body with:

```java
        runRefreshTask("刷新市场数据", () -> {
            boolean success = marketDataService.refreshMarketOverview();
            if (!success) {
                throw new BusinessException("市场数据刷新返回失败");
            }
            logger.info("市场数据刷新成功");
        });
```

Replace `refreshEtfRsi` body with:

```java
        runRefreshTask("刷新ETF RSI数据", () -> {
            int count = rsiAnalysisService.refreshAllEtfRsi();
            logger.info("ETF RSI数据刷新完成，共刷新 {} 条记录", count);
        });
```

Replace `refreshEtfMa` body with:

```java
        runRefreshTask("刷新ETF MA策略数据", () -> {
            int count = maStrategyService.refreshAllEtfMa();
            logger.info("ETF MA策略数据刷新完成，共刷新 {} 条记录", count);
        });
```

Replace `refreshFundRecommendations` body with:

```java
        runRefreshTask("刷新基金推荐数据", () -> {
            fundAnalysisService.refreshFundRecommendations();
            logger.info("基金推荐数据刷新完成");
        });
```

Replace `refreshFundPortfolioRsi` body with:

```java
        runRefreshTask("刷新基金组合 RSI 数据", () -> {
            boolean success = fundPortfolioService.refreshPortfolioRsi();
            if (!success) {
                throw new BusinessException("基金组合 RSI 数据刷新返回失败");
            }
            sleepForRateLimit(2000);
            boolean historySuccess = fundPortfolioService.refreshPortfolioRsiHistory(100);
            if (!historySuccess) {
                throw new BusinessException("基金组合 RSI 历史数据刷新返回失败");
            }
        });
```

Replace `refreshMomentumStrategy` body with:

```java
        runRefreshTask("刷新动量策略数据", () -> {
            int count = momentumStrategyService.refreshMomentumStrategy();
            logger.info("动量策略数据刷新完成，共生成 {} 条新交易记录", count);
        });
```

Replace `executeDailyMomentumStrategy` body with:

```java
        runRefreshTask("执行每日动量策略", () -> {
            int count = momentumStrategyService.refreshMomentumStrategy();
            logger.info("每日动量策略执行完成，共生成 {} 条新交易记录", count);
        });
```

Replace `fullDataRefresh` body with:

```java
        runRefreshTask("执行完整数据刷新", () -> {
            logger.info("正在刷新市场数据...");
            marketDataService.refreshMarketOverview();
            sleepForRateLimit(5000);

            logger.info("正在刷新ETF RSI数据...");
            int rsiCount = rsiAnalysisService.refreshAllEtfRsi();
            logger.info("刷新了 {} 条RSI记录", rsiCount);
            sleepForRateLimit(5000);

            logger.info("正在刷新ETF MA策略数据...");
            int maCount = maStrategyService.refreshAllEtfMa();
            logger.info("刷新了 {} 条MA记录", maCount);
            sleepForRateLimit(5000);

            logger.info("正在刷新基金组合 RSI 数据...");
            if (!fundPortfolioService.refreshPortfolioRsi()) {
                throw new BusinessException("基金组合 RSI 数据刷新返回失败");
            }
            sleepForRateLimit(2000);

            logger.info("正在刷新基金组合 RSI 历史数据...");
            if (!fundPortfolioService.refreshPortfolioRsiHistory(100)) {
                throw new BusinessException("基金组合 RSI 历史数据刷新返回失败");
            }
            sleepForRateLimit(5000);

            logger.info("正在刷新动量策略数据...");
            int momentumCount = momentumStrategyService.refreshMomentumStrategy();
            logger.info("刷新了 {} 条动量策略交易记录", momentumCount);
        });
```

Add the sleep helper:

```java
    /**
     * 等待第三方接口限流窗口
     *
     * @param millis 等待毫秒数
     */
    private void sleepForRateLimit(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("刷新任务等待被中断", e);
        }
    }
```

- [ ] **Step 3: Make dynamic email schedule expose failures**

In `DynamicScheduleConfig`, inside the scheduled lambda, replace the catch block with:

```java
                                } catch (RuntimeException e) {
                                    logger.error("发送每日报告邮件异常 ({})", time, e);
                                    throw e;
                                } catch (Exception e) {
                                    logger.error("发送每日报告邮件异常 ({})", time, e);
                                    throw new IllegalStateException("发送每日报告邮件异常: " + time, e);
                                }
```

- [ ] **Step 4: Verify backend tests**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
```

Expected: all backend tests pass.

- [ ] **Step 5: Commit**

Run:

```bash
git add backend/src/main/java/com/fund/analysis/scheduled/DataRefreshTask.java backend/src/main/java/com/fund/analysis/config/DynamicScheduleConfig.java
git commit -m "fix: expose scheduled refresh failures"
```

---

### Task 6: Tighten API Validation And Fresh Data Queries

**Files:**
- Modify: `backend/src/main/java/com/fund/analysis/controller/EtfController.java`
- Modify: `backend/src/main/java/com/fund/analysis/controller/MaStrategyController.java`
- Modify: `backend/src/main/java/com/fund/analysis/controller/RsiBacktestController.java`
- Modify: `backend/src/main/resources/mapper/FundInfoMapper.xml`
- Modify: `frontend/src/pages/EtfManagement.jsx`

- [ ] **Step 1: Fix latest fund recommendation query**

Replace `selectLatestRecommendations` in `FundInfoMapper.xml` with:

```xml
    <select id="selectLatestRecommendations" resultType="com.fund.analysis.entity.FundInfo">
        SELECT t1.* FROM fund_info t1
        INNER JOIN (
            SELECT fund_code, MAX(data_time) as max_time
            FROM fund_info
            WHERE (is_custom = 0 OR is_custom IS NULL)
              AND deleted = 0
            GROUP BY fund_code
        ) t2 ON t1.fund_code = t2.fund_code AND t1.data_time = t2.max_time
        WHERE (t1.is_custom = 0 OR t1.is_custom IS NULL)
          AND t1.deleted = 0
        ORDER BY t1.calmar_rank
        LIMIT #{limit}
    </select>
```

- [ ] **Step 2: Add ETF controller validation**

Add imports to `EtfController.java`:

```java
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.DataUnavailableException;
```

Replace `addEtf`, `updateEtf`, and `deleteEtf` with:

```java
    @PostMapping("/add")
    public Result<Void> addEtf(@RequestBody EtfInfo etfInfo) {
        validateEtfInfo(etfInfo, false);
        etfInfo.setCreateTime(new Date());
        etfInfo.setUpdateTime(new Date());
        etfInfoMapper.insert(etfInfo);
        return Result.success("添加成功", null);
    }

    @PostMapping("/update")
    public Result<Void> updateEtf(@RequestBody EtfInfo etfInfo) {
        validateEtfInfo(etfInfo, true);
        etfInfo.setUpdateTime(new Date());
        int updated = etfInfoMapper.updateById(etfInfo);
        if (updated == 0) {
            throw new DataUnavailableException("ETF不存在: " + etfInfo.getId());
        }
        return Result.success("更新成功", null);
    }

    @PostMapping("/delete/{id}")
    public Result<Void> deleteEtf(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("ETF ID不能为空");
        }
        int deleted = etfInfoMapper.deleteById(id);
        if (deleted == 0) {
            throw new DataUnavailableException("ETF不存在: " + id);
        }
        return Result.success("删除成功", null);
    }
```

Add helper methods:

```java
    /**
     * 校验 ETF 请求参数
     *
     * @param etfInfo ETF 信息
     * @param requireId 是否要求 ID
     */
    private void validateEtfInfo(EtfInfo etfInfo, boolean requireId) {
        if (etfInfo == null) {
            throw new BadRequestException("ETF信息不能为空");
        }
        if (requireId && (etfInfo.getId() == null || etfInfo.getId() <= 0)) {
            throw new BadRequestException("ETF ID不能为空");
        }
        if (etfInfo.getEtfName() == null || etfInfo.getEtfName().trim().isEmpty()) {
            throw new BadRequestException("ETF名称不能为空");
        }
        if (etfInfo.getEtfCode() == null || etfInfo.getEtfCode().trim().isEmpty()) {
            throw new BadRequestException("ETF代码不能为空");
        }
        if (etfInfo.getCategory() == null || etfInfo.getCategory() < 1 || etfInfo.getCategory() > 6) {
            throw new BadRequestException("ETF分类必须在1-6之间");
        }
        if (etfInfo.getEnabled() == null || (etfInfo.getEnabled() != 0 && etfInfo.getEnabled() != 1)) {
            throw new BadRequestException("启用状态必须为0或1");
        }
        validateThreshold(etfInfo.getRsiBuyThreshold(), "RSI买入阈值");
        validateThreshold(etfInfo.getRsiSellThreshold(), "RSI卖出阈值");
    }

    /**
     * 校验 RSI 阈值
     *
     * @param threshold 阈值
     * @param fieldName 字段名称
     */
    private void validateThreshold(Double threshold, String fieldName) {
        if (threshold != null && (threshold < 0 || threshold > 100)) {
            throw new BadRequestException(fieldName + "必须在0-100之间");
        }
    }
```

- [ ] **Step 3: Add MA backtest validation**

In `MaStrategyController`, add:

```java
    /**
     * 校验回测初始资金
     *
     * @param initialCapital 初始资金
     */
    private void validateInitialCapital(BigDecimal initialCapital) {
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("初始资金必须大于0");
        }
    }
```

Call it before running the service:

```java
        validateInitialCapital(initialCapital);
```

- [ ] **Step 4: Add RSI backtest validation**

In `RsiBacktestController`, add:

```java
    /**
     * 校验 RSI 回测参数
     *
     * @param initialCapital 初始资金
     * @param rsiPeriod RSI周期
     * @param rsiBuyThreshold 买入阈值
     * @param rsiSellThreshold 卖出阈值
     * @param fixedAmountPerTrade 每笔交易金额
     */
    private void validateBacktestParams(BigDecimal initialCapital, Integer rsiPeriod,
                                        BigDecimal rsiBuyThreshold, BigDecimal rsiSellThreshold,
                                        BigDecimal fixedAmountPerTrade) {
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("初始资金必须大于0");
        }
        if (rsiPeriod == null || rsiPeriod < 2 || rsiPeriod > 100) {
            throw new BadRequestException("RSI周期必须在2-100之间");
        }
        if (rsiBuyThreshold == null || rsiBuyThreshold.compareTo(BigDecimal.ZERO) <= 0
                || rsiBuyThreshold.compareTo(new BigDecimal("100")) >= 0) {
            throw new BadRequestException("RSI买入阈值必须在0-100之间");
        }
        if (rsiSellThreshold == null || rsiSellThreshold.compareTo(BigDecimal.ZERO) <= 0
                || rsiSellThreshold.compareTo(new BigDecimal("100")) >= 0) {
            throw new BadRequestException("RSI卖出阈值必须在0-100之间");
        }
        if (rsiBuyThreshold.compareTo(rsiSellThreshold) >= 0) {
            throw new BadRequestException("RSI买入阈值必须小于卖出阈值");
        }
        if (fixedAmountPerTrade == null || fixedAmountPerTrade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("每笔交易金额必须大于0");
        }
    }
```

Call it before running the service:

```java
        validateBacktestParams(initialCapital, rsiPeriod, rsiBuyThreshold, rsiSellThreshold, fixedAmountPerTrade);
```

- [ ] **Step 5: Make ETF frontend respect backend response code**

In `EtfManagement.jsx`, replace the submit branch with:

```jsx
      const response = editingRecord
        ? await etfApi.update({ ...values, id: editingRecord.id })
        : await etfApi.add(values)

      if (response.code !== 0) {
        message.error(response.message || '保存失败')
        return
      }

      message.success(editingRecord ? '更新成功' : '添加成功')
      setModalVisible(false)
      await loadData()
```

Replace delete success handling with:

```jsx
          const response = await etfApi.delete(record.id)
          if (response.code !== 0) {
            message.error(response.message || '删除失败')
            return
          }
          message.success('删除成功')
          await loadData()
```

- [ ] **Step 6: Verify backend and frontend**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
cd /Users/fciasth/project/trade/fund/frontend
npm run build
```

Expected: backend tests and frontend build pass.

- [ ] **Step 7: Commit**

Run:

```bash
git add backend/src/main/java/com/fund/analysis/controller backend/src/main/resources/mapper/FundInfoMapper.xml frontend/src/pages/EtfManagement.jsx
git commit -m "fix: validate api writes and latest fund data"
```

---

### Task 7: Make Frontend Errors And Formatting Explicit

**Files:**
- Create: `frontend/src/utils/formatters.js`
- Modify: `frontend/src/services/api.js`
- Modify: `frontend/src/pages/Dashboard.jsx`
- Modify: `frontend/src/pages/FundPortfolio.jsx`
- Modify: `frontend/src/pages/FundRecommendation.jsx`
- Modify: `frontend/src/pages/MomentumStrategy.jsx`
- Modify: `frontend/src/utils/momentumBacktestRange.js`
- Modify: `frontend/src/utils/momentumBacktestRange.test.js`
- Modify: `frontend/package.json`

- [ ] **Step 1: Add formatter utility**

Create `frontend/src/utils/formatters.js`:

```javascript
import dayjs from 'dayjs'

// 判断值是否为空值，0 不视为空值。
export function isNil(value) {
  return value === null || value === undefined || value === ''
}

// 格式化普通数字，空值显示为占位符。
export function formatNumber(value, digits = 2, emptyText = '-') {
  if (isNil(value)) {
    return emptyText
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return emptyText
  }
  return number.toFixed(digits)
}

// 格式化百分比数字，空值显示为占位符。
export function formatPercent(value, digits = 2, emptyText = '-') {
  const formatted = formatNumber(value, digits, emptyText)
  return formatted === emptyText ? formatted : `${formatted}%`
}

// 格式化金额，空值显示为占位符。
export function formatCurrency(value, digits = 2, emptyText = '-') {
  if (isNil(value)) {
    return emptyText
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return emptyText
  }
  return `¥${number.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })}`
}

// 格式化日期时间，非法日期显示为占位符。
export function formatDateTime(value, emptyText = '-') {
  if (isNil(value)) {
    return emptyText
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : emptyText
}
```

- [ ] **Step 2: Add frontend test script and ESM type**

Update `frontend/package.json`:

```json
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "node --test src/utils/*.test.js"
  },
```

- [ ] **Step 3: Normalize API errors without duplicate console noise**

In `frontend/src/services/api.js`, replace the response error interceptor with:

```javascript
  error => {
    const message = error.response?.data?.message || error.message || '网络错误'
    error.normalizedMessage = message
    error.statusCode = error.response?.status
    return Promise.reject(error)
  }
)
```

- [ ] **Step 4: Make Dashboard portfolio RSI failure visible**

In `Dashboard.jsx`, add state:

```jsx
  const [portfolioRsiError, setPortfolioRsiError] = useState(null)
```

Replace the `Promise.all` block with:

```jsx
        const marketResponse = await marketApi.getOverview()
        if (marketResponse.code === 0) {
          setMarketData(marketResponse.data)
        } else {
          setError(marketResponse.message)
          return
        }

        try {
          const portfolioRsiResponse = await portfolioApi.getPortfolioRsi()
          if (portfolioRsiResponse.code === 0) {
            setPortfolioRsiData(portfolioRsiResponse.data)
            setPortfolioRsiError(null)
          } else {
            setPortfolioRsiData(null)
            setPortfolioRsiError(portfolioRsiResponse.message || '组合 RSI 加载失败')
          }
        } catch (portfolioError) {
          setPortfolioRsiData(null)
          setPortfolioRsiError(portfolioError.normalizedMessage || portfolioError.message || '组合 RSI 加载失败')
        }
```

Inside the portfolio RSI card, render warning before values:

```jsx
            {portfolioRsiError && (
              <Alert
                type="warning"
                showIcon
                message="组合 RSI 加载失败"
                description={portfolioRsiError}
                style={{ marginBottom: 12 }}
              />
            )}
```

- [ ] **Step 5: Fix URL parameter parsing**

Replace `frontend/src/utils/momentumBacktestRange.js` with:

```javascript
const START_DATE_PARAM = 'startDate'
const END_DATE_PARAM = 'endDate'
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

export function getMomentumBacktestRangeFromSearchParams(searchParams) {
  const startDate = searchParams.get(START_DATE_PARAM)
  const endDate = searchParams.get(END_DATE_PARAM)

  if (!startDate && !endDate) {
    return { range: null, error: null }
  }

  if (!startDate || !endDate) {
    return { range: null, error: '动量策略回测区间 URL 参数不完整' }
  }

  if (!DATE_PATTERN.test(startDate) || !DATE_PATTERN.test(endDate)) {
    return { range: null, error: '动量策略回测区间 URL 参数格式错误' }
  }

  return { range: { startDate, endDate }, error: null }
}

export function createMomentumBacktestSearchParams(range) {
  return {
    [START_DATE_PARAM]: range.startDate,
    [END_DATE_PARAM]: range.endDate,
  }
}
```

Update `momentumBacktestRange.test.js` expected values:

```javascript
  assert.deepEqual(getMomentumBacktestRangeFromSearchParams(searchParams), {
    range: {
      startDate: '2025-01-01',
      endDate: '2025-12-31',
    },
    error: null,
  })
```

```javascript
  assert.deepEqual(getMomentumBacktestRangeFromSearchParams(new URLSearchParams()), {
    range: null,
    error: null,
  })
```

Add this test:

```javascript
test('URL 参数不完整时返回显式错误', () => {
  assert.deepEqual(getMomentumBacktestRangeFromSearchParams(new URLSearchParams('startDate=2025-01-01')), {
    range: null,
    error: '动量策略回测区间 URL 参数不完整',
  })
})
```

- [ ] **Step 6: Sync MomentumStrategy with URL state**

In `MomentumStrategy.jsx`, change initial parsing:

```jsx
  const initialBacktestState = useMemo(
    () => getMomentumBacktestRangeFromSearchParams(searchParams),
    [searchParams],
  )
  const initialBacktestRange = initialBacktestState.range
  const [urlRangeError, setUrlRangeError] = useState(initialBacktestState.error)
```

Add effect after `reloadStrategyData` is defined:

```jsx
  useEffect(() => {
    const nextState = getMomentumBacktestRangeFromSearchParams(searchParams)
    setUrlRangeError(nextState.error)
    setActiveBacktestRange(nextState.range)
    setBacktestStartDate(nextState.range ? dayjs(nextState.range.startDate) : null)
    setBacktestEndDate(nextState.range ? dayjs(nextState.range.endDate) : null)
    reloadStrategyData(nextState.range)
  }, [searchParams])
```

Remove the old mount-only effect:

```jsx
  useEffect(() => {
    reloadStrategyData()
  }, [])
```

Render URL error below the page title:

```jsx
      {urlRangeError && (
        <Alert
          type="error"
          showIcon
          message="回测区间参数错误"
          description={urlRangeError}
          style={{ marginBottom: 16 }}
        />
      )}
```

Add `Alert` to the Ant Design import list.

- [ ] **Step 7: Replace falsy numeric display on touched pages**

In `FundPortfolio.jsx` and `FundRecommendation.jsx`, replace checks like:

```jsx
if (!val) return '-'
```

with:

```jsx
if (val == null || val === '') return '-'
```

For `Statistic` values in `FundPortfolio.jsx`, replace:

```jsx
value={rsiData.rsi14 ? rsiData.rsi14.toFixed(2) : 'N/A'}
valueStyle={{ color: rsiData.rsi14 ? getRsiColor(rsiData.rsi14) : '#000' }}
```

with:

```jsx
value={rsiData.rsi14 != null ? rsiData.rsi14.toFixed(2) : 'N/A'}
valueStyle={{ color: rsiData.rsi14 != null ? getRsiColor(rsiData.rsi14) : '#000' }}
```

Apply the same explicit `!= null` pattern to `rsi90` and `weeklyRsi14`.

- [ ] **Step 8: Verify frontend tests and build**

Run:

```bash
cd /Users/fciasth/project/trade/fund/frontend
npm test
npm run build
```

Expected: tests pass and build passes.

- [ ] **Step 9: Keep frontend utility tests**

The utility test files existed before this plan. Keep them because they are not newly created temporary tests.

- [ ] **Step 10: Commit**

Run:

```bash
git add frontend/src frontend/package.json frontend/package-lock.json
git commit -m "fix: expose frontend errors and normalize formatting"
```

---

### Task 8: Improve Frontend Loading And Responsive Layout

**Files:**
- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/components/MainLayout.jsx`
- Modify: `frontend/src/pages/MaStrategy.jsx`
- Modify: `frontend/src/pages/RsiBacktest.jsx`

- [ ] **Step 1: Lazy-load route pages**

Replace page imports in `App.jsx` with:

```jsx
import React, { Suspense, lazy } from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { ConfigProvider, Spin } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import MainLayout from './components/MainLayout'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const RsiAnalysis = lazy(() => import('./pages/RsiAnalysis'))
const MaStrategy = lazy(() => import('./pages/MaStrategy'))
const MomentumStrategy = lazy(() => import('./pages/MomentumStrategy'))
const FundRecommendation = lazy(() => import('./pages/FundRecommendation'))
const FundPortfolio = lazy(() => import('./pages/FundPortfolio'))
const EtfManagement = lazy(() => import('./pages/EtfManagement'))
const SystemConfig = lazy(() => import('./pages/SystemConfig'))
const RsiBacktest = lazy(() => import('./pages/RsiBacktest'))
```

Wrap `Routes` with:

```jsx
          <Suspense fallback={
            <div className="loading-container">
              <Spin size="large" tip="页面加载中..." />
            </div>
          }>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/rsi-analysis" element={<RsiAnalysis />} />
              <Route path="/rsi-backtest" element={<RsiBacktest />} />
              <Route path="/ma-strategy" element={<MaStrategy />} />
              <Route path="/momentum-strategy" element={<MomentumStrategy />} />
              <Route path="/fund-recommendation" element={<FundRecommendation />} />
              <Route path="/fund-portfolio" element={<FundPortfolio />} />
              <Route path="/etf-management" element={<EtfManagement />} />
              <Route path="/system-config" element={<SystemConfig />} />
            </Routes>
          </Suspense>
```

- [ ] **Step 2: Make layout responsive**

In `MainLayout.jsx`, add import:

```jsx
import { Layout, Menu, Grid } from 'antd'
```

Add after `const { Header, Sider, Content } = Layout`:

```jsx
const { useBreakpoint } = Grid
```

Inside `MainLayout`, add:

```jsx
  const screens = useBreakpoint()
  const isMobile = !screens.md
  const siderWidth = collapsed ? 80 : 200
```

Update `Sider` props:

```jsx
        breakpoint="md"
        collapsedWidth={isMobile ? 0 : 80}
```

Update `Sider` style:

```jsx
          zIndex: 1000,
```

Update content layout margin:

```jsx
      <Layout style={{ marginLeft: isMobile ? 0 : siderWidth, transition: 'all 0.2s' }}>
```

Update `Header` style for mobile:

```jsx
          padding: isMobile ? '0 12px' : '0 24px',
```

Update `Content` style:

```jsx
          margin: isMobile ? '12px' : '24px',
          padding: isMobile ? '12px' : '24px',
```

- [ ] **Step 3: Make MA backtest form responsive**

In `MaStrategy.jsx`, replace four `Col span={6}` instances in the backtest form with:

```jsx
<Col xs={24} sm={12} lg={6}>
```

- [ ] **Step 4: Make RSI backtest form responsive**

In `RsiBacktest.jsx`, replace six `Col span={4}` instances in the parameter form with:

```jsx
<Col xs={24} sm={12} lg={4}>
```

Replace the second row `Col span={4}` instances with:

```jsx
<Col xs={24} sm={12} lg={4}>
```

- [ ] **Step 5: Verify build output**

Run:

```bash
cd /Users/fciasth/project/trade/fund/frontend
npm run build
```

Expected: build passes. The main initial chunk should be smaller than before route lazy loading; if Vite still warns because vendor chunks exceed 500KB, note the exact emitted chunk names in the final report.

- [ ] **Step 6: Commit**

Run:

```bash
git add frontend/src/App.jsx frontend/src/components/MainLayout.jsx frontend/src/pages/MaStrategy.jsx frontend/src/pages/RsiBacktest.jsx
git commit -m "fix: improve frontend loading and responsive layout"
```

---

### Task 9: Update Documentation And Final Verification

**Files:**
- Modify: `README.md`
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`

- [ ] **Step 1: Fix API response documentation**

In `README.md`, replace the API response example with:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Replace the line:

```text
错误响应仍使用同一结构，同时 HTTP 状态码反映错误类型：
```

with:

```text
成功响应使用 code=0；错误响应仍使用同一结构，code 使用 HTTP 状态码，同时 HTTP 状态码反映错误类型：
```

- [ ] **Step 2: Add frontend test command to README**

In the verification command section, add:

```bash
cd /Users/fciasth/project/trade/fund/frontend
npm test
npm run build
```

- [ ] **Step 3: Mention Docker verification prerequisite**

In the Docker verification section, add:

```text
`docker compose config` 需要本机已安装 Docker CLI；如果命令不存在，应记录为环境缺口，不要用其他命令伪造验证成功。
```

- [ ] **Step 4: Ensure no temporary tests remain**

Run:

```bash
find backend/src/test frontend/src -name '*Temporary*' -o -name '*temp*'
git status --short
```

Expected: no temporary test files from this plan remain. Existing test files may remain.

- [ ] **Step 5: Run full verification**

Run:

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test
cd /Users/fciasth/project/trade/fund/frontend
npm test
npm run build
cd /Users/fciasth/project/trade/fund
command -v docker
```

Expected:

```text
backend tests pass
frontend tests pass
frontend build passes
docker path printed if Docker CLI is installed
```

If `command -v docker` prints nothing, record `Docker CLI not installed` as an environment limitation.

- [ ] **Step 6: Run Docker compose config if Docker exists**

Run only when `command -v docker` printed a path:

```bash
cd /Users/fciasth/project/trade/fund
docker compose config
```

Expected: compose config renders successfully.

- [ ] **Step 7: Commit docs**

Run:

```bash
git add README.md frontend/package.json frontend/package-lock.json
git commit -m "docs: update verification and api contract"
```

---

## Final Review Checklist

- [ ] `mvn test` passes.
- [ ] `npm test` passes.
- [ ] `npm run build` passes.
- [ ] `docker compose config` passes when Docker CLI is installed; otherwise the missing CLI is explicitly reported.
- [ ] `git status --short` contains no untracked temporary test files.
- [ ] No new silent fallback, mock success, swallowed exception, or hidden degradation path was introduced.
- [ ] User's pre-existing change in `backend/src/main/java/com/fund/analysis/service/FundAnalysisService.java` was preserved and integrated rather than reverted.
- [ ] README says success `code=0`, matching `Result.success()`.
