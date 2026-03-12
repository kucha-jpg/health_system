package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.dto.LoginRequest;
import com.health.system.dto.RegisterRequest;
import com.health.system.common.BusinessException;
import com.health.system.entity.User;
import com.health.system.entity.UserRole;
import com.health.system.mapper.RoleMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.mapper.UserRoleMapper;
import com.health.system.security.JwtUtils;
import com.health.system.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils,
                           RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw BusinessException.forbidden("该账号已被禁用");
        }

        Long currentVersion = user.getLoginVersion();
        long nextVersion = (currentVersion == null ? 0L : currentVersion) + 1L;
        user.setLoginVersion(nextVersion);
        userMapper.updateById(user);

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
        return result;
    }

    @Override
    public void register(RegisterRequest request) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exists != null) {
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

        Long patientRoleId = roleMapper.selectOne(new LambdaQueryWrapper<com.health.system.entity.Role>()
                .eq(com.health.system.entity.Role::getRoleName, "PATIENT")).getId();
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(patientRoleId);
        userRoleMapper.insert(userRole);
    }
}
