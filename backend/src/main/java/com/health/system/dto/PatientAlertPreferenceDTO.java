package com.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatientAlertPreferenceDTO {

    @NotBlank(message = "指标类型不能为空")
    private String indicatorType;

    private String highRule;

    private String mediumRule;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;
}
