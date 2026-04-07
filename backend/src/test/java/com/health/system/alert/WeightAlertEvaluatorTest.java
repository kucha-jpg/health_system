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
class WeightAlertEvaluatorTest {

    @Mock
    private HealthDataMapper healthDataMapper;

    @Test
    void evaluate_shouldReturnHigh_whenOverHighThreshold() {
        WeightAlertEvaluator evaluator = new WeightAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of());

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "体重", "105", "100", "90"));

        assertNotNull(decision);
        assertEquals("HIGH", decision.level());
        assertEquals("WT_HIGH_RULE", decision.reasonCode());
    }

    @Test
    void evaluate_shouldReturnTrendUp_whenPredictedOverHigh() {
        WeightAlertEvaluator evaluator = new WeightAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of(
                weight("88", 3),
                weight("94", 2),
                weight("101", 1)
        ));

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "体重", "92", "100", "90"));

        assertNotNull(decision);
        assertEquals("MEDIUM", decision.level());
        assertEquals("WT_TREND_UP", decision.reasonCode());
    }

    @Test
    void evaluate_shouldReturnNull_whenNormal() {
        WeightAlertEvaluator evaluator = new WeightAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of());

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "体重", "75", "100", "90"));

        assertNull(decision);
    }

    private HealthData weight(String value, int daysAgo) {
        HealthData data = new HealthData();
        data.setValue(value);
        data.setReportTime(LocalDateTime.now().minusDays(daysAgo));
        return data;
    }
}
