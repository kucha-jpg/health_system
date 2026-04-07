package com.health.system.alert;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RiskScoreSupport {

    private RiskScoreSupport() {
    }

    public static int mediumScore(BigDecimal... ratios) {
        BigDecimal maxRatio = maxRatio(ratios);
        BigDecimal extra = maxRatio.subtract(BigDecimal.ONE).max(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(120));
        int score = BigDecimal.valueOf(50).add(extra).setScale(0, RoundingMode.HALF_UP).intValue();
        return clamp(score, 50, 79);
    }

    public static int severeScore(BigDecimal... ratios) {
        BigDecimal maxRatio = maxRatio(ratios);
        BigDecimal extra = maxRatio.subtract(BigDecimal.ONE).max(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(180));
        int score = BigDecimal.valueOf(80).add(extra).setScale(0, RoundingMode.HALF_UP).intValue();
        return clamp(score, 80, 100);
    }

    public static BigDecimal ratio(int value, int threshold) {
        if (threshold <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(threshold), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal ratio(BigDecimal value, BigDecimal threshold) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return value.divide(threshold, 4, RoundingMode.HALF_UP);
    }

    public static int clamp(int score, int min, int max) {
        return Math.max(min, Math.min(max, score));
    }

    public static String riskLevel(int score) {
        if (score >= 80) {
            return "HIGH";
        }
        if (score >= 50) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public static int[] parsePressure(String rule, int defaultSystolic, int defaultDiastolic) {
        if (rule == null || rule.isBlank()) {
            return new int[]{defaultSystolic, defaultDiastolic};
        }
        String[] arr = rule.split("/");
        if (arr.length != 2) {
            return new int[]{defaultSystolic, defaultDiastolic};
        }
        try {
            return new int[]{Integer.parseInt(arr[0]), Integer.parseInt(arr[1])};
        } catch (NumberFormatException ex) {
            return new int[]{defaultSystolic, defaultDiastolic};
        }
    }

    public static BigDecimal parseDecimalRule(String rule, BigDecimal defaultValue) {
        if (rule == null || rule.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(rule);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static BigDecimal maxRatio(BigDecimal... ratios) {
        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal ratio : ratios) {
            if (ratio != null && ratio.compareTo(max) > 0) {
                max = ratio;
            }
        }
        return max;
    }
}
