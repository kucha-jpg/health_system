package com.health.system.alert;

public record AlertEvaluationContext(Long userId,
                                     String indicatorType,
                                     String value,
                                     String highRule,
                                     String mediumRule) {
}
