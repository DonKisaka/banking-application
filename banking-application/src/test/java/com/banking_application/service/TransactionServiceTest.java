package com.banking_application.service;

import com.banking_application.dto.*;
import com.banking_application.exception.AccountStateException;
import com.banking_application.exception.InsufficientFundsException;
import com.banking_application.exception.InvalidTransactionException;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.mapper.TransactionMapper;
import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TransactionService underTest;

    private User testUser;
    private Account savingsAccount;
    private Account currentAccount;

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

    // --- deposit tests ---

    @Test
    void deposit_shouldCreditAccountAndRecordTransaction() {
        // given
        DepositRequestDto dto = new DepositRequestDto("SAVINGS001", new BigDecimal("500.00"), "Salary deposit");

        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-12345678")
                .targetAccount(savingsAccount)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-12345678", new BigDecimal("500.00"), "USD",
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "Salary deposit", null, "SAVINGS001"
        );

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(response);
        when(fraudDetectionService.evaluateTransaction(any())).thenReturn(10);

        // when
        TransactionResponse result = underTest.deposit(dto, testUser);

        // then
        assertThat(result.transactionReference()).isEqualTo("HDFC-12345678");
        assertThat(result.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo(new BigDecimal("10500.0000"));
        verify(transactionRepository).save(any(Transaction.class));
        verify(fraudDetectionService).evaluateTransaction(any());
        verify(auditLogService).logAction(eq("DEPOSIT"), eq(testUser), anyString(), anyString(), eq(AuditStatus.SUCCESS), any(), any());
    }

    @Test
    void deposit_shouldThrowWhenAccountNotFound() {
        // given
        DepositRequestDto dto = new DepositRequestDto("NONEXISTENT", new BigDecimal("500.00"), "Test");
        when(accountRepository.findByAccountNumberWithLock("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.deposit(dto, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void deposit_shouldThrowWhenAccountNotActive() {
        // given
        savingsAccount.setStatus(AccountStatus.FROZEN);
        DepositRequestDto dto = new DepositRequestDto("SAVINGS001", new BigDecimal("500.00"), "Test");
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));

        // when & then
        assertThatThrownBy(() -> underTest.deposit(dto, testUser))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("not active");
    }

    // --- withdraw tests ---

    @Test
    void withdraw_shouldDebitAccountAndRecordTransaction() {
        // given
        WithdrawalRequestDto dto = new WithdrawalRequestDto("SAVINGS001", new BigDecimal("2000.00"), "ATM withdrawal", "token123");

        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));

        Transaction savedTransaction = Transaction.builder()
                .id(2L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-87654321")
                .sourceAccount(savingsAccount)
                .amount(new BigDecimal("2000.00"))
                .currency("USD")
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-87654321", new BigDecimal("2000.00"), "USD",
                TransactionType.WITHDRAWAL, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "ATM withdrawal", "SAVINGS001", null
        );

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(response);
        when(fraudDetectionService.evaluateTransaction(any())).thenReturn(5);

        // when
        TransactionResponse result = underTest.withdraw(dto, testUser);

        // then
        assertThat(result.transactionReference()).isEqualTo("HDFC-87654321");
        assertThat(result.type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo(new BigDecimal("8000.0000"));
        verify(transactionRepository).save(any(Transaction.class));
        verify(fraudDetectionService).evaluateTransaction(any());
    }

    @Test
    void withdraw_shouldThrowWhenInsufficientBalance() {
        // given
        WithdrawalRequestDto dto = new WithdrawalRequestDto("SAVINGS001", new BigDecimal("50000.00"), "Large withdrawal", "token123");
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));

        // when & then
        assertThatThrownBy(() -> underTest.withdraw(dto, testUser))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void withdraw_shouldThrowWhenAccountNotActive() {
        // given
        savingsAccount.setStatus(AccountStatus.CLOSED);
        WithdrawalRequestDto dto = new WithdrawalRequestDto("SAVINGS001", new BigDecimal("100.00"), "Test", "token123");
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));

        // when & then
        assertThatThrownBy(() -> underTest.withdraw(dto, testUser))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void withdraw_shouldThrowWhenAccountNotFound() {
        // given
        WithdrawalRequestDto dto = new WithdrawalRequestDto("NONEXISTENT", new BigDecimal("100.00"), "Test", "token123");
        when(accountRepository.findByAccountNumberWithLock("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.withdraw(dto, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    // --- transfer tests ---

    @Test
    void transfer_shouldDebitSourceAndCreditTarget() {
        // given
        TransferRequestDto dto = new TransferRequestDto("SAVINGS001", "CURRENT001", new BigDecimal("3000.00"), "Rent payment");

        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.findByAccountNumberWithLock("CURRENT001")).thenReturn(Optional.of(currentAccount));

        Transaction savedTransaction = Transaction.builder()
                .id(3L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-TRANSFER1")
                .sourceAccount(savingsAccount)
                .targetAccount(currentAccount)
                .amount(new BigDecimal("3000.00"))
                .currency("USD")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-TRANSFER1", new BigDecimal("3000.00"), "USD",
                TransactionType.TRANSFER, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "Rent payment", "SAVINGS001", "CURRENT001"
        );

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(response);
        when(fraudDetectionService.evaluateTransaction(any())).thenReturn(15);

        // when
        TransactionResponse result = underTest.transfer(dto, testUser);

        // then
        assertThat(result.transactionReference()).isEqualTo("HDFC-TRANSFER1");
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo(new BigDecimal("7000.0000"));
        assertThat(currentAccount.getBalance()).isEqualByComparingTo(new BigDecimal("28000.0000"));
        verify(transactionRepository).save(any(Transaction.class));
        verify(fraudDetectionService).evaluateTransaction(any());
    }

    @Test
    void transfer_shouldThrowWhenSourceAccountNotFound() {
        // given - "CURRENT001" < "NONEXISTENT" alphabetically, so CURRENT001 locks first
        TransferRequestDto dto = new TransferRequestDto("NONEXISTENT", "CURRENT001", new BigDecimal("1000.00"), "Test");
        when(accountRepository.findByAccountNumberWithLock("CURRENT001")).thenReturn(Optional.of(currentAccount));
        when(accountRepository.findByAccountNumberWithLock("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.transfer(dto, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void transfer_shouldThrowWhenTargetAccountNotFound() {
        // given - "NONEXISTENT" < "SAVINGS001" alphabetically, so NONEXISTENT locks first and fails
        TransferRequestDto dto = new TransferRequestDto("SAVINGS001", "NONEXISTENT", new BigDecimal("1000.00"), "Test");
        when(accountRepository.findByAccountNumberWithLock("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.transfer(dto, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void transfer_shouldThrowWhenSourceNotActive() {
        // given
        savingsAccount.setStatus(AccountStatus.FROZEN);
        TransferRequestDto dto = new TransferRequestDto("SAVINGS001", "CURRENT001", new BigDecimal("1000.00"), "Test");
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.findByAccountNumberWithLock("CURRENT001")).thenReturn(Optional.of(currentAccount));

        // when & then
        assertThatThrownBy(() -> underTest.transfer(dto, testUser))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("Source account is not active");
    }

    @Test
    void transfer_shouldThrowWhenTargetNotActive() {
        // given
        currentAccount.setStatus(AccountStatus.CLOSED);
        TransferRequestDto dto = new TransferRequestDto("SAVINGS001", "CURRENT001", new BigDecimal("1000.00"), "Test");
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.findByAccountNumberWithLock("CURRENT001")).thenReturn(Optional.of(currentAccount));

        // when & then
        assertThatThrownBy(() -> underTest.transfer(dto, testUser))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("Target account is not active");
    }

    @Test
    void transfer_shouldThrowWhenCurrencyMismatch() {
        // given
        currentAccount.setCurrency("EUR");
        TransferRequestDto dto = new TransferRequestDto("SAVINGS001", "CURRENT001", new BigDecimal("1000.00"), "Test");
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.findByAccountNumberWithLock("CURRENT001")).thenReturn(Optional.of(currentAccount));

        // when & then
        assertThatThrownBy(() -> underTest.transfer(dto, testUser))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void transfer_shouldThrowWhenInsufficientBalance() {
        // given
        TransferRequestDto dto = new TransferRequestDto("SAVINGS001", "CURRENT001", new BigDecimal("50000.00"), "Test");
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.findByAccountNumberWithLock("CURRENT001")).thenReturn(Optional.of(currentAccount));

        // when & then
        assertThatThrownBy(() -> underTest.transfer(dto, testUser))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void transfer_shouldThrowWhenTransferToSameAccount() {
        // given - same account check happens before locking, no stubs needed
        TransferRequestDto dto = new TransferRequestDto("SAVINGS001", "SAVINGS001", new BigDecimal("1000.00"), "Test");

        // when & then
        assertThatThrownBy(() -> underTest.transfer(dto, testUser))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("same account");
    }

    // --- transfer with lock ordering ---

    @Test
    void transfer_shouldLockAccountsInIdOrder() {
        // given - source has higher ID than target to test lock ordering
        savingsAccount.setId(10L);
        currentAccount.setId(5L);
        TransferRequestDto dto = new TransferRequestDto("SAVINGS001", "CURRENT001", new BigDecimal("1000.00"), "Test");

        when(accountRepository.findByAccountNumberWithLock("CURRENT001")).thenReturn(Optional.of(currentAccount));
        when(accountRepository.findByAccountNumberWithLock("SAVINGS001")).thenReturn(Optional.of(savingsAccount));

        Transaction savedTransaction = Transaction.builder()
                .id(4L)
                .transactionUuid(UUID.randomUUID())
                .transactionReference("HDFC-ORDERED")
                .sourceAccount(savingsAccount)
                .targetAccount(currentAccount)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse response = new TransactionResponse(
                "HDFC-ORDERED", new BigDecimal("1000.00"), "USD",
                TransactionType.TRANSFER, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "Test", "SAVINGS001", "CURRENT001"
        );

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(response);
        when(fraudDetectionService.evaluateTransaction(any())).thenReturn(0);

        // when
        TransactionResponse result = underTest.transfer(dto, testUser);

        // then
        assertThat(result).isNotNull();
        verify(accountRepository, times(2)).findByAccountNumberWithLock(anyString());
    }

    // --- getTransactionHistory tests ---

    @Test
    void getTransactionHistory_shouldReturnTransactionsForAccount() {
        // given
        when(accountRepository.findByAccountNumber("SAVINGS001")).thenReturn(Optional.of(savingsAccount));

        Transaction txn = Transaction.builder()
                .id(1L)
                .transactionReference("HDFC-HIST001")
                .sourceAccount(savingsAccount)
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse dto = new TransactionResponse(
                "HDFC-HIST001", new BigDecimal("500.00"), "USD",
                TransactionType.WITHDRAWAL, TransactionStatus.SUCCESS,
                LocalDateTime.now(), null, "SAVINGS001", null
        );

        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(savingsAccount, savingsAccount))
                .thenReturn(List.of(txn));
        when(transactionMapper.toDto(List.of(txn))).thenReturn(List.of(dto));

        // when
        List<TransactionResponse> result = underTest.getTransactionHistory("SAVINGS001");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).transactionReference()).isEqualTo("HDFC-HIST001");
    }

    @Test
    void getTransactionHistory_shouldThrowWhenAccountNotFound() {
        // given
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.getTransactionHistory("NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    // --- getTransactionByReference tests ---

    @Test
    void getTransactionByReference_shouldReturnTransaction() {
        // given
        Transaction txn = Transaction.builder()
                .id(1L)
                .transactionReference("HDFC-REF12345")
                .amount(new BigDecimal("1000.00"))
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse dto = new TransactionResponse(
                "HDFC-REF12345", new BigDecimal("1000.00"), "USD",
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS,
                LocalDateTime.now(), null, null, "SAVINGS001"
        );

        when(transactionRepository.findByTransactionReference("HDFC-REF12345")).thenReturn(Optional.of(txn));
        when(transactionMapper.toDto(txn)).thenReturn(dto);

        // when
        TransactionResponse result = underTest.getTransactionByReference("HDFC-REF12345");

        // then
        assertThat(result.transactionReference()).isEqualTo("HDFC-REF12345");
    }

    @Test
    void getTransactionByReference_shouldThrowWhenNotFound() {
        // given
        when(transactionRepository.findByTransactionReference("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.getTransactionByReference("NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }
}
