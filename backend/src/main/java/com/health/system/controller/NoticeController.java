package com.health.system.controller;

import com.health.system.common.ApiResponse;
import com.health.system.entity.SystemNotice;
import com.health.system.service.SystemNoticeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final SystemNoticeService systemNoticeService;

    public NoticeController(SystemNoticeService systemNoticeService) {
        this.systemNoticeService = systemNoticeService;
    }

    @GetMapping
    public ApiResponse<List<SystemNotice>> listOnline(Authentication authentication) {
        String roleType = resolveRoleType(authentication);
        String username = authentication != null ? authentication.getName() : null;
        return ApiResponse.success(systemNoticeService.listNoticesForUser(false, null, null, null, roleType, username));
    }

    private String resolveRoleType(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority == null ? null : authority.getAuthority();
            if ("ROLE_DOCTOR".equals(value)) {
                return "DOCTOR";
            }
            if ("ROLE_PATIENT".equals(value)) {
                return "PATIENT";
            }
            if ("ROLE_ADMIN".equals(value)) {
                return "ADMIN";
            }
        }
        return null;
    }
}
