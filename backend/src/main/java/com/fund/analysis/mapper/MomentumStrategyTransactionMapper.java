package com.fund.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.analysis.entity.MomentumStrategyTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 21日动量策略交易记录数据访问接口
 * 继承MyBatis Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface MomentumStrategyTransactionMapper extends BaseMapper<MomentumStrategyTransaction> {
    
    /**
     * 查询指定日期范围内的交易记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 交易记录列表
     */
    List<MomentumStrategyTransaction> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * 查询所有交易记录（按日期倒序）
     * @return 交易记录列表
     */
    List<MomentumStrategyTransaction> selectAllOrderByDateDesc();

    /**
     * 查询指定日期之前的交易记录（按日期正序）
     * @param beforeDate 指定日期
     * @return 指定日期之前的交易记录列表
     */
    List<MomentumStrategyTransaction> selectBeforeDate(@Param("beforeDate") Date beforeDate);
    
    /**
     * 查询指定ETF代码的交易记录
     * @param etfCode ETF代码
     * @return 交易记录列表
     */
    List<MomentumStrategyTransaction> selectByEtfCode(@Param("etfCode") String etfCode);
    
    /**
     * 删除指定日期之前的数据
     * @param beforeDate 日期
     */
    int deleteBeforeDate(@Param("beforeDate") Date beforeDate);

    /**
     * 删除指定日期范围内的数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 删除记录数
     */
    int deleteByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * 查询最新的交易日期
     * @return 最新的交易日期，如果没有记录则返回null
     */
    Date selectLatestTransactionDate();
}
