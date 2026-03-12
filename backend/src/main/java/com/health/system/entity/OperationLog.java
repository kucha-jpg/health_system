package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLog extends BaseEntity {
    private String username;
    private String roleType;
    private String requestMethod;
    private String requestUri;
    private Integer success;
    private String message;
}
