package com.fund.analysis.service;

import com.fund.analysis.dto.DashboardDecisionDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DashboardDecisionServiceTest {

    /**
     * 验证决策驾驶舱DTO保留模块健康状态和操作入口
     */
    @Test
    void dashboardDecisionDtoKeepsVisibleModuleHealthAndOperations() {
        DashboardDecisionDTO dto = new DashboardDecisionDTO();
        dto.setDataStatus(new DashboardDecisionDTO.DataStatusDTO());
        dto.getDataStatus().setStatus("partial");
        dto.getDataStatus().setMessage("组合 RSI 加载失败");

        DashboardDecisionDTO.OperationDTO operation = new DashboardDecisionDTO.OperationDTO();
        operation.setKey("rsi-backtest");
        operation.setTitle("执行RSI回测");
        operation.setTargetPath("/rsi-backtest");
        operation.setDanger(false);
        dto.getOperations().add(operation);

        assertEquals("partial", dto.getDataStatus().getStatus());
        assertEquals("组合 RSI 加载失败", dto.getDataStatus().getMessage());
        assertEquals("执行RSI回测", dto.getOperations().get(0).getTitle());
        assertFalse(dto.getOperations().get(0).getDanger());
    }
}
