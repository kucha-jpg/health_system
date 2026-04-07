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
class BloodPressureAlertEvaluatorTest {

    @Mock
    private HealthDataMapper healthDataMapper;

    @Test
    void evaluate_shouldReturnHigh_whenOverHighThreshold() {
        BloodPressureAlertEvaluator evaluator = new BloodPressureAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of());

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "血压", "160/100", "140/90", "130/85"));

        assertNotNull(decision);
        assertEquals("HIGH", decision.level());
        assertEquals("BP_HIGH_RULE", decision.reasonCode());
    }

    @Test
    void evaluate_shouldReturnPersistentHigh_whenThreeDaysHigh() {
        BloodPressureAlertEvaluator evaluator = new BloodPressureAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of(
                bp("150/95", 0),
                bp("152/96", 1),
                bp("151/94", 2)
        ));

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "血压", "150/95", "140/90", "130/85"));

        assertNotNull(decision);
        assertEquals("HIGH", decision.level());
        assertEquals("BP_PERSISTENT_HIGH", decision.reasonCode());
    }

    @Test
    void evaluate_shouldReturnMedium_whenOverMediumThresholdOnly() {
        BloodPressureAlertEvaluator evaluator = new BloodPressureAlertEvaluator(healthDataMapper);
        when(healthDataMapper.selectList(any())).thenReturn(List.of());

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "血压", "132/86", "150/95", "130/85"));

        assertNotNull(decision);
        assertEquals("MEDIUM", decision.level());
        assertEquals("BP_MEDIUM_RULE", decision.reasonCode());
    }

    @Test
    void evaluate_shouldReturnNull_whenNormal() {
        BloodPressureAlertEvaluator evaluator = new BloodPressureAlertEvaluator(healthDataMapper);

        AlertDecision decision = evaluator.evaluate(new AlertEvaluationContext(1L, "血压", "120/80", "150/95", "130/85"));

        assertNull(decision);
    }

    private HealthData bp(String value, int daysAgo) {
        HealthData data = new HealthData();
        data.setValue(value);
        data.setReportTime(LocalDateTime.now().minusDays(daysAgo));
        return data;
    }
}
