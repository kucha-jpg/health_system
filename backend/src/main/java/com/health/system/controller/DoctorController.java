package com.health.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.health.system.common.ApiResponse;
import com.health.system.dto.DoctorGroupAddDoctorDTO;
import com.health.system.dto.AlertHandleDTO;
import com.health.system.dto.DoctorGroupAddPatientDTO;
import com.health.system.dto.DoctorGroupDTO;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.User;
import com.health.system.service.DoctorGroupService;
import com.health.system.service.DoctorPatientInsightService;
import com.health.system.service.HealthAlertService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final HealthAlertService healthAlertService;
    private final DoctorGroupService doctorGroupService;
    private final DoctorPatientInsightService doctorPatientInsightService;

    public DoctorController(HealthAlertService healthAlertService,
                            DoctorGroupService doctorGroupService,
                            DoctorPatientInsightService doctorPatientInsightService) {
        this.healthAlertService = healthAlertService;
        this.doctorGroupService = doctorGroupService;
        this.doctorPatientInsightService = doctorPatientInsightService;
    }

    @GetMapping("/alerts")
    public ApiResponse<Map<String, Object>> openAlerts(Authentication authentication,
                                                        @RequestParam(required = false) String riskLevel,
                                                        @RequestParam(required = false) Integer minRiskScore,
                                                        @RequestParam(defaultValue = "risk_desc") String sortBy,
                                                        @RequestParam(defaultValue = "1") Integer pageNo,
                                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(healthAlertService.listOpenAlerts(authentication.getName(), riskLevel, minRiskScore, sortBy, pageNo, pageSize));
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

    @PostMapping("/groups/{id}/doctors")
    public ApiResponse<Void> addDoctor(Authentication authentication,
                                       @PathVariable Long id,
                                       @Valid @RequestBody DoctorGroupAddDoctorDTO dto) {
        doctorGroupService.addDoctorToGroup(authentication.getName(), id, dto.getDoctorUserId());
        return ApiResponse.success("添加成功", null);
    }

    @GetMapping("/groups/{id}/patients")
    public ApiResponse<List<User>> listGroupPatients(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(doctorGroupService.listGroupPatients(authentication.getName(), id));
    }

    @GetMapping("/groups/{id}/doctors")
    public ApiResponse<List<User>> listGroupDoctors(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(doctorGroupService.listGroupDoctors(authentication.getName(), id));
    }

    @GetMapping("/patients/{patientUserId}/insight")
    public ApiResponse<Map<String, Object>> patientInsight(Authentication authentication,
                                                           @PathVariable Long patientUserId,
                                                           @RequestParam(required = false) String indicatorType,
                                                           @RequestParam(defaultValue = "month") String timeRange) {
        return ApiResponse.success(
                doctorPatientInsightService.patientInsight(authentication.getName(), patientUserId, indicatorType, timeRange)
        );
    }
}
