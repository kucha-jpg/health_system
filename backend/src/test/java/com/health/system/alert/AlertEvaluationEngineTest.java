package com.health.system.alert;

import com.health.system.entity.AlertRule;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.mapper.PatientAlertPreferenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEvaluationEngineTest {

    @Mock
    private AlertRuleMapper alertRuleMapper;
    @Mock
    private PatientAlertPreferenceMapper patientAlertPreferenceMapper;

    @Test
    void constructor_shouldThrow_whenMultipleEvaluatorsSupportSameKnownIndicator() {
        AlertEvaluator e1 = new TestEvaluator(IndicatorTypes.BLOOD_PRESSURE, new AlertDecision("MEDIUM", 60, "MEDIUM", "A", "A"));
        AlertEvaluator e2 = new TestEvaluator(IndicatorTypes.BLOOD_PRESSURE, new AlertDecision("HIGH", 80, "HIGH", "B", "B"));

        assertThrows(IllegalStateException.class,
                () -> new AlertEvaluationEngine(alertRuleMapper, patientAlertPreferenceMapper, List.of(e1, e2)));
    }

    @Test
    void evaluate_shouldThrow_whenRuntimeConflictDetected() {
        AlertEvaluator e1 = new TestEvaluator("自定义", new AlertDecision("MEDIUM", 60, "MEDIUM", "A", "A"));
        AlertEvaluator e2 = new TestEvaluator("自定义", new AlertDecision("HIGH", 80, "HIGH", "B", "B"));
        AlertEvaluationEngine engine = new AlertEvaluationEngine(alertRuleMapper, patientAlertPreferenceMapper, List.of(e1, e2));

        AlertRule rule = new AlertRule();
        rule.setIndicatorType("自定义");
        rule.setEnabled(1);
        rule.setHighRule("100");
        rule.setMediumRule("80");
        when(alertRuleMapper.selectOne(any())).thenReturn(rule);

        assertThrows(IllegalStateException.class,
                () -> engine.evaluate(1L, "自定义", "90"));
    }

    @Test
    void evaluate_shouldUseSingleEvaluator_whenNoConflict() {
        AlertDecision expected = new AlertDecision("MEDIUM", 65, "MEDIUM", "OK", "OK");
        AlertEvaluator evaluator = new TestEvaluator("自定义", expected);
        AlertEvaluationEngine engine = new AlertEvaluationEngine(alertRuleMapper, patientAlertPreferenceMapper, List.of(evaluator));

        AlertRule rule = new AlertRule();
        rule.setIndicatorType("自定义");
        rule.setEnabled(1);
        rule.setHighRule("100");
        rule.setMediumRule("80");
        when(alertRuleMapper.selectOne(any())).thenReturn(rule);

        AlertDecision actual = engine.evaluate(1L, "自定义", "90");

        assertEquals(expected, actual);
    }

    private static class TestEvaluator implements AlertEvaluator {

        private final String indicatorType;
        private final AlertDecision decision;

        private TestEvaluator(String indicatorType, AlertDecision decision) {
            this.indicatorType = indicatorType;
            this.decision = decision;
        }

        @Override
        public boolean supports(String indicatorType) {
            return this.indicatorType.equals(indicatorType);
        }

        @Override
        public AlertDecision evaluate(AlertEvaluationContext context) {
            return decision;
        }
    }
}
