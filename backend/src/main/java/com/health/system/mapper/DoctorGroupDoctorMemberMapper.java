package com.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.system.entity.DoctorGroupDoctorMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface DoctorGroupDoctorMemberMapper extends BaseMapper<DoctorGroupDoctorMember> {

    @Update("UPDATE doctor_group_doctor_member SET deleted = 0 WHERE group_id = #{groupId} AND doctor_user_id = #{doctorUserId} AND deleted = 1")
    int restoreDeletedByGroupAndDoctor(@Param("groupId") Long groupId, @Param("doctorUserId") Long doctorUserId);
}
