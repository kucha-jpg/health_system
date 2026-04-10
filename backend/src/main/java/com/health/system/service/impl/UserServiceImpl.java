package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.system.common.BusinessException;
import com.health.system.common.SecurityInputSanitizer;
import com.health.system.dto.UserDTO;
import com.health.system.entity.Role;
import com.health.system.entity.User;
import com.health.system.entity.UserRole;
import com.health.system.mapper.RoleMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.mapper.UserRoleMapper;
import com.health.system.service.UserService;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Map<String, Object> listUsers(String keyword, String roleType, Integer status, Integer pageNo, Integer pageSize) {
        String safeKeyword = SecurityInputSanitizer.sanitizeKeyword(keyword, 64, "账号查询关键词");
        String safeRoleType = SecurityInputSanitizer.sanitizeRoleType(roleType);
        int safePageNo = Math.min(Math.max(pageNo == null ? 1 : pageNo, 1), 1000);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .ne(User::getRoleType, "ADMIN")
                .orderByDesc(User::getCreateTime);

        if (StringUtils.hasText(safeKeyword)) {
            wrapper.and(w -> w.like(User::getUsername, safeKeyword)
                    .or().like(User::getName, safeKeyword)
                    .or().like(User::getPhone, safeKeyword));
        }
        if (StringUtils.hasText(safeRoleType)) {
            wrapper.eq(User::getRoleType, safeRoleType);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        Page<User> page = userMapper.selectPage(new Page<>(safePageNo, safePageSize), wrapper);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNo", safePageNo);
        result.put("pageSize", safePageSize);
        return result;
    }

    @Override
    public void createUser(UserDTO dto) {
        if ("ADMIN".equals(dto.getRoleType())) {
            throw BusinessException.badRequest("不支持创建管理员账号");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) != null) {
            throw BusinessException.conflict("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword() == null ? "123456" : dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setName(dto.getName());
        user.setRoleType(dto.getRoleType());
        user.setStatus(dto.getStatus());
        user.setLoginVersion(0L);
        userMapper.insert(user);

        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleName, dto.getRoleType()));
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    @Override
    public void updateUser(UserDTO dto) {
        User user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setRoleType(dto.getRoleType());
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);

        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleName, dto.getRoleType()));
        UserRole userRole = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        if (userRole != null) {
            userRole.setRoleId(role.getId());
            userRoleMapper.updateById(userRole);
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setStatus(status);
        if (status != null && status != 1) {
            Long currentVersionObj = user.getLoginVersion();
            long currentVersion = currentVersionObj == null ? 0L : currentVersionObj;
            user.setLoginVersion(currentVersion + 1L);
        }
        userMapper.updateById(user);
    }
}
