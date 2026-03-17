package com.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HealthIndicatorTypeDTO {
    private Long id;

    @NotBlank(message = "指标类型不能为空")
    private String indicatorType;

    @NotBlank(message = "展示名称不能为空")
    private String displayName;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;
}
