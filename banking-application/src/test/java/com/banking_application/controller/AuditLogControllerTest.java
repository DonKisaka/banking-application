package com.banking_application.controller;

import com.banking_application.config.JwtAuthenticationFilter;
import com.banking_application.config.JwtService;
import com.banking_application.config.SecurityConfig;
import com.banking_application.dto.AuditLogResponseDto;
import com.banking_application.model.*;
import com.banking_application.repository.UserRepository;
import com.banking_application.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuditLogController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserRepository userRepository;

    private User adminUser;
    private User targetUser;
    private AuditLogResponseDto sampleLog;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("admin")
                .email("admin@test.com")
                .password("encoded")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        targetUser = User.builder()
                .id(2L)
                .userUuid(UUID.randomUUID())
                .username("targetuser")
                .email("target@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        sampleLog = new AuditLogResponseDto(
                1L, "targetuser", "LOGIN_SUCCESS",
                "auth", "127.0.0.1",
                AuditStatus.SUCCESS, "User logged in successfully"
        );
    }

    @Test
    void getUserAuditLogs_shouldReturn200() throws Exception {
        UUID userUuid = targetUser.getUserUuid();
        Page<AuditLogResponseDto> page = new PageImpl<>(List.of(sampleLog));

        when(userRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(targetUser));
        when(auditLogService.getUserAuditLogs(any(User.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/audit-logs/user/" + userUuid)
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action").value("LOGIN_SUCCESS"));
    }

    @Test
    void getAuditLogsByAction_shouldReturn200() throws Exception {
        Page<AuditLogResponseDto> page = new PageImpl<>(List.of(sampleLog));
        when(auditLogService.getAuditLogsByAction(anyString(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("action", "LOGIN_SUCCESS")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getRecentUserActivity_shouldReturn200() throws Exception {
        UUID userUuid = targetUser.getUserUuid();
        when(userRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(targetUser));
        when(auditLogService.getRecentUserActivity(any(User.class)))
                .thenReturn(List.of(sampleLog));

        mockMvc.perform(get("/api/admin/audit-logs/user/" + userUuid + "/recent")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("targetuser"));
    }

    @Test
    void getUserAuditLogs_shouldReturn400WhenUserNotFound() throws Exception {
        UUID unknownUuid = UUID.randomUUID();
        when(userRepository.findByUserUuid(unknownUuid)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/audit-logs/user/" + unknownUuid)
                        .with(user(adminUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("User not found"));
    }
}
