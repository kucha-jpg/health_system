package com.health.system.service;

import com.health.system.dto.HealthDataDTO;

import java.util.Map;

public interface HealthDataService {
    void create(String username, HealthDataDTO dto);

    Map<String, Object> list(String username, String indicatorType, String timeRange, Integer pageNo, Integer pageSize);

    void update(String username, Long id, HealthDataDTO dto);

    void delete(String username, Long id);
}
