package com.banking_application.service;

import com.banking_application.dto.DepositRequestDto;
import com.banking_application.dto.TransactionResponse;
import com.banking_application.dto.TransferRequestDto;
import com.banking_application.dto.WithdrawalRequestDto;
import com.banking_application.exception.AccountStateException;
import com.banking_application.exception.InvalidTransactionException;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.mapper.TransactionMapper;
import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    // Exception Testing for unknown account on deposit
    @Test
    void givenUnknownAccount_whenDeposit_thenThrowsResourceNotFoundException() {
        // Given
        DepositRequestDto dto = new DepositRequestDto("UNKNOWN", BigDecimal.TEN, "test");
        User user = User.builder().username("caleb").build();

        given(accountRepository.findByAccountNumberWithLock(dto.accountNumber()))
                .willReturn(Optional.empty());

        // When + Then
        assertThatThrownBy(() -> transactionService.deposit(dto, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account")
                .hasMessageContaining("accountNumber")
                .hasMessageContaining(dto.accountNumber());

        then(transactionRepository).shouldHaveNoInteractions();
        then(fraudDetectionService).shouldHaveNoInteractions();
    }

    // Given-When-Then + ArgumentCaptor for successful deposit
    @Test
    void givenActiveAccount_whenDeposit_thenBalanceCreditedTransactionSavedAndEvaluated() {
        // Given
        DepositRequestDto dto = new DepositRequestDto("ACC123", BigDecimal.valueOf(100), "Initial deposit");
        User user = User.builder().username("natalia").build();

        Account account = new Account();
        account.setAccountNumber(dto.accountNumber());
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency("KSH");
        account.setBalance(BigDecimal.ZERO);

        given(accountRepository.findByAccountNumberWithLock(dto.accountNumber()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any(Transaction.class)))
                .willAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    tx.setTransactionReference("REF123");
                    tx.setCreatedAt(LocalDateTime.now());
                    return tx;
                });

        TransactionResponse txResponse = new TransactionResponse(
                "REF123",
                dto.amount(),
                "KSH",
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                LocalDateTime.now(),
                dto.description(),
                null,
                dto.accountNumber()
        );
        given(transactionMapper.toDto(any(Transaction.class))).willReturn(txResponse);

        // When
        TransactionResponse result = transactionService.deposit(dto, user);

        // Then
        then(accountRepository).should().findByAccountNumberWithLock(dto.accountNumber());
        then(transactionRepository).should().save(transactionCaptor.capture());
        then(fraudDetectionService).should().evaluateTransaction(transactionCaptor.getValue());
        then(transactionMapper).should().toDto(any(Transaction.class));

        Transaction saved = transactionCaptor.getValue();
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(saved.getTargetAccount()).isEqualTo(account);
        assertThat(saved.getInitiatedBy()).isEqualTo(user);
        assertThat(saved.getAmount()).isEqualTo(dto.amount());
        assertThat(account.getBalance()).isEqualByComparingTo(dto.amount());

        assertThat(result).isEqualTo(txResponse);
    }

    // Exception Testing for inactive account on withdrawal
    @Test
    void givenInactiveAccount_whenWithdraw_thenThrowsAccountStateException() {
        // Given
        WithdrawalRequestDto dto = new WithdrawalRequestDto("ACC123", BigDecimal.ONE, "test", "token");
        User user = User.builder().username("joy").build();

        Account account = new Account();
        account.setAccountNumber(dto.accountNumber());
        account.setStatus(AccountStatus.FROZEN);

        given(accountRepository.findByAccountNumberWithLock(dto.accountNumber()))
                .willReturn(Optional.of(account));

        // When + Then
        assertThatThrownBy(() -> transactionService.withdraw(dto, user))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("not active");

        then(transactionRepository).shouldHaveNoInteractions();
    }

    // Exception Testing for self-transfer
    @Test
    void givenSameSourceAndTarget_whenTransfer_thenThrowsInvalidTransactionExceptionAndNoLookups() {
        // Given
        TransferRequestDto dto = new TransferRequestDto("ACC123", "ACC123", BigDecimal.ONE, TransactionType.TRANSFER, "self");
        User user = User.builder().username("kimberly").build();

        // When + Then
        assertThatThrownBy(() -> transactionService.transfer(dto, user))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("same account");

        then(accountRepository).shouldHaveNoInteractions();
        then(transactionRepository).shouldHaveNoInteractions();
    }

    // Exception Testing for unknown account in history
    @Test
    void givenUnknownAccount_whenGetTransactionHistory_thenThrowsResourceNotFoundException() {
        // Given
        String accountNumber = "UNKNOWN";
        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.empty());

        // When + Then
        assertThatThrownBy(() -> transactionService.getTransactionHistory(accountNumber))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Exception Testing for missing transaction reference
    @Test
    void givenUnknownReference_whenGetTransactionByReference_thenThrowsResourceNotFoundException() {
        // Given
        String reference = "MISSING";
        given(transactionRepository.findByTransactionReference(reference))
                .willReturn(Optional.empty());

        // When + Then
        assertThatThrownBy(() -> transactionService.getTransactionByReference(reference))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction")
                .hasMessageContaining("reference")
                .hasMessageContaining(reference);
    }

    // Given-When-Then for transaction history retrieval
    @Test
    void givenExistingAccount_whenGetTransactionHistory_thenRepositoryAndMapperUsed() {
        // Given
        String accountNumber = "ACC999";
        Account account = new Account();
        account.setAccountNumber(accountNumber);

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        Transaction tx = Transaction.builder()
                .amount(BigDecimal.TEN)
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .build();

        given(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(account, account))
                .willReturn(List.of(tx));

        TransactionResponse dto = new TransactionResponse(
                "REF", BigDecimal.TEN, "USD",
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS,
                LocalDateTime.now(), "desc", null, accountNumber
        );
        given(transactionMapper.toDto(List.of(tx))).willReturn(List.of(dto));

        // When
        List<TransactionResponse> result = transactionService.getTransactionHistory(accountNumber);

        // Then
        then(accountRepository).should().findByAccountNumber(accountNumber);
        then(transactionRepository).should()
                .findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(account, account);
        then(transactionMapper).should().toDto(List.of(tx));

        assertThat(result).containsExactly(dto);
    }
}

