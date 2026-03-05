package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_alert")
public class HealthAlert extends BaseEntity {
    private Long userId;
    private Long healthDataId;
    private String indicatorType;
    private String value;
    private String level;
    private String reason;
    private String status;
    private Long handledBy;
    private String handleRemark;
    private LocalDateTime handledTime;
}
