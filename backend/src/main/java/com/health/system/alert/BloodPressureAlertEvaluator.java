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
        int systolic;
        int diastolic;
        try {
            systolic = Integer.parseInt(values[0]);
            diastolic = Integer.parseInt(values[1]);
        } catch (NumberFormatException ex) {
            return null;
        }
        int[] high = RiskScoreSupport.parsePressure(context.highRule(), 140, 90);
        int[] medium = RiskScoreSupport.parsePressure(context.mediumRule(), 130, 85);

        boolean highRisk = systolic >= high[0] || diastolic >= high[1];
        boolean mediumRisk = systolic >= medium[0] || diastolic >= medium[1];

        if (!highRisk && !mediumRisk) {
            return null;
        }

        if (highRisk) {
            BigDecimal sysRatio = RiskScoreSupport.ratio(systolic, high[0]);
            BigDecimal diaRatio = RiskScoreSupport.ratio(diastolic, high[1]);
            int score = RiskScoreSupport.severeScore(sysRatio, diaRatio);
            String reasonCode = "BP_HIGH_RULE";
            String reasonText = "血压超过高风险阈值";
                List<HealthData> recentData = selectRecentByIndicatorType(context.userId(), 7);
            if (isThreeDayPersistentHigh(recentData, high[0])) {
                score = Math.min(100, score + 6);
                reasonCode = "BP_PERSISTENT_HIGH";
                reasonText = "连续3天收缩压高于高风险阈值";
            } else {
                int predicted = linearPredictNextSystolic(recentData);
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
        List<HealthData> recentData = selectRecentByIndicatorType(context.userId(), 7);
        int predicted = linearPredictNextSystolic(recentData);
        if (predicted >= medium[0]) {
            score = Math.min(79, score + 4);
            reasonCode = "BP_TREND_UP";
            reasonText = "血压趋势预测接近高风险阈值";
        }
        return new AlertDecision("MEDIUM", score, RiskScoreSupport.riskLevel(score), reasonCode, reasonText);
    }

    private boolean isThreeDayPersistentHigh(List<HealthData> records, int threshold) {
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

    private int linearPredictNextSystolic(List<HealthData> records) {
        if (records == null || records.size() < 2) {
            return Integer.MIN_VALUE;
        }
        List<HealthData> sorted = records.stream()
                .filter(item -> item.getReportTime() != null && item.getValue() != null)
                .sorted((a, b) -> a.getReportTime().compareTo(b.getReportTime()))
                .toList();
        if (sorted.size() < 2) {
            return Integer.MIN_VALUE;
        }
        double n = sorted.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;
        for (int i = 0; i < sorted.size(); i++) {
            String[] arr = sorted.get(i).getValue().split("/");
            if (arr.length != 2) {
                return Integer.MIN_VALUE;
            }
            int y;
            try {
                y = Integer.parseInt(arr[0]);
            } catch (NumberFormatException ex) {
                return Integer.MIN_VALUE;
            }
            double x = i + 1;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double denominator = n * sumXX - sumX * sumX;
        if (denominator == 0) {
            return Integer.MIN_VALUE;
        }
        double k = (n * sumXY - sumX * sumY) / denominator;
        double b = (sumY - k * sumX) / n;
        return (int) Math.round(k * (n + 1) + b);
    }

    private List<HealthData> selectRecentByIndicatorType(Long userId, int days) {
        return healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, userId)
                .eq(HealthData::getIndicatorType, IndicatorTypes.BLOOD_PRESSURE)
                .ge(HealthData::getReportTime, LocalDateTime.now().minusDays(days))
                .orderByDesc(HealthData::getReportTime)
                .last("limit 20"));
    }
}
