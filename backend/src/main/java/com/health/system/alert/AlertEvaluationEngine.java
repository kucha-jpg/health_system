package com.health.system.alert;

import com.health.system.entity.AlertRule;
import com.health.system.entity.PatientAlertPreference;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.mapper.PatientAlertPreferenceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertEvaluationEngine {

    private final AlertRuleMapper alertRuleMapper;
    private final PatientAlertPreferenceMapper patientAlertPreferenceMapper;
    private final List<AlertEvaluator> evaluators;

    public AlertEvaluationEngine(AlertRuleMapper alertRuleMapper,
                                 PatientAlertPreferenceMapper patientAlertPreferenceMapper,
                                 List<AlertEvaluator> evaluators) {
        this.alertRuleMapper = alertRuleMapper;
        this.patientAlertPreferenceMapper = patientAlertPreferenceMapper;
        this.evaluators = evaluators;
        validateKnownIndicatorConflicts();
    }

    public AlertDecision evaluate(Long userId, String indicatorType, String value) {
        AlertRule alertRule = alertRuleMapper.selectOne(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getIndicatorType, indicatorType)
                .eq(AlertRule::getEnabled, 1)
                .last("limit 1"));
        if (alertRule == null) {
            return null;
        }

        PatientAlertPreference preference = patientAlertPreferenceMapper.selectOne(new LambdaQueryWrapper<PatientAlertPreference>()
                .eq(PatientAlertPreference::getUserId, userId)
                .eq(PatientAlertPreference::getIndicatorType, indicatorType)
                .eq(PatientAlertPreference::getEnabled, 1)
                .last("limit 1"));

        String highRule = preference != null && preference.getHighRule() != null && !preference.getHighRule().isBlank()
                ? preference.getHighRule()
                : alertRule.getHighRule();
        String mediumRule = preference != null && preference.getMediumRule() != null && !preference.getMediumRule().isBlank()
                ? preference.getMediumRule()
                : alertRule.getMediumRule();

        AlertEvaluationContext context = new AlertEvaluationContext(userId, indicatorType, value, highRule, mediumRule);
        List<AlertEvaluator> matched = new ArrayList<>();
        for (AlertEvaluator evaluator : evaluators) {
            if (evaluator.supports(indicatorType)) {
                matched.add(evaluator);
            }
        }
        if (matched.size() > 1) {
            throw new IllegalStateException("AlertEvaluator conflict for indicator: " + indicatorType);
        }
        if (matched.size() == 1) {
            return matched.get(0).evaluate(context);
        }
        return null;
    }

    private void validateKnownIndicatorConflicts() {
        validateSingleEvaluator(IndicatorTypes.BLOOD_PRESSURE);
        validateSingleEvaluator(IndicatorTypes.BLOOD_SUGAR);
        validateSingleEvaluator(IndicatorTypes.WEIGHT);
    }

    private void validateSingleEvaluator(String indicatorType) {
        long count = evaluators.stream().filter(e -> e.supports(indicatorType)).count();
        if (count > 1) {
            throw new IllegalStateException("Multiple AlertEvaluator registered for indicator: " + indicatorType);
        }
    }
}
