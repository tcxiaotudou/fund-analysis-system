package com.fund.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.analysis.entity.EtfInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ETF信息数据访问接口
 * 继承MyBatis Plus的BaseMapper，提供基础CRUD操作
 */
@Mapper
public interface EtfInfoMapper extends BaseMapper<EtfInfo> {
    
    /**
     * 查询所有启用的ETF
     * @return 启用的ETF列表
     */
    List<EtfInfo> selectEnabledEtfs();
    
    /**
     * 根据ETF代码查询ETF信息
     * @param code ETF代码
     * @return ETF信息
     */
    EtfInfo selectByCode(String code);
}

