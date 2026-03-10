package com.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RolePermissionDTO {
    @NotNull(message = "角色ID不能为空")
    private Long id;

    @NotBlank(message = "权限不能为空")
    private String permission;
}
