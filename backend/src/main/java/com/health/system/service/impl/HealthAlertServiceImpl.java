package com.health.system.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.system.alert.AlertDecision;
import com.health.system.alert.AlertEvaluationEngine;
import com.health.system.common.BusinessException;
import com.health.system.common.CacheNames;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.HealthAlertService;
import com.health.system.service.support.DoctorAccessSupport;
import com.health.system.service.support.MonitorOverviewAssembler;

@Service
public class HealthAlertServiceImpl implements HealthAlertService {

    private final HealthAlertMapper healthAlertMapper;
    private final HealthDataMapper healthDataMapper;
    private final UserMapper userMapper;
    private final AlertEvaluationEngine alertEvaluationEngine;
    private final DoctorGroupMapper doctorGroupMapper;
    private final DoctorGroupMemberMapper doctorGroupMemberMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final DoctorAccessSupport doctorAccessSupport;
    private final MonitorOverviewAssembler monitorOverviewAssembler;

    public HealthAlertServiceImpl(HealthAlertMapper healthAlertMapper,
                                  HealthDataMapper healthDataMapper,
                                  UserMapper userMapper,
                                  AlertEvaluationEngine alertEvaluationEngine,
                                  DoctorGroupMapper doctorGroupMapper,
                                  DoctorGroupMemberMapper doctorGroupMemberMapper,
                                  AlertRuleMapper alertRuleMapper,
                                  DoctorAccessSupport doctorAccessSupport,
                                  MonitorOverviewAssembler monitorOverviewAssembler) {
        this.healthAlertMapper = healthAlertMapper;
        this.healthDataMapper = healthDataMapper;
        this.userMapper = userMapper;
        this.alertEvaluationEngine = alertEvaluationEngine;
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.doctorAccessSupport = doctorAccessSupport;
        this.monitorOverviewAssembler = monitorOverviewAssembler;
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PATIENT_REPORT_SUMMARY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_OPEN_ALERTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PATIENT_ALERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ADMIN_MONITOR_OVERVIEW, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true)
        })
    public void evaluateAndCreateAlert(Long userId, Long healthDataId, String indicatorType, String value) {
        AlertDecision decision = evaluate(userId, indicatorType, value);
        if (decision == null) {
            return;
        }
        HealthAlert existing = healthAlertMapper.selectOne(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, userId)
                .eq(HealthAlert::getIndicatorType, indicatorType)
                .eq(HealthAlert::getReasonCode, decision.reasonCode())
                .eq(HealthAlert::getStatus, "OPEN")
                .orderByDesc(HealthAlert::getCreateTime)
                .last("limit 1"));
        if (existing != null) {
            existing.setHealthDataId(healthDataId);
            existing.setValue(value);
            existing.setLevel(decision.level());
            existing.setRiskScore(decision.riskScore());
            existing.setRiskLevel(decision.riskLevel());
            existing.setReasonText(decision.reasonText());
            healthAlertMapper.updateById(existing);
            return;
        }
        HealthAlert alert = new HealthAlert();
        alert.setUserId(userId);
        alert.setHealthDataId(healthDataId);
        alert.setIndicatorType(indicatorType);
        alert.setValue(value);
        alert.setLevel(decision.level());
        alert.setRiskScore(decision.riskScore());
        alert.setRiskLevel(decision.riskLevel());
        alert.setReasonCode(decision.reasonCode());
        alert.setReasonText(decision.reasonText());
        alert.setStatus("OPEN");
        healthAlertMapper.insert(alert);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PATIENT_REPORT_SUMMARY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_OPEN_ALERTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PATIENT_ALERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ADMIN_MONITOR_OVERVIEW, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true)
    })
    public void deleteByHealthDataId(Long healthDataId) {
        healthAlertMapper.delete(new LambdaQueryWrapper<HealthAlert>().eq(HealthAlert::getHealthDataId, healthDataId));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.DOCTOR_OPEN_ALERTS,
            key = "#doctorUsername + '::' + (#riskLevel == null ? '' : #riskLevel.toUpperCase()) + '::' + (#minRiskScore == null ? -1 : #minRiskScore) + '::' + (#sortBy == null ? 'risk_desc' : #sortBy.toLowerCase()) + '::' + (#pageNo == null ? 1 : #pageNo) + '::' + (#pageSize == null ? 20 : #pageSize)")
    public Map<String, Object> listOpenAlerts(String doctorUsername,
                                              String riskLevel,
                                              Integer minRiskScore,
                                              String sortBy,
                                              Integer pageNo,
                                              Integer pageSize) {
                                            User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
                                            Set<Long> patientIds = doctorAccessSupport.resolveAccessiblePatientIds(doctor.getId());
        int safePageNo = Math.min(Math.max(pageNo == null ? 1 : pageNo, 1), 1000);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);
        if (patientIds.isEmpty()) {
            return pagedResult(List.of(), 0, safePageNo, safePageSize);
        }

        LambdaQueryWrapper<HealthAlert> wrapper = new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getStatus, "OPEN")
                .in(HealthAlert::getUserId, patientIds);

        if (StringUtils.hasText(riskLevel)) {
            wrapper.eq(HealthAlert::getRiskLevel, riskLevel.trim().toUpperCase());
        }

        if (minRiskScore != null) {
            int safeScore = Math.max(0, Math.min(minRiskScore, 100));
            wrapper.ge(HealthAlert::getRiskScore, safeScore);
        }

        String normalizedSort = StringUtils.hasText(sortBy) ? sortBy.trim().toLowerCase() : "risk_desc";
        if ("time_desc".equals(normalizedSort)) {
            wrapper.orderByDesc(HealthAlert::getCreateTime)
                    .orderByDesc(HealthAlert::getRiskScore);
        } else {
            wrapper.orderByDesc(HealthAlert::getRiskScore)
                    .orderByDesc(HealthAlert::getCreateTime);
        }

        Page<HealthAlert> page = healthAlertMapper.selectPage(new Page<>(safePageNo, safePageSize), wrapper);
        return pagedResult(page.getRecords(), page.getTotal(), safePageNo, safePageSize);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PATIENT_ALERT_LIST,
            key = "#username + '::' + (#status == null ? '' : #status.toUpperCase()) + '::' + (#pageNo == null ? 1 : #pageNo) + '::' + (#pageSize == null ? 20 : #pageSize)")
    public Map<String, Object> listMyAlerts(String username, String status, Integer pageNo, Integer pageSize) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        int safePageNo = Math.min(Math.max(pageNo == null ? 1 : pageNo, 1), 1000);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);

        LambdaQueryWrapper<HealthAlert> wrapper = new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, user.getId())
                .orderByDesc(HealthAlert::getCreateTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(HealthAlert::getStatus, status);
        }
        Page<HealthAlert> page = healthAlertMapper.selectPage(new Page<>(safePageNo, safePageSize), wrapper);
        return pagedResult(page.getRecords(), page.getTotal(), safePageNo, safePageSize);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PATIENT_REPORT_SUMMARY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_OPEN_ALERTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PATIENT_ALERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ADMIN_MONITOR_OVERVIEW, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true)
        })
    public void handleAlert(String doctorUsername, Long id, String handleRemark) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        HealthAlert alert = healthAlertMapper.selectById(id);
        if (alert == null) {
            throw BusinessException.notFound("预警不存在");
        }
        doctorAccessSupport.assertPatientAccessible(doctor.getId(), alert.getUserId(), "预警不在您的可处理范围内");
        if (!"OPEN".equals(alert.getStatus())) {
            throw BusinessException.conflict("预警已处理");
        }
        alert.setStatus("CLOSED");
        alert.setHandledBy(doctor.getId());
        alert.setHandledTime(LocalDateTime.now());
        alert.setHandleRemark(handleRemark);
        healthAlertMapper.updateById(alert);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ADMIN_MONITOR_OVERVIEW, key = "'overview'")
    public Map<String, Object> monitorOverview() {
        long totalUsers = userMapper.selectCount(null);
        long totalHealthData = healthDataMapper.selectCount(null);
        long openAlerts = healthAlertMapper.selectCount(new LambdaQueryWrapper<HealthAlert>().eq(HealthAlert::getStatus, "OPEN"));
        List<HealthData> latestHealthData = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .orderByDesc(HealthData::getReportTime)
                .last("limit 10"));

        LocalDateTime monthStart = LocalDateTime.now().minusDays(30);
        List<HealthData> recentMonthData = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .ge(HealthData::getReportTime, monthStart)
                .orderByDesc(HealthData::getReportTime)
                .last("limit 5000"));

        List<DoctorGroup> groups = doctorGroupMapper.selectList(new LambdaQueryWrapper<DoctorGroup>()
                .orderByDesc(DoctorGroup::getCreateTime)
                .last("limit 200"));
        List<DoctorGroupMember> groupMembers = groups.isEmpty()
                ? List.of()
                : doctorGroupMemberMapper.selectList(new LambdaQueryWrapper<DoctorGroupMember>()
                .in(DoctorGroupMember::getGroupId, groups.stream().map(DoctorGroup::getId).toList()));

        var enabledRuleIndicators = alertRuleMapper.selectList(new LambdaQueryWrapper<com.health.system.entity.AlertRule>()
            .eq(com.health.system.entity.AlertRule::getEnabled, 1)
            .eq(com.health.system.entity.AlertRule::getDeleted, 0))
            .stream()
            .map(com.health.system.entity.AlertRule::getIndicatorType)
            .filter(StringUtils::hasText)
            .collect(java.util.stream.Collectors.toSet());

        return monitorOverviewAssembler.assemble(
                totalUsers,
                totalHealthData,
                openAlerts,
                latestHealthData,
                recentMonthData,
            enabledRuleIndicators,
                groups,
                groupMembers
        );
    }

    private Map<String, Object> pagedResult(List<HealthAlert> records, long total, int pageNo, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", total);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return result;
    }

    private AlertDecision evaluate(Long userId, String indicatorType, String value) {
        return alertEvaluationEngine.evaluate(userId, indicatorType, value);
    }

}
