package com.health.system.alert;

import com.health.system.entity.HealthData;
import com.health.system.mapper.HealthDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BloodSugarAlertEvaluator implements AlertEvaluator {

    private static final int RECENT_DAYS = 7;
    private static final int RECENT_LIMIT = 20;

    private final HealthDataMapper healthDataMapper;

    public BloodSugarAlertEvaluator(HealthDataMapper healthDataMapper) {
        this.healthDataMapper = healthDataMapper;
    }

    @Override
    public boolean supports(String indicatorType) {
        return IndicatorTypes.BLOOD_SUGAR.equals(indicatorType);
    }

    @Override
    public AlertDecision evaluate(AlertEvaluationContext context) {
        BigDecimal value;
        try {
            value = new BigDecimal(context.value());
        } catch (NumberFormatException ex) {
            return null;
        }

        BigDecimal high = RiskScoreSupport.parseDecimalRule(context.highRule(), BigDecimal.valueOf(11));
        BigDecimal medium = RiskScoreSupport.parseDecimalRule(context.mediumRule(), BigDecimal.valueOf(7.8));

        List<HealthData> recentData = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, context.userId())
                .eq(HealthData::getIndicatorType, IndicatorTypes.BLOOD_SUGAR)
                .ge(HealthData::getReportTime, LocalDateTime.now().minusDays(RECENT_DAYS))
                .orderByDesc(HealthData::getReportTime)
                .last("limit " + RECENT_LIMIT));
        BigDecimal predicted = RiskScoreSupport.linearPredictDecimal(recentData);

        if (value.compareTo(high) >= 0) {
            int score = RiskScoreSupport.severeScore(RiskScoreSupport.ratio(value, high));
            String reasonCode = "BS_HIGH_RULE";
            String reasonText = "血糖超过高风险阈值";
            if (predicted != null && predicted.compareTo(high) >= 0) {
                score = Math.min(100, score + 4);
                reasonCode = "BS_TREND_UP";
                reasonText = "血糖趋势预测将持续超阈值";
            }
            return new AlertDecision("HIGH", score, RiskScoreSupport.riskLevel(score), reasonCode, reasonText);
        }

        if (value.compareTo(medium) >= 0) {
            int score = RiskScoreSupport.mediumScore(RiskScoreSupport.ratio(value, medium));
            String reasonCode = "BS_MEDIUM_RULE";
            String reasonText = "血糖达到中风险阈值";
            if (predicted != null && predicted.compareTo(high) >= 0) {
                score = Math.min(79, score + 4);
                reasonCode = "BS_TREND_UP";
                reasonText = "血糖趋势预测接近高风险阈值";
            }
            return new AlertDecision("MEDIUM", score, RiskScoreSupport.riskLevel(score), reasonCode, reasonText);
        }
        return null;
    }
}
