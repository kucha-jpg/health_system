package com.health.system.service.impl;

import com.health.system.dto.HealthDataDTO;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.HealthAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDataServiceImplTest {

    @Mock
    private HealthDataMapper healthDataMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private HealthAlertService healthAlertService;

    @InjectMocks
    private HealthDataServiceImpl healthDataService;

    @Captor
    private ArgumentCaptor<HealthData> dataCaptor;

    @Test
    void create_shouldInsertHealthData_whenBloodPressureValid() {
        User user = new User();
        user.setId(100L);
        user.setUsername("patient01");
        when(userMapper.selectOne(any())).thenReturn(user);

        HealthDataDTO dto = new HealthDataDTO();
        dto.setIndicatorType("血压");
        dto.setValue("120/80");
        dto.setReportTime(LocalDateTime.of(2026, 1, 1, 8, 0));
        dto.setRemark("晨起测量");

        healthDataService.create("patient01", dto);

        verify(healthDataMapper).insert(dataCaptor.capture());
        HealthData inserted = dataCaptor.getValue();
        assertEquals(100L, inserted.getUserId());
        assertEquals("血压", inserted.getIndicatorType());
        assertEquals("120/80", inserted.getValue());
        assertEquals("晨起测量", inserted.getRemark());
        verify(healthAlertService).evaluateAndCreateAlert(any(), any(), any(), any());
    }

    @Test
    void create_shouldThrow_whenBloodSugarOutOfRange() {
        User user = new User();
        user.setId(100L);
        when(userMapper.selectOne(any())).thenReturn(user);

        HealthDataDTO dto = new HealthDataDTO();
        dto.setIndicatorType("血糖");
        dto.setValue("31");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> healthDataService.create("patient01", dto));
        assertEquals("血糖必须在0-30之间", ex.getMessage());
    }

    @Test
    void update_shouldThrow_whenDataNotOwnedByCurrentUser() {
        User user = new User();
        user.setId(100L);
        when(userMapper.selectOne(any())).thenReturn(user);

        HealthData old = new HealthData();
        old.setId(1L);
        old.setUserId(200L);
        when(healthDataMapper.selectById(1L)).thenReturn(old);

        HealthDataDTO dto = new HealthDataDTO();
        dto.setIndicatorType("体重");
        dto.setValue("60");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> healthDataService.update("patient01", 1L, dto));
        assertEquals("数据不存在或无权限", ex.getMessage());
    }
}
