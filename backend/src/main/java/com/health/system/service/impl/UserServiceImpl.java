package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.util.List;

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
    public List<User> listUsers(String keyword, String roleType, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .ne(User::getRoleType, "ADMIN")
                .orderByDesc(User::getCreateTime);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (StringUtils.hasText(roleType)) {
            wrapper.eq(User::getRoleType, roleType);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        return userMapper.selectList(wrapper);
    }

    @Override
    public void createUser(UserDTO dto) {
        if ("ADMIN".equals(dto.getRoleType())) {
            throw new RuntimeException("不支持创建管理员账号");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword() == null ? "123456" : dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setName(dto.getName());
        user.setRoleType(dto.getRoleType());
        user.setStatus(dto.getStatus());
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
            throw new RuntimeException("用户不存在");
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
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }
}
