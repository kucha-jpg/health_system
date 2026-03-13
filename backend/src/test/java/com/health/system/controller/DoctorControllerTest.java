package com.health.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
        when(healthAlertService.listOpenAlerts("doc-a", "HIGH", 80, "time_desc")).thenReturn(List.of(item));

        ApiResponse<List<HealthAlert>> response = doctorController.openAlerts(authentication, "HIGH", 80, "time_desc");

        verify(healthAlertService).listOpenAlerts("doc-a", "HIGH", 80, "time_desc");
        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals("HIGH", response.getData().get(0).getRiskLevel());
    }
}
