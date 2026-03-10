package com.health.system.config;

import com.health.system.service.OperationLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OperationLogFilter extends OncePerRequestFilter {

    private final OperationLogService operationLogService;

    public OperationLogFilter(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        filterChain.doFilter(request, response);

        if (!uri.startsWith("/api/") || "GET".equalsIgnoreCase(method)) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
        String role = auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority()
                : "UNKNOWN";
        boolean success = response.getStatus() < 400;
        operationLogService.save(username, role, method, uri, success, "HTTP " + response.getStatus());
    }
}
