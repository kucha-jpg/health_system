package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.entity.OperationLog;
import com.health.system.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final OperationLogService operationLogService;

    public AdminLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<List<OperationLog>> latest(@RequestParam(defaultValue = "100") int limit,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String roleType,
                                                  @RequestParam(required = false) Integer success) {
        return ApiResponse.success(operationLogService.latestLogs(limit, keyword, roleType, success));
    }
}
