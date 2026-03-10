package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.entity.OperationLog;
import com.health.system.mapper.OperationLogMapper;
import com.health.system.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<OperationLog> latestLogs(int limit) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreateTime)
                .last("limit " + Math.max(limit, 1)));
    }
}
