package com.health.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.entity.AlertRule;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupDoctorMember;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.mapper.DoctorGroupDoctorMemberMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
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
    private final DoctorGroupMapper doctorGroupMapper;
    private final DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper;
    private final DoctorGroupMemberMapper doctorGroupMemberMapper;

    public HealthAlertServiceImpl(HealthAlertMapper healthAlertMapper,
                                  HealthDataMapper healthDataMapper,
                                  UserMapper userMapper,
                                  AlertRuleMapper alertRuleMapper,
                                  DoctorGroupMapper doctorGroupMapper,
                                  DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper,
                                  DoctorGroupMemberMapper doctorGroupMemberMapper) {
        this.healthAlertMapper = healthAlertMapper;
        this.healthDataMapper = healthDataMapper;
        this.userMapper = userMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupDoctorMemberMapper = doctorGroupDoctorMemberMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
    }

    @Override
    public void evaluateAndCreateAlert(Long userId, Long healthDataId, String indicatorType, String value) {
        AlertDecision decision = evaluate(userId, indicatorType, value);
        if (decision == null) {
            return;
        }
        HealthAlert existing = healthAlertMapper.selectOne(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, userId)
                .eq(HealthAlert::getIndicatorType, indicatorType)
                .eq(HealthAlert::getReasonCode, decision.reasonCode())
                .eq(HealthAlert::getStatus, "OPEN")
                .orderByDesc(HealthAlert::getCreateTime)
                .last("limit 1"));
        if (existing != null) {
            existing.setHealthDataId(healthDataId);
            existing.setValue(value);
            existing.setLevel(decision.level());
            existing.setRiskScore(decision.riskScore());
            existing.setRiskLevel(decision.riskLevel());
            existing.setReasonText(decision.reasonText());
            healthAlertMapper.updateById(existing);
            return;
        }
        HealthAlert alert = new HealthAlert();
        alert.setUserId(userId);
        alert.setHealthDataId(healthDataId);
        alert.setIndicatorType(indicatorType);
        alert.setValue(value);
        alert.setLevel(decision.level());
        alert.setRiskScore(decision.riskScore());
        alert.setRiskLevel(decision.riskLevel());
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
    public List<HealthAlert> listOpenAlerts(String doctorUsername, String riskLevel, Integer minRiskScore, String sortBy) {
        User doctor = resolveDoctor(doctorUsername);
        Set<Long> patientIds = resolveAccessiblePatientIds(doctor.getId());
        if (patientIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<HealthAlert> wrapper = new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getStatus, "OPEN")
                .in(HealthAlert::getUserId, patientIds);

        if (StringUtils.hasText(riskLevel)) {
            wrapper.eq(HealthAlert::getRiskLevel, riskLevel.trim().toUpperCase());
        }

        if (minRiskScore != null) {
            int safeScore = Math.max(0, Math.min(minRiskScore, 100));
            wrapper.ge(HealthAlert::getRiskScore, safeScore);
        }

        String normalizedSort = StringUtils.hasText(sortBy) ? sortBy.trim().toLowerCase() : "risk_desc";
        if ("time_desc".equals(normalizedSort)) {
            wrapper.orderByDesc(HealthAlert::getCreateTime)
                    .orderByDesc(HealthAlert::getRiskScore);
        } else {
            wrapper.orderByDesc(HealthAlert::getRiskScore)
                    .orderByDesc(HealthAlert::getCreateTime);
        }

        return healthAlertMapper.selectList(wrapper);
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
        User doctor = resolveDoctor(doctorUsername);
        HealthAlert alert = healthAlertMapper.selectById(id);
        if (alert == null) {
            throw BusinessException.notFound("预警不存在");
        }
        assertDoctorCanAccessPatient(doctor.getId(), alert.getUserId());
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

    private AlertDecision evaluate(Long userId, String indicatorType, String value) {
        AlertRule rule = alertRuleMapper.selectOne(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getIndicatorType, indicatorType)
                .eq(AlertRule::getEnabled, 1)
                .last("limit 1"));
        return switch (indicatorType) {
            case "血压" -> evaluateBloodPressure(userId, value, rule);
            case "血糖" -> evaluateBloodSugar(value, rule);
            case "体重" -> evaluateWeight(value, rule);
            default -> null;
        };
    }

    private AlertDecision evaluateBloodPressure(Long userId, String value, AlertRule rule) {
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
            int score = severeScore(
                    ratio(systolic, highSystolic),
                    ratio(diastolic, highDiastolic)
            );
            return new AlertDecision("HIGH", score, riskLevel(score), "BP_CRITICAL", "血压达到危急阈值，建议立即就医");
        }
        if (systolic >= mediumSystolic || diastolic >= mediumDiastolic) {
            if (isThreeDayPersistentPressureHigh(userId, mediumSystolic, mediumDiastolic)) {
                int score = Math.min(79, mediumScore(
                        ratio(systolic, mediumSystolic),
                        ratio(diastolic, mediumDiastolic)
                ) + 8);
                return new AlertDecision("MEDIUM", score, riskLevel(score), "BP_PERSISTENT_HIGH", "血压连续3天高于阈值，建议尽快随访干预");
            }
            int score = mediumScore(
                    ratio(systolic, mediumSystolic),
                    ratio(diastolic, mediumDiastolic)
            );
            return new AlertDecision("MEDIUM", score, riskLevel(score), "BP_HIGH", "血压偏高，建议复测并医生随访");
        }
        return null;
    }

    private boolean isThreeDayPersistentPressureHigh(Long userId, int systolicThreshold, int diastolicThreshold) {
        LocalDateTime begin = LocalDateTime.now().minusDays(7);
        List<HealthData> bloodPressureData = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, userId)
                .eq(HealthData::getIndicatorType, "血压")
                .ge(HealthData::getReportTime, begin)
                .orderByDesc(HealthData::getReportTime));
        if (bloodPressureData.isEmpty()) {
            return false;
        }

        Set<LocalDate> highDays = new HashSet<>();
        for (HealthData item : bloodPressureData) {
            String[] arr = item.getValue() == null ? new String[0] : item.getValue().split("/");
            if (arr.length != 2) {
                continue;
            }
            try {
                int s = Integer.parseInt(arr[0]);
                int d = Integer.parseInt(arr[1]);
                if (s >= systolicThreshold || d >= diastolicThreshold) {
                    LocalDate day = item.getReportTime() == null ? null : item.getReportTime().toLocalDate();
                    if (day != null) {
                        highDays.add(day);
                    }
                }
            } catch (NumberFormatException ignore) {
                // Ignore malformed historical rows and continue checking other records.
            }
        }

        LocalDate today = LocalDate.now();
        return highDays.contains(today)
                && highDays.contains(today.minusDays(1))
                && highDays.contains(today.minusDays(2));
    }

    private AlertDecision evaluateBloodSugar(String value, AlertRule rule) {
        BigDecimal bloodSugar = new BigDecimal(value);
        BigDecimal high = parseDecimalRule(rule == null ? null : rule.getHighRule(), BigDecimal.valueOf(16.7));
        BigDecimal medium = parseDecimalRule(rule == null ? null : rule.getMediumRule(), BigDecimal.valueOf(11.1));
        if (bloodSugar.compareTo(high) >= 0) {
            int score = severeScore(ratio(bloodSugar, high));
            return new AlertDecision("HIGH", score, riskLevel(score), "GLUCOSE_CRITICAL", "血糖显著升高，建议尽快干预");
        }
        if (bloodSugar.compareTo(medium) >= 0) {
            int score = mediumScore(ratio(bloodSugar, medium));
            return new AlertDecision("MEDIUM", score, riskLevel(score), "GLUCOSE_HIGH", "血糖偏高，建议重点观察");
        }
        return null;
    }

    private AlertDecision evaluateWeight(String value, AlertRule rule) {
        BigDecimal weight = new BigDecimal(value);
        BigDecimal high = parseDecimalRule(rule == null ? null : rule.getHighRule(), BigDecimal.valueOf(200));
        if (weight.compareTo(high) >= 0) {
            int score = mediumScore(ratio(weight, high));
            return new AlertDecision("MEDIUM", score, riskLevel(score), "WEIGHT_HIGH", "体重异常偏高，建议医生评估");
        }
        return null;
    }

    private int mediumScore(BigDecimal... ratios) {
        BigDecimal maxRatio = maxRatio(ratios);
        BigDecimal extra = maxRatio.subtract(BigDecimal.ONE).max(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(120));
        int score = BigDecimal.valueOf(50).add(extra).setScale(0, RoundingMode.HALF_UP).intValue();
        return clamp(score, 50, 79);
    }

    private int severeScore(BigDecimal... ratios) {
        BigDecimal maxRatio = maxRatio(ratios);
        BigDecimal extra = maxRatio.subtract(BigDecimal.ONE).max(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(180));
        int score = BigDecimal.valueOf(80).add(extra).setScale(0, RoundingMode.HALF_UP).intValue();
        return clamp(score, 80, 100);
    }

    private BigDecimal maxRatio(BigDecimal... ratios) {
        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal ratio : ratios) {
            if (ratio != null && ratio.compareTo(max) > 0) {
                max = ratio;
            }
        }
        return max;
    }

    private BigDecimal ratio(int value, int threshold) {
        if (threshold <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(threshold), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal value, BigDecimal threshold) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return value.divide(threshold, 4, RoundingMode.HALF_UP);
    }

    private int clamp(int score, int min, int max) {
        return Math.max(min, Math.min(max, score));
    }

    private String riskLevel(int score) {
        if (score >= 80) {
            return "HIGH";
        }
        if (score >= 50) {
            return "MEDIUM";
        }
        return "LOW";
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

    private User resolveDoctor(String doctorUsername) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, doctorUsername));
        if (doctor == null || !"DOCTOR".equals(doctor.getRoleType())) {
            throw BusinessException.notFound("医生不存在");
        }
        return doctor;
    }

    private void assertDoctorCanAccessPatient(Long doctorId, Long patientUserId) {
        Set<Long> patientIds = resolveAccessiblePatientIds(doctorId);
        if (!patientIds.contains(patientUserId)) {
            throw BusinessException.forbidden("预警不在您的可处理范围内");
        }
    }

    private Set<Long> resolveAccessiblePatientIds(Long doctorId) {
        Set<Long> groupIds = resolveAccessibleGroupIds(doctorId);
        if (groupIds.isEmpty()) {
            return Set.of();
        }
        List<DoctorGroupMember> patientMembers = doctorGroupMemberMapper.selectList(new LambdaQueryWrapper<DoctorGroupMember>()
                .in(DoctorGroupMember::getGroupId, groupIds));
        if (patientMembers.isEmpty()) {
            return Set.of();
        }
        Set<Long> patientIds = new HashSet<>();
        for (DoctorGroupMember item : patientMembers) {
            patientIds.add(item.getPatientUserId());
        }
        return patientIds;
    }

    private Set<Long> resolveAccessibleGroupIds(Long doctorId) {
        List<DoctorGroup> ownerGroups = doctorGroupMapper.selectList(new LambdaQueryWrapper<DoctorGroup>()
                .eq(DoctorGroup::getDoctorId, doctorId));
        List<DoctorGroupDoctorMember> collaborativeGroups = doctorGroupDoctorMemberMapper.selectList(
                new LambdaQueryWrapper<DoctorGroupDoctorMember>()
                        .eq(DoctorGroupDoctorMember::getDoctorUserId, doctorId)
        );

        Set<Long> groupIds = new HashSet<>();
        for (DoctorGroup item : ownerGroups) {
            groupIds.add(item.getId());
        }
        for (DoctorGroupDoctorMember item : collaborativeGroups) {
            groupIds.add(item.getGroupId());
        }
        return groupIds;
    }

    private record AlertDecision(String level, int riskScore, String riskLevel, String reasonCode, String reasonText) {
    }
}
