package com.health.system.service;

import java.util.Map;

public interface PatientReportService {
    Map<String, Object> summary(String username, String range);

    Map<String, Object> submitSummaryTask(String username, String range);

    Map<String, Object> summaryTaskResult(String username, String taskId);
}
