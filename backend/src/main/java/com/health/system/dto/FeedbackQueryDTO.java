package com.health.system.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FeedbackQueryDTO {
    private String keyword;
    private String roleType;
    private Integer status;
    private Integer replyStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer pageNo;
    private Integer pageSize;
}
