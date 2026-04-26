package com.fund.analysis.controller;

import com.fund.analysis.dto.Result;
import com.fund.analysis.exception.BadRequestException;
import com.fund.analysis.exception.BusinessException;
import com.fund.analysis.service.EmailService;
import com.fund.analysis.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/system-config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/email")
    public Result<Map<String, String>> getEmailConfig() {
        return Result.success(systemConfigService.getEmailConfigs());
    }

    @PostMapping("/email")
    public Result<Void> saveEmailConfig(@RequestBody Map<String, String> configMap) {
        systemConfigService.saveEmailConfigs(configMap);
        return Result.success("保存成功", null);
    }

    @PostMapping("/email/send-now")
    public Result<Void> sendEmailNow() {
        if (!systemConfigService.isEmailEnabled()) {
            throw new BadRequestException("邮件发送功能未启用，请先在配置中启用邮件功能");
        }

        String recipients = systemConfigService.getConfigValue("email_recipients");
        if (recipients == null || recipients.trim().isEmpty()) {
            throw new BadRequestException("未配置邮件接收人，请先在配置中设置收件人");
        }

        boolean success = emailService.sendDailyReport();
        if (!success) {
            throw new BusinessException("邮件发送失败，请检查邮件配置是否正确");
        }
        return Result.success("邮件发送成功", null);
    }
}
