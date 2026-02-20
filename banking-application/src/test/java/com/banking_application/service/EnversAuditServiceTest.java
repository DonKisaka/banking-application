package com.banking_application.service;

import com.banking_application.dto.RevisionInfoDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.TransactionRepository;
import com.banking_application.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class EnversAuditServiceTest {

    @Autowired
    private EnversAuditService enversAuditService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Account testAccount;
    private Transaction testTransaction;

    @BeforeEach
    @Transactional
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        testUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("envers-user-" + unique)
                .email("envers-" + unique + "@test.com")
                .password("encoded")
                .phoneNumber("+1234567890")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        testAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("ENVERS001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();
        testAccount = accountRepository.save(testAccount);

        testTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .targetAccount(testAccount)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();
        testTransaction = transactionRepository.save(testTransaction);
    }

    @Test
    void getAccountRevisions_shouldReturnRevisionsAfterCreateAndUpdate() {
        List<RevisionInfoDto> revisions = enversAuditService.getAccountRevisions(testAccount.getId());
        assertThat(revisions).isNotEmpty();
        assertThat(revisions.get(0).revisionNumber()).isNotNull();
        assertThat(revisions.get(0).revisionTimestamp()).isNotNull();

        testAccount.setBalance(new BigDecimal("5100.00"));
        accountRepository.save(testAccount);

        List<RevisionInfoDto> revisionsAfterUpdate = enversAuditService.getAccountRevisions(testAccount.getId());
        assertThat(revisionsAfterUpdate).hasSizeGreaterThan(revisions.size());
    }

    @Test
    void getAccountAtRevision_shouldReturnHistoricalState() {
        BigDecimal originalBalance = testAccount.getBalance();
        List<RevisionInfoDto> revisions = enversAuditService.getAccountRevisions(testAccount.getId());
        assertThat(revisions).isNotEmpty();

        Number firstRevision = revisions.get(0).revisionNumber();
        Account historical = enversAuditService.getAccountAtRevision(testAccount.getId(), firstRevision);
        assertThat(historical.getBalance()).isEqualByComparingTo(originalBalance);
    }

    @Test
    void getAccountRevisions_shouldThrowWhenAccountNotFound() {
        assertThatThrownBy(() -> enversAuditService.getAccountRevisions(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account");
    }

    @Test
    void getAccountRevisionsByAccountNumber_shouldReturnRevisions() {
        List<RevisionInfoDto> revisions = enversAuditService.getAccountRevisionsByAccountNumber("ENVERS001");
        assertThat(revisions).isNotEmpty();
    }

    @Test
    void getTransactionRevisions_shouldReturnRevisions() {
        List<RevisionInfoDto> revisions = enversAuditService.getTransactionRevisions(testTransaction.getId());
        assertThat(revisions).isNotEmpty();
    }

    @Test
    void getUserRevisions_shouldReturnRevisions() {
        List<RevisionInfoDto> revisions = enversAuditService.getUserRevisions(testUser.getId());
        assertThat(revisions).isNotEmpty();
    }

    @Test
    void getUserAtRevision_shouldReturnHistoricalUser() {
        List<RevisionInfoDto> revisions = enversAuditService.getUserRevisions(testUser.getId());
        assertThat(revisions).isNotEmpty();

        User historical = enversAuditService.getUserAtRevision(testUser.getId(), revisions.get(0).revisionNumber());
        assertThat(historical.getUsername()).startsWith("envers-user-");
    }
}
