package com.health.system.service;

import java.util.Map;

public interface DoctorPatientInsightService {
    Map<String, Object> patientInsight(String doctorUsername, Long patientUserId, String indicatorType, String timeRange);
}
