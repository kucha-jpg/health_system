package com.health.system.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.dto.HealthIndicatorTypeDTO;
import com.health.system.entity.AlertRule;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.HealthIndicatorType;
import com.health.system.entity.PatientAlertPreference;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.HealthIndicatorTypeMapper;
import com.health.system.mapper.PatientAlertPreferenceMapper;
import com.health.system.service.HealthIndicatorTypeService;

@Service
public class HealthIndicatorTypeServiceImpl implements HealthIndicatorTypeService {

    private static final Set<String> CORE_INDICATORS = Set.of(
            "血压", "血糖", "体重", "服药",
            "BLOOD_PRESSURE", "BLOOD_SUGAR", "WEIGHT", "MEDICATION"
    );

    private final HealthIndicatorTypeMapper healthIndicatorTypeMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final HealthDataMapper healthDataMapper;
    private final PatientAlertPreferenceMapper patientAlertPreferenceMapper;
    private final HealthAlertMapper healthAlertMapper;

    public HealthIndicatorTypeServiceImpl(HealthIndicatorTypeMapper healthIndicatorTypeMapper,
                                          AlertRuleMapper alertRuleMapper,
                                          HealthDataMapper healthDataMapper,
                                          PatientAlertPreferenceMapper patientAlertPreferenceMapper,
                                          HealthAlertMapper healthAlertMapper) {
        this.healthIndicatorTypeMapper = healthIndicatorTypeMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.healthDataMapper = healthDataMapper;
        this.patientAlertPreferenceMapper = patientAlertPreferenceMapper;
        this.healthAlertMapper = healthAlertMapper;
    }

    @Override
    public List<HealthIndicatorType> listTypes(boolean includeDisabled) {
        LambdaQueryWrapper<HealthIndicatorType> wrapper = new LambdaQueryWrapper<HealthIndicatorType>()
                .orderByAsc(HealthIndicatorType::getIndicatorType);
        if (!includeDisabled) {
            wrapper.eq(HealthIndicatorType::getEnabled, 1);
        }
        return healthIndicatorTypeMapper.selectList(wrapper);
    }

    @Override
    public void createType(HealthIndicatorTypeDTO dto) {
        String indicatorType = normalizeType(dto.getIndicatorType());
        HealthIndicatorType exists = healthIndicatorTypeMapper.selectOne(
                new LambdaQueryWrapper<HealthIndicatorType>().eq(HealthIndicatorType::getIndicatorType, indicatorType)
        );
        if (exists != null) {
            throw BusinessException.conflict("指标类型已存在");
        }
        HealthIndicatorType type = new HealthIndicatorType();
        type.setIndicatorType(indicatorType);
        type.setDisplayName(dto.getDisplayName().trim());
        type.setEnabled(dto.getEnabled());
        healthIndicatorTypeMapper.insert(type);
    }

    @Override
    public void updateType(HealthIndicatorTypeDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.badRequest("指标类型ID不能为空");
        }
        HealthIndicatorType type = healthIndicatorTypeMapper.selectById(dto.getId());
        if (type == null) {
            throw BusinessException.notFound("指标类型不存在");
        }

        String indicatorType = normalizeType(dto.getIndicatorType());
        String currentIndicatorType = normalizeType(type.getIndicatorType());
        if (isCoreIndicator(currentIndicatorType) && !currentIndicatorType.equals(indicatorType)) {
            throw BusinessException.badRequest("内置核心指标不允许修改指标类型编码");
        }

        HealthIndicatorType duplicate = healthIndicatorTypeMapper.selectOne(
                new LambdaQueryWrapper<HealthIndicatorType>()
                        .eq(HealthIndicatorType::getIndicatorType, indicatorType)
                        .ne(HealthIndicatorType::getId, dto.getId())
        );
        if (duplicate != null) {
            throw BusinessException.conflict("指标类型重复");
        }

        type.setIndicatorType(indicatorType);
        type.setDisplayName(dto.getDisplayName().trim());
        type.setEnabled(dto.getEnabled());
        healthIndicatorTypeMapper.updateById(type);
    }

    @Override
    public void deleteType(Long id) {
        if (id == null) {
            throw BusinessException.badRequest("指标类型ID不能为空");
        }
        HealthIndicatorType type = healthIndicatorTypeMapper.selectById(id);
        if (type == null) {
            throw BusinessException.notFound("指标类型不存在");
        }

        String indicatorType = type.getIndicatorType();
    if (isCoreIndicator(indicatorType)) {
        throw BusinessException.badRequest("内置核心指标不允许删除");
    }

        long ruleRefs = alertRuleMapper.selectCount(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getIndicatorType, indicatorType)
        );
        long dataRefs = healthDataMapper.selectCount(
                new LambdaQueryWrapper<HealthData>().eq(HealthData::getIndicatorType, indicatorType)
        );
        long prefRefs = patientAlertPreferenceMapper.selectCount(
                new LambdaQueryWrapper<PatientAlertPreference>().eq(PatientAlertPreference::getIndicatorType, indicatorType)
        );
        long alertRefs = healthAlertMapper.selectCount(
                new LambdaQueryWrapper<HealthAlert>().eq(HealthAlert::getIndicatorType, indicatorType)
        );

        long totalRefs = ruleRefs + dataRefs + prefRefs + alertRefs;
        if (totalRefs > 0) {
            throw BusinessException.badRequest("该指标类型已被规则或历史数据引用，无法删除");
        }

        healthIndicatorTypeMapper.deleteById(id);
    }

    @Override
    public boolean isEnabledType(String indicatorType) {
        if (!StringUtils.hasText(indicatorType)) {
            return false;
        }
        Long count = healthIndicatorTypeMapper.selectCount(
                new LambdaQueryWrapper<HealthIndicatorType>()
                        .eq(HealthIndicatorType::getIndicatorType, indicatorType.trim())
                        .eq(HealthIndicatorType::getEnabled, 1)
        );
        return count != null && count > 0;
    }

    private String normalizeType(String indicatorType) {
        if (!StringUtils.hasText(indicatorType)) {
            throw BusinessException.badRequest("指标类型不能为空");
        }
        return indicatorType.trim();
    }

    private boolean isCoreIndicator(String indicatorType) {
        String normalized = normalizeType(indicatorType);
        return CORE_INDICATORS.contains(normalized);
    }
}
