package com.fund.analysis.service;

import com.fund.analysis.config.DynamicScheduleConfig;
import com.fund.analysis.entity.SystemConfig;
import com.fund.analysis.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @Mock
    private DynamicScheduleConfig dynamicScheduleConfig;

    @InjectMocks
    private SystemConfigService systemConfigService;

    @Test
    void blankEmailPasswordDoesNotOverwriteExistingValue() {
        Map<String, String> config = Collections.singletonMap("emailPassword", "");

        systemConfigService.saveEmailConfigs(config);

        verify(systemConfigMapper, never()).insert(any(SystemConfig.class));
        verify(systemConfigMapper, never()).updateById(any(SystemConfig.class));
        verify(dynamicScheduleConfig).reloadTasks();
    }

    @Test
    void savingEmailConfigReloadsSchedule() {
        SystemConfig existing = new SystemConfig();
        existing.setConfigKey("email_enabled");
        existing.setConfigValue("false");
        when(systemConfigMapper.selectOne(any())).thenReturn(existing);

        Map<String, String> config = new HashMap<>();
        config.put("emailEnabled", "true");

        systemConfigService.saveEmailConfigs(config);

        verify(systemConfigMapper).updateById(existing);
        verify(dynamicScheduleConfig).reloadTasks();
    }
}
