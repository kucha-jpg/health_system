package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.NoticeContentSanitizer;
import com.health.system.common.SecurityInputSanitizer;
import com.health.system.dto.SystemNoticeDTO;
import com.health.system.entity.SystemNotice;
import com.health.system.mapper.SystemNoticeMapper;
import com.health.system.service.SystemNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class SystemNoticeServiceImpl implements SystemNoticeService {

    private static final String AUDIENCE_ALL = "ALL";
    private static final Set<String> ALLOWED_AUDIENCES = Set.of(AUDIENCE_ALL, "DOCTOR", "PATIENT");

    private final SystemNoticeMapper systemNoticeMapper;

    public SystemNoticeServiceImpl(SystemNoticeMapper systemNoticeMapper) {
        this.systemNoticeMapper = systemNoticeMapper;
    }

    @Override
    public List<SystemNotice> listNotices(boolean includeOffline, String keyword, Integer status, String targetRole, String visibleRoleType) {
        String safeKeyword = SecurityInputSanitizer.sanitizeKeyword(keyword, 100, "公告关键词");
        String safeTargetRole = normalizeAudience(targetRole, false);
        String safeVisibleRoleType = normalizeVisibleRoleType(visibleRoleType);
        LambdaQueryWrapper<SystemNotice> wrapper = new LambdaQueryWrapper<SystemNotice>()
                .orderByDesc(SystemNotice::getCreateTime);

        if (StringUtils.hasText(safeKeyword)) {
            wrapper.and(w -> w.like(SystemNotice::getTitle, safeKeyword)
                    .or().like(SystemNotice::getContent, safeKeyword));
        }
        if (status != null) {
            wrapper.eq(SystemNotice::getStatus, status);
        }
        if (!includeOffline) {
            wrapper.eq(SystemNotice::getStatus, 1);
        }

        List<SystemNotice> notices = systemNoticeMapper.selectList(wrapper);
        return notices.stream()
                .filter(item -> matchesTargetRoleFilter(item, safeTargetRole))
                .filter(item -> matchesVisibleRole(item, safeVisibleRoleType))
                .toList();
    }

    @Override
    public void createNotice(SystemNoticeDTO dto) {
        SystemNotice notice = new SystemNotice();
        notice.setTitle(dto.getTitle());
        notice.setContent(NoticeContentSanitizer.sanitizeRichHtml(dto.getContent()));
        notice.setTargetRole(normalizeAudience(dto.getTargetRole(), true));
        notice.setStatus(dto.getStatus());
        systemNoticeMapper.insert(notice);
    }

    @Override
    public void updateNotice(SystemNoticeDTO dto) {
        SystemNotice exists = systemNoticeMapper.selectById(dto.getId());
        if (exists == null) {
            throw BusinessException.notFound("公告不存在");
        }
        exists.setTitle(dto.getTitle());
        exists.setContent(NoticeContentSanitizer.sanitizeRichHtml(dto.getContent()));
        exists.setTargetRole(normalizeAudience(dto.getTargetRole(), true));
        exists.setStatus(dto.getStatus());
        systemNoticeMapper.updateById(exists);
    }

    @Override
    public void deleteNotice(Long id) {
        systemNoticeMapper.deleteById(id);
    }

    private String normalizeAudience(String targetRole, boolean required) {
        if (!StringUtils.hasText(targetRole)) {
            if (required) {
                throw BusinessException.badRequest("公告投放对象不能为空");
            }
            return null;
        }
        String normalized = targetRole.trim().toUpperCase();
        if (!ALLOWED_AUDIENCES.contains(normalized)) {
            throw BusinessException.badRequest("公告投放对象非法");
        }
        return normalized;
    }

    private String normalizeVisibleRoleType(String visibleRoleType) {
        if (!StringUtils.hasText(visibleRoleType)) {
            return null;
        }
        String roleType = SecurityInputSanitizer.sanitizeRoleType(visibleRoleType);
        if ("ADMIN".equals(roleType)) {
            return null;
        }
        return roleType;
    }

    private boolean matchesTargetRoleFilter(SystemNotice item, String filterTargetRole) {
        if (!StringUtils.hasText(filterTargetRole)) {
            return true;
        }
        return filterTargetRole.equals(resolveAudience(item));
    }

    private boolean matchesVisibleRole(SystemNotice item, String visibleRoleType) {
        if (!StringUtils.hasText(visibleRoleType)) {
            return true;
        }
        String audience = resolveAudience(item);
        return AUDIENCE_ALL.equals(audience) || visibleRoleType.equals(audience);
    }

    private String resolveAudience(SystemNotice item) {
        if (item == null || !StringUtils.hasText(item.getTargetRole())) {
            return AUDIENCE_ALL;
        }
        String normalized = item.getTargetRole().trim().toUpperCase();
        if (!ALLOWED_AUDIENCES.contains(normalized)) {
            return AUDIENCE_ALL;
        }
        return normalized;
    }
}
