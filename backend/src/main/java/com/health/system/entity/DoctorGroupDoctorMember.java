package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doctor_group_doctor_member")
public class DoctorGroupDoctorMember extends BaseEntity {
    private Long groupId;
    private Long doctorUserId;
}
