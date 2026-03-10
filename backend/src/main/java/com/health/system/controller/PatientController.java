package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.dto.HealthDataDTO;
import com.health.system.dto.PatientArchiveDTO;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.PatientArchive;
import com.health.system.service.HealthAlertService;
import com.health.system.service.HealthDataService;
import com.health.system.service.PatientReportService;
import com.health.system.service.PatientArchiveService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final PatientArchiveService patientArchiveService;
    private final HealthDataService healthDataService;
    private final HealthAlertService healthAlertService;
    private final PatientReportService patientReportService;

    public PatientController(PatientArchiveService patientArchiveService,
                             HealthDataService healthDataService,
                             HealthAlertService healthAlertService,
                             PatientReportService patientReportService) {
        this.patientArchiveService = patientArchiveService;
        this.healthDataService = healthDataService;
        this.healthAlertService = healthAlertService;
        this.patientReportService = patientReportService;
    }

    @GetMapping("/home")
    public ApiResponse<Map<String, String>> home() {
        return ApiResponse.success(Map.of("message", "患者首页"));
    }

    @GetMapping("/archive")
    public ApiResponse<PatientArchive> getArchive(Authentication authentication) {
        return ApiResponse.success(patientArchiveService.getMyArchive(authentication.getName()));
    }

    @PostMapping("/archive")
    public ApiResponse<Void> saveArchive(@Valid @RequestBody PatientArchiveDTO dto, Authentication authentication) {
        patientArchiveService.saveMyArchive(authentication.getName(), dto);
        return ApiResponse.success("保存成功", null);
    }

    @PutMapping("/archive")
    public ApiResponse<Void> updateArchive(@Valid @RequestBody PatientArchiveDTO dto, Authentication authentication) {
        patientArchiveService.saveMyArchive(authentication.getName(), dto);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/archive/{id}")
    public ApiResponse<Void> deleteArchive(@PathVariable Long id, Authentication authentication) {
        patientArchiveService.deleteMyArchive(authentication.getName(), id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/data")
    public ApiResponse<Void> reportData(@Valid @RequestBody HealthDataDTO dto, Authentication authentication) {
        healthDataService.create(authentication.getName(), dto);
        return ApiResponse.success("上报成功", null);
    }

    @GetMapping("/data")
    public ApiResponse<List<HealthData>> listData(@RequestParam(required = false, name = "indicator_type") String indicatorType,
                                                  @RequestParam(required = false) String timeRange,
                                                  Authentication authentication) {
        return ApiResponse.success(healthDataService.list(authentication.getName(), indicatorType, timeRange));
    }

    @PutMapping("/data/{id}")
    public ApiResponse<Void> updateData(@PathVariable Long id,
                                        @Valid @RequestBody HealthDataDTO dto,
                                        Authentication authentication) {
        healthDataService.update(authentication.getName(), id, dto);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/data/{id}")
    public ApiResponse<Void> deleteData(@PathVariable Long id, Authentication authentication) {
        healthDataService.delete(authentication.getName(), id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/alerts")
    public ApiResponse<List<HealthAlert>> listMyAlerts(@RequestParam(required = false) String status,
                                                        Authentication authentication) {
        return ApiResponse.success(healthAlertService.listMyAlerts(authentication.getName(), status));
    }

    @GetMapping("/reports/summary")
    public ApiResponse<Map<String, Object>> reportSummary(@RequestParam(defaultValue = "week") String range,
                                                          Authentication authentication) {
        return ApiResponse.success(patientReportService.summary(authentication.getName(), range));
    }
}
