package com.fund.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.analysis.entity.StockBondBalance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 股债平衡策略数据访问接口
 * 继承MyBatis Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface StockBondBalanceMapper extends BaseMapper<StockBondBalance> {
    
    /**
     * 查询最新的股债平衡建议
     * @return 股债平衡建议
     */
    StockBondBalance selectLatest();
    
    /**
     * 删除旧数据（保留最新的N条）
     * @param keepCount 保留数量
     */
    void deleteOldData(@org.apache.ibatis.annotations.Param("keepCount") Integer keepCount);
}

