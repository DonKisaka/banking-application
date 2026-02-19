package com.banking_application.controller;

import com.banking_application.config.JwtAuthenticationFilter;
import com.banking_application.config.JwtService;
import com.banking_application.config.SecurityConfig;
import com.banking_application.dto.FraudAlertResponseDto;
import com.banking_application.dto.ResolveAlertRequestDto;
import com.banking_application.model.*;
import com.banking_application.repository.UserRepository;
import com.banking_application.service.FraudDetectionService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = FraudAlertController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class FraudAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FraudDetectionService fraudDetectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserRepository userRepository;

    private User adminUser;
    private User targetUser;
    private FraudAlertResponseDto sampleAlert;

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
                .username("suspect")
                .email("suspect@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        sampleAlert = new FraudAlertResponseDto(
                1L, "TXN-REF-001", "suspect",
                85, "HIGH_VALUE;RAPID_TRANSACTIONS;",
                FraudStatus.PENDING_REVIEW
        );
    }

    @Test
    void getPendingAlerts_shouldReturn200() throws Exception {
        when(fraudDetectionService.getPendingAlerts()).thenReturn(List.of(sampleAlert));

        mockMvc.perform(get("/api/admin/fraud-alerts/pending")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].riskScore").value(85));
    }

    @Test
    void getAlertsByRiskScore_shouldReturn200() throws Exception {
        when(fraudDetectionService.getAlertsByRiskScore(70)).thenReturn(List.of(sampleAlert));

        mockMvc.perform(get("/api/admin/fraud-alerts")
                        .param("minScore", "70")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void resolveAlert_shouldReturn200() throws Exception {
        ResolveAlertRequestDto request = new ResolveAlertRequestDto(
                "Verified as legitimate", "No action needed", FraudStatus.DISMISSED
        );
        FraudAlertResponseDto resolved = new FraudAlertResponseDto(
                1L, "TXN-REF-001", "suspect", 85,
                "HIGH_VALUE;RAPID_TRANSACTIONS;", FraudStatus.DISMISSED
        );
        when(fraudDetectionService.resolveAlert(eq(1L), anyString(), anyString(), any(FraudStatus.class)))
                .thenReturn(resolved);

        mockMvc.perform(put("/api/admin/fraud-alerts/1/resolve")
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test
    void getUserFraudHistory_shouldReturn200() throws Exception {
        UUID userUuid = targetUser.getUserUuid();
        Page<FraudAlertResponseDto> page = new PageImpl<>(List.of(sampleAlert));

        when(userRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(targetUser));
        when(fraudDetectionService.getUserFraudHistory(any(User.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/fraud-alerts/user/" + userUuid)
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void hasUnresolvedAlerts_shouldReturn200() throws Exception {
        UUID userUuid = targetUser.getUserUuid();
        when(userRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(targetUser));
        when(fraudDetectionService.hasUnresolvedAlerts(any(User.class))).thenReturn(true);

        mockMvc.perform(get("/api/admin/fraud-alerts/user/" + userUuid + "/unresolved")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnresolvedAlerts").value(true));
    }
}
