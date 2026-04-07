package com.health.system.service.support;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupDoctorMember;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupDoctorMemberMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.UserMapper;

@Component
public class DoctorAccessSupport {

    private final UserMapper userMapper;
    private final DoctorGroupMapper doctorGroupMapper;
    private final DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper;
    private final DoctorGroupMemberMapper doctorGroupMemberMapper;

    public DoctorAccessSupport(UserMapper userMapper,
                               DoctorGroupMapper doctorGroupMapper,
                               DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper,
                               DoctorGroupMemberMapper doctorGroupMemberMapper) {
        this.userMapper = userMapper;
        this.doctorGroupMapper = doctorGroupMapper;
        this.doctorGroupDoctorMemberMapper = doctorGroupDoctorMemberMapper;
        this.doctorGroupMemberMapper = doctorGroupMemberMapper;
    }

    public User requireDoctor(String username) {
        User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (doctor == null || !"DOCTOR".equals(doctor.getRoleType())) {
            throw BusinessException.notFound("医生不存在");
        }
        return doctor;
    }

    public void assertGroupAccessible(Long doctorId, Long groupId) {
        DoctorGroup group = doctorGroupMapper.selectById(groupId);
        if (group == null) {
            throw BusinessException.notFound("群组不存在");
        }
        if (doctorId.equals(group.getDoctorId())) {
            return;
        }
        Long count = doctorGroupDoctorMemberMapper.selectCount(new LambdaQueryWrapper<DoctorGroupDoctorMember>()
                .eq(DoctorGroupDoctorMember::getGroupId, groupId)
                .eq(DoctorGroupDoctorMember::getDoctorUserId, doctorId));
        if (count == null || count == 0) {
            throw BusinessException.forbidden("群组不存在或无权限");
        }
    }

    public void assertPatientAccessible(Long doctorId, Long patientUserId, String forbiddenMessage) {
        Set<Long> patientIds = resolveAccessiblePatientIds(doctorId);
        if (!patientIds.contains(patientUserId)) {
            throw BusinessException.forbidden(forbiddenMessage);
        }
    }

    public Set<Long> resolveAccessiblePatientIds(Long doctorId) {
        Set<Long> groupIds = resolveAccessibleGroupIds(doctorId);
        if (groupIds.isEmpty()) {
            return Set.of();
        }
        List<DoctorGroupMember> patientMembers = doctorGroupMemberMapper.selectList(new LambdaQueryWrapper<DoctorGroupMember>()
                .in(DoctorGroupMember::getGroupId, groupIds));
        if (patientMembers.isEmpty()) {
            return Set.of();
        }
        Set<Long> patientIds = new HashSet<>();
        for (DoctorGroupMember item : patientMembers) {
            patientIds.add(item.getPatientUserId());
        }
        return patientIds;
    }

    public Set<Long> resolveAccessibleGroupIds(Long doctorId) {
        List<DoctorGroup> ownerGroups = doctorGroupMapper.selectList(new LambdaQueryWrapper<DoctorGroup>()
                .eq(DoctorGroup::getDoctorId, doctorId));
        List<DoctorGroupDoctorMember> collaborativeGroups = doctorGroupDoctorMemberMapper.selectList(
                new LambdaQueryWrapper<DoctorGroupDoctorMember>()
                        .eq(DoctorGroupDoctorMember::getDoctorUserId, doctorId)
        );

        Set<Long> groupIds = new HashSet<>();
        for (DoctorGroup item : ownerGroups) {
            groupIds.add(item.getId());
        }
        for (DoctorGroupDoctorMember item : collaborativeGroups) {
            groupIds.add(item.getGroupId());
        }
        return groupIds;
    }
}
