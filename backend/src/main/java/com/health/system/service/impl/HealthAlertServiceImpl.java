package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.HealthAlertService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HealthAlertServiceImpl implements HealthAlertService {

    private final HealthAlertMapper healthAlertMapper;
    private final HealthDataMapper healthDataMapper;
    private final UserMapper userMapper;

    public HealthAlertServiceImpl(HealthAlertMapper healthAlertMapper, HealthDataMapper healthDataMapper, UserMapper userMapper) {
        this.healthAlertMapper = healthAlertMapper;
        this.healthDataMapper = healthDataMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void evaluateAndCreateAlert(Long userId, Long healthDataId, String indicatorType, String value) {
        AlertDecision decision = evaluate(indicatorType, value);
        if (decision == null) {
            return;
        }
        HealthAlert alert = new HealthAlert();
        alert.setUserId(userId);
        alert.setHealthDataId(healthDataId);
        alert.setIndicatorType(indicatorType);
        alert.setValue(value);
        alert.setLevel(decision.level());
        alert.setReasonCode(decision.reasonCode());
        alert.setReasonText(decision.reasonText());
        alert.setStatus("OPEN");
        healthAlertMapper.insert(alert);
    }

    @Override
    public void deleteByHealthDataId(Long healthDataId) {
        healthAlertMapper.delete(new LambdaQueryWrapper<HealthAlert>().eq(HealthAlert::getHealthDataId, healthDataId));
    }

    @Override
    public List<HealthAlert> listOpenAlerts() {
        return healthAlertMapper.selectList(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getStatus, "OPEN")
                .orderByDesc(HealthAlert::getCreateTime));
    }

    @Override
    public List<HealthAlert> listMyAlerts(String username, String status) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        LambdaQueryWrapper<HealthAlert> wrapper = new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, user.getId())
                .orderByDesc(HealthAlert::getCreateTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(HealthAlert::getStatus, status);
        }
        return healthAlertMapper.selectList(wrapper);
    }

    @Override
    public void handleAlert(String doctorUsername, Long id, String handleRemark) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, doctorUsername));
        if (doctor == null) {
            throw new RuntimeException("医生不存在");
        }
        HealthAlert alert = healthAlertMapper.selectById(id);
        if (alert == null) {
            throw new RuntimeException("预警不存在");
        }
        if (!"OPEN".equals(alert.getStatus())) {
            throw new RuntimeException("预警已处理");
        }
        alert.setStatus("CLOSED");
        alert.setHandledBy(doctor.getId());
        alert.setHandledTime(LocalDateTime.now());
        alert.setHandleRemark(handleRemark);
        healthAlertMapper.updateById(alert);
    }

    @Override
    public Map<String, Object> monitorOverview() {
        long totalUsers = userMapper.selectCount(null);
        long totalHealthData = healthDataMapper.selectCount(null);
        long openAlerts = healthAlertMapper.selectCount(new LambdaQueryWrapper<HealthAlert>().eq(HealthAlert::getStatus, "OPEN"));
        List<HealthData> latestHealthData = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .orderByDesc(HealthData::getReportTime)
                .last("limit 10"));

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("totalHealthData", totalHealthData);
        result.put("openAlerts", openAlerts);
        result.put("latestHealthData", latestHealthData);
        return result;
    }

    private AlertDecision evaluate(String indicatorType, String value) {
        return switch (indicatorType) {
            case "血压" -> evaluateBloodPressure(value);
            case "血糖" -> evaluateBloodSugar(value);
            case "体重" -> evaluateWeight(value);
            default -> null;
        };
    }

    private AlertDecision evaluateBloodPressure(String value) {
        String[] arr = value.split("/");
        if (arr.length != 2) {
            return null;
        }
        int systolic = Integer.parseInt(arr[0]);
        int diastolic = Integer.parseInt(arr[1]);
        if (systolic >= 180 || diastolic >= 120) {
            return new AlertDecision("HIGH", "BP_CRITICAL", "血压达到危急阈值，建议立即就医");
        }
        if (systolic >= 140 || diastolic >= 90) {
            return new AlertDecision("MEDIUM", "BP_HIGH", "血压偏高，建议复测并医生随访");
        }
        return null;
    }

    private AlertDecision evaluateBloodSugar(String value) {
        BigDecimal bloodSugar = new BigDecimal(value);
        if (bloodSugar.compareTo(BigDecimal.valueOf(16.7)) >= 0) {
            return new AlertDecision("HIGH", "GLUCOSE_CRITICAL", "血糖显著升高，建议尽快干预");
        }
        if (bloodSugar.compareTo(BigDecimal.valueOf(11.1)) >= 0) {
            return new AlertDecision("MEDIUM", "GLUCOSE_HIGH", "血糖偏高，建议重点观察");
        }
        return null;
    }

    private AlertDecision evaluateWeight(String value) {
        BigDecimal weight = new BigDecimal(value);
        if (weight.compareTo(BigDecimal.valueOf(200)) >= 0) {
            return new AlertDecision("MEDIUM", "WEIGHT_HIGH", "体重异常偏高，建议医生评估");
        }
        return null;
    }

    private record AlertDecision(String level, String reasonCode, String reasonText) {
    }
}
