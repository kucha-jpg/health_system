package com.health.system.alert;

import com.health.system.entity.HealthData;
import com.health.system.mapper.HealthDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class WeightAlertEvaluator implements AlertEvaluator {

    private final HealthDataMapper healthDataMapper;

    public WeightAlertEvaluator(HealthDataMapper healthDataMapper) {
        this.healthDataMapper = healthDataMapper;
    }

    @Override
    public boolean supports(String indicatorType) {
        return IndicatorTypes.WEIGHT.equals(indicatorType);
    }

    @Override
    public AlertDecision evaluate(AlertEvaluationContext context) {
        BigDecimal value;
        try {
            value = new BigDecimal(context.value());
        } catch (NumberFormatException ex) {
            return null;
        }

        BigDecimal high = RiskScoreSupport.parseDecimalRule(context.highRule(), BigDecimal.valueOf(100));
        BigDecimal medium = RiskScoreSupport.parseDecimalRule(context.mediumRule(), BigDecimal.valueOf(90));

        List<HealthData> recentData = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
            .eq(HealthData::getUserId, context.userId())
            .eq(HealthData::getIndicatorType, IndicatorTypes.WEIGHT)
            .ge(HealthData::getReportTime, LocalDateTime.now().minusDays(14))
            .orderByDesc(HealthData::getReportTime)
            .last("limit 20"));
        BigDecimal predicted = linearPredictNextDecimal(recentData);

        if (value.compareTo(high) >= 0) {
            int score = RiskScoreSupport.severeScore(RiskScoreSupport.ratio(value, high));
            String reasonCode = "WT_HIGH_RULE";
            String reasonText = "体重超过高风险阈值";
            if (predicted != null && predicted.compareTo(high) >= 0) {
                score = Math.min(100, score + 4);
                reasonCode = "WT_TREND_UP";
                reasonText = "体重趋势预测将持续超阈值";
            }
            return new AlertDecision("HIGH", score, RiskScoreSupport.riskLevel(score), reasonCode, reasonText);
        }

        if (value.compareTo(medium) >= 0) {
            int score = RiskScoreSupport.mediumScore(RiskScoreSupport.ratio(value, medium));
            String reasonCode = "WT_MEDIUM_RULE";
            String reasonText = "体重达到中风险阈值";
            if (predicted != null && predicted.compareTo(high) >= 0) {
                score = Math.min(79, score + 4);
                reasonCode = "WT_TREND_UP";
                reasonText = "体重趋势预测接近高风险阈值";
            }
            return new AlertDecision("MEDIUM", score, RiskScoreSupport.riskLevel(score), reasonCode, reasonText);
        }
        return null;
    }

    private BigDecimal linearPredictNextDecimal(List<HealthData> records) {
        if (records == null || records.size() < 2) {
            return null;
        }
        List<HealthData> sorted = records.stream()
            .filter(item -> item.getReportTime() != null && item.getValue() != null)
            .sorted((a, b) -> a.getReportTime().compareTo(b.getReportTime()))
                .toList();
        if (sorted.size() < 2) {
            return null;
        }
        double n = sorted.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;
        for (int i = 0; i < sorted.size(); i++) {
            double y;
            try {
                y = Double.parseDouble(sorted.get(i).getValue());
            } catch (NumberFormatException ex) {
                return null;
            }
            double x = i + 1;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double denominator = n * sumXX - sumX * sumX;
        if (denominator == 0) {
            return null;
        }
        double k = (n * sumXY - sumX * sumY) / denominator;
        double b = (sumY - k * sumX) / n;
        return BigDecimal.valueOf(k * (n + 1) + b);
    }
}
