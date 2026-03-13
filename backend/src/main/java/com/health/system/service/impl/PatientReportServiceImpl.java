package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.PatientReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
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
            throw BusinessException.notFound("用户不存在");
        }
        LocalDateTime start = "month".equalsIgnoreCase(range) ? LocalDateTime.now().minusMonths(1) : LocalDateTime.now().minusWeeks(1);

        List<HealthData> dataList = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, user.getId())
                .ge(HealthData::getReportTime, start)
                .orderByDesc(HealthData::getReportTime));

        long alertCount = healthAlertMapper.selectCount(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, user.getId())
                .ge(HealthAlert::getCreateTime, start));

        List<HealthAlert> alerts = healthAlertMapper.selectList(new LambdaQueryWrapper<HealthAlert>()
            .eq(HealthAlert::getUserId, user.getId())
            .ge(HealthAlert::getCreateTime, start)
            .orderByAsc(HealthAlert::getCreateTime));

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
        result.put("riskTrend", buildRiskTrend(alerts));
        return result;
    }

    private List<Map<String, Object>> buildRiskTrend(List<HealthAlert> alerts) {
        Map<String, int[]> agg = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (HealthAlert alert : alerts) {
            if (alert.getCreateTime() == null) {
                continue;
            }
            Integer scoreObj = alert.getRiskScore();
            int score = scoreObj == null ? 60 : scoreObj;
            String day = alert.getCreateTime().format(fmt);
            int[] stat = agg.computeIfAbsent(day, k -> new int[]{0, 0});
            stat[0] += score;
            stat[1] += 1;
        }

        return agg.entrySet().stream().map(entry -> {
            int total = entry.getValue()[0];
            int count = entry.getValue()[1];
            int avg = count == 0 ? 0 : (int) Math.round((double) total / count);
            Map<String, Object> row = new HashMap<>();
            row.put("date", entry.getKey());
            row.put("avgRiskScore", avg);
            row.put("alertCount", count);
            return row;
        }).toList();
    }
}
