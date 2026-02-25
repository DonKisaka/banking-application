package com.banking_application.service;

import com.banking_application.dto.FraudAlertResponseDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.mapper.FraudAlertMapper;
import com.banking_application.model.*;
import com.banking_application.repository.FraudAlertRepository;
import com.banking_application.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private FraudAlertMapper fraudAlertMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Captor
    private ArgumentCaptor<FraudAlert> fraudAlertCaptor;

    // Given-When-Then + Mocking + ArgumentCaptor + Verification
    @Test
    void givenHighRiskTransaction_whenEvaluateTransaction_thenAlertPersistedAndRiskScoreReturned() {
        // Given
        User sourceUser = User.builder().id(1L).username("alice").build();
        User targetUser = User.builder().id(2L).username("bob").build();

        Account source = new Account();
        source.setUser(sourceUser);
        Account target = new Account();
        target.setUser(targetUser);

        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("100000"))
                .transactionType(TransactionType.TRANSFER)
                .sourceAccount(source)
                .targetAccount(target)
                .build();

        Transaction recent1 = Transaction.builder()
                .amount(new BigDecimal("10"))
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        Transaction recent2 = Transaction.builder()
                .amount(new BigDecimal("20"))
                .createdAt(LocalDateTime.now().minusMinutes(4))
                .build();
        Transaction recent3 = Transaction.builder()
                .amount(new BigDecimal("30"))
                .createdAt(LocalDateTime.now().minusMinutes(3))
                .build();

        given(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(source, source))
                .willReturn(List.of(recent1, recent2, recent3));

        // When
        int riskScore = fraudDetectionService.evaluateTransaction(transaction);

        // Then
        assertThat(riskScore).isGreaterThanOrEqualTo(70);

        then(fraudAlertRepository).should().save(fraudAlertCaptor.capture());
        FraudAlert saved = fraudAlertCaptor.getValue();

        assertThat(saved.getTransaction()).isEqualTo(transaction);
        assertThat(saved.getUser()).isEqualTo(transaction.getInitiatedBy());
        assertThat(saved.getRiskScore()).isEqualTo(riskScore);
        assertThat(saved.getStatus()).isEqualTo(FraudStatus.PENDING_REVIEW);
        assertThat(saved.getReasonCode()).isNotBlank();
    }

    // Given-When-Then + Verification for low-risk transaction
    @Test
    void givenLowRiskTransaction_whenEvaluateTransaction_thenNoAlertPersisted() {
        // Given
        Transaction transaction = Transaction.builder()
                .amount(new BigDecimal("10"))
                .transactionType(TransactionType.DEPOSIT)
                .build();

        // When
        int riskScore = fraudDetectionService.evaluateTransaction(transaction);

        // Then
        assertThat(riskScore).isLessThan(70);
        then(transactionRepository).shouldHaveNoInteractions();
        then(fraudAlertRepository).should(never()).save(any(FraudAlert.class));
    }

    // Given-When-Then for resolving alert
    @Test
    void givenExistingAlert_whenResolveAlert_thenUpdatedAndReturned() {
        // Given
        Long alertId = 1L;
        FraudAlert alert = FraudAlert.builder()
                .id(alertId)
                .status(FraudStatus.PENDING_REVIEW)
                .build();

        given(fraudAlertRepository.findById(alertId))
                .willReturn(Optional.of(alert));

        FraudAlert saved = FraudAlert.builder()
                .id(alertId)
                .status(FraudStatus.RESOLVED)
                .adminRemarks("checked")
                .actionTaken("allow")
                .build();

        given(fraudAlertRepository.save(alert)).willReturn(saved);

        FraudAlertResponseDto dto = new FraudAlertResponseDto(
                alertId, "REF123", "alice", 80, "HIGH_VALUE", FraudStatus.RESOLVED
        );
        given(fraudAlertMapper.toDto(saved)).willReturn(dto);

        // When
        FraudAlertResponseDto result = fraudDetectionService.resolveAlert(
                alertId, "checked", "allow", FraudStatus.RESOLVED);

        // Then
        then(fraudAlertRepository).should().findById(alertId);
        then(fraudAlertRepository).should().save(alert);
        then(fraudAlertMapper).should().toDto(saved);

        assertThat(result).isEqualTo(dto);
    }

    // Exception Testing for resolving non-existent alert
    @Test
    void givenMissingAlert_whenResolveAlert_thenThrowsResourceNotFoundException() {
        // Given
        Long alertId = 42L;
        given(fraudAlertRepository.findById(alertId)).willReturn(Optional.empty());

        // When + Then
        assertThatThrownBy(() -> fraudDetectionService.resolveAlert(
                alertId, "checked", "allow", FraudStatus.RESOLVED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FraudAlert")
                .hasMessageContaining("id")
                .hasMessageContaining(alertId.toString());
    }

    // Given-When-Then for user history and unresolved alerts
    @Test
    void givenUser_whenGetUserFraudHistoryAndHasUnresolvedAlerts_thenDelegatesToRepository() {
        // Given
        User user = User.builder().id(1L).build();
        var pageable = PageRequest.of(0, 10);

        FraudAlert alert = FraudAlert.builder()
                .id(1L)
                .status(FraudStatus.PENDING_REVIEW)
                .build();
        Page<FraudAlert> page = new PageImpl<>(List.of(alert), pageable, 1);

        given(fraudAlertRepository.findByUserOrderByCreatedAtDesc(user, pageable))
                .willReturn(page);

        FraudAlertResponseDto dto = new FraudAlertResponseDto(
                1L, "REF", "alice", 80, "HIGH_VALUE", FraudStatus.PENDING_REVIEW
        );
        given(fraudAlertMapper.toDto(alert)).willReturn(dto);

        given(fraudAlertRepository.existsByUserAndStatus(user, FraudStatus.PENDING_REVIEW))
                .willReturn(true);

        // When
        Page<FraudAlertResponseDto> history = fraudDetectionService.getUserFraudHistory(user, pageable);
        boolean hasUnresolved = fraudDetectionService.hasUnresolvedAlerts(user);

        // Then
        then(fraudAlertRepository).should().findByUserOrderByCreatedAtDesc(user, pageable);
        then(fraudAlertMapper).should().toDto(alert);

        assertThat(history.getContent()).containsExactly(dto);
        assertThat(hasUnresolved).isTrue();
    }
}

