package com.health.system.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.health.system.common.ApiResponse;
import com.health.system.common.BusinessException;
import com.health.system.service.AdminGroupGovernanceService;

@RestController
@RequestMapping("/api/admin/groups")
public class AdminGroupController {

    private final AdminGroupGovernanceService governanceService;

    public AdminGroupController(AdminGroupGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean operableOnly,
            @RequestParam(required = false, defaultValue = "1") int pageNo,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        return ApiResponse.success(governanceService.listGroups(keyword, status, operableOnly, pageNo, pageSize));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(governanceService.stats());
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id) {
        return ApiResponse.success(governanceService.approve(id));
    }

    @PatchMapping("/{id}/archive")
    public ApiResponse<Map<String, Object>> archive(@PathVariable Long id) {
        return ApiResponse.success(governanceService.archive(id));
    }

    @PatchMapping("/{id}/cross-dept")
    public ApiResponse<Map<String, Object>> crossDept(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String targetDept = Objects.toString(body.get("targetDept"), "");
        @SuppressWarnings("unchecked")
        List<Long> doctorIds = body.get("doctorIds") instanceof List<?> list
                ? list.stream().map(obj -> obj instanceof Number ? ((Number) obj).longValue() : Long.parseLong(String.valueOf(obj))).toList()
                : List.of();
        return ApiResponse.success(governanceService.crossDept(id, targetDept, doctorIds));
    }

    @PostMapping("/batch-approve")
    public ApiResponse<Map<String, Object>> batchApprove(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(governanceService.batchApprove(parseIds(body)));
    }

    @PostMapping("/batch-archive")
    public ApiResponse<Map<String, Object>> batchArchive(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(governanceService.batchArchive(parseIds(body)));
    }

    @PostMapping("/batch-cross-dept")
    public ApiResponse<Map<String, Object>> batchCrossDept(@RequestBody Map<String, Object> body) {
        String targetDept = Objects.toString(body.get("targetDept"), "");
        @SuppressWarnings("unchecked")
        List<Long> doctorIds = body.get("doctorIds") instanceof List<?> list
                ? list.stream().map(obj -> obj instanceof Number ? ((Number) obj).longValue() : Long.parseLong(String.valueOf(obj))).toList()
                : List.of();
        return ApiResponse.success(governanceService.batchCrossDept(parseIds(body), targetDept, doctorIds));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteGroup(@PathVariable Long id) {
        return ApiResponse.success(governanceService.deleteGroup(id));
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseIds(Map<String, Object> body) {
        Object idsObj = body.get("ids");
        if (idsObj instanceof List<?> list) {
            return list.stream()
                    .map(obj -> obj instanceof Number ? ((Number) obj).longValue() : Long.parseLong(String.valueOf(obj)))
                    .toList();
        }
        throw BusinessException.badRequest("ids 参数格式不正确");
    }
}
