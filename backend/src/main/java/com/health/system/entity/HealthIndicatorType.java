package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_indicator_type")
public class HealthIndicatorType extends BaseEntity {
    private String indicatorType;
    private String displayName;
    private Integer enabled;
}
