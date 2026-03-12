package com.health.system.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityActorUtils {

    private SecurityActorUtils() {
    }

    public static RequestActor currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
        String role = auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority()
                : "UNKNOWN";
        return new RequestActor(username, role);
    }
}