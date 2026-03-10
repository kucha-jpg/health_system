package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doctor_group")
public class DoctorGroup extends BaseEntity {
    private Long doctorId;
    private String groupName;
    private String description;
}
