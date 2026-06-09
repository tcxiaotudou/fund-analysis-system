package com.fund.analysis.config;

import com.fund.analysis.service.EmailService;
import com.fund.analysis.service.SystemConfigService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时任务配置类
 * 根据系统配置动态调度邮件发送任务
 */
@Configuration
public class DynamicScheduleConfig implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(DynamicScheduleConfig.class);

    @Lazy
    @Autowired
    private SystemConfigService systemConfigService;

    @Lazy
    @Autowired
    private EmailService emailService;

    private ScheduledTaskRegistrar taskRegistrar;
    private List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();
    private ThreadPoolTaskScheduler scheduler;

    /**
     * 配置任务调度器线程池
     * 使用多线程池避免任务相互阻塞
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 设置线程池大小，允许多个定时任务并发执行
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        logger.info("任务调度器线程池已配置，线程池大小: 10");
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.taskRegistrar = taskRegistrar;
        // 不在这里初始化任务，避免循环依赖
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        logger.info("动态定时任务配置初始化");
        registerEmailTasks();
    }

    /**
     * 注册邮件发送定时任务
     * 根据配置的时间点创建多个定时任务
     */
    public void registerEmailTasks() {
        // 取消所有现有的定时任务
        cancelAllTasks();

        // 检查邮件功能是否启用
        if (!systemConfigService.isEmailEnabled()) {
            logger.info("邮件发送功能未启用，跳过注册定时任务");
            return;
        }

        // 获取配置的发送时间
        String scheduleConfig = systemConfigService.getEmailSchedule();
        if (scheduleConfig == null || scheduleConfig.isEmpty()) {
            logger.warn("未配置邮件发送时间，使用默认时间 12:00,14:50");
            scheduleConfig = "12:00,14:50";
        }

        // 解析时间配置
        String[] times = scheduleConfig.split(",");
        for (String timeRaw : times) {
            final String time = timeRaw.trim();
            if (time.isEmpty()) {
                continue;
            }

            try {
                // 解析时间 HH:MM
                String[] parts = time.split(":");
                if (parts.length != 2) {
                    logger.error("时间格式错误: {}", time);
                    continue;
                }

                String hour = parts[0].trim();
                String minute = parts[1].trim();

                // 构建 cron 表达式：秒 分 时 日 月 周
                // 周一到周五发送
                final String cronExpression = String.format("0 %s %s ? * MON-FRI", minute, hour);

                // 注册定时任务
                if (taskRegistrar != null) {
                    ScheduledFuture<?> future = taskRegistrar.getScheduler().schedule(
                            () -> {
                                logger.info("========== 开始发送每日报告邮件 ({}) ==========", time);
                                try {
                                    boolean success = emailService.sendDailyReport();
                                    if (success) {
                                        logger.info("每日报告邮件发送成功 ({})", time);
                                    } else {
                                        logger.info("每日报告邮件发送跳过（邮件功能未启用或未配置）({})", time);
                                    }
                                } catch (RuntimeException e) {
                                    logger.error("发送每日报告邮件异常 ({})", time, e);
                                    throw e;
                                } catch (Exception e) {
                                    logger.error("发送每日报告邮件异常 ({})", time, e);
                                    throw new IllegalStateException("发送每日报告邮件异常: " + time, e);
                                }
                                logger.info("========== 每日报告邮件发送完成 ({}) ==========", time);
                            },
                            triggerContext -> {
                                CronTrigger trigger = new CronTrigger(cronExpression);
                                return trigger.nextExecutionTime(triggerContext);
                            }
                    );

                    scheduledFutures.add(future);
                    logger.info("成功注册邮件发送定时任务: {} (cron: {})", time, cronExpression);
                }
            } catch (Exception e) {
                logger.error("注册定时任务失败: {}", time, e);
            }
        }

        logger.info("邮件发送定时任务注册完成，共注册 {} 个任务", scheduledFutures.size());
    }

    /**
     * 取消所有定时任务
     */
    private void cancelAllTasks() {
        for (ScheduledFuture<?> future : scheduledFutures) {
            if (future != null && !future.isCancelled()) {
                future.cancel(false);
            }
        }
        scheduledFutures.clear();
        logger.info("已取消所有邮件发送定时任务");
    }

    /**
     * 重新加载定时任务
     * 在配置更新时调用
     */
    public void reloadTasks() {
        logger.info("重新加载邮件发送定时任务");
        registerEmailTasks();
    }
}
