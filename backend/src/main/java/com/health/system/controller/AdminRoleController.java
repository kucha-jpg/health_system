package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.dto.RolePermissionDTO;
import com.health.system.entity.Role;
import com.health.system.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

    private final RoleService roleService;

    public AdminRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<Role>> list() {
        return ApiResponse.success(roleService.listRoles());
    }

    @PutMapping
    public ApiResponse<Void> updatePermission(@Valid @RequestBody RolePermissionDTO dto) {
        roleService.updatePermission(dto.getId(), dto.getPermission());
        return ApiResponse.success("更新成功", null);
    }
}
