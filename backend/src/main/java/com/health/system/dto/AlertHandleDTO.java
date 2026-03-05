package com.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AlertHandleDTO {
    @NotBlank(message = "处理备注不能为空")
    private String handleRemark;
}
