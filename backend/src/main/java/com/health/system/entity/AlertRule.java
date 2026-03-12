package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_rule")
public class AlertRule extends BaseEntity {
    private String indicatorType;
    private String highRule;
    private String mediumRule;
    private Integer enabled;
}
