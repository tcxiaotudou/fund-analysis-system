package com.fund.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.analysis.entity.FundInfo;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 基金信息数据访问接口
 * 继承MyBatis Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface FundInfoMapper extends BaseMapper<FundInfo> {
    
    /**
     * 查询最新的基金推荐列表
     * @param limit 查询数量
     * @return 基金信息列表
     */
    List<FundInfo> selectLatestRecommendations(@Param("limit") Integer limit);
    
    /**
     * 删除旧的基金数据（保留最新的N条）
     * @param keepCount 保留数量
     */
    void deleteOldData(@Param("keepCount") Integer keepCount);
}

