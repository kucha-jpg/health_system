package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    private String username;
    @JsonIgnore
    private String password;
    private String name;
    private String phone;
    private String roleType;
    private Integer status;
    private Long loginVersion;
}
