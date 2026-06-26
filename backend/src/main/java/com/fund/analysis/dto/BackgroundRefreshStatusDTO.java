package com.fund.analysis.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台刷新状态数据传输对象
 */
@Data
public class BackgroundRefreshStatusDTO {

    /**
     * 刷新任务ID
     */
    private String jobId;

    /**
     * 刷新状态：idle、running、success、error
     */
    private String status;

    /**
     * 当前状态说明
     */
    private String message;

    /**
     * 开始时间
     */
    private String startedAt;

    /**
     * 结束时间
     */
    private String finishedAt;

    /**
     * 刷新结果摘要
     */
    private Map<String, Object> result = new LinkedHashMap<>();
}
