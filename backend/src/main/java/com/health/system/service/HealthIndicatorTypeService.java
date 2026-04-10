package com.health.system.service;

import java.util.List;

import com.health.system.dto.HealthIndicatorTypeDTO;
import com.health.system.entity.HealthIndicatorType;

public interface HealthIndicatorTypeService {
    List<HealthIndicatorType> listTypes(boolean includeDisabled);

    void createType(HealthIndicatorTypeDTO dto);

    void updateType(HealthIndicatorTypeDTO dto);

    void deleteType(Long id);

    boolean isEnabledType(String indicatorType);
}
