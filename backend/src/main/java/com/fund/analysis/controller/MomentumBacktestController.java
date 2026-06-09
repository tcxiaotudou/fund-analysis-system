package com.fund.analysis.controller;

import com.fund.analysis.dto.Result;
import com.fund.analysis.entity.MomentumStrategyPerformance;
import com.fund.analysis.entity.MomentumStrategyTransaction;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.service.MomentumStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 21日动量策略回测控制器
 */
@RestController
@RequestMapping("/momentum-strategy/backtest")
public class MomentumBacktestController {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private MomentumStrategyService momentumStrategyService;

    @PostMapping("/run")
    public Result<String> runBacktest(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "100000") BigDecimal initialCapital) {
        if (startDate.after(endDate)) {
            throw new BadRequestException("开始日期不能晚于结束日期");
        }
        if (initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("初始资金必须大于0");
        }

        MomentumStrategyService.BacktestResult result = momentumStrategyService.runBacktestWithPerformance(
                startDate, endDate, initialCapital);
        List<MomentumStrategyTransaction> transactions = result.getTransactions();
        List<MomentumStrategyPerformance> performances = result.getPerformances();

        if (performances.isEmpty()) {
            throw new DataUnavailableException("回测未生成每日绩效，请检查日期范围和ETF数据");
        }

        momentumStrategyService.deleteByDateRange(startDate, endDate);
        momentumStrategyService.deletePerformanceByDateRange(startDate, endDate);
        momentumStrategyService.saveTransactions(transactions);
        momentumStrategyService.savePerformanceRecords(performances);

        long buyCount = transactions.stream().filter(t -> "buy".equals(t.getTransactionType())).count();
        long sellCount = transactions.stream().filter(t -> "sell".equals(t.getTransactionType())).count();
        String message = String.format(
                "回测完成！\n回测期间: %s 至 %s\n初始资金: %.2f\n生成交易记录: %d 条\n生成每日绩效: %d 条\n买入次数: %d\n卖出次数: %d",
                dateFormat.format(startDate),
                dateFormat.format(endDate),
                initialCapital,
                transactions.size(),
                performances.size(),
                buyCount,
                sellCount
        );

        return Result.success(message);
    }
}
