package com.health.system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.system.common.BusinessException;
import com.health.system.common.CacheNames;
import com.health.system.dto.HealthDataDTO;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.HealthAlertService;
import com.health.system.service.HealthDataService;
import com.health.system.service.HealthIndicatorTypeService;

@Service
public class HealthDataServiceImpl implements HealthDataService {

    private final HealthDataMapper healthDataMapper;
    private final UserMapper userMapper;
    private final HealthAlertService healthAlertService;
    private final HealthIndicatorTypeService healthIndicatorTypeService;

    public HealthDataServiceImpl(HealthDataMapper healthDataMapper,
                                 UserMapper userMapper,
                                 HealthAlertService healthAlertService,
                                 HealthIndicatorTypeService healthIndicatorTypeService) {
        this.healthDataMapper = healthDataMapper;
        this.userMapper = userMapper;
        this.healthAlertService = healthAlertService;
        this.healthIndicatorTypeService = healthIndicatorTypeService;
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PATIENT_REPORT_SUMMARY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PATIENT_HEALTH_DATA_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true)
        })
    public void create(String username, HealthDataDTO dto) {
        Long userId = getCurrentUserId(username);
        validateData(dto.getIndicatorType(), dto.getValue());

        HealthData data = new HealthData();
        data.setUserId(userId);
        data.setIndicatorType(dto.getIndicatorType());
        data.setValue(dto.getValue());
        data.setReportTime(dto.getReportTime() == null ? LocalDateTime.now() : dto.getReportTime());
        data.setRemark(dto.getRemark());
        healthDataMapper.insert(data);
        healthAlertService.evaluateAndCreateAlert(userId, data.getId(), data.getIndicatorType(), data.getValue());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PATIENT_HEALTH_DATA_LIST,
            key = "#username + '::' + (#indicatorType == null ? '' : #indicatorType) + '::' + (#timeRange == null ? '' : #timeRange.toLowerCase()) + '::' + (#pageNo == null ? 1 : #pageNo) + '::' + (#pageSize == null ? 20 : #pageSize)")
    public Map<String, Object> list(String username, String indicatorType, String timeRange, Integer pageNo, Integer pageSize) {
        Long userId = getCurrentUserId(username);
        int safePageNo = Math.min(Math.max(pageNo == null ? 1 : pageNo, 1), 1000);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);

        LambdaQueryWrapper<HealthData> wrapper = new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, userId)
                .orderByDesc(HealthData::getReportTime);

        if (StringUtils.hasText(indicatorType)) {
            wrapper.eq(HealthData::getIndicatorType, indicatorType);
        }
        if (StringUtils.hasText(timeRange)) {
            LocalDateTime start = null;
            if ("day".equalsIgnoreCase(timeRange)) {
                start = LocalDateTime.now().minusDays(1);
            } else if ("week".equalsIgnoreCase(timeRange)) {
                start = LocalDateTime.now().minusWeeks(1);
            } else if ("month".equalsIgnoreCase(timeRange)) {
                start = LocalDateTime.now().minusMonths(1);
            }
            if (start != null) {
                wrapper.ge(HealthData::getReportTime, start);
            }
        }

        Page<HealthData> page = healthDataMapper.selectPage(new Page<>(safePageNo, safePageSize), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNo", safePageNo);
        result.put("pageSize", safePageSize);
        return result;
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PATIENT_REPORT_SUMMARY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PATIENT_HEALTH_DATA_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true)
        })
    public void update(String username, Long id, HealthDataDTO dto) {
        Long userId = getCurrentUserId(username);
        HealthData old = healthDataMapper.selectById(id);
        if (old == null || !userId.equals(old.getUserId())) {
            throw BusinessException.notFound("数据不存在或无权限");
        }
        validateData(dto.getIndicatorType(), dto.getValue());
        old.setIndicatorType(dto.getIndicatorType());
        old.setValue(dto.getValue());
        old.setReportTime(dto.getReportTime() == null ? old.getReportTime() : dto.getReportTime());
        old.setRemark(dto.getRemark());
        healthDataMapper.updateById(old);
        healthAlertService.deleteByHealthDataId(old.getId());
        healthAlertService.evaluateAndCreateAlert(userId, old.getId(), old.getIndicatorType(), old.getValue());
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PATIENT_REPORT_SUMMARY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PATIENT_HEALTH_DATA_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true)
        })
    public void delete(String username, Long id) {
        Long userId = getCurrentUserId(username);
        HealthData old = healthDataMapper.selectById(id);
        if (old == null || !userId.equals(old.getUserId())) {
            throw BusinessException.notFound("数据不存在或无权限");
        }
        healthAlertService.deleteByHealthDataId(id);
        healthDataMapper.deleteById(id);
    }

    private void validateData(String indicatorType, String value) {
        if (!StringUtils.hasText(indicatorType) || !StringUtils.hasText(value)) {
            throw BusinessException.badRequest("指标类型和值不能为空");
        }
        if (!healthIndicatorTypeService.isEnabledType(indicatorType)) {
            throw BusinessException.badRequest("指标类型未启用或不存在");
        }
        switch (indicatorType) {
            case "血压" -> {
                if (!value.matches("^[1-9]\\d{1,2}/[1-9]\\d{1,2}$")) {
                    throw BusinessException.badRequest("血压格式必须为xx/xx，且为正数");
                }
            }
            case "血糖" -> {
                BigDecimal v = parsePositiveNumber(value, "血糖必须是正数");
                if (v.compareTo(BigDecimal.valueOf(30)) > 0) {
                    throw BusinessException.badRequest("血糖必须在0-30之间");
                }
            }
            case "体重" -> {
                BigDecimal v = parsePositiveNumber(value, "体重必须是正数");
                if (v.compareTo(BigDecimal.valueOf(500)) > 0) {
                    throw BusinessException.badRequest("体重数值异常，请确认后再提交");
                }
            }
            case "服药" -> {
                if (!("已服药".equals(value) || "未服药".equals(value) || "1".equals(value) || "0".equals(value))) {
                    throw BusinessException.badRequest("服药值仅支持 已服药/未服药/1/0");
                }
            }
            default -> {
                // Extensible indicator types can be configured by admin and use relaxed value validation.
            }
        }
    }

    private BigDecimal parsePositiveNumber(String value, String msg) {
        try {
            BigDecimal n = new BigDecimal(value);
            if (n.compareTo(BigDecimal.ZERO) <= 0) {
                throw BusinessException.badRequest(msg);
            }
            return n;
        } catch (NumberFormatException ex) {
            throw BusinessException.badRequest(msg);
        }
    }

    private Long getCurrentUserId(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user.getId();
    }
}
