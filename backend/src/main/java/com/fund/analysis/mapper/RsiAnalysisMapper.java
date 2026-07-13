package com.fund.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.analysis.entity.RsiAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * RSI分析数据访问接口
 * 继承MyBatis Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface RsiAnalysisMapper extends BaseMapper<RsiAnalysis> {
    
    /**
     * 查询最新的RSI分析记录
     * @param limit 查询数量
     * @return RSI分析记录列表
     */
    List<RsiAnalysis> selectLatestAnalysis(@Param("limit") Integer limit);
    
    /**
     * 查询所有启用标的的当前RSI分析记录
     * @return 当前RSI分析记录
     */
    List<RsiAnalysis> selectCurrentAnalysis();
    
    /**
     * 查询指定标的和周期的最新RSI分析记录
     * @param code 标的代码
     * @param period RSI周期
     * @return RSI分析记录
     */
    RsiAnalysis selectLatestByCodeAndPeriod(@Param("code") String code, @Param("period") Integer period);
    
    /**
     * 删除指定标的和周期的旧数据（保留最新的N条）
     * @param code 标的代码
     * @param period RSI周期
     * @param keepCount 保留数量
     */
    void deleteOldData(@Param("code") String code, @Param("period") Integer period, @Param("keepCount") Integer keepCount);
}
