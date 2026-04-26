package com.fund.analysis.controller;

import com.fund.analysis.dto.MomentumPerformanceDTO;
import com.fund.analysis.dto.MomentumTransactionDTO;
import com.fund.analysis.dto.Result;
import com.fund.analysis.service.MomentumStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 21日动量策略控制器
 */
@RestController
@RequestMapping("/momentum-strategy")
public class MomentumStrategyController {

    @Autowired
    private MomentumStrategyService momentumStrategyService;

    @GetMapping("/transactions")
    public Result<List<MomentumTransactionDTO>> getAllTransactions() {
        return Result.success(momentumStrategyService.getAllTransactions());
    }

    @GetMapping("/transactions/range")
    public Result<List<MomentumTransactionDTO>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return Result.success(momentumStrategyService.getTransactionsByDateRange(startDate, endDate));
    }

    @GetMapping("/transactions/etf")
    public Result<List<MomentumTransactionDTO>> getTransactionsByEtfCode(@RequestParam String etfCode) {
        return Result.success(momentumStrategyService.getTransactionsByEtfCode(etfCode));
    }

    @GetMapping("/performance")
    public Result<List<MomentumPerformanceDTO>> getPerformance() {
        return Result.success(momentumStrategyService.calculatePerformance());
    }
}
