package com.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.system.entity.DoctorGroupMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface DoctorGroupMemberMapper extends BaseMapper<DoctorGroupMember> {

    @Update("UPDATE doctor_group_member SET deleted = 0 WHERE group_id = #{groupId} AND patient_user_id = #{patientUserId} AND deleted = 1")
    int restoreDeletedByGroupAndPatient(@Param("groupId") Long groupId, @Param("patientUserId") Long patientUserId);
}
