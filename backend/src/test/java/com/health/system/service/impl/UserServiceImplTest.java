package com.health.system.service.impl;

import com.health.system.dto.UserDTO;
import com.health.system.entity.Role;
import com.health.system.entity.User;
import com.health.system.entity.UserRole;
import com.health.system.mapper.RoleMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<UserRole> userRoleCaptor;

    @Test
    void createUser_shouldCreateUserAndRoleRelation() {
        UserDTO dto = new UserDTO();
        dto.setUsername("doctor01");
        dto.setPassword("123456");
        dto.setPhone("13800001111");
        dto.setName("张医生");
        dto.setRoleType("DOCTOR");
        dto.setStatus(1);

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("ENCODED_PASSWORD");

        Role role = new Role();
        role.setId(2L);
        when(roleMapper.selectOne(any())).thenReturn(role);

        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        userService.createUser(dto);

        verify(userMapper).insert(userCaptor.capture());
        User created = userCaptor.getValue();
        assertEquals("doctor01", created.getUsername());
        assertEquals("ENCODED_PASSWORD", created.getPassword());
        assertEquals("DOCTOR", created.getRoleType());

        verify(userRoleMapper).insert(userRoleCaptor.capture());
        UserRole userRole = userRoleCaptor.getValue();
        assertEquals(10L, userRole.getUserId());
        assertEquals(2L, userRole.getRoleId());
    }

    @Test
    void createUser_shouldThrowWhenRoleIsAdmin() {
        UserDTO dto = new UserDTO();
        dto.setRoleType("ADMIN");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.createUser(dto));
        assertEquals("不支持创建管理员账号", ex.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }
}
