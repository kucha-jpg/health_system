package com.health.system.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.CacheNames;
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

    public DoctorGroupServiceImpl(DoctorGroupMapper doctorGroupMapper,
                                  DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper,
                                  DoctorGroupMemberMapper doctorGroupMemberMapper,
                                  UserMapper userMapper,
                                  DoctorAccessSupport doctorAccessSupport) {
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupDoctorMemberMapper = doctorGroupDoctorMemberMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
        this.userMapper = userMapper;
        this.doctorAccessSupport = doctorAccessSupport;
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_OPEN_ALERTS, allEntries = true)
        })
    public void createGroup(String doctorUsername, DoctorGroupDTO dto) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        DoctorGroup group = new DoctorGroup();
        group.setDoctorId(doctor.getId());
        group.setGroupName(dto.getGroupName());
        group.setDescription(dto.getDescription());
        doctorGroupMapper.insert(group);
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

        List<Long> memberGroupIds = myMemberships.stream().map(DoctorGroupDoctorMember::getGroupId).toList();
        List<DoctorGroup> memberGroups = doctorGroupMapper.selectBatchIds(memberGroupIds);

        Map<Long, DoctorGroup> merged = new LinkedHashMap<>();
        for (DoctorGroup item : ownedGroups) {
            merged.put(item.getId(), item);
        }
        for (DoctorGroup item : memberGroups) {
            if (item != null) {
                merged.put(item.getId(), item);
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(DoctorGroup::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_OPEN_ALERTS, allEntries = true)
        })
    public void addDoctorToGroup(String doctorUsername, Long groupId, Long doctorUserId) {
        User operator = doctorAccessSupport.requireDoctor(doctorUsername);
        DoctorGroup group = doctorGroupMapper.selectById(groupId);
        if (group == null) {
            throw BusinessException.notFound("群组不存在");
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
        for (DoctorGroupDoctorMember item : members) {
            User doctor = userMapper.selectById(item.getDoctorUserId());
            if (doctor != null && "DOCTOR".equals(doctor.getRoleType())
                    && result.stream().noneMatch(u -> u.getId().equals(doctor.getId()))) {
                result.add(doctor);
            }
        }
        return result;
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DOCTOR_PATIENT_INSIGHT, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DOCTOR_OPEN_ALERTS, allEntries = true)
        })
    public void addPatientToGroup(String doctorUsername, Long groupId, Long patientUserId) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        doctorAccessSupport.assertGroupAccessible(doctor.getId(), groupId);
        User patient = userMapper.selectById(patientUserId);
        if (patient == null || !"PATIENT".equals(patient.getRoleType())) {
            throw BusinessException.notFound("患者不存在");
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
    }

    @Override
    public List<User> listGroupPatients(String doctorUsername, Long groupId) {
        User doctor = doctorAccessSupport.requireDoctor(doctorUsername);
        doctorAccessSupport.assertGroupAccessible(doctor.getId(), groupId);
        List<DoctorGroupMember> members = doctorGroupMemberMapper.selectList(new LambdaQueryWrapper<DoctorGroupMember>()
                .eq(DoctorGroupMember::getGroupId, groupId));
        List<User> patients = new ArrayList<>();
        for (DoctorGroupMember member : members) {
            User user = userMapper.selectById(member.getPatientUserId());
            if (user != null && "PATIENT".equals(user.getRoleType())) {
                patients.add(user);
            }
        }
        return patients;
    }

}
