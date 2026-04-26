package com.fund.analysis.controller;

import com.fund.analysis.dto.Result;
import com.fund.analysis.entity.EtfInfo;
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

    @PostMapping("/add")
    public Result<Void> addEtf(@RequestBody EtfInfo etfInfo) {
        etfInfo.setCreateTime(new Date());
        etfInfo.setUpdateTime(new Date());
        etfInfoMapper.insert(etfInfo);
        return Result.success("添加成功", null);
    }

    @PostMapping("/update")
    public Result<Void> updateEtf(@RequestBody EtfInfo etfInfo) {
        etfInfo.setUpdateTime(new Date());
        etfInfoMapper.updateById(etfInfo);
        return Result.success("更新成功", null);
    }

    @PostMapping("/delete/{id}")
    public Result<Void> deleteEtf(@PathVariable Long id) {
        etfInfoMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
