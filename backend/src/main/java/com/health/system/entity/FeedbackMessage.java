package com.health.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("feedback_message")
public class FeedbackMessage extends BaseEntity {
    private Long senderUserId;
    private String senderUsername;
    private String senderRoleType;
    private String content;
    private Integer status;
    private String replyContent;
    private LocalDateTime repliedTime;
    private Integer replyRead;
    private LocalDateTime replyReadTime;
}
