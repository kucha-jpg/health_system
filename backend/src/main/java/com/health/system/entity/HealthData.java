package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_data")
public class HealthData extends BaseEntity {
    private Long userId;
    private String indicatorType;
    private String value;
    private LocalDateTime reportTime;
    private String remark;
}
