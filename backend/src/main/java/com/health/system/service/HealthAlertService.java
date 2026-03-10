package com.health.system.service;

import com.health.system.entity.HealthAlert;

import java.util.List;
import java.util.Map;

public interface HealthAlertService {

    void evaluateAndCreateAlert(Long userId, Long healthDataId, String indicatorType, String value);

    void deleteByHealthDataId(Long healthDataId);

    List<HealthAlert> listOpenAlerts();

    List<HealthAlert> listMyAlerts(String username, String status);

    void handleAlert(String doctorUsername, Long id, String handleRemark);

    Map<String, Object> monitorOverview();
}
