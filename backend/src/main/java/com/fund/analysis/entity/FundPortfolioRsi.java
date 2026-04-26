package com.fund.analysis.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 基金组合RSI实体类
 * 对应数据库表 fund_portfolio_rsi
 */
public class FundPortfolioRsi {
    
    private Long id;
    private BigDecimal rsi14;
    private BigDecimal rsi90;
    private BigDecimal weeklyRsi14;
    private Integer fundCount;
    private String fundCodes;
    private Date dataTime;
    private Date createTime;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BigDecimal getRsi14() {
        return rsi14;
    }
    
    public void setRsi14(BigDecimal rsi14) {
        this.rsi14 = rsi14;
    }
    
    public BigDecimal getRsi90() {
        return rsi90;
    }
    
    public void setRsi90(BigDecimal rsi90) {
        this.rsi90 = rsi90;
    }
    
    public BigDecimal getWeeklyRsi14() {
        return weeklyRsi14;
    }
    
    public void setWeeklyRsi14(BigDecimal weeklyRsi14) {
        this.weeklyRsi14 = weeklyRsi14;
    }
    
    public Integer getFundCount() {
        return fundCount;
    }
    
    public void setFundCount(Integer fundCount) {
        this.fundCount = fundCount;
    }
    
    public String getFundCodes() {
        return fundCodes;
    }
    
    public void setFundCodes(String fundCodes) {
        this.fundCodes = fundCodes;
    }
    
    public Date getDataTime() {
        return dataTime;
    }
    
    public void setDataTime(Date dataTime) {
        this.dataTime = dataTime;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}

