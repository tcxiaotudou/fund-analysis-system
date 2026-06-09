package com.fund.analysis.controller;

import com.fund.analysis.dto.Result;
import com.fund.analysis.entity.EtfInfo;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.DataUnavailableException;
import com.fund.analysis.mapper.EtfInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * ETF 管理控制器
 */
@RestController
@RequestMapping("/etf")
public class EtfController {

    @Autowired
    private EtfInfoMapper etfInfoMapper;

    @GetMapping("/list")
    public Result<List<EtfInfo>> getEtfList() {
        List<EtfInfo> etfList = etfInfoMapper.selectList(null);
        etfList.sort((a, b) -> b.getId().compareTo(a.getId()));
        return Result.success(etfList);
    }

    @GetMapping("/enabled")
    public Result<List<EtfInfo>> getEnabledEtfs() {
        return Result.success(etfInfoMapper.selectEnabledEtfs());
    }

    /**
     * 新增 ETF 配置
     *
     * @param etfInfo ETF 信息
     * @return 新增结果
     */
    @PostMapping("/add")
    public Result<Void> addEtf(@RequestBody EtfInfo etfInfo) {
        validateEtfInfo(etfInfo, false);
        etfInfo.setCreateTime(new Date());
        etfInfo.setUpdateTime(new Date());
        etfInfoMapper.insert(etfInfo);
        return Result.success("添加成功", null);
    }

    /**
     * 更新 ETF 配置
     *
     * @param etfInfo ETF 信息
     * @return 更新结果
     */
    @PostMapping("/update")
    public Result<Void> updateEtf(@RequestBody EtfInfo etfInfo) {
        validateEtfInfo(etfInfo, true);
        etfInfo.setUpdateTime(new Date());
        int updated = etfInfoMapper.updateById(etfInfo);
        if (updated == 0) {
            throw new DataUnavailableException("ETF不存在: " + etfInfo.getId());
        }
        return Result.success("更新成功", null);
    }

    /**
     * 删除 ETF 配置
     *
     * @param id ETF ID
     * @return 删除结果
     */
    @PostMapping("/delete/{id}")
    public Result<Void> deleteEtf(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("ETF ID不能为空");
        }
        int deleted = etfInfoMapper.deleteById(id);
        if (deleted == 0) {
            throw new DataUnavailableException("ETF不存在: " + id);
        }
        return Result.success("删除成功", null);
    }

    /**
     * 校验 ETF 请求参数
     *
     * @param etfInfo ETF 信息
     * @param requireId 是否要求 ID
     */
    private void validateEtfInfo(EtfInfo etfInfo, boolean requireId) {
        if (etfInfo == null) {
            throw new BadRequestException("ETF信息不能为空");
        }
        if (requireId && (etfInfo.getId() == null || etfInfo.getId() <= 0)) {
            throw new BadRequestException("ETF ID不能为空");
        }
        if (etfInfo.getEtfName() == null || etfInfo.getEtfName().trim().isEmpty()) {
            throw new BadRequestException("ETF名称不能为空");
        }
        if (etfInfo.getEtfCode() == null || etfInfo.getEtfCode().trim().isEmpty()) {
            throw new BadRequestException("ETF代码不能为空");
        }
        if (etfInfo.getCategory() == null || etfInfo.getCategory() < 1 || etfInfo.getCategory() > 6) {
            throw new BadRequestException("ETF分类必须在1-6之间");
        }
        if (etfInfo.getEnabled() == null || (etfInfo.getEnabled() != 0 && etfInfo.getEnabled() != 1)) {
            throw new BadRequestException("启用状态必须为0或1");
        }
        validateThreshold(etfInfo.getRsiBuyThreshold(), "RSI买入阈值");
        validateThreshold(etfInfo.getRsiSellThreshold(), "RSI卖出阈值");
    }

    /**
     * 校验 RSI 阈值
     *
     * @param threshold 阈值
     * @param fieldName 字段名称
     */
    private void validateThreshold(Double threshold, String fieldName) {
        if (threshold != null && (threshold < 0 || threshold > 100)) {
            throw new BadRequestException(fieldName + "必须在0-100之间");
        }
    }
}
