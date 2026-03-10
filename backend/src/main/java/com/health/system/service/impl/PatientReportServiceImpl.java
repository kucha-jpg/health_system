package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.PatientReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PatientReportServiceImpl implements PatientReportService {

    private final UserMapper userMapper;
    private final HealthDataMapper healthDataMapper;
    private final HealthAlertMapper healthAlertMapper;

    public PatientReportServiceImpl(UserMapper userMapper, HealthDataMapper healthDataMapper, HealthAlertMapper healthAlertMapper) {
        this.userMapper = userMapper;
        this.healthDataMapper = healthDataMapper;
        this.healthAlertMapper = healthAlertMapper;
    }

    @Override
    public Map<String, Object> summary(String username, String range) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        LocalDateTime start = "month".equalsIgnoreCase(range) ? LocalDateTime.now().minusMonths(1) : LocalDateTime.now().minusWeeks(1);

        List<HealthData> dataList = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, user.getId())
                .ge(HealthData::getReportTime, start)
                .orderByDesc(HealthData::getReportTime));

        long alertCount = healthAlertMapper.selectCount(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, user.getId())
                .ge(HealthAlert::getCreateTime, start));

        Map<String, Integer> byType = new HashMap<>();
        for (HealthData data : dataList) {
            byType.put(data.getIndicatorType(), byType.getOrDefault(data.getIndicatorType(), 0) + 1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("range", "month".equalsIgnoreCase(range) ? "month" : "week");
        result.put("reportCount", dataList.size());
        result.put("alertCount", alertCount);
        result.put("byType", byType);
        result.put("latestData", dataList.stream().limit(10).toList());
        return result;
    }
}
