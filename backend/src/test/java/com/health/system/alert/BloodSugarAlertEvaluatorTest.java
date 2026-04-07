package com.health.system.alert;

import com.health.system.entity.HealthData;
import com.health.system.mapper.HealthDataMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloodSugarAlertEvaluatorTest {

    @Mock
    private HealthDataMapper healthDataMapper;

    @Test
    void evaluate_shouldReturnHigh_whenOverHighThreshold() {
        BloodSugarAlertEvaluator evaluator = new BloodSugarAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of());

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "血糖", "12.0", "11", "7.8"));

        assertNotNull(decision);
        assertEquals("HIGH", decision.level());
        assertEquals("BS_HIGH_RULE", decision.reasonCode());
    }

    @Test
    void evaluate_shouldReturnTrendUp_whenPredictedOverHigh() {
        BloodSugarAlertEvaluator evaluator = new BloodSugarAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of(
                sugar("8.0", 3),
                sugar("10.0", 2),
                sugar("12.0", 1)
        ));

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "血糖", "8.5", "11", "7.8"));

        assertNotNull(decision);
        assertEquals("MEDIUM", decision.level());
        assertEquals("BS_TREND_UP", decision.reasonCode());
    }

    @Test
    void evaluate_shouldReturnNull_whenNormal() {
        BloodSugarAlertEvaluator evaluator = new BloodSugarAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of());

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "血糖", "6.2", "11", "7.8"));

        assertNull(decision);
    }

    private HealthData sugar(String value, int daysAgo) {
        HealthData data = new HealthData();
        data.setValue(value);
        data.setReportTime(LocalDateTime.now().minusDays(daysAgo));
        return data;
    }
}
