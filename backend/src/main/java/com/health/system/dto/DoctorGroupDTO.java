package com.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DoctorGroupDTO {
    private Long id;

    @NotBlank(message = "群组名称不能为空")
    private String groupName;

    private String description;
}
