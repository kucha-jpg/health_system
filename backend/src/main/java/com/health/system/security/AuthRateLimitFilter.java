package com.health.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.system.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String KEY_PREFIX = "auth:rate_limit:";
    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);
    private static final String STRATEGY_REDIS = "redis";
    private static final String STRATEGY_LOCAL = "local";
    private static final String STRATEGY_AUTO = "auto";

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;
    private final ConcurrentHashMap<String, RateWindow> localWindows = new ConcurrentHashMap<>();
    private final AtomicLong localLastCleanupAt = new AtomicLong(0);

    @Value("${security.auth-rate-limit.max-requests:10}")
    private int maxRequests;

    @Value("${security.auth-rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Value("${security.auth-rate-limit.strategy:auto}")
    private String strategy;

    @Value("${security.auth-rate-limit.cleanup-interval-seconds:300}")
    private long cleanupIntervalSeconds;

    public AuthRateLimitFilter(ObjectMapper objectMapper, StringRedisTemplate stringRedisTemplate) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptText(
                "local current = redis.call('INCR', KEYS[1]) " +
                "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                "return current"
        );
        this.rateLimitScript.setResultType(Long.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isProtectedAuthPost(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String route = request.getRequestURI();
        String clientIp = resolveClientIp(request);
        String key = KEY_PREFIX + route + "::" + clientIp;

        String normalizedStrategy = resolveStrategy();
        boolean blocked = switch (normalizedStrategy) {
            case STRATEGY_LOCAL -> isBlockedByLocal(key);
            case STRATEGY_REDIS -> isBlockedByRedis(key, false);
            default -> isBlockedByRedis(key, true);
        };

        if (blocked) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.fail(429, "请求过于频繁，请稍后再试")
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedAuthPost(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return LOGIN_PATH.equals(path) || REGISTER_PATH.equals(path);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveStrategy() {
        if (strategy == null || strategy.isBlank()) {
            return STRATEGY_AUTO;
        }
        String normalized = strategy.trim().toLowerCase();
        if (STRATEGY_LOCAL.equals(normalized) || STRATEGY_REDIS.equals(normalized)) {
            return normalized;
        }
        return STRATEGY_AUTO;
    }

    private boolean isBlockedByRedis(String key, boolean fallbackToLocal) {
        try {
            Long current = stringRedisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(Math.max(1L, windowSeconds))
            );
            if (current == null) {
                return fallbackToLocal && isBlockedByLocal(key);
            }
            return current > Math.max(1, maxRequests);
        } catch (Exception ex) {
            if (fallbackToLocal) {
                log.warn("Auth rate limit degraded to local mode due to Redis error: {}", ex.getMessage());
                return isBlockedByLocal(key);
            }
            // Keep fail-open behavior for explicit redis mode to avoid global auth outage.
            log.warn("Auth rate limit degraded to pass-through due to Redis error: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isBlockedByLocal(String key) {
        long now = System.currentTimeMillis();
        long windowMs = Math.max(1, windowSeconds) * 1000L;
        RateWindow counter = localWindows.computeIfAbsent(key, unused -> new RateWindow(now));

        boolean blocked;
        synchronized (counter) {
            if (now - counter.windowStartMs >= windowMs) {
                counter.windowStartMs = now;
                counter.count.set(0);
            }
            blocked = counter.count.incrementAndGet() > Math.max(1, maxRequests);
        }

        cleanupLocalWindowsIfNeeded(now, windowMs);
        return blocked;
    }

    private void cleanupLocalWindowsIfNeeded(long now, long windowMs) {
        long cleanupIntervalMs = Math.max(30, cleanupIntervalSeconds) * 1000L;
        long last = localLastCleanupAt.get();
        if (now - last < cleanupIntervalMs) {
            return;
        }
        if (!localLastCleanupAt.compareAndSet(last, now)) {
            return;
        }

        long staleBefore = now - (windowMs * 2);
        for (Map.Entry<String, RateWindow> entry : localWindows.entrySet()) {
            RateWindow win = entry.getValue();
            if (win.windowStartMs < staleBefore) {
                localWindows.remove(entry.getKey(), win);
            }
        }
    }

    private static final class RateWindow {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStartMs;

        private RateWindow(long windowStartMs) {
            this.windowStartMs = windowStartMs;
        }
    }
}
