package com.health.system.service.impl;

import com.health.system.entity.HealthAlert;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HealthAlertServiceImplTest {

    @Mock
    private HealthAlertMapper healthAlertMapper;
    @Mock
    private HealthDataMapper healthDataMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private HealthAlertServiceImpl healthAlertService;

    @Captor
    private ArgumentCaptor<HealthAlert> alertCaptor;

    @Test
    void evaluateAndCreateAlert_shouldCreateHighLevelAlert_whenBloodPressureCritical() {
        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血压", "190/120");

        verify(healthAlertMapper).insert(alertCaptor.capture());
        HealthAlert saved = alertCaptor.getValue();
        assertEquals("HIGH", saved.getLevel());
        assertEquals("OPEN", saved.getStatus());
        assertEquals("血压", saved.getIndicatorType());
    }

    @Test
    void evaluateAndCreateAlert_shouldNotCreateAlert_whenIndicatorIsNormal() {
        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血糖", "6.1");
        verify(healthAlertMapper, org.mockito.Mockito.never()).insert(org.mockito.Mockito.any());
    }
}
