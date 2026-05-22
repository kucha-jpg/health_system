package com.health.system.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.CacheNames;
import com.health.system.config.CacheEvictionSupport;
import com.health.system.dto.DoctorGroupDTO;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupDoctorMember;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupDoctorMemberMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.DoctorGroupService;
import com.health.system.service.support.DoctorAccessSupport;

@Service
public class DoctorGroupServiceImpl implements DoctorGroupService {

    private final DoctorGroupMapper doctorGroupMapper;
    private final DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper;
    private final DoctorGroupMemberMapper doctorGroupMemberMapper;
    private final UserMapper userMapper;
    private final DoctorAccessSupport doctorAccessSupport;
    private final CacheEvictionSupport cacheEvictionSupport;

    public DoctorGroupServiceImpl(DoctorGroupMapper doctorGroupMapper,
                                  DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper,
                                  DoctorGroupMemberMapper doctorGroupMemberMapper,
                                  UserMapper userMapper,
                                  DoctorAccessSupport doctorAccessSupport,
                                  CacheEvictionSupport cacheEvictionSupport) {
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupDoctorMemberMapper = doctorGroupDoctorMemberMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
        this.userMapper = userMapper;
        this.doctorAccessSupport = doctorAccessSupport;
        this.cacheEvictionSupport = cacheEvictionSupport;
    }

