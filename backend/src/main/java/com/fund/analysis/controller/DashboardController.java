package com.fund.analysis.controller;

import com.fund.analysis.dto.DashboardDecisionDTO;
import com.fund.analysis.dto.Result;
import com.fund.analysis.service.DashboardDecisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 决策驾驶舱控制器
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    /**
     * 决策驾驶舱服务
     */
    @Autowired
    private DashboardDecisionService dashboardDecisionService;

    /**
     * 获取决策驾驶舱数据
     *
     * @return 决策驾驶舱数据
     */
    @GetMapping("/decision")
    public Result<DashboardDecisionDTO> getDecisionDashboard() {
        return Result.success(dashboardDecisionService.getDecisionDashboard());
    }
}
