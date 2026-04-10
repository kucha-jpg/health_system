package com.health.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthRateLimitFilterTest {

    private StringRedisTemplate stringRedisTemplate;
    private AuthRateLimitFilter filter;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        filter = new AuthRateLimitFilter(new ObjectMapper(), stringRedisTemplate);

        ReflectionTestUtils.setField(filter, "maxRequests", 2);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60L);
        ReflectionTestUtils.setField(filter, "cleanupIntervalSeconds", 300L);
    }

    @Test
    void shouldBlockWhenRedisCounterExceedsLimit() throws Exception {
        ReflectionTestUtils.setField(filter, "strategy", "redis");
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(3L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldFallbackToLocalWhenAutoAndRedisFails() throws Exception {
        ReflectionTestUtils.setField(filter, "strategy", "auto");
        ReflectionTestUtils.setField(filter, "maxRequests", 1);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RuntimeException("redis down"));

        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> null).when(chain).doFilter(any(), any());

        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/auth/login");
        first.setRemoteAddr("127.0.0.2");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, chain);

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/auth/login");
        second.setRemoteAddr("127.0.0.2");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, chain);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void shouldIgnoreNonProtectedEndpoint() throws Exception {
        ReflectionTestUtils.setField(filter, "strategy", "redis");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/user");
        request.setRemoteAddr("127.0.0.3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(any(), any());
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), anyString());
    }
}
