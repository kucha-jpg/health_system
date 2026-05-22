package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.RequestActor;
import com.health.system.common.SecurityActorUtils;
import com.health.system.dto.GroupGovernanceDTO;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupDoctorMember;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.OperationLog;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupDoctorMemberMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.OperationLogMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.entity.FeedbackMessage;
import com.health.system.mapper.FeedbackMessageMapper;
import com.health.system.common.CacheNames;
import com.health.system.config.CacheEvictionSupport;
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
    private final DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogService operationLogService;
    private final FeedbackMessageMapper feedbackMessageMapper;
    private final CacheEvictionSupport cacheEvictionSupport;

    public AdminGroupGovernanceServiceImpl(DoctorGroupMapper doctorGroupMapper,
                                           DoctorGroupMemberMapper doctorGroupMemberMapper,
                                           DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper,
                                           UserMapper userMapper,
                                           OperationLogMapper operationLogMapper,
                                           OperationLogService operationLogService,
                                           FeedbackMessageMapper feedbackMessageMapper,
                                           CacheEvictionSupport cacheEvictionSupport) {
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
        this.doctorGroupDoctorMemberMapper = doctorGroupDoctorMemberMapper;
        this.userMapper = userMapper;
        this.operationLogMapper = operationLogMapper;
        this.operationLogService = operationLogService;
        this.feedbackMessageMapper = feedbackMessageMapper;
        this.cacheEvictionSupport = cacheEvictionSupport;
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
        notifyDoctor(group.getDoctorId(), group.getGroupName(), "审核通过");
        evictDoctorCaches();
        return simpleResult(id, "ACTIVE");
    }

    @Override
    public Map<String, Object> archive(Long id) {
        DoctorGroup group = requireGroup(id);
        group.setGovernanceStatus("ARCHIVED");
        doctorGroupMapper.updateById(group);
        logAction(id, "archive", "归档");
        evictDoctorCaches();
        return simpleResult(id, "ARCHIVED");
    }

    @Override
    public Map<String, Object> crossDept(Long id, String targetDept, List<Long> doctorIds) {
        DoctorGroup group = requireGroup(id);
        group.setGovernanceStatus("CROSS_DEPT");
        if (targetDept != null && !targetDept.isBlank()) {
            group.setTargetDept(targetDept);
        }
        doctorGroupMapper.updateById(group);
        logAction(id, "cross-dept", "跨科室处理" + (targetDept != null && !targetDept.isBlank() ? "，目标科室：" + targetDept : ""));

        if (doctorIds != null) {
            for (Long doctorId : doctorIds) {
                addCollaboratorIfAbsent(id, doctorId);
            }
        }

        evictDoctorCaches();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", id);
        result.put("governanceStatus", "CROSS_DEPT");
        return result;
    }

    private void addCollaboratorIfAbsent(Long groupId, Long doctorUserId) {
        User doctor = userMapper.selectById(doctorUserId);
        if (doctor == null || !"DOCTOR".equals(doctor.getRoleType())) return;
        DoctorGroupDoctorMember exists = doctorGroupDoctorMemberMapper.selectOne(new LambdaQueryWrapper<DoctorGroupDoctorMember>()
                .eq(DoctorGroupDoctorMember::getGroupId, groupId)
                .eq(DoctorGroupDoctorMember::getDoctorUserId, doctorUserId));
        if (exists != null) return;
        DoctorGroupDoctorMember member = new DoctorGroupDoctorMember();
        member.setGroupId(groupId);
        member.setDoctorUserId(doctorUserId);
        doctorGroupDoctorMemberMapper.insert(member);
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
            notifyDoctor(group.getDoctorId(), group.getGroupName(), "审核通过");
            processed++;
        }
        evictDoctorCaches();
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
        evictDoctorCaches();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processed", processed);
        result.put("skipped", ids.size() - processed);
        return result;
    }

    @Override
    public Map<String, Object> batchCrossDept(List<Long> ids, String targetDept, List<Long> doctorIds) {
        int processed = 0;
        for (Long id : ids) {
            DoctorGroup group = doctorGroupMapper.selectById(id);
            if (group == null || "ARCHIVED".equals(group.getGovernanceStatus())) continue;
            group.setGovernanceStatus("CROSS_DEPT");
            if (targetDept != null && !targetDept.isBlank()) {
                group.setTargetDept(targetDept);
            }
            doctorGroupMapper.updateById(group);
            logAction(id, "batch-cross-dept", "批量跨科室处理" + (targetDept != null && !targetDept.isBlank() ? "，目标科室：" + targetDept : ""));
            if (doctorIds != null) {
                for (Long doctorId : doctorIds) {
                    addCollaboratorIfAbsent(id, doctorId);
                }
            }
            processed++;
        }
        evictDoctorCaches();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processed", processed);
        result.put("skipped", ids.size() - processed);
        return result;
    }

    @Override
    public Map<String, Object> deleteGroup(Long id) {
        DoctorGroup group = requireGroup(id);
        doctorGroupMemberMapper.delete(new LambdaQueryWrapper<DoctorGroupMember>()
                .eq(DoctorGroupMember::getGroupId, id));
        doctorGroupDoctorMemberMapper.delete(new LambdaQueryWrapper<DoctorGroupDoctorMember>()
                .eq(DoctorGroupDoctorMember::getGroupId, id));
        doctorGroupMapper.deleteById(id);
        logAction(id, "delete", "删除群组：" + group.getGroupName());
        evictDoctorCaches();
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

    private void evictDoctorCaches() {
        cacheEvictionSupport.evictByPrefix(CacheNames.DOCTOR_OPEN_ALERTS, "");
        cacheEvictionSupport.evictByPrefix(CacheNames.DOCTOR_PATIENT_INSIGHT, "");
    }

    private void logAction(Long groupId, String action, String message) {
        RequestActor actor = SecurityActorUtils.currentActor();
        String username = actor != null ? actor.username() : "system";
        String roleType = actor != null ? actor.role() : "ADMIN";
        operationLogService.save(username, roleType, "PATCH",
                BASE_URI + "/" + groupId + "/" + action, true, message);
    }

    private void notifyDoctor(Long doctorId, String groupName, String action) {
        User doctor = userMapper.selectById(doctorId);
        if (doctor == null) return;
        FeedbackMessage msg = new FeedbackMessage();
        msg.setSenderUserId(0L);
        msg.setSenderUsername("系统");
        msg.setSenderRoleType("ADMIN");
        msg.setContent("群组「" + groupName + "」" + action + "，现已可使用。");
        msg.setStatus(1);
        msg.setReplyContent("系统审核通知");
        msg.setRepliedTime(java.time.LocalDateTime.now());
        msg.setReplyRead(0);
        msg.setReplyReadTime(null);
        feedbackMessageMapper.insert(msg);
    }
}
