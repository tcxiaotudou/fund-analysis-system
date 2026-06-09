package com.fund.analysis.controller;

import com.fund.analysis.dto.MaStrategyBacktestDTO;
import com.fund.analysis.dto.MaStrategyDTO;
import com.fund.analysis.dto.Result;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.service.MaStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 移动平均线策略控制器
 */
@RestController
@RequestMapping("/ma-strategy")
public class MaStrategyController {

    @Autowired
    private MaStrategyService maStrategyService;

    @GetMapping("/calculate")
    public Result<MaStrategyDTO> getMaStrategy(@RequestParam String code) {
        MaStrategyDTO maData = maStrategyService.getLatestMaStrategy(code);
        if (maData == null) {
            throw new DataUnavailableException("未找到MA策略数据: " + code);
        }
        return Result.success(maData);
    }

    @GetMapping("/buy-signals")
    public Result<List<MaStrategyDTO>> getMaBuySignals() {
        return Result.success(maStrategyService.getMaBuySignals());
    }

    @GetMapping("/sell-signals")
    public Result<List<MaStrategyDTO>> getMaSellSignals() {
        return Result.success(maStrategyService.getMaSellSignals());
    }

    @GetMapping("/latest")
    public Result<List<MaStrategyDTO>> getLatestMaStrategies() {
        return Result.success(maStrategyService.getLatestAllMaStrategies());
    }

    @PostMapping("/backtest")
    public Result<MaStrategyBacktestDTO> runBacktest(
            @RequestParam String etfCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false, defaultValue = "100000") BigDecimal initialCapital) {
        if (startDate.after(endDate)) {
            throw new BadRequestException("开始日期不能晚于结束日期");
        }
        validateInitialCapital(initialCapital);
        MaStrategyBacktestDTO result = maStrategyService.runBacktest(etfCode, startDate, endDate, initialCapital);
        if (result == null) {
            throw new DataUnavailableException("回测失败：数据不足或ETF代码无效");
        }
        return Result.success(result);
    }

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
}
