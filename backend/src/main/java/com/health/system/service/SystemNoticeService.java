package com.health.system.service;

import com.health.system.dto.SystemNoticeDTO;
import com.health.system.entity.SystemNotice;

import java.util.List;

public interface SystemNoticeService {
    List<SystemNotice> listNotices(boolean includeOffline, String keyword, Integer status, String targetRole, String visibleRoleType);

    List<SystemNotice> listNoticesForUser(boolean includeOffline, String keyword, Integer status, String targetRole, String visibleRoleType, String visibleUsername);

    void createNotice(SystemNoticeDTO dto);

    void updateNotice(SystemNoticeDTO dto);

    void deleteNotice(Long id);
}
