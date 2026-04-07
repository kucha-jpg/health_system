package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("patient_alert_preference")
public class PatientAlertPreference extends BaseEntity {
    private Long userId;
    private String indicatorType;
    private String highRule;
    private String mediumRule;
    private Integer enabled;
}
