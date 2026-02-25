package com.banking_application.service;

import com.banking_application.dto.StatementResponseDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.model.Account;
import com.banking_application.model.AccountStatement;
import com.banking_application.model.User;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.AccountStatementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountStatementRepository accountStatementRepository;

    @InjectMocks
    private StatementService statementService;

    // Exception Testing when account not found
    @Test
    void givenUnknownAccount_whenGetStatementsForAccount_thenThrowsResourceNotFoundException() {
        // Given
        String accountNumber = "UNKNOWN";
        User user = User.builder().id(1L).build();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.empty());

        // When + Then
        assertThatThrownBy(() -> statementService.getStatementsForAccount(accountNumber, user, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account")
                .hasMessageContaining("accountNumber")
                .hasMessageContaining(accountNumber);
    }

    // Exception Testing when account does not belong to user
    @Test
    void givenAccountOfAnotherUser_whenGetStatementsForAccount_thenThrowsResourceNotFoundException() {
        // Given
        String accountNumber = "ACC123";
        User owner = User.builder().id(2L).build();
        User otherUser = User.builder().id(1L).build();

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUser(owner);

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        // When + Then
        assertThatThrownBy(() -> statementService.getStatementsForAccount(accountNumber, otherUser, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Given-When-Then + Mocking + Verification for successful retrieval
    @Test
    void givenValidAccountAndUser_whenGetStatementsForAccount_thenReturnsMappedDtos() {
        // Given
        String accountNumber = "ACC999";
        User user = User.builder().id(1L).build();

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUser(user);

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        AccountStatement statement = AccountStatement.builder()
                .statementUuid(UUID.randomUUID())
                .account(account)
                .periodStart(LocalDate.of(2025, 1, 1))
                .periodEnd(LocalDate.of(2025, 1, 31))
                .openingBalance(new BigDecimal("100.00"))
                .closingBalance(new BigDecimal("150.00"))
                .transactionCount(5)
                .generatedAt(LocalDateTime.now())
                .build();

        given(accountStatementRepository.findByAccountOrderByPeriodEndDesc(
                eq(account),
                any(PageRequest.class)
        )).willReturn(List.of(statement));

        // When
        List<StatementResponseDto> result =
                statementService.getStatementsForAccount(accountNumber, user, 5);

        // Then
        then(accountRepository).should().findByAccountNumber(accountNumber);
        then(accountStatementRepository).should()
                .findByAccountOrderByPeriodEndDesc(eq(account), any(PageRequest.class));

        assertThat(result).hasSize(1);
        StatementResponseDto dto = result.get(0);
        assertThat(dto.statementUuid()).isEqualTo(statement.getStatementUuid());
        assertThat(dto.periodStart()).isEqualTo(statement.getPeriodStart());
        assertThat(dto.periodEnd()).isEqualTo(statement.getPeriodEnd());
        assertThat(dto.openingBalance()).isEqualTo(statement.getOpeningBalance());
        assertThat(dto.closingBalance()).isEqualTo(statement.getClosingBalance());
        assertThat(dto.transactionCount()).isEqualTo(statement.getTransactionCount());
        assertThat(dto.generatedAt()).isEqualTo(statement.getGeneratedAt());
    }
}

