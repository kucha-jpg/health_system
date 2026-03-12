package com.health.system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.entity.AlertRule;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.HealthAlertService;

@Service
public class HealthAlertServiceImpl implements HealthAlertService {

    private final HealthAlertMapper healthAlertMapper;
    private final HealthDataMapper healthDataMapper;
    private final UserMapper userMapper;
    private final AlertRuleMapper alertRuleMapper;

    public HealthAlertServiceImpl(HealthAlertMapper healthAlertMapper,
                                  HealthDataMapper healthDataMapper,
                                  UserMapper userMapper,
                                  AlertRuleMapper alertRuleMapper) {
        this.healthAlertMapper = healthAlertMapper;
        this.healthDataMapper = healthDataMapper;
        this.userMapper = userMapper;
        this.alertRuleMapper = alertRuleMapper;
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
            throw BusinessException.notFound("用户不存在");
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
            throw BusinessException.notFound("医生不存在");
        }
        HealthAlert alert = healthAlertMapper.selectById(id);
        if (alert == null) {
            throw BusinessException.notFound("预警不存在");
        }
        if (!"OPEN".equals(alert.getStatus())) {
            throw BusinessException.conflict("预警已处理");
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
        AlertRule rule = alertRuleMapper.selectOne(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getIndicatorType, indicatorType)
                .eq(AlertRule::getEnabled, 1)
                .last("limit 1"));
        return switch (indicatorType) {
            case "血压" -> evaluateBloodPressure(value, rule);
            case "血糖" -> evaluateBloodSugar(value, rule);
            case "体重" -> evaluateWeight(value, rule);
            default -> null;
        };
    }

    private AlertDecision evaluateBloodPressure(String value, AlertRule rule) {
        String[] arr = value.split("/");
        if (arr.length != 2) {
            return null;
        }
        int systolic = Integer.parseInt(arr[0]);
        int diastolic = Integer.parseInt(arr[1]);

        int highSystolic = 180;
        int highDiastolic = 120;
        int mediumSystolic = 140;
        int mediumDiastolic = 90;
        if (rule != null) {
            int[] highRule = parsePressure(rule.getHighRule(), highSystolic, highDiastolic);
            int[] mediumRule = parsePressure(rule.getMediumRule(), mediumSystolic, mediumDiastolic);
            highSystolic = highRule[0];
            highDiastolic = highRule[1];
            mediumSystolic = mediumRule[0];
            mediumDiastolic = mediumRule[1];
        }

        if (systolic >= highSystolic || diastolic >= highDiastolic) {
            return new AlertDecision("HIGH", "BP_CRITICAL", "血压达到危急阈值，建议立即就医");
        }
        if (systolic >= mediumSystolic || diastolic >= mediumDiastolic) {
            return new AlertDecision("MEDIUM", "BP_HIGH", "血压偏高，建议复测并医生随访");
        }
        return null;
    }

    private AlertDecision evaluateBloodSugar(String value, AlertRule rule) {
        BigDecimal bloodSugar = new BigDecimal(value);
        BigDecimal high = parseDecimalRule(rule == null ? null : rule.getHighRule(), BigDecimal.valueOf(16.7));
        BigDecimal medium = parseDecimalRule(rule == null ? null : rule.getMediumRule(), BigDecimal.valueOf(11.1));
        if (bloodSugar.compareTo(high) >= 0) {
            return new AlertDecision("HIGH", "GLUCOSE_CRITICAL", "血糖显著升高，建议尽快干预");
        }
        if (bloodSugar.compareTo(medium) >= 0) {
            return new AlertDecision("MEDIUM", "GLUCOSE_HIGH", "血糖偏高，建议重点观察");
        }
        return null;
    }

    private AlertDecision evaluateWeight(String value, AlertRule rule) {
        BigDecimal weight = new BigDecimal(value);
        BigDecimal high = parseDecimalRule(rule == null ? null : rule.getHighRule(), BigDecimal.valueOf(200));
        if (weight.compareTo(high) >= 0) {
            return new AlertDecision("MEDIUM", "WEIGHT_HIGH", "体重异常偏高，建议医生评估");
        }
        return null;
    }

    private int[] parsePressure(String rule, int defaultSystolic, int defaultDiastolic) {
        if (rule == null || rule.isBlank()) {
            return new int[]{defaultSystolic, defaultDiastolic};
        }
        String[] arr = rule.split("/");
        if (arr.length != 2) {
            return new int[]{defaultSystolic, defaultDiastolic};
        }
        try {
            return new int[]{Integer.parseInt(arr[0]), Integer.parseInt(arr[1])};
        } catch (NumberFormatException ex) {
            return new int[]{defaultSystolic, defaultDiastolic};
        }
    }

    private BigDecimal parseDecimalRule(String rule, BigDecimal defaultValue) {
        if (rule == null || rule.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(rule);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private record AlertDecision(String level, String reasonCode, String reasonText) {
    }
}
