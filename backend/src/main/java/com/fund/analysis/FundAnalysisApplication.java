package com.fund.analysis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基金和ETF投资策略分析系统 - 主启动类
 * @author Fund Analysis Team
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.fund.analysis.mapper")
public class FundAnalysisApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(FundAnalysisApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("基金和ETF投资策略分析系统启动成功！");
        System.out.println("访问地址: http://localhost:8080/api");
        System.out.println("========================================\n");
    }
}

