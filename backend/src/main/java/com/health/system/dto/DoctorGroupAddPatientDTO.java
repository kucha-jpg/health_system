package com.health.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DoctorGroupAddPatientDTO {
    @NotNull(message = "患者ID不能为空")
    private Long patientUserId;
}
