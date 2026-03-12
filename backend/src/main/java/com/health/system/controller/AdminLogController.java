package com.health.system.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.health.system.common.ApiResponse;
import com.health.system.common.CsvUtils;
import com.health.system.common.OperationLogMessageBuilder;
import com.health.system.common.QueryDtoBuilders;
import com.health.system.common.RequestActor;
import com.health.system.common.SecurityActorUtils;
import com.health.system.dto.OperationLogQueryDTO;
import com.health.system.entity.OperationLog;
import com.health.system.service.OperationLogService;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private static final int EXPORT_LIMIT_MIN = 1;
    private static final int EXPORT_LIMIT_MAX = 5000;
    private static final String EXPORT_URI = "/api/admin/logs/export";
    private static final String EXPORT_FILENAME = "operation_logs_export.csv";

    private final OperationLogService operationLogService;

    public AdminLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<List<OperationLog>> latest(@RequestParam(defaultValue = "100") int limit,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String roleType,
                                                  @RequestParam(required = false) Integer success,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return ApiResponse.success(operationLogService.latestLogs(limit,
                QueryDtoBuilders.operationLogQuery(keyword, roleType, success, startTime, endTime, null, null)));
    }

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String roleType,
                                                 @RequestParam(required = false) Integer success,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(operationLogService.latestLogsPaged(
                QueryDtoBuilders.operationLogQuery(keyword, roleType, success, startTime, endTime, pageNo, pageSize)
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "1000") int limit,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String roleType,
                                         @RequestParam(required = false) Integer success,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        long startedAt = System.currentTimeMillis();
        int safeLimit = Math.min(Math.max(limit, EXPORT_LIMIT_MIN), EXPORT_LIMIT_MAX);
        OperationLogQueryDTO query = QueryDtoBuilders.operationLogQuery(keyword, roleType, success, startTime, endTime, null, null);
        List<OperationLog> logs = operationLogService.latestLogs(safeLimit, query);

        StringBuilder csv = new StringBuilder();
        csv.append("时间,用户,角色,方法,路径,结果,信息\n");
        for (OperationLog item : logs) {
            csv.append(CsvUtils.escape(item.getCreateTime() == null ? "" : item.getCreateTime().toString())).append(',')
                    .append(CsvUtils.escape(item.getUsername())).append(',')
                    .append(CsvUtils.escape(item.getRoleType())).append(',')
                    .append(CsvUtils.escape(item.getRequestMethod())).append(',')
                    .append(CsvUtils.escape(item.getRequestUri())).append(',')
                    .append(item.getSuccess() != null && item.getSuccess() == 1 ? "成功" : "失败").append(',')
                    .append(CsvUtils.escape(item.getMessage()))
                    .append('\n');
        }

        byte[] body = CsvUtils.utf8WithBom(csv);
        String filename = URLEncoder.encode(EXPORT_FILENAME, StandardCharsets.UTF_8);

        writeExportLog(limit, safeLimit, logs.size(), startedAt, keyword, roleType, success, startTime, endTime);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .header("X-Requested-Limit", String.valueOf(limit))
                .header("X-Effective-Limit", String.valueOf(safeLimit))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    private void writeExportLog(int requestedLimit,
                                int effectiveLimit,
                                int exportedRows,
                                long startedAt,
                                String keyword,
                                String roleType,
                                Integer success,
                                LocalDateTime startTime,
                                LocalDateTime endTime) {
        RequestActor actor = SecurityActorUtils.currentActor();
        long durationMs = Math.max(System.currentTimeMillis() - startedAt, 0L);

        String message = OperationLogMessageBuilder.buildLogExportMessage(
                requestedLimit,
                effectiveLimit,
                exportedRows,
                durationMs,
                keyword,
                roleType,
                success,
                startTime,
                endTime
        );

        operationLogService.save(actor.username(), actor.role(), "GET", EXPORT_URI, true, message);
    }
}
