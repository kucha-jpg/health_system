package com.health.system.service;

import com.health.system.dto.AlertRuleDTO;
import com.health.system.entity.AlertRule;

import java.util.List;

public interface AlertRuleService {
    List<AlertRule> listRules();

    void createRule(AlertRuleDTO dto);

    void updateRule(AlertRuleDTO dto);
}
