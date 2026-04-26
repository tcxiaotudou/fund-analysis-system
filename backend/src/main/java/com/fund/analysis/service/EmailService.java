package com.fund.analysis.service;

import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.MaStrategyDTO;
import com.fund.analysis.dto.MomentumPerformanceDTO;
import com.fund.analysis.dto.MomentumTransactionDTO;
import com.fund.analysis.dto.RsiDataDTO;
import com.fund.analysis.entity.FundInfo;
import com.fund.analysis.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 邮件服务类
 * 提供邮件发送功能，支持HTML格式
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private RsiAnalysisService rsiAnalysisService;

    @Autowired
    private FundAnalysisService fundAnalysisService;

    @Autowired
    private FundPortfolioService fundPortfolioService;
    
    @Autowired
    private FundBlacklistService fundBlacklistService;

    @Autowired
    private MaStrategyService maStrategyService;

    @Autowired
    private MomentumStrategyService momentumStrategyService;

    /**
     * 发送每日数据报告邮件
     *
     * @return 发送成功返回true
     */
    public boolean sendDailyReport() {
        try {
            // 检查邮件是否启用
            if (!systemConfigService.isEmailEnabled()) {
                logger.info("邮件发送功能未启用，跳过发送");
                return false;
            }

            // 获取邮件配置
            String recipients = systemConfigService.getConfigValue("email_recipients");
            if (recipients == null || recipients.isEmpty()) {
                logger.warn("未配置邮件接收人，跳过发送");
                return false;
            }

            // 收集各模块数据
            String emailContent = buildEmailContent();

            // 发送邮件
            String subject = "基金分析系统每日报告 - " + dateFormat.format(new Date());
            sendHtmlEmail(recipients, subject, emailContent);

            logger.info("每日报告邮件发送成功");
            return true;

        } catch (Exception e) {
            logger.error("发送每日报告邮件失败", e);
            throw new BusinessException("发送每日报告邮件失败", e);
        }
    }

    /**
     * 构建邮件内容（HTML格式）
     *
     * @return HTML格式的邮件内容
     */
    private String buildEmailContent() {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 1200px; margin: 0 auto; padding: 20px; }");
        html.append("h1 { color: #1890ff; border-bottom: 3px solid #1890ff; padding-bottom: 10px; }");
        html.append("h2 { color: #52c41a; margin-top: 30px; border-left: 4px solid #52c41a; padding-left: 10px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th { background-color: #f0f0f0; padding: 12px; text-align: left; border: 1px solid #ddd; font-weight: bold; }");
        html.append("td { padding: 10px; border: 1px solid #ddd; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append(".positive { color: #cf1322; font-weight: bold; }");
        html.append(".negative { color: #389e0d; font-weight: bold; }");
        html.append(".signal { background-color: #fff7e6; border-left: 4px solid #faad14; padding: 10px; margin: 10px 0; }");
        html.append(".footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #999; font-size: 12px; text-align: center; }");
        html.append(".holding-row { background-color: #e6f7ff; border-left: 3px solid #1890ff !important; }");
        html.append(".holding-badge { display: inline-block; background-color: #1890ff; color: white; padding: 2px 8px; border-radius: 3px; font-size: 12px; margin-left: 8px; }");
        html.append(".market-section { margin-bottom: 15px; }");
        html.append(".section-header { background-color: #e6f7ff; font-weight: bold; color: #1890ff; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");

        // 标题
        html.append("<h1>📊 基金分析系统每日报告</h1>");
        html.append("<p style='color: #999;'>生成时间: ").append(dateFormat.format(new Date())).append("</p>");

        // 1. 市场概览（包含持有基金组合RSI）
        appendMarketOverview(html);

        // 2. MA策略信号（买入和卖出）
        appendMaStrategySignals(html);

        // 3. 动量策略分析
        appendMomentumStrategy(html);

        // 4. ETF RSI分析（买入信号）
        appendRsiBuySignals(html);

        // 5. 基金推荐（已排除黑名单）
        appendFundRecommendations(html);


        // 页脚
        html.append("<div class='footer'>");
        html.append("<p>本邮件由基金分析系统自动生成</p>");
        html.append("<p>如需取消订阅，请在系统配置中关闭邮件发送功能</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 添加市场概览部分
     */
    private void appendMarketOverview(StringBuilder html) {
        try {
            MarketOverviewDTO market = marketDataService.getMarketOverview();
            if (market == null) {
                return;
            }

            html.append("<h2>📈 市场概览</h2>");
            html.append("<table>");
            html.append("<tr><th style='width: 25%;'>指标</th><th style='width: 20%;'>当前值</th><th style='width: 55%;'>说明</th></tr>");

            // 市场RSI指标组
            html.append("<tr class='section-header'>");
            html.append("<td colspan='3'>🔍 市场RSI指标</td>");
            html.append("</tr>");
            
            // RSI14
            if (market.getRsi14() != null) {
                html.append("<tr>");
                html.append("<td>📊 14日RSI</td>");
                html.append("<td><strong>").append(market.getRsi14()).append("</strong></td>");
                html.append("<td>").append(getRsiDescription(market.getRsi14())).append("</td>");
                html.append("</tr>");
            }

            // RSI90
            if (market.getRsi90() != null) {
                html.append("<tr>");
                html.append("<td>📊 90日RSI</td>");
                html.append("<td><strong>").append(market.getRsi90()).append("</strong></td>");
                html.append("<td>").append(getRsiDescription(market.getRsi90())).append("</td>");
                html.append("</tr>");
            }

            // 估值指标组
            html.append("<tr class='section-header'>");
            html.append("<td colspan='3'>💰 估值指标</td>");
            html.append("</tr>");
            
            // 风险溢价
            if (market.getRiskPremium() != null) {
                html.append("<tr>");
                html.append("<td>💎 风险溢价</td>");
                html.append("<td><strong>").append(market.getRiskPremium()).append("</strong></td>");
                html.append("<td>").append(getRiskPremiumDescription(market.getRiskPremium())).append("</td>");
                html.append("</tr>");
            }

            // 5年均线偏离度
            if (market.getMa5yDeviation() != null) {
                html.append("<tr>");
                html.append("<td>📈 5年均线偏离度</td>");
                html.append("<td colspan='2'><strong>").append(market.getMa5yDeviation()).append("</strong></td>");
                html.append("</tr>");
            }

            // 投资建议组
            if (market.getBalanceSuggestion() != null) {
                html.append("<tr class='section-header'>");
                html.append("<td colspan='3'>💡 投资建议</td>");
                html.append("</tr>");
                html.append("<tr>");
                html.append("<td>⚖️ 股债平衡建议</td>");
                html.append("<td colspan='2'><strong>").append(market.getBalanceSuggestion()).append("</strong></td>");
                html.append("</tr>");
            }
            
            // 添加持有基金组合RSI数据
            try {
                Map<String, Object> portfolio = fundPortfolioService.getPortfolioRsiSummary();
                if (portfolio != null && !portfolio.isEmpty()) {
                    Object rsi14Obj = portfolio.get("rsi14");
                    Object rsi90Obj = portfolio.get("rsi90");
                    Object weeklyRsi14Obj = portfolio.get("weeklyRsi14");
                    
                    if (rsi14Obj != null || rsi90Obj != null || weeklyRsi14Obj != null) {
                        // 添加分隔行
                        html.append("<tr class='section-header'>");
                        html.append("<td colspan='3'>🎯 持有基金组合RSI</td>");
                        html.append("</tr>");
                        
                        if (rsi14Obj != null) {
                            BigDecimal rsi14 = (BigDecimal) rsi14Obj;
                            html.append("<tr>");
                            html.append("<td>📌 组合14日RSI</td>");
                            html.append("<td><strong>").append(formatDecimal(rsi14)).append("</strong></td>");
                            html.append("<td>").append(getRsiDescription(rsi14.toString())).append("</td>");
                            html.append("</tr>");
                        }

                        if (rsi90Obj != null) {
                            BigDecimal rsi90 = (BigDecimal) rsi90Obj;
                            html.append("<tr>");
                            html.append("<td>📌 组合90日RSI</td>");
                            html.append("<td><strong>").append(formatDecimal(rsi90)).append("</strong></td>");
                            html.append("<td>").append(getRsiDescription(rsi90.toString())).append("</td>");
                            html.append("</tr>");
                        }

                        if (weeklyRsi14Obj != null) {
                            BigDecimal weeklyRsi14 = (BigDecimal) weeklyRsi14Obj;
                            html.append("<tr>");
                            html.append("<td>📌 组合14周RSI</td>");
                            html.append("<td><strong>").append(formatDecimal(weeklyRsi14)).append("</strong></td>");
                            html.append("<td>").append(getRsiDescription(weeklyRsi14.toString())).append("</td>");
                            html.append("</tr>");
                        }
                    }
                }
            } catch (Exception e2) {
                logger.error("添加持有基金组合RSI数据失败", e2);
                throw new BusinessException("添加持有基金组合RSI数据失败", e2);
            }

            html.append("</table>");

        } catch (Exception e) {
            logger.error("构建市场概览内容失败", e);
            throw new BusinessException("构建市场概览内容失败", e);
        }
    }

    /**
     * 添加RSI买入信号部分
     */
    private void appendRsiBuySignals(StringBuilder html) {
        try {
            List<RsiDataDTO> signals = rsiAnalysisService.getEtfBuySignals();
            if (signals == null || signals.isEmpty()) {
                return;
            }

            html.append("<h2>🔔 ETF RSI 买入信号</h2>");
            html.append("<table>");
            html.append("<tr>");
            html.append("<th>ETF名称</th><th>当前RSI</th><th>区间</th><th>说明</th>");
            html.append("</tr>");

            for (RsiDataDTO signal : signals) {
                html.append("<tr>");
                // 合并代码和名称：名称(代码)
                html.append("<td>").append(signal.getName()).append("(").append(signal.getCode()).append(")</td>");
                html.append("<td>").append(formatDecimal(signal.getCurrentRsi())).append("</td>");
                html.append("<td>").append(signal.getInterval()).append("</td>");
                html.append("<td>").append(signal.getMessage()).append("</td>");
                html.append("</tr>");
            }

            html.append("</table>");

        } catch (Exception e) {
            logger.error("构建RSI买入信号内容失败", e);
            throw new BusinessException("构建RSI买入信号内容失败", e);
        }
    }

    /**
     * 添加MA策略信号部分（买入和卖出信号）
     */
    private void appendMaStrategySignals(StringBuilder html) {
        try {
            // 获取买入和卖出信号
            List<MaStrategyDTO> buySignals = maStrategyService.getMaBuySignals();
            List<MaStrategyDTO> sellSignals = maStrategyService.getMaSellSignals();
            
            // 如果没有任何信号，不显示该部分
            if ((buySignals == null || buySignals.isEmpty()) && 
                (sellSignals == null || sellSignals.isEmpty())) {
                return;
            }

            html.append("<h2>📈 MA策略信号 (双均线策略)</h2>");
            
            // 买入信号
            if (buySignals != null && !buySignals.isEmpty()) {
                html.append("<h3 style='color: #52c41a; margin-top: 20px;'>🟢 买入信号 (").append(buySignals.size()).append(")</h3>");
                html.append("<table>");
                html.append("<tr>");
                html.append("<th>ETF名称</th>");
                html.append("<th>当前价格</th>");
                html.append("<th>10日均线</th>");
                html.append("<th>30日均线</th>");
                html.append("<th>信号说明</th>");
                html.append("<th>数据时间</th>");
                html.append("</tr>");

                for (MaStrategyDTO signal : buySignals) {
                    html.append("<tr>");
                    html.append("<td>").append(signal.getEtfName()).append("<br/><span style='color: #999; font-size: 12px;'>(").append(signal.getEtfCode()).append(")</span></td>");
                    html.append("<td><strong style='color: #cf1322;'>").append(formatDecimal(signal.getCurrentDaily())).append("</strong></td>");
                    html.append("<td>").append(formatDecimal(signal.getMa10())).append("</td>");
                    html.append("<td>").append(formatDecimal(signal.getMa30())).append("</td>");
                    html.append("<td style='font-size: 13px;'>").append(signal.getSignalDescription()).append("</td>");
                    html.append("<td style='font-size: 12px;'>").append(signal.getDataTime()).append("</td>");
                    html.append("</tr>");
                }

                html.append("</table>");
            }
            
            // 卖出信号
            if (sellSignals != null && !sellSignals.isEmpty()) {
                html.append("<h3 style='color: #ff4d4f; margin-top: 20px;'>🔴 卖出信号 (").append(sellSignals.size()).append(")</h3>");
                html.append("<table>");
                html.append("<tr>");
                html.append("<th>ETF名称</th>");
                html.append("<th>当前价格</th>");
                html.append("<th>10日均线</th>");
                html.append("<th>30日均线</th>");
                html.append("<th>信号说明</th>");
                html.append("<th>数据时间</th>");
                html.append("</tr>");

                for (MaStrategyDTO signal : sellSignals) {
                    html.append("<tr>");
                    html.append("<td>").append(signal.getEtfName()).append("<br/><span style='color: #999; font-size: 12px;'>(").append(signal.getEtfCode()).append(")</span></td>");
                    html.append("<td><strong style='color: #389e0d;'>").append(formatDecimal(signal.getCurrentDaily())).append("</strong></td>");
                    html.append("<td>").append(formatDecimal(signal.getMa10())).append("</td>");
                    html.append("<td>").append(formatDecimal(signal.getMa30())).append("</td>");
                    html.append("<td style='font-size: 13px;'>").append(signal.getSignalDescription()).append("</td>");
                    html.append("<td style='font-size: 12px;'>").append(signal.getDataTime()).append("</td>");
                    html.append("</tr>");
                }

                html.append("</table>");
            }
            
            // 策略说明
            html.append("<div class='signal' style='margin-top: 15px;'>");
            html.append("<strong>📖 策略说明：</strong><br/>");
            html.append("买入信号：10日均线上穿30日均线(金叉)<br/>");
            html.append("卖出信号：10日均线下穿30日均线(死叉)<br/>");
            html.append("<span style='color: #999; font-size: 12px;'>注：本策略仅供参考，不构成投资建议</span>");
            html.append("</div>");

        } catch (Exception e) {
            logger.error("构建MA策略信号内容失败", e);
            throw new BusinessException("构建MA策略信号内容失败", e);
        }
    }


    /**
     * 添加基金推荐部分（已排除黑名单）
     */
    private void appendFundRecommendations(StringBuilder html) {
        try {
            List<FundInfo> funds = fundAnalysisService.getFundRecommendations();
            if (funds == null || funds.isEmpty()) {
                return;
            }
            
            // 过滤掉黑名单中的基金
            List<FundInfo> filteredFunds = new java.util.ArrayList<>();
            for (FundInfo fund : funds) {
                if (!fundBlacklistService.isBlacklisted(fund.getFundCode())) {
                    filteredFunds.add(fund);
                }
            }
            
            if (filteredFunds.isEmpty()) {
                return;
            }

            html.append("<h2>🎯 基金推荐</h2>");
            html.append("<table>");
            html.append("<tr>");
            html.append("<th>代码</th><th>名称</th><th>基金经理</th><th>任职年限</th>");
            html.append("<th>5年年化</th><th>今年以来</th><th>规模</th>");
            html.append("</tr>");

            for (FundInfo fund : filteredFunds) {
                // 检查基金是否已持有，如果已持有，添加特殊样式
                boolean isHolding = fund.getIsHolding() != null && fund.getIsHolding() == 1;
                if (isHolding) {
                    html.append("<tr class='holding-row'>");
                } else {
                    html.append("<tr>");
                }
                
                html.append("<td>").append(fund.getFundCode()).append("</td>");
                html.append("<td>");
                html.append(fund.getFundName());
                // 如果已持有，添加持有标识
                if (isHolding) {
                    html.append("<span class='holding-badge'>已持有</span>");
                }
                html.append("</td>");
                html.append("<td>").append(fund.getManagerName()).append("</td>");
                html.append("<td>").append(fund.getManagerYears()).append("</td>");
                html.append("<td>").append(formatPercent(fund.getFiveYearReturn())).append("</td>");
                html.append("<td>").append(fund.getYearToDateReturn()).append("</td>");
                html.append("<td>").append(fund.getScale()).append("</td>");
                html.append("</tr>");
            }

            html.append("</table>");

        } catch (Exception e) {
            logger.error("构建基金推荐内容失败", e);
            throw new BusinessException("构建基金推荐内容失败", e);
        }
    }

    /**
     * 添加动量策略部分
     */
    private void appendMomentumStrategy(StringBuilder html) {
        try {
            // 获取最近的交易记录（最近10条）
            List<MomentumTransactionDTO> allTransactions = momentumStrategyService.getAllTransactions();
            if (allTransactions == null || allTransactions.isEmpty()) {
                return;
            }
            
            // 只取最近4条
            List<MomentumTransactionDTO> recentTransactions = allTransactions.stream()
                    .limit(4)
                    .collect(Collectors.toList());
            
            // 获取收益曲线数据（用于显示当前持仓和累计收益）
            List<MomentumPerformanceDTO> performanceList = momentumStrategyService.calculatePerformance();
            MomentumPerformanceDTO latestPerformance = null;
            if (performanceList != null && !performanceList.isEmpty()) {
                latestPerformance = performanceList.get(performanceList.size() - 1);
            }
            
            html.append("<h2>🚀 21日动量策略</h2>");
            
            // 显示当前持仓和累计收益
            if (latestPerformance != null) {
                html.append("<div class='signal' style='margin-bottom: 20px;'>");
                
                if (latestPerformance.getHoldingEtfName() != null && latestPerformance.getHoldingQuantity() != null) {
                    html.append("当前持仓: <strong>").append(latestPerformance.getHoldingEtfName())
                         .append("(").append(latestPerformance.getHoldingEtfCode()).append(")</strong> ")
                         .append("<strong>").append(latestPerformance.getHoldingQuantity()).append("</strong> 股");
                    
                    if (latestPerformance.getCurrentPrice() != null) {
                        html.append(" @ ").append(formatDecimal(latestPerformance.getCurrentPrice())).append(" 元");
                    }
                    html.append("<br/>");
                } else {
                    html.append("当前持仓: <strong>空仓</strong><br/>");
                }
                
                if (latestPerformance.getDate() != null) {
                    html.append("数据日期: ").append(latestPerformance.getDate());
                }
                
                html.append("</div>");
            }
            
            // 显示最近的交易记录
            if (!recentTransactions.isEmpty()) {
                html.append("<h3 style='color: #1890ff; margin-top: 20px;'>📋 最近交易记录 (").append(recentTransactions.size()).append(")</h3>");
                html.append("<table>");
                html.append("<tr>");
                html.append("<th>交易日期</th>");
                html.append("<th>ETF名称</th>");
                html.append("<th>交易类型</th>");
                html.append("<th>数量</th>");
                html.append("<th>价格</th>");
                html.append("<th>21日动量</th>");
                html.append("</tr>");
                
                for (MomentumTransactionDTO transaction : recentTransactions) {
                    html.append("<tr>");
                    html.append("<td>").append(transaction.getDate() != null ? transaction.getDate() : "-").append("</td>");
                    html.append("<td>").append(transaction.getName() != null ? transaction.getName() : "-")
                         .append("<br/><span style='color: #999; font-size: 12px;'>(")
                         .append(transaction.getCode() != null ? transaction.getCode() : "-").append(")</span></td>");
                    
                    // 交易类型显示
                    String typeDisplay = "-";
                    String typeColor = "#333";
                    if ("buy".equals(transaction.getType())) {
                        typeDisplay = "🟢 买入";
                        typeColor = "#52c41a";
                    } else if ("sell".equals(transaction.getType())) {
                        typeDisplay = "🔴 卖出";
                        typeColor = "#ff4d4f";
                    }
                    html.append("<td><strong style='color: ").append(typeColor).append(";'>").append(typeDisplay).append("</strong></td>");
                    
                    // 数量显示（买入为正，卖出为负）
                    if (transaction.getQuantity() != null) {
                        long qty = transaction.getQuantity();
                        if (qty > 0) {
                            html.append("<td><strong style='color: #52c41a;'>+").append(qty).append("</strong></td>");
                        } else {
                            html.append("<td><strong style='color: #ff4d4f;'>").append(qty).append("</strong></td>");
                        }
                    } else {
                        html.append("<td>-</td>");
                    }
                    
                    html.append("<td>").append(transaction.getPrice() != null ? formatDecimal(transaction.getPrice()) : "-").append("</td>");
                    
                    // 21日动量显示（百分比）
                    if (transaction.getMomentum21() != null) {
                        BigDecimal momentumPercent = transaction.getMomentum21().multiply(BigDecimal.valueOf(100));
                        String momentumColor = "#333";
                        if (momentumPercent.compareTo(BigDecimal.ZERO) > 0) {
                            momentumColor = "#cf1322";
                        } else if (momentumPercent.compareTo(BigDecimal.ZERO) < 0) {
                            momentumColor = "#389e0d";
                        }
                        html.append("<td><strong style='color: ").append(momentumColor).append(";'>")
                             .append(formatDecimal(momentumPercent)).append("%</strong></td>");
                    } else {
                        html.append("<td>-</td>");
                    }
                    
                    html.append("</tr>");
                }
                
                html.append("</table>");
                
                // 策略说明
                html.append("<div class='signal' style='margin-top: 15px;'>");
                html.append("<strong>📖 策略说明：</strong><br/>");
                html.append("21日动量策略：每天选择21日动量最强的ETF持有，当动量最强的ETF发生变化时进行调仓<br/>");
                html.append("策略目标：捕捉市场趋势，通过动量轮动获取超额收益<br/>");
                html.append("<span style='color: #999; font-size: 12px;'>注：本策略仅供参考，不构成投资建议</span>");
                html.append("</div>");
            }
            
        } catch (Exception e) {
            logger.error("构建动量策略内容失败", e);
            throw new BusinessException("构建动量策略内容失败", e);
        }
    }

    /**
     * 发送HTML格式邮件
     *
     * @param recipients 收件人（多个用逗号分隔）
     * @param subject    邮件主题
     * @param content    HTML内容
     */
    private void sendHtmlEmail(String recipients, String subject, String content) throws Exception {
        // 创建 JavaMailSender
        JavaMailSender mailSender = createMailSender();
        
        // 创建邮件消息
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // 设置发件人
        String from = systemConfigService.getConfigValue("email_username");
        helper.setFrom(from);

        // 设置收件人
        String[] recipientArray = recipients.split(",");
        helper.setTo(recipientArray);

        // 设置主题
        helper.setSubject(subject);

        // 设置HTML内容
        helper.setText(content, true);

        // 发送邮件
        mailSender.send(message);
        
        logger.info("邮件发送成功，收件人: {}", recipients);
    }

    /**
     * 创建邮件发送器
     */
    private JavaMailSender createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // 设置SMTP服务器
        String host = systemConfigService.getConfigValue("email_host");
        mailSender.setHost(host);

        // 设置端口
        String portStr = systemConfigService.getConfigValue("email_port");
        int port = Integer.parseInt(portStr);
        mailSender.setPort(port);

        // 设置用户名和密码
        String username = systemConfigService.getConfigValue("email_username");
        String password = systemConfigService.getConfigValue("email_password");
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        // 设置属性
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.debug", "false");

        return mailSender;
    }

    // 辅助方法：格式化百分比
    private String formatPercent(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        String formatted = value.setScale(2, BigDecimal.ROUND_HALF_UP).toString() + "%";
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            return "<span class='positive'>+" + formatted + "</span>";
        } else if (value.compareTo(BigDecimal.ZERO) < 0) {
            return "<span class='negative'>" + formatted + "</span>";
        }
        return formatted;
    }

    // 辅助方法：格式化小数
    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return value.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    // 辅助方法：RSI描述
    private String getRsiDescription(String value) {
        if (value == null || value.isEmpty()) return "无数据";
        try {
            double rsi = Double.parseDouble(value);
            if (rsi >= 70) return "超买区域，注意风险";
            if (rsi >= 50) return "偏强区域";
            if (rsi >= 30) return "中性区域";
            return "超卖区域，可能是机会";
        } catch (Exception e) {
            return value;
        }
    }

    // 辅助方法：风险溢价描述
    private String getRiskPremiumDescription(String value) {
        if (value == null || value.isEmpty()) return "无数据";
        try {
            // 移除可能的百分号
            String numStr = value.replace("%", "").trim();
            double val = Double.parseDouble(numStr);
            if (val >= 3) return "高估区域，谨慎投资";
            if (val >= 1) return "合理区域";
            return "低估区域，可以考虑加仓";
        } catch (Exception e) {
            return value;
        }
    }

}
