package com.banking_application.service;

import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.AccountStatementRepository;
import com.banking_application.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledJobServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountStatementRepository accountStatementRepository;

    @InjectMocks
    private ScheduledJobService underTest;

    private User testUser;
    private Account savingsAccount;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .phoneNumber("+1234567890")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        savingsAccount = Account.builder()
                .id(1L)
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("SAV001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("10000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .interestRate(new BigDecimal("4.00"))
                .minimumBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void applyMonthlyInterest_shouldApplyInterestToActiveSavingsAccounts() {
        when(accountRepository.findByStatusAndAccountType(AccountStatus.ACTIVE, AccountType.SAVINGS))
                .thenReturn(List.of(savingsAccount));
        when(accountRepository.findByAccountNumberWithLock("SAV001"))
                .thenReturn(Optional.of(savingsAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        underTest.applyMonthlyInterest();

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        BigDecimal newBalance = accountCaptor.getValue().getBalance();
        // 10000 * 4% / 12 = 33.3333...
        assertThat(newBalance).isGreaterThan(new BigDecimal("10000"));
        assertThat(newBalance).isLessThan(new BigDecimal("10034"));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction interestTx = txCaptor.getValue();
        assertThat(interestTx.getTransactionType()).isEqualTo(TransactionType.INTEREST);
        assertThat(interestTx.getTargetAccount()).isEqualTo(savingsAccount);
        assertThat(interestTx.getTransactionStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void applyMonthlyInterest_shouldSkipAccountsWithZeroBalance() {
        savingsAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findByStatusAndAccountType(AccountStatus.ACTIVE, AccountType.SAVINGS))
                .thenReturn(List.of(savingsAccount));

        underTest.applyMonthlyInterest();

        verify(accountRepository, never()).findByAccountNumberWithLock(anyString());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void applyMonthlyInterest_shouldSkipAccountsBelowMinimumBalance() {
        savingsAccount.setMinimumBalance(new BigDecimal("50000"));
        when(accountRepository.findByStatusAndAccountType(AccountStatus.ACTIVE, AccountType.SAVINGS))
                .thenReturn(List.of(savingsAccount));
        when(accountRepository.findByAccountNumberWithLock("SAV001"))
                .thenReturn(Optional.of(savingsAccount));

        underTest.applyMonthlyInterest();

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void generateMonthlyStatements_shouldCreateStatementForAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of(savingsAccount));
        when(accountStatementRepository.findByAccountAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByAccountAndCreatedAtBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(accountStatementRepository.save(any(AccountStatement.class))).thenAnswer(i -> i.getArgument(0));

        underTest.generateMonthlyStatements();

        ArgumentCaptor<AccountStatement> captor = ArgumentCaptor.forClass(AccountStatement.class);
        verify(accountStatementRepository).save(captor.capture());
        AccountStatement stmt = captor.getValue();
        assertThat(stmt.getAccount()).isEqualTo(savingsAccount);
        assertThat(stmt.getPeriodStart().getDayOfMonth()).isEqualTo(1);
        assertThat(stmt.getOpeningBalance()).isNotNull();
        assertThat(stmt.getClosingBalance()).isNotNull();
        assertThat(stmt.getTransactionCount()).isEqualTo(0);
    }

    @Test
    void generateMonthlyStatements_shouldSkipWhenStatementAlreadyExists() {
        when(accountRepository.findAll()).thenReturn(List.of(savingsAccount));
        when(accountStatementRepository.findByAccountAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.of(AccountStatement.builder().build()));

        underTest.generateMonthlyStatements();

        verify(transactionRepository, never()).findByAccountAndCreatedAtBetween(any(), any(), any());
        verify(accountStatementRepository, never()).save(any());
    }
}
