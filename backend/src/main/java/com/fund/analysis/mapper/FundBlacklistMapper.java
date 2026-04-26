package com.fund.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.analysis.entity.FundBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 基金黑名单数据访问接口
 * 继承MyBatis Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface FundBlacklistMapper extends BaseMapper<FundBlacklist> {
    
    /**
     * 根据基金代码查询是否在黑名单中
     * @param fundCode 基金代码
     * @return 黑名单记录，如果不存在则返回null
     */
    FundBlacklist selectByFundCode(@Param("fundCode") String fundCode);
}

