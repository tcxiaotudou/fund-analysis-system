package com.fund.analysis.controller;

import com.fund.analysis.dto.MarketOverviewDTO;
import com.fund.analysis.dto.Result;
import com.fund.analysis.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 市场数据控制器
 */
@RestController
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private MarketDataService marketDataService;

    @GetMapping("/overview")
    public Result<MarketOverviewDTO> getMarketOverview() {
        return Result.success(marketDataService.getMarketOverview());
    }
}
