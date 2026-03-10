package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.dto.AlertHandleDTO;
import com.health.system.dto.DoctorGroupAddPatientDTO;
import com.health.system.dto.DoctorGroupDTO;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.User;
import com.health.system.service.DoctorGroupService;
import com.health.system.service.HealthAlertService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final HealthAlertService healthAlertService;
    private final DoctorGroupService doctorGroupService;

    public DoctorController(HealthAlertService healthAlertService, DoctorGroupService doctorGroupService) {
        this.healthAlertService = healthAlertService;
        this.doctorGroupService = doctorGroupService;
    }

    @GetMapping("/alerts")
    public ApiResponse<List<HealthAlert>> openAlerts() {
        return ApiResponse.success(healthAlertService.listOpenAlerts());
    }

    @PostMapping("/alerts/{id}/handle")
    public ApiResponse<Void> handleAlert(Authentication authentication, @PathVariable Long id, @Valid @RequestBody AlertHandleDTO dto) {
        healthAlertService.handleAlert(authentication.getName(), id, dto.getHandleRemark());
        return ApiResponse.success("处理成功", null);
    }

    @GetMapping("/groups")
    public ApiResponse<List<DoctorGroup>> myGroups(Authentication authentication) {
        return ApiResponse.success(doctorGroupService.listMyGroups(authentication.getName()));
    }

    @PostMapping("/groups")
    public ApiResponse<Void> createGroup(Authentication authentication, @Valid @RequestBody DoctorGroupDTO dto) {
        doctorGroupService.createGroup(authentication.getName(), dto);
        return ApiResponse.success("创建成功", null);
    }

    @PostMapping("/groups/{id}/patients")
    public ApiResponse<Void> addPatient(Authentication authentication,
                                        @PathVariable Long id,
                                        @Valid @RequestBody DoctorGroupAddPatientDTO dto) {
        doctorGroupService.addPatientToGroup(authentication.getName(), id, dto.getPatientUserId());
        return ApiResponse.success("添加成功", null);
    }

    @GetMapping("/groups/{id}/patients")
    public ApiResponse<List<User>> listGroupPatients(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(doctorGroupService.listGroupPatients(authentication.getName(), id));
    }
}
