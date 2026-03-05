package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.service.HealthAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitor")
public class AdminMonitorController {

    private final HealthAlertService healthAlertService;

    public AdminMonitorController(HealthAlertService healthAlertService) {
        this.healthAlertService = healthAlertService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(healthAlertService.monitorOverview());
    }
}
