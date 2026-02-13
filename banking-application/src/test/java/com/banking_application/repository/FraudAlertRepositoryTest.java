package com.banking_application.repository;

import com.banking_application.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FraudAlertRepositoryTest {

    @Autowired
    private FraudAlertRepository underTest;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Account sourceAccount;
    private Account targetAccount;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("calebchiira")
                .email("calebchiira@example.com")
                .password("password123")
                .phoneNumber("+254712345678")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        sourceAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("FRAUD-SRC-001")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("10000.00"))
                .currency("KSH")
                .status(AccountStatus.ACTIVE)
                .build();
        sourceAccount = accountRepository.save(sourceAccount);

        targetAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("FRAUD-TGT-001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .currency("KSH")
                .status(AccountStatus.ACTIVE)
                .build();
        targetAccount = accountRepository.save(targetAccount);

        testTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("FRAUD-TXN-001")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("5000.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.PENDING)
                .initiatedBy(testUser)
                .build();
        testTransaction = transactionRepository.save(testTransaction);
    }

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void itShouldFindFraudAlertsByStatus() {
        // given
        FraudAlert pendingAlert = FraudAlert.builder()
                .transaction(testTransaction)
                .user(testUser)
                .riskScore(85)
                .reasonCode("HIGH_AMOUNT")
                .detectionLogic("Amount exceeds threshold")
                .status(FraudStatus.PENDING_REVIEW)
                .build();
        underTest.save(pendingAlert);

        // when
        List<FraudAlert> alerts = underTest.findByStatus(FraudStatus.PENDING_REVIEW);

        // then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getStatus()).isEqualTo(FraudStatus.PENDING_REVIEW);
    }

    @Test
    void itShouldReturnEmptyListWhenNoFraudAlertsWithStatus() {
        // given
        FraudStatus status = FraudStatus.RESOLVED;

        // when
        List<FraudAlert> alerts = underTest.findByStatus(status);

        // then
        assertThat(alerts).isEmpty();
    }

    @Test
    void itShouldFindFraudAlertsByRiskScoreGreaterThanOrEqual() {
        // given
        FraudAlert highRiskAlert = FraudAlert.builder()
                .transaction(testTransaction)
                .user(testUser)
                .riskScore(90)
                .reasonCode("HIGH_RISK")
                .detectionLogic("Multiple risk factors")
                .status(FraudStatus.PENDING_REVIEW)
                .build();
        underTest.save(highRiskAlert);

        // when
        List<FraudAlert> highRiskAlerts = underTest.findByRiskScoreGreaterThanEqual(80);

        // then
        assertThat(highRiskAlerts).hasSize(1);
        assertThat(highRiskAlerts.get(0).getRiskScore()).isGreaterThanOrEqualTo(80);
    }

    @Test
    void itShouldNotFindFraudAlertsWhenRiskScoreBelowThreshold() {
        // given
        FraudAlert lowRiskAlert = FraudAlert.builder()
                .transaction(testTransaction)
                .user(testUser)
                .riskScore(30)
                .reasonCode("LOW_RISK")
                .detectionLogic("Minor anomaly detected")
                .status(FraudStatus.DISMISSED)
                .build();
        underTest.save(lowRiskAlert);

        // when
        List<FraudAlert> highRiskAlerts = underTest.findByRiskScoreGreaterThanEqual(80);

        // then
        assertThat(highRiskAlerts).isEmpty();
    }

    @Test
    void itShouldFindFraudAlertsByStatusOrderedByRiskScoreDesc() {
        // given
        Transaction transaction2 = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("FRAUD-TXN-002")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("3000.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.PENDING)
                .initiatedBy(testUser)
                .build();
        transaction2 = transactionRepository.save(transaction2);

        FraudAlert alert1 = FraudAlert.builder()
                .transaction(testTransaction)
                .user(testUser)
                .riskScore(70)
                .reasonCode("MEDIUM_RISK")
                .status(FraudStatus.PENDING_REVIEW)
                .build();

        FraudAlert alert2 = FraudAlert.builder()
                .transaction(transaction2)
                .user(testUser)
                .riskScore(95)
                .reasonCode("HIGH_RISK")
                .status(FraudStatus.PENDING_REVIEW)
                .build();

        underTest.save(alert1);
        underTest.save(alert2);

        // when
        List<FraudAlert> alerts = underTest.findByStatusOrderByRiskScoreDesc(FraudStatus.PENDING_REVIEW);

        // then
        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).getRiskScore()).isEqualTo(95);
        assertThat(alerts.get(1).getRiskScore()).isEqualTo(70);
    }

    @Test
    void itShouldFindFraudAlertByTransaction() {
        // given
        FraudAlert alert = FraudAlert.builder()
                .transaction(testTransaction)
                .user(testUser)
                .riskScore(85)
                .reasonCode("SUSPICIOUS_PATTERN")
                .status(FraudStatus.INVESTIGATING)
                .build();
        underTest.save(alert);

        // when
        Optional<FraudAlert> found = underTest.findByTransaction(testTransaction);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTransaction().getId()).isEqualTo(testTransaction.getId());
    }

    @Test
    void itShouldNotFindFraudAlertByTransactionWhenDoesNotExist() {
        // given
        Transaction newTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("NEW-TXN-001")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("100.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();
        newTransaction = transactionRepository.save(newTransaction);

        // when
        Optional<FraudAlert> found = underTest.findByTransaction(newTransaction);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void itShouldFindFraudAlertsByUserOrderedByCreatedAtDesc() {
        // given
        Transaction transaction2 = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("FRAUD-TXN-003")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("2000.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.PENDING)
                .initiatedBy(testUser)
                .build();
        transaction2 = transactionRepository.save(transaction2);

        FraudAlert alert1 = FraudAlert.builder()
                .transaction(testTransaction)
                .user(testUser)
                .riskScore(75)
                .reasonCode("ALERT_1")
                .status(FraudStatus.PENDING_REVIEW)
                .build();

        FraudAlert alert2 = FraudAlert.builder()
                .transaction(transaction2)
                .user(testUser)
                .riskScore(80)
                .reasonCode("ALERT_2")
                .status(FraudStatus.PENDING_REVIEW)
                .build();

        underTest.save(alert1);
        underTest.save(alert2);

        // when
        Page<FraudAlert> alertsPage = underTest.findByUserOrderByCreatedAtDesc(testUser, PageRequest.of(0, 10));

        // then
        assertThat(alertsPage.getContent()).hasSize(2);
        assertThat(alertsPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    void itShouldReturnEmptyPageWhenUserHasNoFraudAlerts() {
        // given
        User newUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("vallence")
                .email("vallence@example.com")
                .password("password456")
                .phoneNumber("+254700000000")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        newUser = userRepository.save(newUser);

        // when
        Page<FraudAlert> alertsPage = underTest.findByUserOrderByCreatedAtDesc(newUser, PageRequest.of(0, 10));

        // then
        assertThat(alertsPage.getContent()).isEmpty();
        assertThat(alertsPage.getTotalElements()).isZero();
    }

    @Test
    void itShouldCheckWhenUserHasFraudAlertWithStatus() {
        // given
        FraudAlert alert = FraudAlert.builder()
                .transaction(testTransaction)
                .user(testUser)
                .riskScore(90)
                .reasonCode("CRITICAL")
                .status(FraudStatus.INVESTIGATING)
                .build();
        underTest.save(alert);

        // when
        boolean exists = underTest.existsByUserAndStatus(testUser, FraudStatus.INVESTIGATING);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void itShouldCheckWhenUserDoesNotHaveFraudAlertWithStatus() {
        // given
        FraudAlert alert = FraudAlert.builder()
                .transaction(testTransaction)
                .user(testUser)
                .riskScore(90)
                .reasonCode("CRITICAL")
                .status(FraudStatus.RESOLVED)
                .build();
        underTest.save(alert);

        // when
        boolean exists = underTest.existsByUserAndStatus(testUser, FraudStatus.PENDING_REVIEW);

        // then
        assertThat(exists).isFalse();
    }
}
