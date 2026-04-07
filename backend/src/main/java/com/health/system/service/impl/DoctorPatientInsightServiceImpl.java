package com.health.system.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.CacheNames;
import com.health.system.common.SensitiveDataCipher;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupDoctorMember;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.PatientArchive;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupDoctorMemberMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.PatientArchiveMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.DoctorPatientInsightService;
import com.health.system.service.support.DoctorAccessSupport;

@Service
public class DoctorPatientInsightServiceImpl implements DoctorPatientInsightService {

    private final UserMapper userMapper;
    private final DoctorGroupMapper doctorGroupMapper;
    private final DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper;
    private final DoctorGroupMemberMapper doctorGroupMemberMapper;
    private final PatientArchiveMapper patientArchiveMapper;
    private final HealthDataMapper healthDataMapper;
    private final HealthAlertMapper healthAlertMapper;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final DoctorAccessSupport doctorAccessSupport;

    public DoctorPatientInsightServiceImpl(UserMapper userMapper,
                                           DoctorGroupMapper doctorGroupMapper,
                                           DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper,
                                           DoctorGroupMemberMapper doctorGroupMemberMapper,
                                           PatientArchiveMapper patientArchiveMapper,
                                           HealthDataMapper healthDataMapper,
                                           HealthAlertMapper healthAlertMapper,
                                           SensitiveDataCipher sensitiveDataCipher,
                                           DoctorAccessSupport doctorAccessSupport) {
        this.userMapper = userMapper;
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupDoctorMemberMapper = doctorGroupDoctorMemberMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
        this.patientArchiveMapper = patientArchiveMapper;
        this.healthDataMapper = healthDataMapper;
        this.healthAlertMapper = healthAlertMapper;
        this.sensitiveDataCipher = sensitiveDataCipher;
        this.doctorAccessSupport = doctorAccessSupport;
    }

    @Override
        @Cacheable(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT,
            key = "#doctorUsername + '::' + #patientUserId + '::' + (#indicatorType == null ? '' : #indicatorType) + '::' + (#timeRange == null ? 'month' : #timeRange.toLowerCase())")
    public Map<String, Object> patientInsight(String doctorUsername, Long patientUserId, String indicatorType, String timeRange) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);

        User patient = userMapper.selectById(patientUserId);
        if (patient == null || !"PATIENT".equals(patient.getRoleType())) {
            throw BusinessException.notFound("患者不存在");
        }
        doctorAccessSupport.assertPatientAccessible(doctor.getId(), patientUserId, "该患者不在您的群组中");

        PatientArchive archive = patientArchiveMapper.selectOne(new LambdaQueryWrapper<PatientArchive>()
                .eq(PatientArchive::getUserId, patientUserId));
        decryptArchive(archive);

        LambdaQueryWrapper<HealthData> dataWrapper = new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, patientUserId)
                .orderByAsc(HealthData::getReportTime);
        if (StringUtils.hasText(indicatorType)) {
            dataWrapper.eq(HealthData::getIndicatorType, indicatorType);
        }
        LocalDateTime start = resolveStart(timeRange);
        if (start != null) {
            dataWrapper.ge(HealthData::getReportTime, start);
        }
        List<HealthData> trendData = healthDataMapper.selectList(dataWrapper);

        List<HealthAlert> recentAlerts = healthAlertMapper.selectList(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, patientUserId)
                .orderByDesc(HealthAlert::getCreateTime)
                .last("limit 20"));

        long openAlerts = healthAlertMapper.selectCount(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, patientUserId)
                .eq(HealthAlert::getStatus, "OPEN"));

        Map<String, Object> patientInfo = new HashMap<>();
        patientInfo.put("id", patient.getId());
        patientInfo.put("username", patient.getUsername());
        patientInfo.put("name", patient.getName());
        patientInfo.put("phone", patient.getPhone());
        patientInfo.put("status", patient.getStatus());

        Map<String, Object> result = new HashMap<>();
        result.put("patient", patientInfo);
        result.put("archive", archive);
        result.put("trendData", trendData);
        result.put("recentAlerts", recentAlerts);
        result.put("openAlertCount", openAlerts);
        result.put("timeRange", normalizeTimeRange(timeRange));
        result.put("indicatorType", indicatorType);
        return result;
    }

    private void decryptArchive(PatientArchive archive) {
        if (archive == null) {
            return;
        }
        archive.setMedicalHistory(sensitiveDataCipher.decrypt(archive.getMedicalHistory()));
        archive.setMedicationHistory(sensitiveDataCipher.decrypt(archive.getMedicationHistory()));
        archive.setAllergyHistory(sensitiveDataCipher.decrypt(archive.getAllergyHistory()));
    }

    private LocalDateTime resolveStart(String timeRange) {
        String normalized = normalizeTimeRange(timeRange);
        return switch (normalized) {
            case "day" -> LocalDateTime.now().minusDays(1);
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            default -> null;
        };
    }

    private String normalizeTimeRange(String timeRange) {
        if (!StringUtils.hasText(timeRange)) {
            return "month";
        }
        String value = timeRange.toLowerCase();
        if ("day".equals(value) || "week".equals(value) || "month".equals(value)) {
            return value;
        }
        return "month";
    }
}
