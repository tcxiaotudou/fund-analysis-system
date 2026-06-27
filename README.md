# 基金分析系统

基金分析系统采用前后端分离架构，当前后端主线为 Spring Boot，前端为 React + Vite。仓库已收口为一个完整部署入口：根目录 `docker-compose.yml`。

## 项目结构

```text
fund/
├── backend/                 # Spring Boot 后端
│   ├── database/init.sql    # 完整数据库初始化脚本
│   ├── src/main/java        # 控制器、服务、配置、实体、Mapper
│   ├── src/main/resources   # application.yml、Mapper XML
│   └── pom.xml
├── frontend/                # React 前端
│   ├── src/components       # 公共布局
│   ├── src/pages            # 页面模块
│   ├── src/services/api.js  # 统一 API 封装
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml       # 唯一完整部署入口
├── env.example              # Docker 环境变量示例
└── README.md
```

## 技术栈

- 前端：React 18、React Router 6、Ant Design 5、Axios、Recharts、Vite 4
- 后端：Java 8、Spring Boot 2.7、MyBatis-Plus、MySQL 8、Redis、Gson、Apache HttpClient
- 外部数据：新浪行情接口、韭圈儿相关基金/估值接口
- 邮件：通过系统配置和 SMTP 环境变量发送日报

## 本地开发

### 1. 准备数据库和 Redis

```bash
cd /Users/fciasth/project/trade/fund
mysql -uroot -p < backend/database/init.sql
```

`init.sql` 已包含当前完整表结构和基础数据，不需要再执行额外增量脚本。

### 2. 配置后端环境变量

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/fund_analysis?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="你的 MySQL 密码"
export SPRING_REDIS_HOST="127.0.0.1"
export SPRING_REDIS_PORT="6379"
export SPRING_REDIS_PASSWORD="你的 Redis 密码"
export SPRING_MAIL_HOST="smtp.qq.com"
export SPRING_MAIL_PORT="587"
export SPRING_MAIL_USERNAME=""
export SPRING_MAIL_PASSWORD=""
```

仓库默认配置只提供本地占位值，真实数据库、Redis、邮箱账号和授权码应由运行环境显式提供。

### 3. 启动后端

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn spring-boot:run
```

- API 根路径：`http://localhost:8080/api`
- 健康检查：`http://localhost:8080/api/health`

### 4. 启动前端

```bash
cd /Users/fciasth/project/trade/fund/frontend
npm install
npm run dev
```

开发环境默认访问 `http://localhost:3000`，Vite 会将 `/api` 代理到 `http://localhost:8080`。

## Docker 部署

```bash
cd /Users/fciasth/project/trade/fund
cp env.example .env
docker compose up -d --build
```

根 `docker-compose.yml` 会启动 MySQL、Redis、后端和前端。MySQL 首次创建数据卷时会自动执行 `backend/database/init.sql`。

服务端口由 `.env` 控制：

- `FRONTEND_PORT`：宿主机前端端口，容器内 Nginx 固定监听 `80`
- `BACKEND_PORT`：宿主机后端端口，容器内 Spring Boot 固定监听 `8080`
- `MYSQL_PORT`：宿主机 MySQL 端口
- `REDIS_PORT`：宿主机 Redis 端口

前端 Nginx 通过 compose 服务名代理后端：

```nginx
proxy_pass http://backend:8080;
```

常用命令：

```bash
docker compose config
docker compose ps
docker compose logs -f
docker compose logs -f backend
docker compose logs -f frontend
docker compose down
```

## 核心能力

- 市场概览：14 日/90 日 RSI、股债平衡建议、沪深 300 风险溢价、5 年均线偏离度
- ETF RSI：ETF 买入信号、RSI 区间和阈值管理
- RSI 回测：定额买卖策略回测
- MA 策略：10 日/30 日均线信号和回测
- 21 日动量策略：轮动交易记录、收益表现和回测
- 基金推荐：推荐基金、黑名单、持仓标记
- 基金组合：持仓、权重、组合 RSI 和历史曲线
- 系统配置：邮件配置和真实日报发送

## API 约定

后端统一使用 `/api` 前缀，响应体保持：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

成功响应使用 code=0；错误响应仍使用同一结构，code 使用 HTTP 状态码，同时 HTTP 状态码反映错误类型：

- `400`：请求参数或业务输入错误
- `502`：第三方行情/基金接口错误
- `500`：服务内部错误

保留的主要接口：

- `GET /api/market/overview`
- `GET /api/rsi/*`
- `POST /api/rsi-backtest/run`
- `GET|POST /api/ma-strategy/*`
- `GET|POST /api/momentum-strategy/*`
- `GET|POST /api/fund/*`
- `GET|POST /api/fund/portfolio/*`
- `GET|POST /api/etf/*`
- `GET /api/system-config/email`
- `POST /api/system-config/email`
- `POST /api/system-config/email/send-now`
- `GET /api/health`

`POST /api/system-config/email/test` 已删除，立即发送日报只保留真实发送入口 `POST /api/system-config/email/send-now`。

## 数据库表

- `etf_info`：ETF 基础信息与监控参数
- `rsi_analysis`：RSI 计算结果
- `ma_strategy`：MA 策略结果
- `momentum_strategy_transaction`：动量策略交易记录
- `momentum_strategy_performance`：动量策略每日绩效
- `fund_info`：基金推荐与持仓数据
- `fund_blacklist`：基金黑名单
- `fund_portfolio_rsi`：基金组合 RSI 汇总
- `fund_portfolio_rsi_history`：基金组合 RSI 历史
- `stock_bond_balance`：市场股债平衡建议与估值指标
- `index_valuation`：指数估值缓存
- `system_config`：系统配置

## 验证命令

```bash
cd /Users/fciasth/project/trade/fund/backend
mvn test

cd /Users/fciasth/project/trade/fund/frontend
npm test
npm run build

cd /Users/fciasth/project/trade/fund
docker compose config
```

`docker compose config` 需要本机已安装 Docker CLI；如果命令不存在，应记录为环境缺口，不要用其他命令伪造验证成功。
