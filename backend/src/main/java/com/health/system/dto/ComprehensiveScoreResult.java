package com.health.system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ComprehensiveScoreResult {
    private int totalScore;
    private String riskLevel;
    private List<IndicatorScoreDetail> details;
    private String summary;
    private LocalDateTime calculatedAt;

    @Data
    public static class IndicatorScoreDetail {
        private String indicatorType;
        private String latestValue;
        private Integer riskScore;
        private BigDecimal weight;
        private BigDecimal weightedScore;
        private boolean hasData;
    }
}
