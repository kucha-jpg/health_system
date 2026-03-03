package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.dto.HealthDataDTO;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.HealthDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthDataServiceImpl implements HealthDataService {

    private final HealthDataMapper healthDataMapper;
    private final UserMapper userMapper;

    public HealthDataServiceImpl(HealthDataMapper healthDataMapper, UserMapper userMapper) {
        this.healthDataMapper = healthDataMapper;
        this.userMapper = userMapper;
    }

    @Override
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
    }

    @Override
    public List<HealthData> list(String username, String indicatorType, String timeRange) {
        Long userId = getCurrentUserId(username);
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
        return healthDataMapper.selectList(wrapper);
    }

    @Override
    public void update(String username, Long id, HealthDataDTO dto) {
        Long userId = getCurrentUserId(username);
        HealthData old = healthDataMapper.selectById(id);
        if (old == null || !userId.equals(old.getUserId())) {
            throw new RuntimeException("数据不存在或无权限");
        }
        validateData(dto.getIndicatorType(), dto.getValue());
        old.setIndicatorType(dto.getIndicatorType());
        old.setValue(dto.getValue());
        old.setReportTime(dto.getReportTime() == null ? old.getReportTime() : dto.getReportTime());
        old.setRemark(dto.getRemark());
        healthDataMapper.updateById(old);
    }

    @Override
    public void delete(String username, Long id) {
        Long userId = getCurrentUserId(username);
        HealthData old = healthDataMapper.selectById(id);
        if (old == null || !userId.equals(old.getUserId())) {
            throw new RuntimeException("数据不存在或无权限");
        }
        healthDataMapper.deleteById(id);
    }

    private void validateData(String indicatorType, String value) {
        if (!StringUtils.hasText(indicatorType) || !StringUtils.hasText(value)) {
            throw new RuntimeException("指标类型和值不能为空");
        }
        switch (indicatorType) {
            case "血压" -> {
                if (!value.matches("^[1-9]\\d{1,2}/[1-9]\\d{1,2}$")) {
                    throw new RuntimeException("血压格式必须为xx/xx，且为正数");
                }
            }
            case "血糖" -> {
                BigDecimal v = parsePositiveNumber(value, "血糖必须是正数");
                if (v.compareTo(BigDecimal.valueOf(30)) > 0) {
                    throw new RuntimeException("血糖必须在0-30之间");
                }
            }
            case "体重" -> {
                BigDecimal v = parsePositiveNumber(value, "体重必须是正数");
                if (v.compareTo(BigDecimal.valueOf(500)) > 0) {
                    throw new RuntimeException("体重数值异常，请确认后再提交");
                }
            }
            case "服药" -> {
                if (!("已服药".equals(value) || "未服药".equals(value) || "1".equals(value) || "0".equals(value))) {
                    throw new RuntimeException("服药值仅支持 已服药/未服药/1/0");
                }
            }
            default -> throw new RuntimeException("不支持的指标类型");
        }
    }

    private BigDecimal parsePositiveNumber(String value, String msg) {
        try {
            BigDecimal n = new BigDecimal(value);
            if (n.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException(msg);
            }
            return n;
        } catch (NumberFormatException ex) {
            throw new RuntimeException(msg);
        }
    }

    private Long getCurrentUserId(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getId();
    }
}
