package com.health.system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.health.system.common.ApiResponse;
import com.health.system.dto.AlertRuleDTO;
import com.health.system.dto.HealthIndicatorTypeDTO;
import com.health.system.dto.SystemNoticeDTO;
import com.health.system.entity.AlertRule;
import com.health.system.entity.HealthIndicatorType;
import com.health.system.entity.SystemNotice;
import com.health.system.service.AlertRuleService;
import com.health.system.service.HealthIndicatorTypeService;
import com.health.system.service.SystemNoticeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    private final SystemNoticeService systemNoticeService;
    private final AlertRuleService alertRuleService;
    private final HealthIndicatorTypeService healthIndicatorTypeService;

    public AdminConfigController(SystemNoticeService systemNoticeService,
                                 AlertRuleService alertRuleService,
                                 HealthIndicatorTypeService healthIndicatorTypeService) {
        this.systemNoticeService = systemNoticeService;
        this.alertRuleService = alertRuleService;
        this.healthIndicatorTypeService = healthIndicatorTypeService;
    }

    @GetMapping("/notices")
    public ApiResponse<List<SystemNotice>> list(@RequestParam(defaultValue = "true") boolean includeOffline,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        return ApiResponse.success(systemNoticeService.listNotices(includeOffline, keyword, status));
    }

    @PostMapping("/notices")
    public ApiResponse<Void> create(@Valid @RequestBody SystemNoticeDTO dto) {
        systemNoticeService.createNotice(dto);
        return ApiResponse.success("创建成功", null);
    }

    @PutMapping("/notices")
    public ApiResponse<Void> update(@Valid @RequestBody SystemNoticeDTO dto) {
        systemNoticeService.updateNotice(dto);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/notices/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        systemNoticeService.deleteNotice(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/alert-rules")
    public ApiResponse<List<AlertRule>> listRules() {
        return ApiResponse.success(alertRuleService.listRules());
    }

    @PostMapping("/alert-rules")
    public ApiResponse<Void> createRule(@Valid @RequestBody AlertRuleDTO dto) {
        alertRuleService.createRule(dto);
        return ApiResponse.success("创建成功", null);
    }

    @PutMapping("/alert-rules")
    public ApiResponse<Void> updateRule(@Valid @RequestBody AlertRuleDTO dto) {
        alertRuleService.updateRule(dto);
        return ApiResponse.success("更新成功", null);
    }

    @GetMapping("/indicator-types")
    public ApiResponse<List<HealthIndicatorType>> listIndicatorTypes(
            @RequestParam(defaultValue = "true") boolean includeDisabled) {
        return ApiResponse.success(healthIndicatorTypeService.listTypes(includeDisabled));
    }

    @PostMapping("/indicator-types")
    public ApiResponse<Void> createIndicatorType(@Valid @RequestBody HealthIndicatorTypeDTO dto) {
        healthIndicatorTypeService.createType(dto);
        return ApiResponse.success("创建成功", null);
    }

    @PutMapping("/indicator-types")
    public ApiResponse<Void> updateIndicatorType(@Valid @RequestBody HealthIndicatorTypeDTO dto) {
        healthIndicatorTypeService.updateType(dto);
        return ApiResponse.success("更新成功", null);
    }
}
