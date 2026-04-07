package com.health.system.alert;

public record AlertDecision(String level, int riskScore, String riskLevel, String reasonCode, String reasonText) {
}
