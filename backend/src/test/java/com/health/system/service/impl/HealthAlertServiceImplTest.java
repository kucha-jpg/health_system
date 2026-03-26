package com.health.system.service.impl;

import com.health.system.entity.HealthAlert;
import com.health.system.entity.User;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.mapper.DoctorGroupDoctorMemberMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
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
    private AlertRuleMapper alertRuleMapper;
    @Mock
    private DoctorGroupMapper doctorGroupMapper;
    @Mock
    private DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper;
    @Mock
    private DoctorGroupMemberMapper doctorGroupMemberMapper;

    @InjectMocks
    private HealthAlertServiceImpl healthAlertService;

    @Captor
    private ArgumentCaptor<HealthAlert> alertCaptor;

    @Test
    void listOpenAlerts_shouldReturnEmpty_whenDoctorHasNoAccessiblePatient() {
        User doctor = new User();
        doctor.setId(1L);
        doctor.setRoleType("DOCTOR");

        when(userMapper.selectOne(any())).thenReturn(doctor);
        when(doctorGroupMapper.selectList(any())).thenReturn(List.of());
        when(doctorGroupDoctorMemberMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = healthAlertService.listOpenAlerts("doc-a", null, null, null, 1, 20);

        assertEquals(0, ((List<?>) result.get("list")).size());
        verify(healthAlertMapper, times(0)).selectPage(any(), any());
    }

    @Test
    void evaluateAndCreateAlert_shouldCreateHighLevelAlert_whenBloodPressureCritical() {
        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血压", "190/120");

        verify(healthAlertMapper).insert(alertCaptor.capture());
        verify(alertRuleMapper).selectOne(any());
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
        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血压", "145/92");

        verify(healthAlertMapper).insert(alertCaptor.capture());
        HealthAlert saved = alertCaptor.getValue();
        assertEquals("MEDIUM", saved.getLevel());
        assertEquals("MEDIUM", saved.getRiskLevel());
        assertEquals(54, saved.getRiskScore());
    }

    @Test
    void evaluateAndCreateAlert_shouldNotCreateAlert_whenIndicatorIsNormal() {
        healthAlertService.evaluateAndCreateAlert(1L, 2L, "血糖", "6.1");
        verify(alertRuleMapper).selectOne(any());
        verifyNoInteractions(healthDataMapper, userMapper);
        verify(healthAlertMapper, org.mockito.Mockito.never()).insert(org.mockito.Mockito.any());
    }
}
