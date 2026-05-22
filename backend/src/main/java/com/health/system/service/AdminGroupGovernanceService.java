package com.health.system.service;

import java.util.List;
import java.util.Map;

public interface AdminGroupGovernanceService {

    Map<String, Object> listGroups(String keyword, String status, boolean operableOnly, int pageNo, int pageSize);

    Map<String, Object> stats();

    Map<String, Object> approve(Long id);

    Map<String, Object> archive(Long id);

    Map<String, Object> crossDept(Long id, String targetDept, List<Long> doctorIds);

    Map<String, Object> batchApprove(List<Long> ids);

    Map<String, Object> batchArchive(List<Long> ids);

    Map<String, Object> batchCrossDept(List<Long> ids, String targetDept, List<Long> doctorIds);

    Map<String, Object> deleteGroup(Long id);
}