    @Override
    public void createGroup(String doctorUsername, DoctorGroupDTO dto) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        DoctorGroup group = new DoctorGroup();
        group.setDoctorId(doctor.getId());
        group.setGroupName(dto.getGroupName());
        group.setDescription(dto.getDescription());
        group.setGovernanceStatus("PENDING_REVIEW");
        doctorGroupMapper.insert(group);
        evictGroupRelatedCaches();
    }

    @Override
    public List<DoctorGroup> listMyGroups(String doctorUsername) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        List<DoctorGroup> ownedGroups = doctorGroupMapper.selectList(new LambdaQueryWrapper<DoctorGroup>()
                .eq(DoctorGroup::getDoctorId, doctor.getId())
                .orderByDesc(DoctorGroup::getCreateTime));

        List<DoctorGroupDoctorMember> myMemberships = doctorGroupDoctorMemberMapper.selectList(
                new LambdaQueryWrapper<DoctorGroupDoctorMember>()
                        .eq(DoctorGroupDoctorMember::getDoctorUserId, doctor.getId())
        );
        if (myMemberships.isEmpty()) {
            return ownedGroups;
        }

        List<Long> memberGroupIds = myMemberships.stream()
                .map(DoctorGroupDoctorMember::getGroupId)
                .distinct()
                .toList();
        Map<Long, DoctorGroup> merged = new LinkedHashMap<>();
        for (DoctorGroup item : ownedGroups) {
            merged.put(item.getId(), item);
        }
        if (!memberGroupIds.isEmpty()) {
            List<DoctorGroup> memberGroups = doctorGroupMapper.selectBatchIds(memberGroupIds);
            for (DoctorGroup item : memberGroups) {
                if (item != null) {
                    merged.putIfAbsent(item.getId(), item);
                }
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(DoctorGroup::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    @Override
    @Transactional
    public void addDoctorToGroup(String doctorUsername, Long groupId, Long doctorUserId) {
        User operator = doctorAccessSupport.requireDoctor(doctorUsername);
        DoctorGroup group = doctorGroupMapper.selectById(groupId);
        if (group == null) {
            throw BusinessException.notFound("群组不存在");
        }
        if ("ARCHIVED".equals(group.getGovernanceStatus())) {
            throw BusinessException.forbidden("该群组已被管理员归档，无法操作");
        }
        if ("PENDING_REVIEW".equals(group.getGovernanceStatus())) {
            throw BusinessException.forbidden("该群组正在审核中，审核通过后方可使用");
        }
        if (!operator.getId().equals(group.getDoctorId())) {
            throw BusinessException.forbidden("仅群组创建者可维护协作医生");
        }

        User doctor = userMapper.selectById(doctorUserId);
        if (doctor == null || !"DOCTOR".equals(doctor.getRoleType())) {
            throw BusinessException.notFound("医生不存在");
        }

        if (operator.getId().equals(doctorUserId)) {
            return;
        }

        int restored = doctorGroupDoctorMemberMapper.restoreDeletedByGroupAndDoctor(groupId, doctorUserId);
        if (restored > 0) {
            evictGroupRelatedCaches();
            return;
        }
        DoctorGroupDoctorMember exists = doctorGroupDoctorMemberMapper.selectOne(new LambdaQueryWrapper<DoctorGroupDoctorMember>()
                .eq(DoctorGroupDoctorMember::getGroupId, groupId)
                .eq(DoctorGroupDoctorMember::getDoctorUserId, doctorUserId));
        if (exists != null) {
            return;
        }

        DoctorGroupDoctorMember member = new DoctorGroupDoctorMember();
        member.setGroupId(groupId);
        member.setDoctorUserId(doctorUserId);
        doctorGroupDoctorMemberMapper.insert(member);
        evictGroupRelatedCaches();
    }

    @Override
    public List<User> listGroupDoctors(String doctorUsername, Long groupId) {
        User operator = doctorAccessSupport.requireDoctor(doctorUsername);
        doctorAccessSupport.assertGroupAccessible(operator.getId(), groupId);

        DoctorGroup group = doctorGroupMapper.selectById(groupId);
        if (group == null) {
            throw BusinessException.notFound("群组不存在");
        }

        List<User> result = new ArrayList<>();
        User owner = userMapper.selectById(group.getDoctorId());
        if (owner != null && "DOCTOR".equals(owner.getRoleType())) {
            result.add(owner);
        }

        List<DoctorGroupDoctorMember> members = doctorGroupDoctorMemberMapper.selectList(
                new LambdaQueryWrapper<DoctorGroupDoctorMember>().eq(DoctorGroupDoctorMember::getGroupId, groupId)
        );
        if (!members.isEmpty()) {
            Set<Long> seenIds = new HashSet<>();
            seenIds.add(group.getDoctorId());
            List<Long> doctorIds = members.stream()
                    .map(DoctorGroupDoctorMember::getDoctorUserId)
                    .filter(id -> !seenIds.contains(id))
                    .distinct()
                    .toList();
            if (!doctorIds.isEmpty()) {
                List<User> doctors = userMapper.selectBatchIds(doctorIds);
                for (User doctor : doctors) {
                    if (doctor != null && "DOCTOR".equals(doctor.getRoleType())) {
                        result.add(doctor);
                    }
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void addPatientToGroup(String doctorUsername, Long groupId, Long patientUserId) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        doctorAccessSupport.assertGroupAccessible(doctor.getId(), groupId);
        User patient = userMapper.selectById(patientUserId);
        if (patient == null || !"PATIENT".equals(patient.getRoleType())) {
            throw BusinessException.notFound("患者不存在");
        }
        int restored = doctorGroupMemberMapper.restoreDeletedByGroupAndPatient(groupId, patientUserId);
        if (restored > 0) {
            evictGroupRelatedCaches();
            return;
        }
        DoctorGroupMember exists = doctorGroupMemberMapper.selectOne(new LambdaQueryWrapper<DoctorGroupMember>()
                .eq(DoctorGroupMember::getGroupId, groupId)
                .eq(DoctorGroupMember::getPatientUserId, patientUserId));
        if (exists != null) {
            return;
        }
        DoctorGroupMember member = new DoctorGroupMember();
        member.setGroupId(groupId);
        member.setPatientUserId(patientUserId);
        doctorGroupMemberMapper.insert(member);
        evictGroupRelatedCaches();
    }

    @Override
    public void removePatientFromGroup(String doctorUsername, Long groupId, Long patientUserId) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        doctorAccessSupport.assertGroupAccessible(doctor.getId(), groupId);
        DoctorGroupMember member = doctorGroupMemberMapper.selectOne(new LambdaQueryWrapper<DoctorGroupMember>()
                .eq(DoctorGroupMember::getGroupId, groupId)
                .eq(DoctorGroupMember::getPatientUserId, patientUserId));
        if (member == null) {
            throw BusinessException.notFound("该患者不在群组中");
        }
        doctorGroupMemberMapper.deleteById(member.getId());
        evictGroupRelatedCaches();
    }

    @Override
    public void removeDoctorFromGroup(String doctorUsername, Long groupId, Long doctorUserId) {
        User operator = doctorAccessSupport.requireDoctor(doctorUsername);
        DoctorGroup group = doctorGroupMapper.selectById(groupId);
        if (group == null) {
            throw BusinessException.notFound("群组不存在");
        }
        if (!operator.getId().equals(group.getDoctorId())) {
            throw BusinessException.forbidden("仅群组创建者可移除协作医生");
        }
        if (operator.getId().equals(doctorUserId)) {
            throw BusinessException.badRequest("不能移除群组创建者自身");
        }
        DoctorGroupDoctorMember member = doctorGroupDoctorMemberMapper.selectOne(new LambdaQueryWrapper<DoctorGroupDoctorMember>()
                .eq(DoctorGroupDoctorMember::getGroupId, groupId)
                .eq(DoctorGroupDoctorMember::getDoctorUserId, doctorUserId));
        if (member == null) {
            throw BusinessException.notFound("该医生不在群组中");
        }
        doctorGroupDoctorMemberMapper.deleteById(member.getId());
        evictGroupRelatedCaches();
    }

    @Override
    public List<User> listGroupPatients(String doctorUsername, Long groupId) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        doctorAccessSupport.assertGroupAccessible(doctor.getId(), groupId);
        List<DoctorGroupMember> members = doctorGroupMemberMapper.selectList(new LambdaQueryWrapper<DoctorGroupMember>()
                .eq(DoctorGroupMember::getGroupId, groupId));
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> patientIds = members.stream()
                .map(DoctorGroupMember::getPatientUserId)
                .distinct()
                .toList();
        List<User> allUsers = userMapper.selectBatchIds(patientIds);
        List<User> patients = new ArrayList<>();
        for (User user : allUsers) {
            if (user != null && "PATIENT".equals(user.getRoleType())) {
                patients.add(user);
            }
        }
        return patients;
    }

    private void evictGroupRelatedCaches() {
        // Group membership changes affect all doctors' alert views and patient insights.
        // Use clear() since the set of affected doctors is not known at this point.
        cacheEvictionSupport.evictByPrefix(CacheNames.DOCTOR_OPEN_ALERTS, "");
        cacheEvictionSupport.evictByPrefix(CacheNames.DOCTOR_PATIENT_INSIGHT, "");
    }
}
