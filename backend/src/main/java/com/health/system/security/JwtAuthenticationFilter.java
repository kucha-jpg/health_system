package com.health.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.system.common.ApiResponse;
import com.health.system.common.ErrorCode;
import com.health.system.entity.User;
import com.health.system.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtils.parseToken(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                Long loginVersion = claims.get("loginVersion", Long.class);

                User user = userMapper.selectById(claims.get("userId", Long.class));
                if (user == null) {
                    writeUnauthorized(response, "未登录或登录已过期");
                    return;
                }
                if (user.getStatus() == null || user.getStatus() != 1) {
                    writeUnauthorized(response, "该账号已被禁用");
                    return;
                }
                Long currentVersionObj = user.getLoginVersion();
                long tokenVersion = loginVersion == null ? 0L : loginVersion;
                long currentVersion = currentVersionObj == null ? 0L : currentVersionObj;
                if (tokenVersion != currentVersion) {
                    writeUnauthorized(response, "在其他地方登录");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authentication.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                writeUnauthorized(response, "未登录或登录已过期");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.code());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.UNAUTHORIZED.code(), message)));
        response.getWriter().flush();
    }
}
