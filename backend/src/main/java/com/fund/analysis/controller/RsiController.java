package com.fund.analysis.controller;

import com.fund.analysis.dto.Result;
import com.fund.analysis.dto.RsiDataDTO;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.service.RsiAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RSI 分析控制器
 */
@RestController
@RequestMapping("/rsi")
public class RsiController {

    @Autowired
    private RsiAnalysisService rsiAnalysisService;

    @GetMapping("/calculate")
    public Result<RsiDataDTO> getRsi(
            @RequestParam String code,
            @RequestParam(defaultValue = "14") Integer period) {
        RsiDataDTO rsiData = rsiAnalysisService.getLatestRsi(code, period);
        if (rsiData == null) {
            throw new DataUnavailableException("未找到RSI数据: " + code + ", period=" + period);
        }
        return Result.success(rsiData);
    }

    @GetMapping("/etf-signals")
    public Result<List<RsiDataDTO>> getEtfAnalysis() {
        return Result.success(rsiAnalysisService.getEtfAnalysis());
    }
}
