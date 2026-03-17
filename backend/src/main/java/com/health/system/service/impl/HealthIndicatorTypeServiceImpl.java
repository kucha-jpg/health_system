package com.health.system.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.dto.HealthIndicatorTypeDTO;
import com.health.system.entity.HealthIndicatorType;
import com.health.system.mapper.HealthIndicatorTypeMapper;
import com.health.system.service.HealthIndicatorTypeService;

@Service
public class HealthIndicatorTypeServiceImpl implements HealthIndicatorTypeService {

    private final HealthIndicatorTypeMapper healthIndicatorTypeMapper;

    public HealthIndicatorTypeServiceImpl(HealthIndicatorTypeMapper healthIndicatorTypeMapper) {
        this.healthIndicatorTypeMapper = healthIndicatorTypeMapper;
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
}
