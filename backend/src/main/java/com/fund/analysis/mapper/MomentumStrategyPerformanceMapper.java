package com.fund.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.analysis.entity.MomentumStrategyPerformance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 动量策略每日绩效数据访问接口
 */
@Mapper
public interface MomentumStrategyPerformanceMapper extends BaseMapper<MomentumStrategyPerformance> {

    /**
     * 查询所有每日绩效（按日期正序）
     * @return 每日绩效列表
     */
    List<MomentumStrategyPerformance> selectAllOrderByDateAsc();

    /**
     * 查询指定日期范围内的每日绩效
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日绩效列表
     */
    List<MomentumStrategyPerformance> selectByDateRange(@Param("startDate") Date startDate,
                                                        @Param("endDate") Date endDate);

    /**
     * 删除指定日期范围内的每日绩效
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 删除记录数
     */
    int deleteByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
