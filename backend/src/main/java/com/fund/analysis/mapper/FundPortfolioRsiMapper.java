package com.fund.analysis.mapper;

import com.fund.analysis.entity.FundPortfolioRsi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 基金组合RSI数据Mapper
 */
@Mapper
public interface FundPortfolioRsiMapper {
    
    /**
     * 插入基金组合RSI数据
     * @param rsi 基金组合RSI数据
     * @return 影响行数
     */
    int insert(FundPortfolioRsi rsi);
    
    /**
     * 查询最新的基金组合RSI数据
     * @return 最新的基金组合RSI数据
     */
    FundPortfolioRsi selectLatest();
    
    /**
     * 删除旧数据，只保留最新的N条
     * @param keepCount 保留数量
     * @return 影响行数
     */
    int deleteOldData(@Param("keepCount") int keepCount);
}

