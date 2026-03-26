package com.health.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.health.system.common.ApiResponse;
import com.health.system.entity.HealthAlert;
import com.health.system.service.DoctorGroupService;
import com.health.system.service.DoctorPatientInsightService;
import com.health.system.service.HealthAlertService;

@ExtendWith(MockitoExtension.class)
class DoctorControllerTest {

    @Mock
    private HealthAlertService healthAlertService;

    @Test
    void openAlerts_shouldPassRiskFilterParamsToService() {
        DoctorGroupService doctorGroupService = mock(DoctorGroupService.class);
        DoctorPatientInsightService doctorPatientInsightService = mock(DoctorPatientInsightService.class);
        DoctorController doctorController = new DoctorController(healthAlertService, doctorGroupService, doctorPatientInsightService);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("doc-a");

        HealthAlert item = new HealthAlert();
        item.setId(1L);
        item.setRiskLevel("HIGH");
        item.setRiskScore(88);
        when(healthAlertService.listOpenAlerts("doc-a", "HIGH", 80, "time_desc", 1, 20))
                .thenReturn(Map.of("list", List.of(item), "total", 1L));

        ApiResponse<Map<String, Object>> response = doctorController.openAlerts(authentication, "HIGH", 80, "time_desc", 1, 20);

        verify(healthAlertService).listOpenAlerts("doc-a", "HIGH", 80, "time_desc", 1, 20);
        assertEquals(200, response.getCode());
        assertEquals(1, ((List<?>) response.getData().get("list")).size());
    }
}
