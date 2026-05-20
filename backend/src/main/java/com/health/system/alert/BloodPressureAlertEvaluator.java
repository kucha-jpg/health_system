package com.health.system.alert;

import com.health.system.entity.HealthData;
import com.health.system.mapper.HealthDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BloodPressureAlertEvaluator implements AlertEvaluator {

    private static final int RECENT_DAYS = 7;
    private static final int RECENT_LIMIT = 20;

    private final HealthDataMapper healthDataMapper;

    public BloodPressureAlertEvaluator(HealthDataMapper healthDataMapper) {
        this.healthDataMapper = healthDataMapper;
    }

    @Override
    public boolean supports(String indicatorType) {
        return IndicatorTypes.BLOOD_PRESSURE.equals(indicatorType);
    }

    @Override
    public AlertDecision evaluate(AlertEvaluationContext context) {
        String[] values = context.value().split("/");
        if (values.length != 2) {
            return null;
        }
        int systolic, diastolic;
        try {
            systolic = Integer.parseInt(values[0]);
            diastolic = Integer.parseInt(values[1]);
        } catch (NumberFormatException ex) {
            return null;
        }
        int[] high = RiskScoreSupport.parsePressure(context.highRule(), 140, 90);
        int[] medium = RiskScoreSupport.parsePressure(context.mediumRule(), 130, 85);

        // Guangzhou seasonal adjustment: hot-humid climate (May-Oct) causes vasodilation.
        // Thresholds are relaxed by 5 mmHg during this period to reduce false positives.
        if (SeasonalAdjustmentSupport.isGuangzhouHotSeason()) {
            high[0] = SeasonalAdjustmentSupport.adjustedSystolicThreshold(high[0]);
            high[1] = SeasonalAdjustmentSupport.adjustedDiastolicThreshold(high[1]);
            medium[0] = SeasonalAdjustmentSupport.adjustedSystolicThreshold(medium[0]);
            medium[1] = SeasonalAdjustmentSupport.adjustedDiastolicThreshold(medium[1]);
        }

        boolean highRisk = systolic >= high[0] || diastolic >= high[1];
        boolean mediumRisk = systolic >= medium[0] || diastolic >= medium[1];

        if (!highRisk && !mediumRisk) {
            return null;
        }

        List<HealthData> recentData = selectRecent(context.userId());

        if (highRisk) {
            BigDecimal sysRatio = RiskScoreSupport.ratio(systolic, high[0]);
            BigDecimal diaRatio = RiskScoreSupport.ratio(diastolic, high[1]);
            int score = RiskScoreSupport.severeScore(sysRatio, diaRatio);
            String reasonCode = "BP_HIGH_RULE";
            String reasonText = "血压超过高风险阈值";
            if (isPersistentHigh(recentData, high[0])) {
                score = Math.min(100, score + 6);
                reasonCode = "BP_PERSISTENT_HIGH";
                reasonText = "连续3天收缩压高于高风险阈值";
            } else {
                int predicted = RiskScoreSupport.linearPredictInt(recentData, Integer.MIN_VALUE);
                if (predicted >= high[0]) {
                    score = Math.min(100, score + 3);
                    reasonCode = "BP_TREND_UP";
                    reasonText = "血压趋势预测将超阈值";
                }
            }
            return new AlertDecision("HIGH", score, RiskScoreSupport.riskLevel(score), reasonCode, reasonText);
        }

        BigDecimal sysRatio = RiskScoreSupport.ratio(systolic, medium[0]);
        BigDecimal diaRatio = RiskScoreSupport.ratio(diastolic, medium[1]);
        int score = RiskScoreSupport.mediumScore(sysRatio, diaRatio);
        String reasonCode = "BP_MEDIUM_RULE";
        String reasonText = "血压达到中风险阈值";
        int predicted = RiskScoreSupport.linearPredictInt(recentData, Integer.MIN_VALUE);
        if (predicted >= high[0]) {
            score = Math.min(79, score + 4);
            reasonCode = "BP_TREND_UP";
            reasonText = "血压趋势预测接近高风险阈值";
        }
        return new AlertDecision("MEDIUM", score, RiskScoreSupport.riskLevel(score), reasonCode, reasonText);
    }

    private boolean isPersistentHigh(List<HealthData> records, int threshold) {
        if (records == null || records.isEmpty()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            LocalDate target = today.minusDays(i);
            boolean found = records.stream().anyMatch(item -> {
                if (item.getReportTime() == null || item.getValue() == null) {
                    return false;
                }
                if (!target.equals(item.getReportTime().toLocalDate())) {
                    return false;
                }
                String[] arr = item.getValue().split("/");
                if (arr.length != 2) {
                    return false;
                }
                try {
                    return Integer.parseInt(arr[0]) >= threshold;
                } catch (NumberFormatException ex) {
                    return false;
                }
            });
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private List<HealthData> selectRecent(Long userId) {
        return healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, userId)
                .eq(HealthData::getIndicatorType, IndicatorTypes.BLOOD_PRESSURE)
                .ge(HealthData::getReportTime, LocalDateTime.now().minusDays(RECENT_DAYS))
                .orderByDesc(HealthData::getReportTime)
                .last("limit " + RECENT_LIMIT));
    }
}
