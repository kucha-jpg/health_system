package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doctor_group_member")
public class DoctorGroupMember extends BaseEntity {
    private Long groupId;
    private Long patientUserId;
}
