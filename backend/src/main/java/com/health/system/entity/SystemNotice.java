package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_notice")
public class SystemNotice extends BaseEntity {
    private String title;
    private String content;
    private String targetRole;
    private Integer status;
}
