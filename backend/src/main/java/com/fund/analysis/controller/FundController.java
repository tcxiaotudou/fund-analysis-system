package com.fund.analysis.controller;

import com.fund.analysis.dto.Result;
import com.fund.analysis.entity.FundBlacklist;
import com.fund.analysis.entity.FundInfo;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.service.FundAnalysisService;
import com.fund.analysis.service.FundBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基金控制器
 */
@RestController
@RequestMapping("/fund")
public class FundController {

    /**
     * 基金分析服务
     */
    @Autowired
    private FundAnalysisService fundAnalysisService;

    /**
     * 基金黑名单服务
     */
    @Autowired
    private FundBlacklistService fundBlacklistService;

    /**
     * 获取基金推荐列表
     *
     * @return 基金推荐列表
     */
    @GetMapping("/recommendations")
    public Result<List<FundInfo>> getFundRecommendations() {
        return Result.success(fundAnalysisService.getFundRecommendations());
    }

    /**
     * 重新获取基金推荐列表
     *
     * @return 最新基金推荐列表
     */
    @PostMapping("/recommendations/refresh")
    public Result<List<FundInfo>> refreshFundRecommendations() {
        return Result.success("基金推荐数据刷新成功", fundAnalysisService.refreshFundRecommendations());
    }

    /**
     * 获取基金黑名单
     *
     * @return 基金黑名单
     */
    @GetMapping("/blacklist")
    public Result<List<FundBlacklist>> getBlacklist() {
        return Result.success(fundBlacklistService.getAllBlacklist());
    }

    /**
     * 添加基金到黑名单
     *
     * @param request 黑名单请求参数
     * @return 添加结果
     */
    @PostMapping("/blacklist")
    public Result<Void> addToBlacklist(@RequestBody Map<String, String> request) {
        String fundCode = requireText(request.get("fundCode"), "基金代码不能为空");
        String excludeReason = requireText(request.get("excludeReason"), "排除原因不能为空");
        String fundName = request.get("fundName");
        String excludedBy = request.getOrDefault("excludedBy", "系统用户");

        fundBlacklistService.addToBlacklist(fundCode, fundName, excludeReason, excludedBy);
        return Result.success("基金已添加到黑名单", null);
    }

    /**
     * 从黑名单移除基金
     *
     * @param fundCode 基金代码
     * @return 移除结果
     */
    @DeleteMapping("/blacklist/{fundCode}")
    public Result<Void> removeFromBlacklist(@PathVariable String fundCode) {
        boolean removed = fundBlacklistService.removeFromBlacklist(fundCode);
        if (!removed) {
            throw new DataUnavailableException("基金不在黑名单中: " + fundCode);
        }
        return Result.success("基金已从黑名单移除", null);
    }

    /**
     * 检查基金是否在黑名单中
     *
     * @param fundCode 基金代码
     * @return 黑名单检查结果
     */
    @GetMapping("/blacklist/{fundCode}")
    public Result<Map<String, Object>> checkBlacklist(@PathVariable String fundCode) {
        FundBlacklist blacklist = fundBlacklistService.getBlacklist(fundCode);
        Map<String, Object> result = new HashMap<>();
        result.put("isBlacklisted", blacklist != null);
        result.put("blacklist", blacklist);
        return Result.success(result);
    }

    /**
     * 更新基金持有状态
     *
     * @param request 持有状态请求参数
     * @return 更新结果
     */
    @PostMapping("/holding")
    public Result<Void> updateHoldingStatus(@RequestBody Map<String, Object> request) {
        String fundCode = requireText((String) request.get("fundCode"), "基金代码不能为空");
        Object rawIsHolding = request.get("isHolding");
        if (!(rawIsHolding instanceof Number)) {
            throw new BadRequestException("持有状态不能为空");
        }
        Integer isHolding = ((Number) rawIsHolding).intValue();

        boolean updated = fundAnalysisService.updateHoldingStatus(fundCode, isHolding);
        if (!updated) {
            throw new DataUnavailableException("基金不存在: " + fundCode);
        }
        return Result.success(isHolding == 1 ? "已标记为持有" : "已取消持有", null);
    }

    /**
     * 手动添加持有基金
     *
     * @param request 持有基金请求参数
     * @return 添加结果
     */
    @PostMapping("/add-holding")
    public Result<Void> addHoldingFund(@RequestBody Map<String, String> request) {
        String fundCode = requireText(request.get("fundCode"), "基金代码不能为空");
        String fundName = requireText(request.get("fundName"), "基金名称不能为空");
        fundAnalysisService.addCustomHoldingFund(fundCode, fundName);
        return Result.success("基金已添加到持有列表", null);
    }

    /**
     * 校验必填文本
     *
     * @param value 文本值
     * @param message 错误消息
     * @return 去除空白后的文本
     */
    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }
}
