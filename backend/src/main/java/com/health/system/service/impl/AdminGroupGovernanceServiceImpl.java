package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.RequestActor;
import com.health.system.common.SecurityActorUtils;
import com.health.system.dto.GroupGovernanceDTO;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.OperationLog;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.OperationLogMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.AdminGroupGovernanceService;
import com.health.system.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminGroupGovernanceServiceImpl implements AdminGroupGovernanceService {

    private static final String BASE_URI = "/api/admin/groups";

    private final DoctorGroupMapper doctorGroupMapper;
    private final DoctorGroupMemberMapper doctorGroupMemberMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogService operationLogService;

    public AdminGroupGovernanceServiceImpl(DoctorGroupMapper doctorGroupMapper,
                                           DoctorGroupMemberMapper doctorGroupMemberMapper,
                                           UserMapper userMapper,
                                           OperationLogMapper operationLogMapper,
                                           OperationLogService operationLogService) {
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
        this.userMapper = userMapper;
        this.operationLogMapper = operationLogMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public Map<String, Object> listGroups(String keyword, String status, boolean operableOnly, int pageNo, int pageSize) {
        List<DoctorGroup> groups = doctorGroupMapper.selectList(
                new LambdaQueryWrapper<DoctorGroup>().orderByDesc(DoctorGroup::getCreateTime));

        List<DoctorGroupMember> allMembers = doctorGroupMemberMapper.selectList(null);

        Map<Long, Integer> patientCountMap = new HashMap<>();
        for (DoctorGroupMember m : allMembers) {
            patientCountMap.put(m.getGroupId(), patientCountMap.getOrDefault(m.getGroupId(), 0) + 1);
        }

        Map<Long, String> lastActionTimeMap = buildLastActionTimeMap();

        List<GroupGovernanceDTO> list = new ArrayList<>();
        for (DoctorGroup g : groups) {
            GroupGovernanceDTO dto = new GroupGovernanceDTO();
            dto.setGroupId(g.getId());
            dto.setGroupName(g.getGroupName());
            dto.setPatientCount(patientCountMap.getOrDefault(g.getId(), 0));
            dto.setGovernanceStatus(g.getGovernanceStatus());
            dto.setTargetDept(g.getTargetDept());
            dto.setLastActionTime(lastActionTimeMap.getOrDefault(g.getId(), null));

            User doctor = userMapper.selectById(g.getDoctorId());
            dto.setDoctorName(doctor != null ? (doctor.getName() != null ? doctor.getName() : doctor.getUsername()) : "-");

            list.add(dto);
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            list = list.stream().filter(d -> d.getGroupName() != null && d.getGroupName().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(d -> status.equals(d.getGovernanceStatus()))
                    .collect(Collectors.toList());
        }
        if (operableOnly) {
            list = list.stream().filter(d -> !"ARCHIVED".equals(d.getGovernanceStatus()))
                    .collect(Collectors.toList());
        }

        int total = list.size();
        int from = (pageNo - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<GroupGovernanceDTO> paged = from < total ? list.subList(from, to) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", paged);
        result.put("total", total);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public Map<String, Object> stats() {
        List<DoctorGroup> groups = doctorGroupMapper.selectList(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGroups", groups.size());
        result.put("pendingReview", groups.stream().filter(g -> "PENDING_REVIEW".equals(g.getGovernanceStatus())).count());
        result.put("pendingArchive", groups.stream().filter(g -> "PENDING_ARCHIVE".equals(g.getGovernanceStatus())).count());
        result.put("crossDept", groups.stream().filter(g -> "CROSS_DEPT".equals(g.getGovernanceStatus())).count());
        return result;
    }

    @Override
    public Map<String, Object> approve(Long id) {
        DoctorGroup group = requireGroup(id);
        group.setGovernanceStatus("ACTIVE");
        group.setTargetDept(null);
        doctorGroupMapper.updateById(group);
        logAction(id, "approve", "审核通过");
        return simpleResult(id, "ACTIVE");
    }

    @Override
    public Map<String, Object> archive(Long id) {
        DoctorGroup group = requireGroup(id);
        group.setGovernanceStatus("ARCHIVED");
        doctorGroupMapper.updateById(group);
        logAction(id, "archive", "归档");
        return simpleResult(id, "ARCHIVED");
    }

    @Override
    public Map<String, Object> crossDept(Long id, String targetDept) {
        DoctorGroup group = requireGroup(id);
        if (targetDept == null || targetDept.isBlank()) {
            throw BusinessException.badRequest("目标科室不能为空");
        }
        group.setGovernanceStatus("CROSS_DEPT");
        group.setTargetDept(targetDept);
        doctorGroupMapper.updateById(group);
        logAction(id, "cross-dept", "跨科室处理，目标科室：" + targetDept);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", id);
        result.put("governanceStatus", "CROSS_DEPT");
        result.put("targetDept", targetDept);
        return result;
    }

    @Override
    public Map<String, Object> batchApprove(List<Long> ids) {
        int processed = 0;
        for (Long id : ids) {
            DoctorGroup group = doctorGroupMapper.selectById(id);
            if (group == null || "ARCHIVED".equals(group.getGovernanceStatus())) continue;
            group.setGovernanceStatus("ACTIVE");
            group.setTargetDept(null);
            doctorGroupMapper.updateById(group);
            logAction(id, "batch-approve", "批量审核通过");
            processed++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processed", processed);
        result.put("skipped", ids.size() - processed);
        return result;
    }

    @Override
    public Map<String, Object> batchArchive(List<Long> ids) {
        int processed = 0;
        for (Long id : ids) {
            DoctorGroup group = doctorGroupMapper.selectById(id);
            if (group == null || "ARCHIVED".equals(group.getGovernanceStatus())) continue;
            group.setGovernanceStatus("ARCHIVED");
            doctorGroupMapper.updateById(group);
            logAction(id, "batch-archive", "批量归档");
            processed++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processed", processed);
        result.put("skipped", ids.size() - processed);
        return result;
    }

    @Override
    public Map<String, Object> batchCrossDept(List<Long> ids, String targetDept) {
        if (targetDept == null || targetDept.isBlank()) {
            throw BusinessException.badRequest("目标科室不能为空");
        }
        int processed = 0;
        for (Long id : ids) {
            DoctorGroup group = doctorGroupMapper.selectById(id);
            if (group == null || "ARCHIVED".equals(group.getGovernanceStatus())) continue;
            group.setGovernanceStatus("CROSS_DEPT");
            group.setTargetDept(targetDept);
            doctorGroupMapper.updateById(group);
            logAction(id, "batch-cross-dept", "批量跨科室处理，目标科室：" + targetDept);
            processed++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processed", processed);
        result.put("skipped", ids.size() - processed);
        return result;
    }

    @Override
    public Map<String, Object> deleteGroup(Long id) {
        DoctorGroup group = requireGroup(id);
        doctorGroupMapper.deleteById(id);
        logAction(id, "delete", "删除群组：" + group.getGroupName());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", id);
        result.put("deleted", true);
        return result;
    }

    private DoctorGroup requireGroup(Long id) {
        DoctorGroup group = doctorGroupMapper.selectById(id);
        if (group == null) {
            throw BusinessException.notFound("群组不存在");
        }
        return group;
    }

    private Map<String, Object> simpleResult(Long groupId, String governanceStatus) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", groupId);
        result.put("governanceStatus", governanceStatus);
        return result;
    }

    private Map<Long, String> buildLastActionTimeMap() {
        Map<Long, String> map = new LinkedHashMap<>();
        List<OperationLog> recentLogs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .likeRight(OperationLog::getRequestUri, BASE_URI)
                        .orderByDesc(OperationLog::getCreateTime));
        for (OperationLog log : recentLogs) {
            String uri = log.getRequestUri();
            try {
                String lastPart = uri.substring(uri.lastIndexOf('/') + 1);
                if (lastPart.matches("\\d+")) {
                    Long groupId = Long.parseLong(lastPart);
                    map.putIfAbsent(groupId,
                            log.getCreateTime() != null ? log.getCreateTime().toString() : null);
                }
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private void logAction(Long groupId, String action, String message) {
        RequestActor actor = SecurityActorUtils.currentActor();
        String username = actor != null ? actor.username() : "system";
        String roleType = actor != null ? actor.role() : "ADMIN";
        operationLogService.save(username, roleType, "PATCH",
                BASE_URI + "/" + groupId + "/" + action, true, message);
    }
}
