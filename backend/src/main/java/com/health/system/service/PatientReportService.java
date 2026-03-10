package com.health.system.service;

import java.util.Map;

public interface PatientReportService {
    Map<String, Object> summary(String username, String range);
}
