package com.health.system.common;

import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

public final class SecurityInputSanitizer {

    private static final Pattern SQLI_PATTERN = Pattern.compile("(--|/\\*|\\*/|;|\\b(select|union|drop|insert|update|delete|truncate|alter)\\b)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> ROLE_TYPES = Set.of("ADMIN", "DOCTOR", "PATIENT");

    private SecurityInputSanitizer() {
    }

    public static String sanitizeKeyword(String keyword, int maxLength, String fieldName) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > maxLength) {
            throw BusinessException.badRequest(fieldName + "长度超限");
        }
        if (SQLI_PATTERN.matcher(normalized).find()) {
            throw BusinessException.badRequest(fieldName + "包含非法字符");
        }
        return normalized;
    }

    public static String sanitizeRoleType(String roleType) {
        if (!StringUtils.hasText(roleType)) {
            return null;
        }
        String normalized = roleType.trim().toUpperCase();
        if (!ROLE_TYPES.contains(normalized)) {
            throw BusinessException.badRequest("角色类型非法");
        }
        return normalized;
    }
}
