package com.health.system.common;

import java.time.LocalDateTime;

import com.health.system.dto.FeedbackQueryDTO;
import com.health.system.dto.OperationLogQueryDTO;

public final class QueryDtoBuilders {

    private QueryDtoBuilders() {
    }

    public static OperationLogQueryDTO operationLogQuery(String keyword,
                                                         String roleType,
                                                         Integer success,
                                                         LocalDateTime startTime,
                                                         LocalDateTime endTime,
                                                         Integer pageNo,
                                                         Integer pageSize) {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setKeyword(SecurityInputSanitizer.sanitizeKeyword(keyword, 64, "日志关键词"));
        query.setRoleType(SecurityInputSanitizer.sanitizeRoleType(roleType));
        query.setSuccess(success);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return query;
    }

    public static FeedbackQueryDTO feedbackQuery(String keyword,
                                                 String roleType,
                                                 Integer status,
                                                 Integer replyStatus,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 Integer pageNo,
                                                 Integer pageSize) {
        FeedbackQueryDTO query = new FeedbackQueryDTO();
        query.setKeyword(SecurityInputSanitizer.sanitizeKeyword(keyword, 64, "反馈关键词"));
        query.setRoleType(SecurityInputSanitizer.sanitizeRoleType(roleType));
        query.setStatus(status);
        query.setReplyStatus(replyStatus);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return query;
    }
}