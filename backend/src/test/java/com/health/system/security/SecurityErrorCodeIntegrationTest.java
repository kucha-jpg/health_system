package com.health.system.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.health.system.common.GlobalExceptionHandler;
import com.health.system.config.SecurityConfig;
import com.health.system.controller.AdminUserController;
import com.health.system.controller.AuthController;
import com.health.system.mapper.AlertRuleMapper;
import com.health.system.mapper.DoctorGroupMapper;
import com.health.system.mapper.DoctorGroupMemberMapper;
import com.health.system.mapper.FeedbackMessageMapper;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.OperationLogMapper;
import com.health.system.mapper.PatientArchiveMapper;
import com.health.system.mapper.PermissionMapper;
import com.health.system.mapper.RoleMapper;
import com.health.system.mapper.SystemNoticeMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.mapper.UserRoleMapper;
import com.health.system.service.AuthService;
import com.health.system.service.OperationLogService;
import com.health.system.service.UserService;

@WebMvcTest(controllers = {AuthController.class, AdminUserController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@MockBean(classes = {
    AlertRuleMapper.class,
    DoctorGroupMapper.class,
    DoctorGroupMemberMapper.class,
    FeedbackMessageMapper.class,
    HealthAlertMapper.class,
    HealthDataMapper.class,
    OperationLogMapper.class,
    PatientArchiveMapper.class,
    PermissionMapper.class,
    RoleMapper.class,
    SystemNoticeMapper.class,
    UserMapper.class,
    UserRoleMapper.class
})
class SecurityErrorCodeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private OperationLogService operationLogService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void registerShouldReturn400WhenUsernameBlank() throws Exception {
        String invalidBody = """
            {
              "username": "",
              "password": "123456",
              "phone": "13800000000",
              "name": "bad"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void adminApiShouldReturn401WhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/admin/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminApiShouldReturn403WhenDoctorRole() throws Exception {
        mockMvc.perform(get("/api/admin/user").with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
}
