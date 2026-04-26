package com.fund.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.analysis.entity.MaStrategy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 移动平均线策略数据访问接口
 * 继承MyBatis Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface MaStrategyMapper extends BaseMapper<MaStrategy> {
    
    /**
     * 查询有买入信号的MA策略记录
     * @return 买入信号列表
     */
    List<MaStrategy> selectBuySignals();
    
    /**
     * 查询有卖出信号的MA策略记录
     * @return 卖出信号列表
     */
    List<MaStrategy> selectSellSignals();
    
    /**
     * 查询指定ETF代码的最新MA策略记录
     * @param etfCode ETF代码
     * @return MA策略记录
     */
    MaStrategy selectLatestByCode(@Param("etfCode") String etfCode);
    
    /**
     * 删除指定ETF代码的旧数据（保留最新的N条）
     * @param etfCode ETF代码
     * @param keepCount 保留数量
     */
    void deleteOldData(@Param("etfCode") String etfCode, @Param("keepCount") Integer keepCount);
    
    /**
     * 查询最新的所有MA策略记录
     * @return MA策略记录列表
     */
    List<MaStrategy> selectLatestAll();
}

