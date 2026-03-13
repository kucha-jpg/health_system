package com.health.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DoctorGroupAddDoctorDTO {
    @NotNull(message = "医生ID不能为空")
    private Long doctorUserId;
}
