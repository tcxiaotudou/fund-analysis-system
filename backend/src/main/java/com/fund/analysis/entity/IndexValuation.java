package com.fund.analysis.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 指数估值缓存实体
 */
@Data
public class IndexValuation implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 指数代码
     */
    private String indexCode;

    /**
     * 指数名称
     */
    private String name;

    /**
     * 历史低位占比文案
     */
    private String historyLowText;

    /**
     * 估值状态文案
     */
    private String valuationLabel;

    /**
     * 展示级别
     */
    private String level;

    /**
     * PE日期
     */
    private String peDate;

    /**
     * PE值
     */
    private String pe;

    /**
     * PE百分位
     */
    private String pePercentile;

    /**
     * 数据时间
     */
    private Date dataTime;

    /**
     * 创建时间
     */
    private Date createTime;
}
