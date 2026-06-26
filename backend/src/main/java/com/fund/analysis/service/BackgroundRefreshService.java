package com.fund.analysis.service;

import com.fund.analysis.dto.BackgroundRefreshStatusDTO;
import com.fund.analysis.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 后台刷新任务服务
 */
@Service
public class BackgroundRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(BackgroundRefreshService.class);

    /**
     * 状态时间格式化器
     */
    private static final DateTimeFormatter STATUS_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * 管理端刷新服务
     */
    private final AdminRefreshService adminRefreshService;

    /**
     * 后台刷新线程池
     */
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "background-refresh-worker");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 刷新状态锁
     */
    private final Object statusLock = new Object();

    /**
     * 当前刷新状态
     */
    private BackgroundRefreshStatusDTO currentStatus;

    /**
     * 创建后台刷新任务服务
     *
     * @param adminRefreshService 管理端刷新服务
     */
    public BackgroundRefreshService(AdminRefreshService adminRefreshService) {
        this.adminRefreshService = adminRefreshService;
    }

    /**
     * 启动后台全量刷新
     *
     * @return 后台刷新状态
     */
    public BackgroundRefreshStatusDTO startFullRefresh() {
        synchronized (statusLock) {
            if (currentStatus != null && "running".equals(currentStatus.getStatus())) {
                BackgroundRefreshStatusDTO runningStatus = copyStatus(currentStatus);
                runningStatus.setMessage("后台刷新任务正在执行");
                return runningStatus;
            }

            String jobId = UUID.randomUUID().toString();
            BackgroundRefreshStatusDTO status = new BackgroundRefreshStatusDTO();
            status.setJobId(jobId);
            status.setStatus("running");
            status.setMessage("后台刷新任务已启动");
            status.setStartedAt(formatNow());
            currentStatus = status;

            try {
                refreshExecutor.submit(() -> runFullRefresh(jobId));
            } catch (RuntimeException e) {
                currentStatus = buildErrorStatus(jobId, "后台刷新任务提交失败: " + e.getMessage(), status.getStartedAt());
                throw new BusinessException("后台刷新任务提交失败", e);
            }

            return copyStatus(currentStatus);
        }
    }

    /**
     * 获取后台刷新状态
     *
     * @return 后台刷新状态
     */
    public BackgroundRefreshStatusDTO getStatus() {
        synchronized (statusLock) {
            if (currentStatus == null) {
                BackgroundRefreshStatusDTO idleStatus = new BackgroundRefreshStatusDTO();
                idleStatus.setStatus("idle");
                idleStatus.setMessage("暂无后台刷新任务");
                return idleStatus;
            }
            return copyStatus(currentStatus);
        }
    }

    /**
     * 关闭后台刷新线程池
     */
    @PreDestroy
    public void shutdown() {
        refreshExecutor.shutdownNow();
    }

    /**
     * 执行全量刷新任务
     *
     * @param jobId 刷新任务ID
     */
    private void runFullRefresh(String jobId) {
        String startedAt = null;
        synchronized (statusLock) {
            if (currentStatus != null && jobId.equals(currentStatus.getJobId())) {
                startedAt = currentStatus.getStartedAt();
                currentStatus.setMessage("后台刷新任务执行中");
            }
        }

        try {
            Map<String, Object> result = adminRefreshService.refreshAll();
            synchronized (statusLock) {
                if (currentStatus != null && jobId.equals(currentStatus.getJobId())) {
                    currentStatus.setStatus("success");
                    currentStatus.setMessage("后台刷新任务完成");
                    currentStatus.setFinishedAt(formatNow());
                    currentStatus.setResult(new LinkedHashMap<>(result));
                }
            }
        } catch (RuntimeException e) {
            logger.error("后台刷新任务失败，jobId={}", jobId, e);
            synchronized (statusLock) {
                currentStatus = buildErrorStatus(jobId, "后台刷新任务失败: " + e.getMessage(), startedAt);
            }
        }
    }

    /**
     * 构建失败状态
     *
     * @param jobId 刷新任务ID
     * @param message 失败消息
     * @param startedAt 开始时间
     * @return 失败状态
     */
    private BackgroundRefreshStatusDTO buildErrorStatus(String jobId, String message, String startedAt) {
        BackgroundRefreshStatusDTO status = new BackgroundRefreshStatusDTO();
        status.setJobId(jobId);
        status.setStatus("error");
        status.setMessage(message);
        status.setStartedAt(startedAt);
        status.setFinishedAt(formatNow());
        return status;
    }

    /**
     * 复制刷新状态，避免外部修改内部状态
     *
     * @param source 原始状态
     * @return 状态副本
     */
    private BackgroundRefreshStatusDTO copyStatus(BackgroundRefreshStatusDTO source) {
        BackgroundRefreshStatusDTO copy = new BackgroundRefreshStatusDTO();
        copy.setJobId(source.getJobId());
        copy.setStatus(source.getStatus());
        copy.setMessage(source.getMessage());
        copy.setStartedAt(source.getStartedAt());
        copy.setFinishedAt(source.getFinishedAt());
        copy.setResult(new LinkedHashMap<>(source.getResult()));
        return copy;
    }

    /**
     * 格式化当前时间
     *
     * @return 当前时间文本
     */
    private String formatNow() {
        return STATUS_TIME_FORMATTER.format(Instant.now());
    }
}
