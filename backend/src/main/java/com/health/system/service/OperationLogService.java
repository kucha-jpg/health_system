package com.health.system.service;

import com.health.system.entity.OperationLog;

import java.util.List;

public interface OperationLogService {
    void save(String username, String roleType, String method, String uri, boolean success, String message);

    List<OperationLog> latestLogs(int limit);
}
