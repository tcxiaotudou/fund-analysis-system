package com.fund.analysis.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 21日动量策略交易记录数据传输对象
 * 用于前后端之间传输交易记录数据
 */
@Data
public class MomentumTransactionDTO {
    
    /**
     * 交易日期（格式化字符串）
     */
    private String date;
    
    /**
     * ETF代码
     */
    private String code;
    
    /**
     * ETF名称
     */
    private String name;
    
    /**
     * 交易类型：buy-买入 sell-卖出
     */
    private String type;
    
    /**
     * 交易数量（买入为正，卖出为负）
     */
    private Long quantity;
    
    /**
     * 交易价格
     */
    private BigDecimal price;
    
    /**
     * 21日动量值
     */
    private BigDecimal momentum21;
}

