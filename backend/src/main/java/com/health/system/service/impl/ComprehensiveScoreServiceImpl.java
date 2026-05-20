package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.alert.IndicatorTypes;
import com.health.system.common.BusinessException;
import com.health.system.dto.ComprehensiveScoreResult;
import com.health.system.dto.ComprehensiveScoreResult.IndicatorScoreDetail;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.ComprehensiveScoreService;
import com.health.system.service.support.DoctorAccessSupport;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ComprehensiveScoreServiceImpl implements ComprehensiveScoreService {

    private static final Map<String, BigDecimal> DEFAULT_WEIGHTS = Map.of(
            IndicatorTypes.BLOOD_PRESSURE, new BigDecimal("0.30"),
            IndicatorTypes.BLOOD_SUGAR, new BigDecimal("0.30"),
            IndicatorTypes.WEIGHT, new BigDecimal("0.20"),
            IndicatorTypes.MEDICATION, new BigDecimal("0.20")
    );

    private static final int DATA_WINDOW_DAYS = 30;
    private static final int MEDICATION_WINDOW_DAYS = 7;

    private final UserMapper userMapper;
    private final HealthDataMapper healthDataMapper;
    private final HealthAlertMapper healthAlertMapper;
    private final DoctorAccessSupport doctorAccessSupport;

    public ComprehensiveScoreServiceImpl(UserMapper userMapper,
                                         HealthDataMapper healthDataMapper,
                                         HealthAlertMapper healthAlertMapper,
                                         DoctorAccessSupport doctorAccessSupport) {
        this.userMapper = userMapper;
        this.healthDataMapper = healthDataMapper;
        this.healthAlertMapper = healthAlertMapper;
        this.doctorAccessSupport = doctorAccessSupport;
    }

    @Override
    public ComprehensiveScoreResult calculate(String patientUsername) {
        User patient = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, patientUsername));
        if (patient == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return buildResult(patient.getId());
    }

    @Override
    public ComprehensiveScoreResult calculateForPatient(String doctorUsername, Long patientUserId) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        doctorAccessSupport.assertPatientAccessible(doctor.getId(), patientUserId, "该患者不在您的群组中");
        User patient = userMapper.selectById(patientUserId);
        if (patient == null || !"PATIENT".equals(patient.getRoleType())) {
            throw BusinessException.notFound("患者不存在");
        }
        return buildResult(patientUserId);
    }

    private ComprehensiveScoreResult buildResult(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(DATA_WINDOW_DAYS);

        List<IndicatorScoreDetail> details = new ArrayList<>();
        details.add(buildDetail(userId, IndicatorTypes.BLOOD_PRESSURE, since));
        details.add(buildDetail(userId, IndicatorTypes.BLOOD_SUGAR, since));
        details.add(buildDetail(userId, IndicatorTypes.WEIGHT, since));
        details.add(buildMedicationDetail(userId));

        List<IndicatorScoreDetail> activeDetails = details.stream()
                .filter(IndicatorScoreDetail::isHasData)
                .toList();

        if (activeDetails.isEmpty()) {
            ComprehensiveScoreResult result = new ComprehensiveScoreResult();
            result.setTotalScore(0);
            result.setRiskLevel("LOW");
            result.setDetails(details);
            result.setSummary("暂无近期健康数据，无法计算综合风险评分");
            result.setCalculatedAt(LocalDateTime.now());
            return result;
        }

        BigDecimal weightSum = activeDetails.stream()
                .map(IndicatorScoreDetail::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeighted = BigDecimal.ZERO;
        for (IndicatorScoreDetail detail : activeDetails) {
            BigDecimal normalizedWeight = detail.getWeight().divide(weightSum, 4, RoundingMode.HALF_UP);
            BigDecimal riskScoreDecimal = BigDecimal.valueOf(detail.getRiskScore());
            BigDecimal weighted = riskScoreDecimal.multiply(normalizedWeight);
            detail.setWeightedScore(weighted.setScale(1, RoundingMode.HALF_UP));
            totalWeighted = totalWeighted.add(weighted);
        }

        int totalScore = totalWeighted.setScale(0, RoundingMode.HALF_UP).intValue();
        String riskLevel;
        if (totalScore >= 80) {
            riskLevel = "HIGH";
        } else if (totalScore >= 50) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        ComprehensiveScoreResult result = new ComprehensiveScoreResult();
        result.setTotalScore(totalScore);
        result.setRiskLevel(riskLevel);
        result.setDetails(details);
        result.setSummary(buildSummary(totalScore, riskLevel, activeDetails));
        result.setCalculatedAt(LocalDateTime.now());
        return result;
    }

    private IndicatorScoreDetail buildDetail(Long userId, String indicatorType, LocalDateTime since) {
        IndicatorScoreDetail detail = new IndicatorScoreDetail();
        detail.setIndicatorType(indicatorType);
        detail.setWeight(DEFAULT_WEIGHTS.getOrDefault(indicatorType, BigDecimal.ZERO));

        HealthData latestData = healthDataMapper.selectOne(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, userId)
                .eq(HealthData::getIndicatorType, indicatorType)
                .orderByDesc(HealthData::getReportTime)
                .last("limit 1"));

        if (latestData == null) {
            detail.setHasData(false);
            detail.setRiskScore(0);
            detail.setLatestValue(null);
            detail.setWeightedScore(BigDecimal.ZERO);
            return detail;
        }

        detail.setHasData(true);
        detail.setLatestValue(latestData.getValue());

        HealthAlert openAlert = healthAlertMapper.selectOne(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, userId)
                .eq(HealthAlert::getIndicatorType, indicatorType)
                .eq(HealthAlert::getStatus, "OPEN")
                .orderByDesc(HealthAlert::getRiskScore)
                .last("limit 1"));

        if (openAlert != null && openAlert.getRiskScore() != null) {
            detail.setRiskScore(openAlert.getRiskScore());
        } else {
            detail.setRiskScore(10);
        }

        detail.setWeightedScore(BigDecimal.ZERO);
        return detail;
    }

    private IndicatorScoreDetail buildMedicationDetail(Long userId) {
        IndicatorScoreDetail detail = new IndicatorScoreDetail();
        detail.setIndicatorType(IndicatorTypes.MEDICATION);
        detail.setWeight(DEFAULT_WEIGHTS.getOrDefault(IndicatorTypes.MEDICATION, BigDecimal.ZERO));

        LocalDateTime medSince = LocalDateTime.now().minusDays(MEDICATION_WINDOW_DAYS);
        List<HealthData> medRecords = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, userId)
                .eq(HealthData::getIndicatorType, IndicatorTypes.MEDICATION)
                .ge(HealthData::getReportTime, medSince)
                .orderByDesc(HealthData::getReportTime));

        if (medRecords.isEmpty()) {
            detail.setHasData(false);
            detail.setRiskScore(0);
            detail.setLatestValue(null);
            detail.setWeightedScore(BigDecimal.ZERO);
            return detail;
        }

        detail.setHasData(true);
        detail.setLatestValue(medRecords.get(0).getValue());

        long notTakenCount = medRecords.stream()
                .filter(r -> "未服药".equals(r.getValue()) || "0".equals(r.getValue()))
                .count();

        if (notTakenCount >= 3) {
            detail.setRiskScore(80);
        } else if (notTakenCount >= 1) {
            detail.setRiskScore(50);
        } else {
            detail.setRiskScore(10);
        }

        detail.setWeightedScore(BigDecimal.ZERO);
        return detail;
    }

    private String buildSummary(int totalScore, String riskLevel, List<IndicatorScoreDetail> details) {
        long highCount = details.stream().filter(d -> d.getRiskScore() >= 80).count();
        long mediumCount = details.stream().filter(d -> d.getRiskScore() >= 50 && d.getRiskScore() < 80).count();

        StringBuilder sb = new StringBuilder();
        sb.append("综合风险评分").append(totalScore).append("分，风险等级：");
        switch (riskLevel) {
            case "HIGH" -> sb.append("高风险");
            case "MEDIUM" -> sb.append("中风险");
            default -> sb.append("低风险");
        }
        if (highCount > 0) {
            sb.append("。").append(highCount).append("项指标处于高风险状态");
        }
        if (mediumCount > 0) {
            sb.append("，").append(mediumCount).append("项指标处于中风险状态");
        }
        sb.append("。权重分配：血压0.30、血糖0.30、体重0.20、服药0.20，基于中国慢性病防治指南设定。");
        return sb.toString();
    }
}
