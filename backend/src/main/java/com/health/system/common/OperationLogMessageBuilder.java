package com.health.system.common;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class OperationLogMessageBuilder {

    private OperationLogMessageBuilder() {
    }

    public static String buildLogExportMessage(int requestedLimit,
                                               int effectiveLimit,
                                               int exportedRows,
                                               long durationMs,
                                               String keyword,
                                               String roleType,
                                               Integer success,
                                               LocalDateTime startTime,
                                               LocalDateTime endTime) {
        return "log-export requestedLimit=" + requestedLimit
                + " effectiveLimit=" + effectiveLimit
                + " exportedRows=" + exportedRows
                + " durationMs=" + durationMs
                + " keyword=" + safeValue(keyword)
                + " roleType=" + safeValue(roleType)
                + " success=" + safeValue(success)
                + " startTime=" + safeValue(startTime)
                + " endTime=" + safeValue(endTime);
    }

    public static String buildFeedbackBatchMessage(Integer targetStatus,
                                                   Map<String, Object> result,
                                                   String extra) {
        return "feedback-batch targetStatus=" + safeValue(targetStatus)
                + " requested=" + safeValue(result.get("requestedCount"))
                + " success=" + safeValue(result.get("successCount"))
                + " skipped=" + safeValue(result.get("skippedCount"))
                + " failed=" + safeValue(sizeOfList(result.get("failedIds")))
                + " " + extra;
    }

    public static String safeValue(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static int sizeOfList(Object value) {
        if (value instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }
}