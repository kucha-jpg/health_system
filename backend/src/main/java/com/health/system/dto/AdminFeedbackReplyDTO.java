package com.health.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminFeedbackReplyDTO {

    @NotNull(message = "反馈ID不能为空")
    private Long id;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容不能超过500字")
    private String replyContent;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
