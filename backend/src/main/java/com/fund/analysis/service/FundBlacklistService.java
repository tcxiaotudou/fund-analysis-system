package com.fund.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fund.analysis.entity.FundBlacklist;
import com.fund.analysis.mapper.FundBlacklistMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 基金黑名单服务类
 * 提供基金排除和解除排除的功能
 */
@Service
public class FundBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(FundBlacklistService.class);
    
    @Autowired
    private FundBlacklistMapper fundBlacklistMapper;
    
    /**
     * 获取所有黑名单基金
     * @return 黑名单列表
     */
    public List<FundBlacklist> getAllBlacklist() {
        return fundBlacklistMapper.selectList(null);
    }
    
    /**
     * 检查基金是否在黑名单中
     * @param fundCode 基金代码
     * @return 如果在黑名单中返回true，否则返回false
     */
    public boolean isBlacklisted(String fundCode) {
        FundBlacklist blacklist = fundBlacklistMapper.selectByFundCode(fundCode);
        return blacklist != null;
    }
    
    /**
     * 获取黑名单记录
     * @param fundCode 基金代码
     * @return 黑名单记录，如果不存在则返回null
     */
    public FundBlacklist getBlacklist(String fundCode) {
        return fundBlacklistMapper.selectByFundCode(fundCode);
    }
    
    /**
     * 添加基金到黑名单
     * @param fundCode 基金代码
     * @param fundName 基金名称
     * @param excludeReason 排除原因
     * @param excludedBy 操作人
     * @return 添加成功返回true，否则返回false
     */
    @Transactional
    public boolean addToBlacklist(String fundCode, String fundName, String excludeReason, String excludedBy) {
        // 检查是否已经在黑名单中
        FundBlacklist existing = fundBlacklistMapper.selectByFundCode(fundCode);
        if (existing != null) {
            // 更新排除原因
            existing.setExcludeReason(excludeReason);
            existing.setExcludedBy(excludedBy);
            existing.setFundName(fundName);
            existing.setUpdateTime(new Date());
            fundBlacklistMapper.updateById(existing);
            logger.info("Updated blacklist for fund: {} - {}", fundCode, fundName);
            return true;
        }

        // 创建新的黑名单记录
        FundBlacklist blacklist = new FundBlacklist();
        blacklist.setFundCode(fundCode);
        blacklist.setFundName(fundName);
        blacklist.setExcludeReason(excludeReason);
        blacklist.setExcludedBy(excludedBy);
        blacklist.setCreateTime(new Date());
        blacklist.setUpdateTime(new Date());

        fundBlacklistMapper.insert(blacklist);
        logger.info("Added fund to blacklist: {} - {}", fundCode, fundName);
        return true;
    }
    
    /**
     * 从黑名单中移除基金
     * @param fundCode 基金代码
     * @return 移除成功返回true，否则返回false
     */
    @Transactional
    public boolean removeFromBlacklist(String fundCode) {
        QueryWrapper<FundBlacklist> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("fund_code", fundCode);
        int deleted = fundBlacklistMapper.delete(queryWrapper);
        if (deleted > 0) {
            logger.info("Removed fund from blacklist: {}", fundCode);
            return true;
        }
        return false;
    }
}
