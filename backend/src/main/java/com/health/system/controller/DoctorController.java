package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.dto.AlertHandleDTO;
import com.health.system.entity.HealthAlert;
import com.health.system.service.HealthAlertService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final HealthAlertService healthAlertService;

    public DoctorController(HealthAlertService healthAlertService) {
        this.healthAlertService = healthAlertService;
    }

    @GetMapping("/alerts")
    public ApiResponse<List<HealthAlert>> openAlerts() {
        return ApiResponse.success(healthAlertService.listOpenAlerts());
    }

    @PostMapping("/alerts/{id}/handle")
    public ApiResponse<Void> handleAlert(Authentication authentication, @PathVariable Long id, @Valid @RequestBody AlertHandleDTO dto) {
        healthAlertService.handleAlert(authentication.getName(), id, dto.getHandleRemark());
        return ApiResponse.success("处理成功", null);
    }
}
