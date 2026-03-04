package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("patient_archive")
public class PatientArchive extends BaseEntity {
    private Long userId;
    private String name;
    private Integer age;
    private String medicalHistory;
    private String medicationHistory;
    private String allergyHistory;
}
