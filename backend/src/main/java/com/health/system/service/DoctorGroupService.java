package com.health.system.service;

import com.health.system.dto.DoctorGroupDTO;
import com.health.system.entity.DoctorGroup;
import com.health.system.entity.User;

import java.util.List;

public interface DoctorGroupService {
    void createGroup(String doctorUsername, DoctorGroupDTO dto);

    List<DoctorGroup> listMyGroups(String doctorUsername);

    void addPatientToGroup(String doctorUsername, Long groupId, Long patientUserId);

    List<User> listGroupPatients(String doctorUsername, Long groupId);
}
