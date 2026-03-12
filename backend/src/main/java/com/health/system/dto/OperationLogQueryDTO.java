package com.health.system.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OperationLogQueryDTO {
    private String keyword;
    private String roleType;
    private Integer success;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer pageNo;
    private Integer pageSize;
}
