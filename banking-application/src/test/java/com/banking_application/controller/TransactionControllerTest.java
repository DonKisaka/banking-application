package com.banking_application.controller;

import com.banking_application.config.JwtAuthenticationFilter;
import com.banking_application.config.JwtService;
import com.banking_application.config.SecurityConfig;
import com.banking_application.dto.*;
import com.banking_application.exception.InvalidTransactionException;
import com.banking_application.model.*;
import com.banking_application.service.TransactionService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private TransactionResponse sampleTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("testuser")
                .email("test@test.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        sampleTransaction = new TransactionResponse(
                "TXN-REF-001", new BigDecimal("500.00"), "USD",
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "Test deposit",
                null, "ACC123456789"
        );
    }

    @Test
    void deposit_shouldReturn201() throws Exception {
        DepositRequestDto request = new DepositRequestDto("ACC123456789", new BigDecimal("500.00"), "Test deposit");
        when(transactionService.deposit(any(), any())).thenReturn(sampleTransaction);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionReference").value("TXN-REF-001"))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    void withdraw_shouldReturn201() throws Exception {
        WithdrawalRequestDto request = new WithdrawalRequestDto(
                "ACC123456789", new BigDecimal("200.00"), "ATM withdrawal", "sec-token-123"
        );
        TransactionResponse withdrawalResponse = new TransactionResponse(
                "TXN-REF-002", new BigDecimal("200.00"), "USD",
                TransactionType.WITHDRAWAL, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "ATM withdrawal",
                "ACC123456789", null
        );
        when(transactionService.withdraw(any(), any())).thenReturn(withdrawalResponse);

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));
    }

    @Test
    void transfer_shouldReturn201() throws Exception {
        TransferRequestDto request = new TransferRequestDto(
                "ACC111111111", "ACC222222222", new BigDecimal("1000.00"), "Rent payment"
        );
        TransactionResponse transferResponse = new TransactionResponse(
                "TXN-REF-003", new BigDecimal("1000.00"), "USD",
                TransactionType.TRANSFER, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "Rent payment",
                "ACC111111111", "ACC222222222"
        );
        when(transactionService.transfer(any(), any())).thenReturn(transferResponse);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.sourceAccountNumber").value("ACC111111111"));
    }

    @Test
    void getTransactionHistory_shouldReturn200() throws Exception {
        when(transactionService.getTransactionHistory("ACC123456789"))
                .thenReturn(List.of(sampleTransaction));

        mockMvc.perform(get("/api/v1/transactions/account/ACC123456789")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionReference").value("TXN-REF-001"));
    }

    @Test
    void getTransactionByReference_shouldReturn200() throws Exception {
        when(transactionService.getTransactionByReference("TXN-REF-001")).thenReturn(sampleTransaction);

        mockMvc.perform(get("/api/v1/transactions/TXN-REF-001")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-REF-001"));
    }

    @Test
    void transfer_shouldReturn422WhenSameAccount() throws Exception {
        TransferRequestDto request = new TransferRequestDto(
                "ACC111111111", "ACC111111111", new BigDecimal("100.00"), "Self transfer"
        );
        when(transactionService.transfer(any(), any()))
                .thenThrow(new InvalidTransactionException("Cannot transfer to the same account"));

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Cannot transfer to the same account"));
    }
}
