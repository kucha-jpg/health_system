package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.dto.UserDTO;
import com.health.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String roleType,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(userService.listUsers(keyword, roleType, status, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody UserDTO dto) {
        userService.createUser(dto);
        return ApiResponse.success("创建成功", null);
    }

    @PutMapping
    public ApiResponse<Void> update(@Valid @RequestBody UserDTO dto) {
        userService.updateUser(dto);
        return ApiResponse.success("更新成功", null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return ApiResponse.success("状态更新成功", null);
    }
}
