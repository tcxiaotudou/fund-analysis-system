package com.fund.analysis.mapper;

import com.fund.analysis.entity.FundPortfolioRsiHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 基金组合RSI历史数据Mapper
 */
@Mapper
public interface FundPortfolioRsiHistoryMapper {
    
    /**
     * 插入基金组合RSI历史数据
     * @param history 基金组合RSI历史数据
     * @return 影响行数
     */
    int insert(FundPortfolioRsiHistory history);
    
    /**
     * 批量插入基金组合RSI历史数据
     * @param historyList 基金组合RSI历史数据列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<FundPortfolioRsiHistory> historyList);
    
    /**
     * 查询最近N天的RSI历史数据
     * @param days 天数
     * @return RSI历史数据列表
     */
    List<FundPortfolioRsiHistory> selectRecentDays(@Param("days") int days);
    
    /**
     * 查询指定日期的RSI历史数据
     * @param dataDate 数据日期
     * @return RSI历史数据
     */
    FundPortfolioRsiHistory selectByDate(@Param("dataDate") String dataDate);
    
    /**
     * 删除旧数据，只保留最新的N天
     * @param keepDays 保留天数
     * @return 影响行数
     */
    int deleteOldData(@Param("keepDays") int keepDays);
    
    /**
     * 删除所有历史数据
     * @return 影响行数
     */
    int deleteAll();
}

