package com.health.system.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_alert")
public class HealthAlert extends BaseEntity {
    private Long userId;
    private Long healthDataId;
    private String indicatorType;
    private String value;
    private Integer riskScore;
    // Both `level` and `riskLevel` store the same value (see custom setters below).
    // This dual-field pattern ensures compatibility with both old code using `level`
    // and new code using `riskLevel` during migration.
    private String level;
    private String riskLevel;
    private String reasonCode;
    private String reasonText;
    private String status;
    private Long handledBy;
    private String handleRemark;
    private LocalDateTime handledTime;

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
        this.level = riskLevel;
    }

    public void setLevel(String level) {
        this.level = level;
        this.riskLevel = level;
    }
}
