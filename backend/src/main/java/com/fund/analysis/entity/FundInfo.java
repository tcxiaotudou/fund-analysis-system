package com.fund.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 基金信息实体类
 * 用于存储基金的详细信息
 */
@Data
@TableName("fund_info")
public class FundInfo implements Serializable {
    
    /**
     * 主键ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 基金代码
     */
    private String fundCode;
    
    /**
     * 基金名称
     */
    private String fundName;
    
    /**
     * 基金经理姓名
     */
    private String managerName;
    
    /**
     * 基金经理管理年限
     */
    private String managerYears;
    
    /**
     * 基金规模（亿元）
     */
    private String scale;
    
    /**
     * 今年以来收益率（%）
     */
    private String yearToDateReturn;
    
    /**
     * 近5年年化收益率
     */
    private BigDecimal fiveYearReturn;
    
    /**
     * 近5年夏普比率排名
     */
    private Integer sharpeRank;
    
    /**
     * 近5年卡玛比率排名
     */
    private Integer calmarRank;
    
    /**
     * 近5年最大回撤（%）
     */
    private String maxDrawdown;
    
    /**
     * 赎回费率信息
     */
    private String redemptionFee;
    
    /**
     * 是否持有：0-否 1-是
     */
    private Integer isHolding;
    
    /**
     * 是否为手动添加：0-来自推荐API 1-手动添加
     */
    private Integer isCustom;
    
    /**
     * 组合中的权重(%)，范围0-100，小数点后2位
     */
    private BigDecimal portfolioWeight;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 数据来源时间
     */
    private Date dataTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer deleted;
}

