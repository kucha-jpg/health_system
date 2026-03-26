package com.health.system.service;

import java.util.Map;

public interface HealthAlertService {

    void evaluateAndCreateAlert(Long userId, Long healthDataId, String indicatorType, String value);

    void deleteByHealthDataId(Long healthDataId);

    Map<String, Object> listOpenAlerts(String doctorUsername, String riskLevel, Integer minRiskScore, String sortBy, Integer pageNo, Integer pageSize);

    Map<String, Object> listMyAlerts(String username, String status, Integer pageNo, Integer pageSize);

    void handleAlert(String doctorUsername, Long id, String handleRemark);

    Map<String, Object> monitorOverview();
}
