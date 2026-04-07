package com.health.system.service.impl;

import com.health.system.alert.AlertDecision;
import com.health.system.alert.AlertEvaluationEngine;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupDoctorMemberMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.support.DoctorAccessSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthAlertServiceImplTest {

    @Mock
    private HealthAlertMapper healthAlertMapper;
    @Mock
    private HealthDataMapper healthDataMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AlertEvaluationEngine alertEvaluationEngine;
    @Mock
    private DoctorGroupMapper doctorGroupMapper;
    @Mock
    private DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper;
    @Mock
    private DoctorGroupMemberMapper doctorGroupMemberMapper;
    @Mock
    private DoctorAccessSupport doctorAccessSupport;

    @InjectMocks
    private HealthAlertServiceImpl healthAlertService;

    @Captor
    private ArgumentCaptor<HealthAlert> alertCaptor;

    @Test
    void listOpenAlerts_shouldReturnEmpty_whenDoctorHasNoAccessiblePatient() {
        User doctor = new User();
        doctor.setId(1L);
        doctor.setRoleType("DOCTOR");

        when(doctorAccessSupport.requireDoctor("doc-a")).thenReturn(doctor);
        when(doctorAccessSupport.resolveAccessiblePatientIds(1L)).thenReturn(java.util.Set.of());

        Map<String, Object> result = healthAlertService.listOpenAlerts("doc-a", null, null, null, 1, 20);

        assertEquals(0, ((List<?>) result.get("list")).size());
        verifyNoInteractions(doctorGroupMemberMapper);
        verify(healthAlertMapper, times(0)).selectPage(any(), any());
    }

    @Test
    void evaluateAndCreateAlert_shouldCreateHighLevelAlert_whenBloodPressureCritical() {
        when(alertEvaluationEngine.evaluate(1L, "血压", "190/120"))
            .thenReturn(new AlertDecision("HIGH", 90, "HIGH", "BP_HIGH_RULE", "血压超过高风险阈值"));

        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血压", "190/120");

        verify(healthAlertMapper).insert(alertCaptor.capture());
        verifyNoInteractions(healthDataMapper, userMapper);
        HealthAlert saved = alertCaptor.getValue();
        assertEquals("HIGH", saved.getLevel());
        assertEquals("HIGH", saved.getRiskLevel());
        assertEquals(90, saved.getRiskScore());
        assertEquals("OPEN", saved.getStatus());
        assertEquals("血压", saved.getIndicatorType());
    }

    @Test
    void evaluateAndCreateAlert_shouldCreateMediumRiskAlert_whenBloodPressureModeratelyHigh() {
        when(alertEvaluationEngine.evaluate(1L, "血压", "145/92"))
            .thenReturn(new AlertDecision("MEDIUM", 54, "MEDIUM", "BP_MEDIUM_RULE", "血压达到中风险阈值"));

        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血压", "145/92");

        verify(healthAlertMapper).insert(alertCaptor.capture());
        HealthAlert saved = alertCaptor.getValue();
        assertEquals("MEDIUM", saved.getLevel());
        assertEquals("MEDIUM", saved.getRiskLevel());
        assertEquals(54, saved.getRiskScore());
    }

    @Test
    void evaluateAndCreateAlert_shouldNotCreateAlert_whenIndicatorIsNormal() {
        when(alertEvaluationEngine.evaluate(1L, "血糖", "6.1")).thenReturn(null);

        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血糖", "6.1");
        verifyNoInteractions(userMapper);
        verify(healthAlertMapper, org.mockito.Mockito.never()).insert(org.mockito.Mockito.any());
    }

    @Test
    void evaluateAndCreateAlert_shouldCreateTrendAlert_whenGlucoseRisingTowardThreshold() {
        when(alertEvaluationEngine.evaluate(1L, "血糖", "10.9"))
                .thenReturn(new AlertDecision("MEDIUM", 66, "MEDIUM", "BS_TREND_UP", "血糖趋势预测接近高风险阈值"));

        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血糖", "10.9");

        verify(healthAlertMapper).insert(alertCaptor.capture());
        HealthAlert saved = alertCaptor.getValue();
        assertEquals("BS_TREND_UP", saved.getReasonCode());
        assertEquals("MEDIUM", saved.getLevel());
        assertEquals("MEDIUM", saved.getRiskLevel());
    }

    @Test
    void evaluateAndCreateAlert_shouldUsePersonalizedThreshold_whenPreferenceEnabled() {
        when(alertEvaluationEngine.evaluate(1L, "血压", "136/86"))
                .thenReturn(new AlertDecision("MEDIUM", 52, "MEDIUM", "BP_MEDIUM_RULE", "血压达到中风险阈值"));

        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血压", "136/86");

        verify(healthAlertMapper).insert(alertCaptor.capture());
        HealthAlert saved = alertCaptor.getValue();
        assertEquals("BP_MEDIUM_RULE", saved.getReasonCode());
        assertEquals("MEDIUM", saved.getLevel());
    }

    @Test
    void evaluateAndCreateAlert_shouldFallbackToSystemRule_whenNoPersonalizedPreference() {
        when(alertEvaluationEngine.evaluate(1L, "血压", "136/86"))
                .thenReturn(new AlertDecision("MEDIUM", 52, "MEDIUM", "BP_MEDIUM_RULE", "血压达到中风险阈值"));

        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血压", "136/86");

        verify(healthAlertMapper).insert(alertCaptor.capture());
        HealthAlert saved = alertCaptor.getValue();
        assertEquals("BP_MEDIUM_RULE", saved.getReasonCode());
        assertEquals("MEDIUM", saved.getLevel());
    }
}
