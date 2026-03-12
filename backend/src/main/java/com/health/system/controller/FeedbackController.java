package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.dto.FeedbackCreateDTO;
import com.health.system.entity.FeedbackMessage;
import com.health.system.service.FeedbackMessageService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackMessageService feedbackMessageService;

    public FeedbackController(FeedbackMessageService feedbackMessageService) {
        this.feedbackMessageService = feedbackMessageService;
    }

    @PostMapping
    public ApiResponse<Void> create(Authentication authentication, @Valid @RequestBody FeedbackCreateDTO dto) {
        feedbackMessageService.createFeedback(authentication.getName(), dto);
        return ApiResponse.success("反馈已提交", null);
    }

    @GetMapping("/mine")
    public ApiResponse<List<FeedbackMessage>> mine(Authentication authentication) {
        return ApiResponse.success(feedbackMessageService.listMine(authentication.getName()));
    }

    @GetMapping("/mine/page")
    public ApiResponse<Map<String, Object>> minePage(Authentication authentication,
                                                     @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                     @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                     @RequestParam(defaultValue = "1") Integer pageNo,
                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(feedbackMessageService.listMinePaged(authentication.getName(), startTime, endTime, pageNo, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(Authentication authentication) {
        return ApiResponse.success(feedbackMessageService.countUnreadReply(authentication.getName()));
    }

    @PostMapping("/mark-read")
    public ApiResponse<Void> markRead(Authentication authentication) {
        feedbackMessageService.markMineReplyRead(authentication.getName());
        return ApiResponse.success("已标记已读", null);
    }
}
