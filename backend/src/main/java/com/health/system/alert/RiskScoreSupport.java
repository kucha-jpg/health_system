package com.health.system.alert;

import com.health.system.entity.HealthData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

    // NOTE: linearPredictDecimal and linearPredictInt share the same linear regression logic.
    // If updating the algorithm, keep both methods in sync.
    public static BigDecimal linearPredictDecimal(List<HealthData> records) {
        if (records == null || records.size() < 2) {
            return null;
        }
        List<HealthData> sorted = records.stream()
                .filter(item -> item.getReportTime() != null && item.getValue() != null)
                .sorted((a, b) -> a.getReportTime().compareTo(b.getReportTime()))
                .toList();
        if (sorted.size() < 2) {
            return null;
        }
        double n = sorted.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < sorted.size(); i++) {
            double y;
            try {
                y = Double.parseDouble(sorted.get(i).getValue());
            } catch (NumberFormatException ex) {
                return null;
            }
            double x = i + 1;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double denominator = n * sumXX - sumX * sumX;
        if (denominator == 0) {
            return null;
        }
        double k = (n * sumXY - sumX * sumY) / denominator;
        double b = (sumY - k * sumX) / n;
        return BigDecimal.valueOf(k * (n + 1) + b);
    }

    public static int linearPredictInt(List<HealthData> records, int defaultResult) {
        if (records == null || records.size() < 2) {
            return defaultResult;
        }
        List<HealthData> sorted = records.stream()
                .filter(item -> item.getReportTime() != null && item.getValue() != null)
                .sorted((a, b) -> a.getReportTime().compareTo(b.getReportTime()))
                .toList();
        if (sorted.size() < 2) {
            return defaultResult;
        }
        double n = sorted.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < sorted.size(); i++) {
            String[] arr = sorted.get(i).getValue().split("/");
            if (arr.length < 1) {
                return defaultResult;
            }
            int y;
            try {
                y = Integer.parseInt(arr[0]);
            } catch (NumberFormatException ex) {
                return defaultResult;
            }
            double x = i + 1;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double denominator = n * sumXX - sumX * sumX;
        if (denominator == 0) {
            return defaultResult;
        }
        double k = (n * sumXY - sumX * sumY) / denominator;
        double b = (sumY - k * sumX) / n;
        return (int) Math.round(k * (n + 1) + b);
    }
}
