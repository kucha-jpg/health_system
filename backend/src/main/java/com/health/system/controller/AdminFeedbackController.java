package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.common.CsvUtils;
import com.health.system.common.OperationLogMessageBuilder;
import com.health.system.common.QueryDtoBuilders;
import com.health.system.common.RequestActor;
import com.health.system.common.SecurityActorUtils;
import com.health.system.dto.AdminFeedbackBatchStatusDTO;
import com.health.system.dto.AdminFeedbackReplyDTO;
import com.health.system.dto.FeedbackQueryDTO;
import com.health.system.entity.FeedbackMessage;
import com.health.system.service.FeedbackMessageService;
import com.health.system.service.OperationLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {

    private static final String BATCH_STATUS_URI = "/api/admin/feedback/batch-status";
    private static final String BATCH_STATUS_BY_FILTER_URI = "/api/admin/feedback/batch-status-by-filter";
    private static final String FEEDBACK_EXPORT_FILENAME = "feedback_export.csv";

    private final FeedbackMessageService feedbackMessageService;
    private final OperationLogService operationLogService;

    public AdminFeedbackController(FeedbackMessageService feedbackMessageService,
                                   OperationLogService operationLogService) {
        this.feedbackMessageService = feedbackMessageService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<List<FeedbackMessage>> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String roleType,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) Integer replyStatus) {
        return ApiResponse.success(feedbackMessageService.listAll(
                QueryDtoBuilders.feedbackQuery(keyword, roleType, status, replyStatus, null, null, null, null)
        ));
    }

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String roleType,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) Integer replyStatus,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(feedbackMessageService.listAllPaged(
                QueryDtoBuilders.feedbackQuery(keyword, roleType, status, replyStatus, startTime, endTime, pageNo, pageSize)
        ));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(feedbackMessageService.getAdminStats());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String roleType,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) Integer replyStatus,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        FeedbackQueryDTO query = QueryDtoBuilders.feedbackQuery(keyword, roleType, status, replyStatus, startTime, endTime, null, null);
        List<FeedbackMessage> data = feedbackMessageService.listAllForExport(query);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,账号,角色,反馈内容,回复内容,状态,提交时间,回复时间\n");
        for (FeedbackMessage item : data) {
            csv.append(item.getId()).append(',')
                    .append(CsvUtils.escape(item.getSenderUsername())).append(',')
                    .append(CsvUtils.escape(item.getSenderRoleType())).append(',')
                    .append(CsvUtils.escape(item.getContent())).append(',')
                    .append(CsvUtils.escape(item.getReplyContent())).append(',')
                    .append(item.getStatus() != null && item.getStatus() == 1 ? "已处理" : "未处理").append(',')
                    .append(CsvUtils.escape(item.getCreateTime() == null ? "" : item.getCreateTime().toString())).append(',')
                    .append(CsvUtils.escape(item.getRepliedTime() == null ? "" : item.getRepliedTime().toString()))
                    .append('\n');
        }

        byte[] body = CsvUtils.utf8WithBom(csv);
        String filename = URLEncoder.encode(FEEDBACK_EXPORT_FILENAME, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping("/pending-count")
    public ApiResponse<Long> pendingCount() {
        return ApiResponse.success(feedbackMessageService.countPending());
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        feedbackMessageService.updateStatus(id, status);
        return ApiResponse.success("状态更新成功", null);
    }

    @PatchMapping("/batch-status")
    public ApiResponse<Map<String, Object>> batchUpdateStatus(@Valid @RequestBody AdminFeedbackBatchStatusDTO dto) {
        Map<String, Object> result = feedbackMessageService.batchUpdateStatus(dto.getIds(), dto.getStatus());
        writeBatchLog(BATCH_STATUS_URI, dto.getStatus(), result, "by-selection");
        return ApiResponse.success(result);
    }

    @PatchMapping("/batch-status-by-filter")
    public ApiResponse<Map<String, Object>> batchUpdateStatusByFilter(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String roleType,
                                                                       @RequestParam(required = false) Integer status,
                                                                       @RequestParam(required = false) Integer replyStatus,
                                                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                                       @RequestParam Integer targetStatus) {
                                        FeedbackQueryDTO query = QueryDtoBuilders.feedbackQuery(keyword, roleType, status, replyStatus, startTime, endTime, null, null);
        Map<String, Object> result = feedbackMessageService.batchUpdateStatusByFilter(query, targetStatus);
        writeBatchLog(BATCH_STATUS_BY_FILTER_URI, targetStatus, result,
                                            "by-filter keyword=" + OperationLogMessageBuilder.safeValue(keyword)
                                                + " roleType=" + OperationLogMessageBuilder.safeValue(roleType)
                                                + " status=" + OperationLogMessageBuilder.safeValue(status)
                                                + " replyStatus=" + OperationLogMessageBuilder.safeValue(replyStatus)
                                                + " startTime=" + OperationLogMessageBuilder.safeValue(startTime)
                                                + " endTime=" + OperationLogMessageBuilder.safeValue(endTime));
        return ApiResponse.success(result);
    }

    private void writeBatchLog(String uri, Integer targetStatus, Map<String, Object> result, String extra) {
        RequestActor actor = SecurityActorUtils.currentActor();

        String message = OperationLogMessageBuilder.buildFeedbackBatchMessage(targetStatus, result, extra);
        operationLogService.save(actor.username(), actor.role(), "PATCH", uri, true, message);
    }

    @PutMapping("/reply")
    public ApiResponse<Void> reply(@Valid @RequestBody AdminFeedbackReplyDTO dto) {
        feedbackMessageService.replyFeedback(dto);
        return ApiResponse.success("回复成功", null);
    }
}
