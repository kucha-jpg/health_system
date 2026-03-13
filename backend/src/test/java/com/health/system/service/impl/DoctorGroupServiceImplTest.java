package com.health.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupDoctorMember;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.User;
import com.health.system.mapper.DoctorGroupDoctorMemberMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class DoctorGroupServiceImplTest {

    @Mock
    private DoctorGroupMapper doctorGroupMapper;
    @Mock
    private DoctorGroupDoctorMemberMapper doctorGroupDoctorMemberMapper;
    @Mock
    private DoctorGroupMemberMapper doctorGroupMemberMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private DoctorGroupServiceImpl doctorGroupService;

    @Test
    void listMyGroups_shouldReturnOwnedAndCollaborativeGroups() {
        User doctor = doctorUser(1L, "doc-a");
        when(userMapper.selectOne(any())).thenReturn(doctor);

        DoctorGroup owned = new DoctorGroup();
        owned.setId(10L);
        owned.setDoctorId(1L);
        DoctorGroup collaborative = new DoctorGroup();
        collaborative.setId(20L);
        collaborative.setDoctorId(2L);

        DoctorGroupDoctorMember member = new DoctorGroupDoctorMember();
        member.setGroupId(20L);
        member.setDoctorUserId(1L);

        when(doctorGroupMapper.selectList(any())).thenReturn(List.of(owned));
        when(doctorGroupDoctorMemberMapper.selectList(any())).thenReturn(List.of(member));
        when(doctorGroupMapper.selectBatchIds(any())).thenReturn(List.of(collaborative));

        List<DoctorGroup> groups = doctorGroupService.listMyGroups("doc-a");

        assertEquals(2, groups.size());
        assertTrue(groups.stream().anyMatch(g -> Long.valueOf(10L).equals(g.getId())));
        assertTrue(groups.stream().anyMatch(g -> Long.valueOf(20L).equals(g.getId())));
    }

    @Test
    void listGroupPatients_shouldAllowCollaboratorDoctor() {
        User doctor = doctorUser(1L, "doc-a");
        when(userMapper.selectOne(any())).thenReturn(doctor);

        DoctorGroup group = new DoctorGroup();
        group.setId(10L);
        group.setDoctorId(2L);
        when(doctorGroupMapper.selectById(10L)).thenReturn(group);
        when(doctorGroupDoctorMemberMapper.selectCount(any())).thenReturn(1L);

        DoctorGroupMember patientMember = new DoctorGroupMember();
        patientMember.setGroupId(10L);
        patientMember.setPatientUserId(99L);
        when(doctorGroupMemberMapper.selectList(any())).thenReturn(List.of(patientMember));

        User patient = new User();
        patient.setId(99L);
        patient.setRoleType("PATIENT");
        patient.setUsername("patient-a");
        when(userMapper.selectById(99L)).thenReturn(patient);

        List<User> result = doctorGroupService.listGroupPatients("doc-a", 10L);

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).getId());
    }

    @Test
    void addDoctorToGroup_shouldRejectNonOwner() {
        User operator = doctorUser(1L, "doc-a");
        when(userMapper.selectOne(any())).thenReturn(operator);

        DoctorGroup group = new DoctorGroup();
        group.setId(10L);
        group.setDoctorId(2L);
        when(doctorGroupMapper.selectById(10L)).thenReturn(group);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> doctorGroupService.addDoctorToGroup("doc-a", 10L, 3L));

        assertEquals("仅群组创建者可维护协作医生", ex.getMessage());
        verify(doctorGroupDoctorMemberMapper, never()).insert(any());
    }

    private User doctorUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoleType("DOCTOR");
        return user;
    }
}
