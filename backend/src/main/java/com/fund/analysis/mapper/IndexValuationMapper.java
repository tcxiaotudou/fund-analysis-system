package com.fund.analysis.mapper;

import com.fund.analysis.entity.IndexValuation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 指数估值缓存数据访问接口
 */
@Mapper
public interface IndexValuationMapper {

    /**
     * 查询指定指数最新估值
     *
     * @param indexCode 指数代码
     * @return 最新估值
     */
    IndexValuation selectLatestByIndexCode(@Param("indexCode") String indexCode);

    /**
     * 新增指数估值缓存
     *
     * @param entity 指数估值
     * @return 新增数量
     */
    int insert(IndexValuation entity);

    /**
     * 删除旧数据
     *
     * @param indexCode 指数代码
     * @param keepCount 保留数量
     */
    void deleteOldData(@Param("indexCode") String indexCode, @Param("keepCount") Integer keepCount);
}
