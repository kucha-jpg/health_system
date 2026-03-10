package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.dto.SystemNoticeDTO;
import com.health.system.entity.SystemNotice;
import com.health.system.service.SystemNoticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    private final SystemNoticeService systemNoticeService;

    public AdminConfigController(SystemNoticeService systemNoticeService) {
        this.systemNoticeService = systemNoticeService;
    }

    @GetMapping("/notices")
    public ApiResponse<List<SystemNotice>> list(@RequestParam(defaultValue = "true") boolean includeOffline) {
        return ApiResponse.success(systemNoticeService.listNotices(includeOffline));
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
}
