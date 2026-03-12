package com.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackCreateDTO {

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 500, message = "反馈内容不能超过500字")
    private String content;
}
