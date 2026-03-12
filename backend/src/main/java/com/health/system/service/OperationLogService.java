package com.health.system.service;

import java.util.List;
import java.util.Map;

import com.health.system.dto.OperationLogQueryDTO;
import com.health.system.entity.OperationLog;

public interface OperationLogService {
    void save(String username, String roleType, String method, String uri, boolean success, String message);

    List<OperationLog> latestLogs(int limit, OperationLogQueryDTO query);

    Map<String, Object> latestLogsPaged(OperationLogQueryDTO query);
}
