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

    private static final int RECENT_DAYS = 14;
    private static final int RECENT_LIMIT = 20;

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
                .ge(HealthData::getReportTime, LocalDateTime.now().minusDays(RECENT_DAYS))
                .orderByDesc(HealthData::getReportTime)
                .last("limit " + RECENT_LIMIT));
        BigDecimal predicted = RiskScoreSupport.linearPredictDecimal(recentData);

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
}
