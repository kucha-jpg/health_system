package com.health.system.service;

import java.util.List;
import java.util.Map;

import com.health.system.entity.HealthAlert;

public interface HealthAlertService {

    void evaluateAndCreateAlert(Long userId, Long healthDataId, String indicatorType, String value);

    void deleteByHealthDataId(Long healthDataId);

    List<HealthAlert> listOpenAlerts(String doctorUsername, String riskLevel, Integer minRiskScore, String sortBy);

    List<HealthAlert> listMyAlerts(String username, String status);

    void handleAlert(String doctorUsername, Long id, String handleRemark);

    Map<String, Object> monitorOverview();
}
