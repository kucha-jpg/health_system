package com.health.system.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.dto.DoctorGroupDTO;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.DoctorGroupService;

@Service
public class DoctorGroupServiceImpl implements DoctorGroupService {

    private final DoctorGroupMapper doctorGroupMapper;
    private final DoctorGroupMemberMapper doctorGroupMemberMapper;
    private final UserMapper userMapper;

    public DoctorGroupServiceImpl(DoctorGroupMapper doctorGroupMapper,
                                  DoctorGroupMemberMapper doctorGroupMemberMapper,
                                  UserMapper userMapper) {
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void createGroup(String doctorUsername, DoctorGroupDTO dto) {
        User doctor = getDoctor(doctorUsername);
        DoctorGroup group = new DoctorGroup();
        group.setDoctorId(doctor.getId());
        group.setGroupName(dto.getGroupName());
        group.setDescription(dto.getDescription());
        doctorGroupMapper.insert(group);
    }

    @Override
    public List<DoctorGroup> listMyGroups(String doctorUsername) {
        User doctor = getDoctor(doctorUsername);
        return doctorGroupMapper.selectList(new LambdaQueryWrapper<DoctorGroup>()
                .eq(DoctorGroup::getDoctorId, doctor.getId())
                .orderByDesc(DoctorGroup::getCreateTime));
    }

    @Override
    public void addPatientToGroup(String doctorUsername, Long groupId, Long patientUserId) {
        User doctor = getDoctor(doctorUsername);
        DoctorGroup group = doctorGroupMapper.selectById(groupId);
        if (group == null || !doctor.getId().equals(group.getDoctorId())) {
            throw BusinessException.forbidden("群组不存在或无权限");
        }
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
        User doctor = getDoctor(doctorUsername);
        DoctorGroup group = doctorGroupMapper.selectById(groupId);
        if (group == null || !doctor.getId().equals(group.getDoctorId())) {
            throw BusinessException.forbidden("群组不存在或无权限");
        }
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

    private User getDoctor(String username) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (doctor == null || !"DOCTOR".equals(doctor.getRoleType())) {
            throw BusinessException.notFound("医生不存在");
        }
        return doctor;
    }
}
