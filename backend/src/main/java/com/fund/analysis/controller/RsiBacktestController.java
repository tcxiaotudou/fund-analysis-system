package com.fund.analysis.controller;

import com.fund.analysis.dto.Result;
import com.fund.analysis.dto.RsiBacktestDTO;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.service.RsiBacktestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;

@RestController
@RequestMapping("/rsi-backtest")
public class RsiBacktestController {

    @Autowired
    private RsiBacktestService rsiBacktestService;

    @PostMapping("/run")
    public Result<RsiBacktestDTO> runBacktest(
            @RequestParam String etfCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false, defaultValue = "100000") BigDecimal initialCapital,
            @RequestParam(required = false, defaultValue = "14") Integer rsiPeriod,
            @RequestParam(required = false, defaultValue = "30") BigDecimal rsiBuyThreshold,
            @RequestParam(required = false, defaultValue = "60") BigDecimal rsiSellThreshold,
            @RequestParam(required = false, defaultValue = "10000") BigDecimal fixedAmountPerTrade) {
        if (startDate.after(endDate)) {
            throw new BadRequestException("开始日期不能晚于结束日期");
        }
        validateBacktestParams(initialCapital, rsiPeriod, rsiBuyThreshold, rsiSellThreshold, fixedAmountPerTrade);

        RsiBacktestDTO result = rsiBacktestService.runBacktest(
                etfCode, startDate, endDate, initialCapital, rsiPeriod,
                rsiBuyThreshold, rsiSellThreshold, fixedAmountPerTrade);

        if (result == null) {
            throw new DataUnavailableException("回测失败：数据不足或ETF代码无效");
        }
        return Result.success(result);
    }

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
}
