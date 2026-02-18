package com.banking_application.service;

import com.banking_application.dto.FraudAlertResponseDto;
import com.banking_application.mapper.FraudAlertMapper;
import com.banking_application.model.*;
import com.banking_application.repository.FraudAlertRepository;
import com.banking_application.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private FraudAlertMapper fraudAlertMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FraudDetectionService underTest;

    private User testUser;
    private Account sourceAccount;
    private Account targetAccount;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        sourceAccount = Account.builder()
                .id(1L)
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("SRC123")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("100000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        User targetUser = User.builder()
                .id(2L)
                .userUuid(UUID.randomUUID())
                .username("targetuser")
                .email("target@example.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        targetAccount = Account.builder()
                .id(2L)
                .accountUuid(UUID.randomUUID())
                .user(targetUser)
                .accountNumber("TGT456")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("5000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        transaction = Transaction.builder()
                .id(1L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-ABCD1234")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("1000.0000"))
                .currency("USD")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- evaluateTransaction tests ---

    @Test
    void evaluateTransaction_shouldReturnLowRiskForNormalTransaction() {
        // given - normal transaction under threshold
        transaction.setAmount(new BigDecimal("500.0000"));
        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(
                any(), any())).thenReturn(List.of());

        // when
        int riskScore = underTest.evaluateTransaction(transaction);

        // then
        assertThat(riskScore).isLessThan(70);
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void evaluateTransaction_shouldFlagHighValueTransaction() {
        // given - transaction over 50,000 threshold
        transaction.setAmount(new BigDecimal("75000.0000"));
        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(
                any(), any())).thenReturn(List.of());

        // when
        int riskScore = underTest.evaluateTransaction(transaction);

        // then
        assertThat(riskScore).isGreaterThanOrEqualTo(40);
    }

    @Test
    void evaluateTransaction_shouldCreateAlertWhenRiskScoreHigh() {
        // given - high value + rapid transactions = risk >= 70
        transaction.setAmount(new BigDecimal("75000.0000"));

        Transaction recentTxn1 = Transaction.builder()
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .amount(new BigDecimal("10000.0000"))
                .build();
        Transaction recentTxn2 = Transaction.builder()
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .amount(new BigDecimal("20000.0000"))
                .build();
        Transaction recentTxn3 = Transaction.builder()
                .createdAt(LocalDateTime.now().minusMinutes(8))
                .amount(new BigDecimal("15000.0000"))
                .build();

        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(
                any(), any())).thenReturn(List.of(recentTxn1, recentTxn2, recentTxn3));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(FraudAlert.builder().build());

        // when
        int riskScore = underTest.evaluateTransaction(transaction);

        // then
        assertThat(riskScore).isGreaterThanOrEqualTo(70);
        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    @Test
    void evaluateTransaction_shouldDetectRapidTransactions() {
        // given - multiple transactions in short window
        transaction.setAmount(new BigDecimal("30000.0000"));

        Transaction recent1 = Transaction.builder()
                .createdAt(LocalDateTime.now().minusMinutes(3))
                .amount(new BigDecimal("5000.0000"))
                .build();
        Transaction recent2 = Transaction.builder()
                .createdAt(LocalDateTime.now().minusMinutes(7))
                .amount(new BigDecimal("5000.0000"))
                .build();
        Transaction recent3 = Transaction.builder()
                .createdAt(LocalDateTime.now().minusMinutes(9))
                .amount(new BigDecimal("5000.0000"))
                .build();

        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(
                any(), any())).thenReturn(List.of(recent1, recent2, recent3));

        // when
        int riskScore = underTest.evaluateTransaction(transaction);

        // then
        assertThat(riskScore).isGreaterThanOrEqualTo(20);
    }

    // --- getPendingAlerts tests ---

    @Test
    void getPendingAlerts_shouldReturnPendingReviewAlerts() {
        // given
        FraudAlert alert = FraudAlert.builder()
                .id(1L)
                .transaction(transaction)
                .user(testUser)
                .riskScore(85)
                .reasonCode("HIGH_VALUE")
                .status(FraudStatus.PENDING_REVIEW)
                .build();

        FraudAlertResponseDto dto = new FraudAlertResponseDto(
                1L, "HDFC-ABCD1234", "testuser", 85, "HIGH_VALUE", FraudStatus.PENDING_REVIEW
        );

        when(fraudAlertRepository.findByStatusOrderByRiskScoreDesc(FraudStatus.PENDING_REVIEW))
                .thenReturn(List.of(alert));
        when(fraudAlertMapper.toDto(List.of(alert))).thenReturn(List.of(dto));

        // when
        List<FraudAlertResponseDto> result = underTest.getPendingAlerts();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).riskScore()).isEqualTo(85);
    }

    // --- getAlertsByRiskScore tests ---

    @Test
    void getAlertsByRiskScore_shouldReturnHighRiskAlerts() {
        // given
        FraudAlert alert = FraudAlert.builder()
                .id(1L)
                .riskScore(90)
                .reasonCode("HIGH_VALUE")
                .status(FraudStatus.PENDING_REVIEW)
                .build();

        FraudAlertResponseDto dto = new FraudAlertResponseDto(
                1L, "HDFC-ABCD1234", "testuser", 90, "HIGH_VALUE", FraudStatus.PENDING_REVIEW
        );

        when(fraudAlertRepository.findByRiskScoreGreaterThanEqual(80)).thenReturn(List.of(alert));
        when(fraudAlertMapper.toDto(List.of(alert))).thenReturn(List.of(dto));

        // when
        List<FraudAlertResponseDto> result = underTest.getAlertsByRiskScore(80);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).riskScore()).isEqualTo(90);
    }

    // --- resolveAlert tests ---

    @Test
    void resolveAlert_shouldUpdateAlertStatus() {
        // given
        FraudAlert alert = FraudAlert.builder()
                .id(1L)
                .transaction(transaction)
                .user(testUser)
                .riskScore(85)
                .reasonCode("HIGH_VALUE")
                .status(FraudStatus.PENDING_REVIEW)
                .build();

        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        FraudAlertResponseDto dto = new FraudAlertResponseDto(
                1L, "HDFC-ABCD1234", "testuser", 85, "HIGH_VALUE", FraudStatus.RESOLVED
        );
        when(fraudAlertMapper.toDto(any(FraudAlert.class))).thenReturn(dto);

        // when
        FraudAlertResponseDto result = underTest.resolveAlert(1L, "Verified legitimate", "Cleared", FraudStatus.RESOLVED);

        // then
        assertThat(alert.getStatus()).isEqualTo(FraudStatus.RESOLVED);
        assertThat(alert.getAdminRemarks()).isEqualTo("Verified legitimate");
        assertThat(alert.getActionTaken()).isEqualTo("Cleared");
        assertThat(alert.getResolvedAt()).isNotNull();
        verify(fraudAlertRepository).save(alert);
    }

    @Test
    void resolveAlert_shouldThrowWhenAlertNotFound() {
        // given
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.resolveAlert(999L, "remarks", "action", FraudStatus.DISMISSED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fraud alert not found");
    }

    // --- getUserFraudHistory tests ---

    @Test
    void getUserFraudHistory_shouldReturnPaginatedResults() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        FraudAlert alert = FraudAlert.builder()
                .id(1L)
                .user(testUser)
                .riskScore(75)
                .status(FraudStatus.RESOLVED)
                .build();

        FraudAlertResponseDto dto = new FraudAlertResponseDto(
                1L, "HDFC-ABCD1234", "testuser", 75, "HIGH_VALUE", FraudStatus.RESOLVED
        );

        Page<FraudAlert> page = new PageImpl<>(List.of(alert));
        when(fraudAlertRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(page);
        when(fraudAlertMapper.toDto(any(FraudAlert.class))).thenReturn(dto);

        // when
        Page<FraudAlertResponseDto> result = underTest.getUserFraudHistory(testUser, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    // --- hasUnresolvedAlerts tests ---

    @Test
    void hasUnresolvedAlerts_shouldReturnTrueWhenPendingExists() {
        // given
        when(fraudAlertRepository.existsByUserAndStatus(testUser, FraudStatus.PENDING_REVIEW)).thenReturn(true);

        // when
        boolean result = underTest.hasUnresolvedAlerts(testUser);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasUnresolvedAlerts_shouldReturnFalseWhenNoPending() {
        // given
        when(fraudAlertRepository.existsByUserAndStatus(testUser, FraudStatus.PENDING_REVIEW)).thenReturn(false);

        // when
        boolean result = underTest.hasUnresolvedAlerts(testUser);

        // then
        assertThat(result).isFalse();
    }
}
