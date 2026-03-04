package com.health.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientArchiveDTO {
    private Long id;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    private String medicalHistory;
    private String medicationHistory;
    private String allergyHistory;
}
