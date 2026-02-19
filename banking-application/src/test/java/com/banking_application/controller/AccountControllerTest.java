package com.banking_application.controller;

import com.banking_application.config.JwtAuthenticationFilter;
import com.banking_application.config.JwtService;
import com.banking_application.config.SecurityConfig;
import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.CreateAccountRequestDto;
import com.banking_application.model.*;
import com.banking_application.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AccountController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User customerUser;
    private AccountResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        customerUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("testcustomer")
                .email("customer@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        sampleResponse = new AccountResponseDto(
                UUID.randomUUID(), "ACC123456789",
                new BigDecimal("5000.00"), "USD",
                AccountStatus.ACTIVE, AccountType.SAVINGS
        );
    }

    @Test
    void createAccount_shouldReturn201WhenValid() throws Exception {
        CreateAccountRequestDto request = new CreateAccountRequestDto(
                AccountType.SAVINGS, new BigDecimal("1000.00"), "USD"
        );
        when(accountService.createAccount(any(), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/accounts")
                        .with(user(customerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC123456789"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }

    @Test
    void getUserAccounts_shouldReturn200WithAccountList() throws Exception {
        when(accountService.getUserAccounts(any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/accounts")
                        .with(user(customerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountNumber").value("ACC123456789"));
    }

    @Test
    void getAccountDetails_shouldReturn200WhenFound() throws Exception {
        when(accountService.getAccountDetails("ACC123456789")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/accounts/ACC123456789")
                        .with(user(customerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5000.00));
    }

    @Test
    void getAccountDetails_shouldReturn400WhenNotFound() throws Exception {
        when(accountService.getAccountDetails("NONEXISTENT"))
                .thenThrow(new IllegalArgumentException("Account not found!"));

        mockMvc.perform(get("/api/v1/accounts/NONEXISTENT")
                        .with(user(customerUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Account not found!"));
    }

    @Test
    void freezeAccount_shouldReturn200() throws Exception {
        AccountResponseDto frozenResponse = new AccountResponseDto(
                sampleResponse.accountUuid(), sampleResponse.accountNumber(),
                sampleResponse.balance(), sampleResponse.currency(),
                AccountStatus.FROZEN, sampleResponse.accountType()
        );
        when(accountService.freezeAccount("ACC123456789")).thenReturn(frozenResponse);

        mockMvc.perform(patch("/api/v1/accounts/ACC123456789/freeze")
                        .with(user(customerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("FROZEN"));
    }

    @Test
    void closeAccount_shouldReturn200() throws Exception {
        AccountResponseDto closedResponse = new AccountResponseDto(
                sampleResponse.accountUuid(), sampleResponse.accountNumber(),
                BigDecimal.ZERO, sampleResponse.currency(),
                AccountStatus.CLOSED, sampleResponse.accountType()
        );
        when(accountService.closeAccount("ACC123456789")).thenReturn(closedResponse);

        mockMvc.perform(patch("/api/v1/accounts/ACC123456789/close")
                        .with(user(customerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("CLOSED"));
    }

    @Test
    void reactivateAccount_shouldReturn200() throws Exception {
        when(accountService.reactivateAccount("ACC123456789")).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/accounts/ACC123456789/reactivate")
                        .with(user(customerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }
}
