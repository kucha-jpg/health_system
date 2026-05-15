package com.health.system.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.system.dto.OperationLogQueryDTO;
import com.health.system.entity.OperationLog;
import com.health.system.mapper.OperationLogMapper;
import com.health.system.service.OperationLogService;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void save(String username, String roleType, String method, String uri, boolean success, String message) {
        OperationLog log = new OperationLog();
        log.setUsername(username);
        log.setRoleType(roleType);
        log.setRequestMethod(method);
        log.setRequestUri(uri);
        log.setSuccess(success ? 1 : 0);
        log.setMessage(message == null ? "" : message);
        operationLogMapper.insert(log);
    }

    @Override
    public List<OperationLog> latestLogs(int limit, OperationLogQueryDTO query) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreateTime);

        applyFilters(wrapper, query);

        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(1, Math.max(limit, 1)), wrapper);
        return page.getRecords();
    }

    @Override
    public Map<String, Object> latestLogsPaged(OperationLogQueryDTO query) {
        Integer pageNoObj = query == null ? null : query.getPageNo();
        Integer pageSizeObj = query == null ? null : query.getPageSize();
        int pageNo = safeInt(pageNoObj, 1);
        int pageSize = safeInt(pageSizeObj, 20);
        int safePageNo = Math.min(Math.max(pageNo, 1), 1000);
        int safePageSize = Math.min(Math.max(pageSize, 1), 200);

        LambdaQueryWrapper<OperationLog> pageWrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreateTime);
        applyFilters(pageWrapper, query);

        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(safePageNo, safePageSize), pageWrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNo", safePageNo);
        result.put("pageSize", safePageSize);
        return result;
    }

    private void applyFilters(LambdaQueryWrapper<OperationLog> wrapper, OperationLogQueryDTO query) {
        String keyword = query == null ? null : query.getKeyword();
        String roleType = query == null ? null : query.getRoleType();
        Integer success = query == null ? null : query.getSuccess();
        LocalDateTime startTime = query == null ? null : query.getStartTime();
        LocalDateTime endTime = query == null ? null : query.getEndTime();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OperationLog::getUsername, keyword)
                    .or().like(OperationLog::getRequestUri, keyword)
                    .or().like(OperationLog::getMessage, keyword));
        }
        if (StringUtils.hasText(roleType)) {
            wrapper.eq(OperationLog::getRoleType, roleType);
        }
        if (success != null) {
            wrapper.eq(OperationLog::getSuccess, success);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreateTime, endTime);
        }
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
