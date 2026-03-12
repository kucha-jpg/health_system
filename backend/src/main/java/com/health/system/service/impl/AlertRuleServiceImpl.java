package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.dto.AlertRuleDTO;
import com.health.system.entity.AlertRule;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.service.AlertRuleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;

    public AlertRuleServiceImpl(AlertRuleMapper alertRuleMapper) {
        this.alertRuleMapper = alertRuleMapper;
    }

    @Override
    public List<AlertRule> listRules() {
        return alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .orderByAsc(AlertRule::getIndicatorType));
    }

    @Override
    public void createRule(AlertRuleDTO dto) {
        AlertRule exists = alertRuleMapper.selectOne(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getIndicatorType, dto.getIndicatorType()));
        if (exists != null) {
            throw BusinessException.conflict("该指标规则已存在");
        }
        AlertRule rule = new AlertRule();
        fill(rule, dto);
        alertRuleMapper.insert(rule);
    }

    @Override
    public void updateRule(AlertRuleDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.badRequest("规则ID不能为空");
        }
        AlertRule rule = alertRuleMapper.selectById(dto.getId());
        if (rule == null) {
            throw BusinessException.notFound("规则不存在");
        }
        AlertRule duplicate = alertRuleMapper.selectOne(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getIndicatorType, dto.getIndicatorType())
                .ne(AlertRule::getId, dto.getId()));
        if (duplicate != null) {
            throw BusinessException.conflict("指标类型重复");
        }
        fill(rule, dto);
        alertRuleMapper.updateById(rule);
    }

    private void fill(AlertRule rule, AlertRuleDTO dto) {
        rule.setIndicatorType(dto.getIndicatorType());
        rule.setHighRule(dto.getHighRule());
        rule.setMediumRule(dto.getMediumRule());
        rule.setEnabled(dto.getEnabled());
    }
}
