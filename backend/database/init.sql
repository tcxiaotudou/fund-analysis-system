-- 基金和ETF投资策略分析系统数据库初始化脚本
-- 明确指定客户端字符集，避免初始化导入中文时被按错误字符集解析
SET NAMES utf8mb4;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS fund_analysis DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fund_analysis;

-- fund_analysis.etf_info definition

CREATE TABLE `etf_info` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `etf_name` varchar(100) NOT NULL COMMENT 'ETF名称',
                            `etf_code` varchar(20) NOT NULL COMMENT 'ETF代码',
                            `category` int DEFAULT '1' COMMENT 'ETF分类：1-指数 2-行业 3-主题 4-债券 5-商品 6-海外',
                            `enabled` int DEFAULT '1' COMMENT '是否启用监控：0-否 1-是',
                            `rsi_buy_threshold` decimal(10,2) DEFAULT '30.00' COMMENT 'RSI买入阈值',
                            `rsi_sell_threshold` decimal(10,2) DEFAULT '70.00' COMMENT 'RSI卖出阈值',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `deleted` int DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_etf_code` (`etf_code`),
                            KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ETF信息表';


-- fund_analysis.fund_blacklist definition

CREATE TABLE `fund_blacklist` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `fund_code` varchar(20) NOT NULL COMMENT '基金代码',
                                  `fund_name` varchar(200) DEFAULT NULL COMMENT '基金名称',
                                  `exclude_reason` varchar(500) NOT NULL COMMENT '排除原因',
                                  `excluded_by` varchar(100) DEFAULT NULL COMMENT '排除操作人',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_fund_code` (`fund_code`),
                                  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基金黑名单表';


-- fund_analysis.fund_info definition

