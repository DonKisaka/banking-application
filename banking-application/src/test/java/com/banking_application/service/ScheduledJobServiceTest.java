package com.banking_application.service;

import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.AccountStatementRepository;
import com.banking_application.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ScheduledJobServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountStatementRepository accountStatementRepository;

    @InjectMocks
    private ScheduledJobService scheduledJobService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    // Given-When-Then + Mocking + ArgumentCaptor for applyMonthlyInterest
    @Test
    void givenEligibleSavingsAccount_whenApplyMonthlyInterest_thenInterestCreditedAndTransactionSaved() {
        // Given
        Account account = new Account();
        account.setAccountNumber("ACC123");
        account.setStatus(AccountStatus.ACTIVE);
        account.setAccountType(AccountType.SAVINGS);
        account.setCurrency("KSH");
        account.setBalance(new BigDecimal("100000.00"));
        account.setInterestRate(new BigDecimal("12.00")); // 12% p.a.
        account.setMinimumBalance(BigDecimal.ZERO);

        given(accountRepository.findByStatusAndAccountType(AccountStatus.ACTIVE, AccountType.SAVINGS))
                .willReturn(List.of(account));

        given(accountRepository.findByAccountNumberWithLock(account.getAccountNumber()))
                .willReturn(java.util.Optional.of(account));

        given(transactionRepository.save(any(Transaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        scheduledJobService.applyMonthlyInterest();

        // Then
        then(transactionRepository).should().save(transactionCaptor.capture());
        then(accountRepository).should().save(account);

        Transaction interestTx = transactionCaptor.getValue();
        assertThat(interestTx.getTransactionType()).isEqualTo(TransactionType.INTEREST);
        assertThat(interestTx.getTargetAccount()).isEqualTo(account);
        assertThat(interestTx.getAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(account.getBalance()).isGreaterThan(new BigDecimal("100000.00"));
    }

    // Given-When-Then for generateMonthlyStatements
    @Test
    void givenAccountWithoutExistingStatement_whenGenerateMonthlyStatements_thenStatementSaved() {
        // Given
        Account account = new Account();
        account.setAccountNumber("ACC999");
        account.setBalance(new BigDecimal("1000.00"));

        given(accountRepository.findAll()).willReturn(List.of(account));

        // No existing statement
        given(accountStatementRepository.findByAccountAndPeriodStartAndPeriodEnd(
                eq(account), any(LocalDate.class), any(LocalDate.class))
        ).willReturn(java.util.Optional.empty());

        // No transactions in period
        given(transactionRepository.findByAccountAndCreatedAtBetween(
                eq(account), any(LocalDateTime.class), any(LocalDateTime.class))
        ).willReturn(Collections.emptyList());

        // When
        scheduledJobService.generateMonthlyStatements();

        // Then
        then(accountStatementRepository).should().save(any(AccountStatement.class));
    }
}

