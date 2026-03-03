package com.health.system.service;

import com.health.system.dto.HealthDataDTO;
import com.health.system.entity.HealthData;

import java.util.List;

public interface HealthDataService {
    void create(String username, HealthDataDTO dto);

    List<HealthData> list(String username, String indicatorType, String timeRange);

    void update(String username, Long id, HealthDataDTO dto);

    void delete(String username, Long id);
}
