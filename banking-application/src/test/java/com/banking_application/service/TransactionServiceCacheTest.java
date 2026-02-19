package com.banking_application.service;

import com.banking_application.dto.*;
import com.banking_application.mapper.TransactionMapper;
import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TransactionServiceCacheTest.TestConfig.class)
class TransactionServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("accountDetails", "transactionHistory", "transactionByRef");
        }

        @Bean
        public AccountRepository accountRepository() {
            return mock(AccountRepository.class);
        }

        @Bean
        public TransactionRepository transactionRepository() {
            return mock(TransactionRepository.class);
        }

        @Bean
        public TransactionMapper transactionMapper() {
            return mock(TransactionMapper.class);
        }

        @Bean
        public FraudDetectionService fraudDetectionService() {
            return mock(FraudDetectionService.class);
        }

        @Bean
        public AuditLogService auditLogService() {
            return mock(AuditLogService.class);
        }

        @Bean
        public AccountService accountService() {
            return mock(AccountService.class);
        }

        @Bean
        public TransactionService transactionService(AccountService accountService,
                                                     AccountRepository accountRepository,
                                                     TransactionRepository transactionRepository,
                                                     TransactionMapper transactionMapper,
                                                     FraudDetectionService fraudDetectionService,
                                                     AuditLogService auditLogService) {
            return new TransactionService(accountService, accountRepository,
                    transactionRepository, transactionMapper,
                    fraudDetectionService, auditLogService);
        }
    }

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CacheManager cacheManager;

    private User testUser;
    private Account savingsAccount;
    private Account currentAccount;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear()
        );
        reset(accountRepository, transactionRepository, transactionMapper,
                fraudDetectionService, auditLogService);

        testUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        savingsAccount = Account.builder()
                .id(1L)
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("SAVINGS001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("10000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();

        currentAccount = Account.builder()
                .id(2L)
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("CURRENT001")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("25000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();
    }

    @Test
    void getTransactionHistory_shouldCacheResultOnSecondCall() {
        Transaction txn = Transaction.builder()
                .id(1L)
                .transactionReference("HDFC-001")
                .sourceAccount(savingsAccount)
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-001", new BigDecimal("500.00"), "USD",
                TransactionType.WITHDRAWAL, TransactionStatus.SUCCESS,
                LocalDateTime.now(), null, "SAVINGS001", null
        );

        when(accountRepository.findByAccountNumber("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(savingsAccount, savingsAccount))
                .thenReturn(List.of(txn));
        when(transactionMapper.toDto(List.of(txn))).thenReturn(List.of(response));

        List<TransactionResponse> first = transactionService.getTransactionHistory("SAVINGS001");
        List<TransactionResponse> second = transactionService.getTransactionHistory("SAVINGS001");

        assertThat(first).isEqualTo(second);
        verify(accountRepository, times(1)).findByAccountNumber("SAVINGS001");
    }

    @Test
    void getTransactionByReference_shouldCacheResultOnSecondCall() {
        Transaction txn = Transaction.builder()
                .id(1L)
                .transactionReference("HDFC-REF001")
                .amount(new BigDecimal("1000.00"))
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-REF001", new BigDecimal("1000.00"), "USD",
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS,
                LocalDateTime.now(), null, null, "SAVINGS001"
        );

        when(transactionRepository.findByTransactionReference("HDFC-REF001")).thenReturn(Optional.of(txn));
        when(transactionMapper.toDto(txn)).thenReturn(response);

        TransactionResponse first = transactionService.getTransactionByReference("HDFC-REF001");
        TransactionResponse second = transactionService.getTransactionByReference("HDFC-REF001");

        assertThat(first).isEqualTo(second);
        verify(transactionRepository, times(1)).findByTransactionReference("HDFC-REF001");
    }

    @Test
    void deposit_shouldEvictTransactionHistoryCacheForAccount() {
        Transaction txn = Transaction.builder()
                .id(1L)
                .transactionReference("HDFC-001")
                .sourceAccount(savingsAccount)
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse historyResponse = new TransactionResponse(
                "HDFC-001", new BigDecimal("500.00"), "USD",
                TransactionType.WITHDRAWAL, TransactionStatus.SUCCESS,
                LocalDateTime.now(), null, "SAVINGS001", null
        );

        when(accountRepository.findByAccountNumber("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(savingsAccount, savingsAccount))
                .thenReturn(List.of(txn));
        when(transactionMapper.toDto(List.of(txn))).thenReturn(List.of(historyResponse));

        transactionService.getTransactionHistory("SAVINGS001");
        verify(accountRepository, times(1)).findByAccountNumber("SAVINGS001");

        Transaction depositTxn = Transaction.builder()
                .id(2L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-DEP001")
                .targetAccount(savingsAccount)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse depositResponse = new TransactionResponse(
                "HDFC-DEP001", new BigDecimal("1000.00"), "USD",
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "Salary", null, "SAVINGS001"
        );

        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(depositTxn);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(depositResponse);
        when(fraudDetectionService.evaluateTransaction(any())).thenReturn(0);

        transactionService.deposit(new DepositRequestDto("SAVINGS001", new BigDecimal("1000.00"), "Salary"), testUser);

        transactionService.getTransactionHistory("SAVINGS001");
        verify(accountRepository, times(2)).findByAccountNumber("SAVINGS001");
    }

    @Test
    void deposit_shouldEvictAccountDetailsCacheForAccount() {
        cacheManager.getCache("accountDetails").put("SAVINGS001", "cachedValue");

        Transaction depositTxn = Transaction.builder()
                .id(1L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-DEP002")
                .targetAccount(savingsAccount)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-DEP002", new BigDecimal("500.00"), "USD",
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "Test", null, "SAVINGS001"
        );

        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(depositTxn);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(response);
        when(fraudDetectionService.evaluateTransaction(any())).thenReturn(0);

        transactionService.deposit(new DepositRequestDto("SAVINGS001", new BigDecimal("500.00"), "Test"), testUser);

        assertThat(cacheManager.getCache("accountDetails").get("SAVINGS001")).isNull();
    }

    @Test
    void transfer_shouldEvictCachesForBothSourceAndTargetAccounts() {
        cacheManager.getCache("accountDetails").put("SAVINGS001", "cachedSource");
        cacheManager.getCache("accountDetails").put("CURRENT001", "cachedTarget");
        cacheManager.getCache("transactionHistory").put("SAVINGS001", "cachedSourceHistory");
        cacheManager.getCache("transactionHistory").put("CURRENT001", "cachedTargetHistory");

        Transaction transferTxn = Transaction.builder()
                .id(1L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-TRF001")
                .sourceAccount(savingsAccount)
                .targetAccount(currentAccount)
                .amount(new BigDecimal("3000.00"))
                .currency("USD")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-TRF001", new BigDecimal("3000.00"), "USD",
                TransactionType.TRANSFER, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "Rent", "SAVINGS001", "CURRENT001"
        );

        when(accountRepository.findByAccountNumberWithLock("CURRENT001")).thenReturn(Optional.of(currentAccount));
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transferTxn);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(response);
        when(fraudDetectionService.evaluateTransaction(any())).thenReturn(0);

        transactionService.transfer(
                new TransferRequestDto("SAVINGS001", "CURRENT001", new BigDecimal("3000.00"), "Rent"),
                testUser
        );

        assertThat(cacheManager.getCache("accountDetails").get("SAVINGS001")).isNull();
        assertThat(cacheManager.getCache("accountDetails").get("CURRENT001")).isNull();
        assertThat(cacheManager.getCache("transactionHistory").get("SAVINGS001")).isNull();
        assertThat(cacheManager.getCache("transactionHistory").get("CURRENT001")).isNull();
    }

    @Test
    void withdraw_shouldEvictAccountDetailsAndTransactionHistoryCache() {
        cacheManager.getCache("accountDetails").put("SAVINGS001", "cachedAccount");
        cacheManager.getCache("transactionHistory").put("SAVINGS001", "cachedHistory");

        Transaction withdrawTxn = Transaction.builder()
                .id(1L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-WDR001")
                .sourceAccount(savingsAccount)
                .amount(new BigDecimal("2000.00"))
                .currency("USD")
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-WDR001", new BigDecimal("2000.00"), "USD",
                TransactionType.WITHDRAWAL, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "ATM", "SAVINGS001", null
        );

        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(withdrawTxn);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(response);
        when(fraudDetectionService.evaluateTransaction(any())).thenReturn(0);

        transactionService.withdraw(
                new WithdrawalRequestDto("SAVINGS001", new BigDecimal("2000.00"), "ATM", "token123"),
                testUser
        );

        assertThat(cacheManager.getCache("accountDetails").get("SAVINGS001")).isNull();
        assertThat(cacheManager.getCache("transactionHistory").get("SAVINGS001")).isNull();
    }

    @Test
    void getTransactionHistory_cacheEntryShouldBeStoredInCorrectCache() {
        when(accountRepository.findByAccountNumber("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(savingsAccount, savingsAccount))
                .thenReturn(List.of());
        when(transactionMapper.toDto(List.<Transaction>of())).thenReturn(List.of());

        transactionService.getTransactionHistory("SAVINGS001");

        assertThat(cacheManager.getCache("transactionHistory").get("SAVINGS001")).isNotNull();
    }
}
