package com.fund.analysis.controller;

import com.fund.analysis.dto.Result;
import com.fund.analysis.entity.FundInfo;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.service.FundPortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 基金组合控制器
 */
@RestController
@RequestMapping("/fund/portfolio")
public class FundPortfolioController {

    @Autowired
    private FundPortfolioService fundPortfolioService;

    @GetMapping("/holdings")
    public Result<List<FundInfo>> getHoldingFunds() {
        return Result.success("获取成功", fundPortfolioService.getHoldingFunds());
    }

    @GetMapping("/rsi")
    public Result<Map<String, Object>> getPortfolioRsi() {
        Map<String, Object> rsiData = fundPortfolioService.getPortfolioRsiSummary();
        if (rsiData.containsKey("error")) {
            throw new DataUnavailableException("计算组合 RSI 失败: " + rsiData.get("error"));
        }
        return Result.success("计算成功", rsiData);
    }

    @PostMapping("/weight")
    public Result<String> updateFundWeight(@RequestParam String fundCode, @RequestParam BigDecimal weight) {
        validateWeight(weight);
        boolean success = fundPortfolioService.updateFundWeight(fundCode, weight);
        if (!success) {
            throw new DataUnavailableException("权重更新失败: " + fundCode);
        }
        return Result.success("权重更新成功", null);
    }

    @PostMapping("/weights/batch")
    public Result<String> updateFundWeights(@RequestBody Map<String, BigDecimal> weights) {
        BigDecimal totalWeight = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
            throw new BadRequestException("权重总和必须等于 100%，当前为: " + totalWeight + "%");
        }
        for (BigDecimal weight : weights.values()) {
            validateWeight(weight);
        }

        boolean success = fundPortfolioService.updateFundWeights(weights);
        if (!success) {
            throw new DataUnavailableException("权重批量更新失败");
        }
        return Result.success("权重批量更新成功", null);
    }

    @GetMapping("/rsi/history")
    public Result<List<Map<String, Object>>> getPortfolioRsiHistory(@RequestParam(defaultValue = "60") int days) {
        if (days <= 0 || days > 200) {
            throw new BadRequestException("天数必须在1-200之间");
        }
        return Result.success("获取成功", fundPortfolioService.getPortfolioRsiHistory(days));
    }

    private void validateWeight(BigDecimal weight) {
        if (weight == null) {
            throw new BadRequestException("权重不能为空");
        }
        if (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("权重必须在 0-100 之间");
        }
        if (weight.scale() > 2) {
            throw new BadRequestException("权重最多保留2位小数");
        }
    }
}
