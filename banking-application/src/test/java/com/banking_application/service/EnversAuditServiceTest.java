package com.banking_application.service;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.RevisionInfoDto;
import com.banking_application.dto.UserAuditDto;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class EnversAuditServiceTest {

    @Autowired
    private EnversAuditService enversAuditService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Account testAccount;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        testUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("envers_user_" + suffix)
                .email("envers_" + suffix + "@example.com")
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
                .accountNumber("ENV" + suffix)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();
        testAccount = accountRepository.save(testAccount);

        testTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-" + suffix)
                .sourceAccount(testAccount)
                .targetAccount(testAccount)
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        testTransaction = transactionRepository.save(testTransaction);
    }

    @Test
    void getAccountRevisions_shouldReturnAtLeastOneRevision() {
        List<RevisionInfoDto> revisions = enversAuditService.getAccountRevisions(testAccount.getId());
        assertThat(revisions).isNotEmpty();
        assertThat(revisions.get(0).revisionNumber()).isNotNull();
        assertThat(revisions.get(0).revisionTimestamp()).isNotNull();
    }

    @Test
    void getAccountAtRevision_shouldReturnAccountState() {
        List<RevisionInfoDto> revisions = enversAuditService.getAccountRevisions(testAccount.getId());
        assertThat(revisions).isNotEmpty();
        Number rev = revisions.get(0).revisionNumber();
        Optional<AccountResponseDto> dto = enversAuditService.getAccountAtRevision(testAccount.getId(), rev);
        assertThat(dto).isPresent();
        assertThat(dto.get().accountNumber()).isEqualTo(testAccount.getAccountNumber());
        assertThat(dto.get().balance()).isEqualByComparingTo(testAccount.getBalance());
    }

    @Test
    void getTransactionRevisions_shouldReturnAtLeastOneRevision() {
        List<RevisionInfoDto> revisions = enversAuditService.getTransactionRevisions(testTransaction.getId());
        assertThat(revisions).isNotEmpty();
    }

    @Test
    void getTransactionAtRevision_shouldReturnTransactionState() {
        List<RevisionInfoDto> revisions = enversAuditService.getTransactionRevisions(testTransaction.getId());
        assertThat(revisions).isNotEmpty();
        Number rev = revisions.get(0).revisionNumber();
        Optional<Transaction> tx = enversAuditService.getTransactionAtRevision(testTransaction.getId(), rev);
        assertThat(tx).isPresent();
        assertThat(tx.get().getTransactionReference()).isEqualTo(testTransaction.getTransactionReference());
    }

    @Test
    void getUserRevisions_shouldReturnAtLeastOneRevision() {
        List<RevisionInfoDto> revisions = enversAuditService.getUserRevisions(testUser.getId());
        assertThat(revisions).isNotEmpty();
    }

    @Test
    void getUserAtRevision_shouldReturnUserState() {
        List<RevisionInfoDto> revisions = enversAuditService.getUserRevisions(testUser.getId());
        assertThat(revisions).isNotEmpty();
        Number rev = revisions.get(0).revisionNumber();
        Optional<UserAuditDto> dto = enversAuditService.getUserAtRevision(testUser.getId(), rev);
        assertThat(dto).isPresent();
        assertThat(dto.get().username()).isEqualTo(testUser.getUsername());
        assertThat(dto.get().email()).isEqualTo(testUser.getEmail());
    }

    @Test
    void getAccountRevisions_afterUpdate_shouldHaveMultipleRevisions() {
        testAccount.setBalance(new BigDecimal("6000.0000"));
        accountRepository.saveAndFlush(testAccount);

        List<RevisionInfoDto> revisions = enversAuditService.getAccountRevisions(testAccount.getId());
        assertThat(revisions).hasSizeGreaterThanOrEqualTo(2);
    }
}
