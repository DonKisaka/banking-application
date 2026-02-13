package com.banking_application.repository;

import com.banking_application.model.Account;
import com.banking_application.model.AccountStatus;
import com.banking_application.model.AccountType;
import com.banking_application.model.Transaction;
import com.banking_application.model.TransactionStatus;
import com.banking_application.model.TransactionType;
import com.banking_application.model.User;
import com.banking_application.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository underTest;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("tomevans")
                .email("tomevans@example.com")
                .password("password123")
                .phoneNumber("+254732456712")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        sourceAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("SOURCE001")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("10000.00"))
                .currency("KSH")
                .status(AccountStatus.ACTIVE)
                .build();
        sourceAccount = accountRepository.save(sourceAccount);

        targetAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("TARGET001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .currency("KSH")
                .status(AccountStatus.ACTIVE)
                .build();
        targetAccount = accountRepository.save(targetAccount);
    }

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void itShouldFindTransactionByTransactionReference() {
        // given
        String transactionReference = "TXN-REF-12345678";
        Transaction transaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference(transactionReference)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("500.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();
        underTest.save(transaction);

        // when
        Optional<Transaction> found = underTest.findByTransactionReference(transactionReference);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionReference()).isEqualTo(transactionReference);
    }

    @Test
    void itShouldNotFindTransactionByTransactionReferenceWhenDoesNotExist() {
        // given
        String transactionReference = "NONEXISTENT-REF";

        // when
        Optional<Transaction> found = underTest.findByTransactionReference(transactionReference);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void itShouldFindTransactionsBySourceOrTargetAccountOrderedByCreatedAtDesc() {
        // given
        Transaction transaction1 = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("TXN-001")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("100.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();

        Transaction transaction2 = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("TXN-002")
                .sourceAccount(targetAccount)
                .targetAccount(sourceAccount)
                .amount(new BigDecimal("200.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();

        underTest.save(transaction1);
        underTest.save(transaction2);

        // when
        List<Transaction> transactions = underTest
                .findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(sourceAccount, sourceAccount);

        // then
        assertThat(transactions).hasSize(2);
    }

    @Test
    void itShouldReturnEmptyListWhenNoTransactionsForAccount() {
        // given
        Account newAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("NEW001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("1000.00"))
                .currency("KSH")
                .status(AccountStatus.ACTIVE)
                .build();
        newAccount = accountRepository.save(newAccount);

        // when
        List<Transaction> transactions = underTest
                .findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(newAccount, newAccount);

        // then
        assertThat(transactions).isEmpty();
    }

    @Test
    void itShouldFindHighValueTransactions() {
        // given
        BigDecimal threshold = new BigDecimal("1000.00");
        LocalDateTime since = LocalDateTime.now().minusDays(1);

        Transaction highValueTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HIGH-VALUE-001")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("5000.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();

        Transaction lowValueTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("LOW-VALUE-001")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("100.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();

        underTest.save(highValueTransaction);
        underTest.save(lowValueTransaction);

        // when
        List<Transaction> highValueTransactions = underTest.findHighValueTransactions(threshold, since);

        // then
        assertThat(highValueTransactions).hasSize(1);
        assertThat(highValueTransactions.get(0).getAmount()).isGreaterThan(threshold);
    }

    @Test
    void itShouldReturnEmptyListWhenNoHighValueTransactions() {
        // given
        BigDecimal threshold = new BigDecimal("10000.00");
        LocalDateTime since = LocalDateTime.now().minusDays(1);

        Transaction lowValueTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("LOW-VALUE-001")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("500.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();
        underTest.save(lowValueTransaction);

        // when
        List<Transaction> highValueTransactions = underTest.findHighValueTransactions(threshold, since);

        // then
        assertThat(highValueTransactions).isEmpty();
    }

    @Test
    void itShouldNotFindHighValueTransactionsBeforeSinceDate() {
        // given
        BigDecimal threshold = new BigDecimal("100.00");
        LocalDateTime since = LocalDateTime.now().plusDays(1); // Future date

        Transaction transaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference("TXN-PAST-001")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("5000.00"))
                .currency("KSH")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();
        underTest.save(transaction);

        // when
        List<Transaction> highValueTransactions = underTest.findHighValueTransactions(threshold, since);

        // then
        assertThat(highValueTransactions).isEmpty();
    }

    @Test
    void itShouldFindTransactionWithDepositType() {
        // given
        String transactionReference = "DEPOSIT-001";
        Transaction depositTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference(transactionReference)
                .targetAccount(targetAccount)
                .amount(new BigDecimal("1000.00"))
                .currency("KSH")
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();
        underTest.save(depositTransaction);

        // when
        Optional<Transaction> found = underTest.findByTransactionReference(transactionReference);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void itShouldFindTransactionWithWithdrawalType() {
        // given
        String transactionReference = "WITHDRAWAL-001";
        Transaction withdrawalTransaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .transactionReference(transactionReference)
                .sourceAccount(sourceAccount)
                .amount(new BigDecimal("500.00"))
                .currency("KSH")
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionStatus(TransactionStatus.SUCCESS)
                .initiatedBy(testUser)
                .build();
        underTest.save(withdrawalTransaction);

        // when
        Optional<Transaction> found = underTest.findByTransactionReference(transactionReference);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
    }
}
