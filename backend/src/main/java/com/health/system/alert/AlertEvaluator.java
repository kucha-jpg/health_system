package com.health.system.alert;

public interface AlertEvaluator {

    boolean supports(String indicatorType);

    AlertDecision evaluate(AlertEvaluationContext context);
}
