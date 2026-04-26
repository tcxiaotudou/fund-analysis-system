package com.fund.analysis.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 基金组合RSI历史数据实体类
 * 对应数据库表 fund_portfolio_rsi_history
 */
public class FundPortfolioRsiHistory {
    
    private Long id;
    private BigDecimal rsi14;
    private Integer fundCount;
    private String fundCodes;
    private String dataDate;
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
    
    public String getDataDate() {
        return dataDate;
    }
    
    public void setDataDate(String dataDate) {
        this.dataDate = dataDate;
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

