package com.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthDataDTO {
    private Long id;

    @NotBlank(message = "指标类型不能为空")
    private String indicatorType;

    @NotBlank(message = "数值不能为空")
    private String value;

    private LocalDateTime reportTime;
    private String remark;
}
