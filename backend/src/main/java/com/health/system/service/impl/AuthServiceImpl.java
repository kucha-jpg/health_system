package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.dto.LoginRequest;
import com.health.system.dto.RegisterRequest;
import com.health.system.common.BusinessException;
import com.health.system.entity.Role;
import com.health.system.entity.User;
import com.health.system.entity.UserRole;
import com.health.system.mapper.RoleMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.mapper.UserRoleMapper;
import com.health.system.security.JwtUtils;
import com.health.system.service.AuthService;
import com.health.system.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final OperationLogService operationLogService;

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils,
                           RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper,
                           OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        String username = request.getUsername();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            safeSaveOperationLog(username, "ANONYMOUS", "POST", "/api/auth/login", false, "LOGIN_FAILED_BAD_CREDENTIALS");
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            safeSaveOperationLog(username, user.getRoleType(), "POST", "/api/auth/login", false, "LOGIN_FAILED_DISABLED_ACCOUNT");
            throw BusinessException.forbidden("该账号已被禁用");
        }

        Long currentVersion = user.getLoginVersion();
        long nextVersion = (currentVersion == null ? 0L : currentVersion) + 1L;
        try {
            user.setLoginVersion(nextVersion);
            userMapper.updateById(user);
        } catch (Exception ex) {
            // Backward compatibility: if login_version is not ready in DB, keep login available.
            nextVersion = currentVersion == null ? 0L : currentVersion;
            log.warn("Skip login version update for user {} due to {}", username, ex.getMessage());
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRoleType(), user.getId(), nextVersion);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("name", user.getName());
        userInfo.put("phone", user.getPhone());
        userInfo.put("roleType", user.getRoleType());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        safeSaveOperationLog(username, user.getRoleType(), "POST", "/api/auth/login", true, "LOGIN_SUCCESS");
        return result;
    }

    @Override
    public void register(RegisterRequest request) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exists != null) {
            safeSaveOperationLog(request.getUsername(), "ANONYMOUS", "POST", "/api/auth/register", false, "REGISTER_FAILED_DUPLICATE_USERNAME");
            throw BusinessException.conflict("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setName(request.getName());
        user.setRoleType("PATIENT");
        user.setStatus(1);
        user.setLoginVersion(0L);
        userMapper.insert(user);

        Role patientRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
            .eq(Role::getRoleName, "PATIENT")
            .last("limit 1"));
        if (patientRole == null) {
            throw BusinessException.notFound("系统未配置PATIENT角色");
        }
        Long patientRoleId = patientRole.getId();
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(patientRoleId);
        userRoleMapper.insert(userRole);
        safeSaveOperationLog(request.getUsername(), "PATIENT", "POST", "/api/auth/register", true, "REGISTER_SUCCESS");
    }

    private void safeSaveOperationLog(String username,
                                      String roleType,
                                      String method,
                                      String uri,
                                      boolean success,
                                      String message) {
        try {
            operationLogService.save(username, roleType, method, uri, success, message);
        } catch (Exception ex) {
            // Logging must never break authentication flow.
            log.warn("Ignore operation log write failure: username={}, uri={}, message={}", username, uri, ex.getMessage());
        }
    }
}
