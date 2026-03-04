package com.health.system.controller;

import com.health.system.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    @GetMapping("/home")
    public ApiResponse<Map<String, String>> home() {
        return ApiResponse.success(Map.of("message", "医生首页"));
    }
}
