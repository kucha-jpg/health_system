package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.CacheNames;
import com.health.system.dto.PatientAlertPreferenceDTO;
import com.health.system.entity.PatientAlertPreference;
import com.health.system.entity.User;
import com.health.system.mapper.PatientAlertPreferenceMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.PatientAlertPreferenceService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PatientAlertPreferenceServiceImpl implements PatientAlertPreferenceService {

    private final PatientAlertPreferenceMapper patientAlertPreferenceMapper;
    private final UserMapper userMapper;

    public PatientAlertPreferenceServiceImpl(PatientAlertPreferenceMapper patientAlertPreferenceMapper,
                                             UserMapper userMapper) {
        this.patientAlertPreferenceMapper = patientAlertPreferenceMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<Map<String, Object>> listMyPreferences(String username) {
        User user = resolvePatient(username);
        List<PatientAlertPreference> list = patientAlertPreferenceMapper.selectList(new LambdaQueryWrapper<PatientAlertPreference>()
                .eq(PatientAlertPreference::getUserId, user.getId())
                .orderByAsc(PatientAlertPreference::getIndicatorType));
        return list.stream().map(this::toView).toList();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PATIENT_ALERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_OPEN_ALERTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PATIENT_REPORT_SUMMARY, allEntries = true)
    })
    public void upsertMyPreference(String username, PatientAlertPreferenceDTO dto) {
        User user = resolvePatient(username);
        validate(dto);

        PatientAlertPreference existing = patientAlertPreferenceMapper.selectOne(new LambdaQueryWrapper<PatientAlertPreference>()
                .eq(PatientAlertPreference::getUserId, user.getId())
                .eq(PatientAlertPreference::getIndicatorType, dto.getIndicatorType())
                .last("limit 1"));

        if (existing == null) {
            PatientAlertPreference record = new PatientAlertPreference();
            record.setUserId(user.getId());
            record.setIndicatorType(dto.getIndicatorType());
            record.setHighRule(dto.getHighRule());
            record.setMediumRule(dto.getMediumRule());
            record.setEnabled(dto.getEnabled());
            patientAlertPreferenceMapper.insert(record);
            return;
        }

        existing.setHighRule(dto.getHighRule());
        existing.setMediumRule(dto.getMediumRule());
        existing.setEnabled(dto.getEnabled());
        patientAlertPreferenceMapper.updateById(existing);
    }

    private User resolvePatient(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !"PATIENT".equals(user.getRoleType())) {
            throw BusinessException.notFound("患者不存在");
        }
        return user;
    }

    private void validate(PatientAlertPreferenceDTO dto) {
        String indicatorType = dto.getIndicatorType();
        if (!StringUtils.hasText(indicatorType)) {
            throw BusinessException.badRequest("指标类型不能为空");
        }
        Integer enabled = dto.getEnabled();
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw BusinessException.badRequest("启用状态非法");
        }

        String highRule = dto.getHighRule();
        String mediumRule = dto.getMediumRule();

        switch (indicatorType) {
            case "血压" -> {
                if (!StringUtils.hasText(highRule) || !isPressureRule(highRule)) {
                    throw BusinessException.badRequest("血压高风险阈值格式必须为xx/xx");
                }
                if (StringUtils.hasText(mediumRule) && !isPressureRule(mediumRule)) {
                    throw BusinessException.badRequest("血压中风险阈值格式必须为xx/xx");
                }
            }
            case "血糖", "体重" -> {
                if (!StringUtils.hasText(highRule) || !isPositiveNumber(highRule)) {
                    throw BusinessException.badRequest("高风险阈值必须为正数");
                }
                if (StringUtils.hasText(mediumRule) && !isPositiveNumber(mediumRule)) {
                    throw BusinessException.badRequest("中风险阈值必须为正数");
                }
            }
            default -> throw BusinessException.badRequest("暂不支持该指标的个性化阈值");
        }
    }

    private boolean isPressureRule(String value) {
        String[] arr = value.split("/");
        if (arr.length != 2) {
            return false;
        }
        try {
            return Integer.parseInt(arr[0]) > 0 && Integer.parseInt(arr[1]) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isPositiveNumber(String value) {
        try {
            return new BigDecimal(value).compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private Map<String, Object> toView(PatientAlertPreference item) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", item.getId());
        view.put("indicatorType", item.getIndicatorType());
        view.put("highRule", item.getHighRule());
        view.put("mediumRule", item.getMediumRule());
        view.put("enabled", item.getEnabled());
        return view;
    }
}