CREATE TABLE `fund_info` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             `fund_code` varchar(20) NOT NULL COMMENT '基金代码',
                             `fund_name` varchar(200) NOT NULL COMMENT '基金名称',
                             `manager_name` varchar(100) DEFAULT NULL COMMENT '基金经理姓名',
                             `manager_years` varchar(50) DEFAULT NULL COMMENT '基金经理管理年限',
                             `scale` varchar(50) DEFAULT NULL COMMENT '基金规模',
                             `year_to_date_return` varchar(20) DEFAULT NULL COMMENT '今年以来收益率',
                             `five_year_return` decimal(10,2) DEFAULT NULL COMMENT '近5年年化收益率',
                             `sharpe_rank` int DEFAULT NULL COMMENT '近5年夏普比率排名',
                             `calmar_rank` int DEFAULT NULL COMMENT '近5年卡玛比率排名',
                             `max_drawdown` varchar(20) DEFAULT NULL COMMENT '近5年最大回撤',
                             `redemption_fee` varchar(500) DEFAULT NULL COMMENT '赎回费率信息',
                             `is_holding` int DEFAULT '0' COMMENT '是否持有：0-否 1-是',
                             `portfolio_weight` decimal(5,2) DEFAULT '0.00' COMMENT '组合中的权重(%)，范围0-100，小数点后2位',
                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                             `data_time` datetime DEFAULT NULL COMMENT '数据来源时间',
                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             `deleted` int DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
                             `is_custom` tinyint DEFAULT '0' COMMENT '是否为手动添加: 0-来自推荐API 1-手动添加',
                             PRIMARY KEY (`id`),
                             KEY `idx_fund_code` (`fund_code`),
                             KEY `idx_five_year_return` (`five_year_return`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基金信息表';


-- fund_analysis.fund_portfolio_rsi definition

CREATE TABLE `fund_portfolio_rsi` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `rsi14` decimal(10,2) DEFAULT NULL COMMENT '14日RSI',
                                      `rsi90` decimal(10,2) DEFAULT NULL COMMENT '90日RSI',
                                      `weekly_rsi14` decimal(10,2) DEFAULT NULL COMMENT '14周RSI',
                                      `fund_count` int DEFAULT '0' COMMENT '持有基金数量',
                                      `fund_codes` text COMMENT '持有基金代码列表（逗号分隔）',
                                      `data_time` datetime DEFAULT NULL COMMENT '数据时间',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_data_time` (`data_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基金组合RSI数据表';


-- fund_analysis.fund_portfolio_rsi_history definition

CREATE TABLE `fund_portfolio_rsi_history` (
                                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                              `rsi14` decimal(10,2) DEFAULT NULL COMMENT '14日RSI',
                                              `fund_count` int DEFAULT '0' COMMENT '持有基金数量',
                                              `fund_codes` text COMMENT '持有基金代码列表（逗号分隔）',
                                              `data_date` varchar(20) DEFAULT NULL COMMENT '数据日期（格式：MM/dd）',
                                              `data_time` datetime DEFAULT NULL COMMENT '数据时间',
                                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              PRIMARY KEY (`id`),
                                              KEY `idx_data_time` (`data_time`),
                                              KEY `idx_data_date` (`data_date`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基金组合RSI历史数据表';


-- fund_analysis.ma_strategy definition

CREATE TABLE `ma_strategy` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `etf_code` varchar(20) NOT NULL COMMENT 'ETF代码',
                               `etf_name` varchar(100) DEFAULT NULL COMMENT 'ETF名称',
                               `ma10` decimal(10,3) DEFAULT NULL COMMENT '10日均线',
                               `current_daily` decimal(10,3) DEFAULT NULL COMMENT '当前日K线收盘价',
                               `ma30` decimal(10,3) DEFAULT NULL COMMENT '30日均线',
                               `boll_mid` decimal(10,3) DEFAULT NULL COMMENT '布林带中轨(20日均线)',
                               `boll_upper` decimal(10,3) DEFAULT NULL COMMENT '布林带上轨',
                               `boll_lower` decimal(10,3) DEFAULT NULL COMMENT '布林带下轨',
                               `is_buy_signal` int DEFAULT '0' COMMENT '是否为买入信号：0-否 1-是',
                               `is_sell_signal` int DEFAULT '0' COMMENT '是否为卖出信号：0-否 1-是',
                               `signal_description` varchar(500) DEFAULT NULL COMMENT '信号说明',
                               `data_time` datetime DEFAULT NULL COMMENT '数据时间',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               PRIMARY KEY (`id`),
                               KEY `idx_etf_code` (`etf_code`),
                               KEY `idx_data_time` (`data_time`),
                               KEY `idx_buy_signal` (`is_buy_signal`)
) ENGINE=InnoDB AUTO_INCREMENT=151 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='移动平均线+布林带组合策略表';


-- fund_analysis.momentum_strategy_transaction definition

CREATE TABLE `momentum_strategy_transaction` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                 `transaction_date` date NOT NULL COMMENT '交易日期',
                                                 `etf_code` varchar(20) NOT NULL COMMENT 'ETF代码',
                                                 `etf_name` varchar(100) DEFAULT NULL COMMENT 'ETF名称',
                                                 `transaction_type` varchar(10) NOT NULL COMMENT '交易类型：buy-买入 sell-卖出',
                                                 `quantity` bigint NOT NULL COMMENT '交易数量（买入为正，卖出为负）',
                                                 `price` decimal(10,3) NOT NULL COMMENT '交易价格',
                                                 `momentum_21` decimal(10,4) DEFAULT NULL COMMENT '21日动量值（用于记录交易时的动量）',
                                                 `initial_capital` decimal(18,2) DEFAULT NULL COMMENT '本轮回测初始资金',
                                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                 PRIMARY KEY (`id`),
                                                 KEY `idx_transaction_date` (`transaction_date`),
                                                 KEY `idx_etf_code` (`etf_code`),
                                                 KEY `idx_transaction_type` (`transaction_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='21日动量策略交易记录表';


-- fund_analysis.momentum_strategy_performance definition

CREATE TABLE `momentum_strategy_performance` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                 `performance_date` date NOT NULL COMMENT '绩效日期',
                                                 `total_value` decimal(18,3) NOT NULL COMMENT '资产总值',
                                                 `return_rate` decimal(10,4) NOT NULL COMMENT '累计收益率（百分比）',
                                                 `holding_etf_code` varchar(20) DEFAULT NULL COMMENT '持仓ETF代码',
                                                 `holding_etf_name` varchar(100) DEFAULT NULL COMMENT '持仓ETF名称',
                                                 `holding_quantity` bigint DEFAULT NULL COMMENT '持仓数量',
                                                 `current_price` decimal(10,3) DEFAULT NULL COMMENT '当前ETF价格',
                                                 `initial_capital` decimal(18,2) NOT NULL COMMENT '本轮回测初始资金',
                                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                 PRIMARY KEY (`id`),
                                                 KEY `idx_performance_date` (`performance_date`),
                                                 KEY `idx_holding_etf_code` (`holding_etf_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='21日动量策略每日绩效表';


-- fund_analysis.rsi_analysis definition

CREATE TABLE `rsi_analysis` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `code` varchar(20) NOT NULL COMMENT '标的代码',
                                `name` varchar(100) DEFAULT NULL COMMENT '标的名称',
                                `period` int NOT NULL COMMENT 'RSI周期',
                                `current_rsi` decimal(10,2) DEFAULT NULL COMMENT '当前RSI值',
                                `high_rsi` decimal(10,2) DEFAULT NULL COMMENT 'RSI最高值',
                                `low_rsi` decimal(10,2) DEFAULT NULL COMMENT 'RSI最低值',
                                `two_thirds_rsi` decimal(10,2) DEFAULT NULL COMMENT 'RSI区间上限',
                                `one_thirds_rsi` decimal(10,2) DEFAULT NULL COMMENT 'RSI区间下限',
                                `high_2_now_low` decimal(10,2) DEFAULT NULL COMMENT '最高点到当前位置的最低值',
                                `days_from_low` int DEFAULT NULL COMMENT '当前RSI距离最低点的天数',
                                `rsi70_days` int DEFAULT '0' COMMENT 'RSI>=70的天数',
                                `rsi65_days` int DEFAULT '0' COMMENT 'RSI>=65的天数',
                                `rsi60_days` int DEFAULT '0' COMMENT 'RSI>=60的天数',
                                `rsi55_days` int DEFAULT '0' COMMENT 'RSI>=55的天数',
                                `total_days` int DEFAULT NULL COMMENT '分析数据总天数',
                                `is_buy_signal` int DEFAULT '0' COMMENT '是否为买入信号：0-否 1-是',
                                `message` varchar(500) DEFAULT NULL COMMENT '分析消息',
                                `data_time` datetime DEFAULT NULL COMMENT '数据时间',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                PRIMARY KEY (`id`),
                                KEY `idx_code` (`code`),
                                KEY `idx_data_time` (`data_time`),
                                KEY `idx_buy_signal` (`is_buy_signal`)
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RSI分析记录表';


-- fund_analysis.stock_bond_balance definition

CREATE TABLE `stock_bond_balance` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `rsi90` decimal(10,2) DEFAULT NULL COMMENT '90日RSI值',
                                      `stock_ratio` int DEFAULT NULL COMMENT '股票配置比例（%）',
                                      `bond_ratio` int DEFAULT NULL COMMENT '债券配置比例（%）',
                                      `suggestion` varchar(50) DEFAULT NULL COMMENT '配置建议描述',
                                      `risk_premium` varchar(20) DEFAULT NULL COMMENT '沪深300风险溢价',
                                      `ma5y_deviation` varchar(50) DEFAULT NULL COMMENT '5年均线偏离度',
                                      `data_time` datetime DEFAULT NULL COMMENT '数据时间',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_data_time` (`data_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股债平衡策略表';


-- fund_analysis.index_valuation definition

CREATE TABLE `index_valuation` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `index_code` varchar(32) NOT NULL COMMENT '指数代码',
                                   `name` varchar(64) NOT NULL COMMENT '指数名称',
                                   `history_low_text` varchar(128) NOT NULL COMMENT '历史低位占比文案',
                                   `valuation_label` varchar(32) NOT NULL COMMENT '估值状态文案',
                                   `level` varchar(32) NOT NULL COMMENT '展示级别',
                                   `pe_date` varchar(16) NOT NULL COMMENT 'PE日期',
                                   `pe` varchar(32) NOT NULL COMMENT 'PE值',
                                   `pe_percentile` varchar(32) NOT NULL COMMENT 'PE百分位',
                                   `data_time` datetime NOT NULL COMMENT '数据时间',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_index_code_data_time` (`index_code`, `data_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='指数估值缓存表';


-- fund_analysis.system_config definition

CREATE TABLE `system_config` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `config_key` varchar(100) NOT NULL COMMENT '配置键',
                                 `config_value` text COMMENT '配置值',
                                 `config_group` varchar(50) DEFAULT NULL COMMENT '配置分组',
                                 `config_name` varchar(100) DEFAULT NULL COMMENT '配置名称',
                                 `description` varchar(500) DEFAULT NULL COMMENT '配置描述',
                                 `enabled` int DEFAULT '1' COMMENT '是否启用：0-否 1-是',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_config_key` (`config_key`),
                                 KEY `idx_config_group` (`config_group`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

-- 插入初始ETF数据
INSERT INTO `etf_info` (`etf_name`, `etf_code`, `category`, `enabled`) VALUES
('中证新能', 'sz399808', 2, 1),
('中证军工', 'sz399967', 2, 1),
('中证传媒', 'sz399971', 2, 1),
('中证银行', 'sz399986', 2, 1),
('中证医疗', 'sz399989', 2, 1),
('中证白酒', 'sz399997', 2, 1),
('中证煤炭', 'sz399998', 2, 1),
('半导体ETF', 'sh512480', 2, 1),
('机器人ETF', 'sz159770', 3, 1),
('智能驾驶ETF', 'sh516520', 3, 1),
('H股ETF', 'sh510900', 6, 1),
('芯片ETF', 'sz159995', 2, 1),
('人工智能ETF', 'sh515070', 3, 1),
('中证红利ETF', 'sh000015', 1, 1),
('恒生科技ETF', 'sh513180', 6, 1),
('港股创新药ETF', 'sh513120', 6, 1),
('电力ETF', 'sh561560', 2, 1),
('钢铁ETF', 'sh515210', 2, 1),
('有色金属ETF', 'sh512400', 2, 1),
('香港证券ETF', 'sh513090', 6, 1),
('油气ETF', 'sz159697', 5, 1),
('机床ETF', 'sz159663', 2, 1),
('通信ETF', 'sh515880', 2, 1),
('法国ETF', 'sh513080', 6, 1),
('德国ETF', 'sh513030', 6, 1),
('标普生物ETF', 'sz161127', 6, 1),
('纳斯达克ETF', 'sh513300', 6, 1),
('美国消费ETF', 'sz162415', 6, 1),
('美国REIT', 'sz160140', 6, 1),
('日本ETF', 'sh513520', 6, 1),
('印度ETF', 'sz164824', 6, 1),
('豆柏ETF', 'sz159985', 5, 1),
('黄金ETF', 'sz159812', 5, 1),
('原油ETF', 'sz160723', 5, 1);

-- 插入初始系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `config_name`, `description`, `enabled`) VALUES
('email_enabled', 'false', 'email', '邮件发送启用', '是否启用邮件发送功能', 1),
('email_recipients', '', 'email', '邮件接收人', '邮件接收人列表，多个邮箱用逗号分隔', 1),
('email_host', 'smtp.qq.com', 'email', 'SMTP服务器', 'SMTP服务器地址', 1),
('email_port', '587', 'email', 'SMTP端口', 'SMTP服务器端口', 1),
('email_username', '', 'email', '发件邮箱', '发件人邮箱地址', 1),
('email_password', '', 'email', '邮箱授权码', '邮箱授权码（QQ邮箱需要使用授权码）', 1),
('email_schedule', '12:00,14:50', 'email', '邮件发送时间', '每天发送邮件的时间点，多个时间用逗号分隔（周一到周五）', 1),
('fund_recommendation_condition_id', '2374632', 'fund', '基金推荐条件ID', '韭圈儿基金推荐接口 condition_id', 1)
ON DUPLICATE KEY UPDATE
`config_value` = VALUES(`config_value`),
`update_time` = CURRENT_TIMESTAMP;


INSERT INTO fund_blacklist
(fund_code, fund_name, exclude_reason, excluded_by, create_time, update_time)
VALUES('004685', '金元顺安元启灵活配置混合', '暂停限购了', '系统用户', '2025-11-22 20:52:58', '2025-11-22 20:52:58');
INSERT INTO fund_blacklist
(fund_code, fund_name, exclude_reason, excluded_by, create_time, update_time)
VALUES('210002', '金鹰红利价值混合A', '卖出永远需要手续费', '系统用户', '2025-11-22 20:53:28', '2025-11-22 20:53:28');
