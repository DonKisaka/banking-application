package com.banking_application.service;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.CreateAccountRequestDto;
import com.banking_application.exception.AccountStateException;
import com.banking_application.exception.DuplicateResourceException;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.mapper.AccountMapper;
import com.banking_application.model.Account;
import com.banking_application.model.AccountStatus;
import com.banking_application.model.AccountType;
import com.banking_application.model.User;
import com.banking_application.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    // Given-When-Then + Mocking + Verification + Exception Testing
    @Test
    void givenSavingsAccountAlreadyExists_whenCreateAccount_thenThrowsDuplicateResourceException() {
        // Given
        User user = User.builder()
                .username("DonaldKisaka")
                .build();

        CreateAccountRequestDto request = new CreateAccountRequestDto(
                AccountType.SAVINGS,
                BigDecimal.valueOf(1_000),
                "KSH"
        );

        Account existingAccount = Account.builder()
                .user(user)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();

        given(accountRepository.findByUserAndAccountType(user, AccountType.SAVINGS))
                .willReturn(Optional.of(existingAccount));

        // When + Then
        assertThatThrownBy(() -> accountService.createAccount(request, user))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Account")
                .hasMessageContaining("type")
                .hasMessageContaining("SAVINGS");

        // Verification - no account is saved or mapped when duplicate detected
        then(accountRepository).should(never()).save(any(Account.class));
        then(accountMapper).shouldHaveNoInteractions();
    }

    // Given-When-Then + Mocking + ArgumentCaptor + Verification
    @Test
    void givenValidSavingsRequestAndNoExistingSavings_whenCreateAccount_thenAccountCreatedAndMapped() {
        // Given
        User user = User.builder()
                .username("alice")
                .build();

        CreateAccountRequestDto request = new CreateAccountRequestDto(
                AccountType.SAVINGS,
                BigDecimal.valueOf(500),
                "KSH"
        );

        given(accountRepository.findByUserAndAccountType(user, AccountType.SAVINGS))
                .willReturn(Optional.empty());

        Account mappedAccount = new Account();
        mappedAccount.setAccountType(AccountType.SAVINGS);
        mappedAccount.setStatus(AccountStatus.ACTIVE);
        mappedAccount.setCurrency("KSH");

        given(accountMapper.toEntity(request)).willReturn(mappedAccount);

        UUID accountUuid = UUID.randomUUID();
        AccountResponseDto responseDto = new AccountResponseDto(
                accountUuid,
                "0707412258",
                BigDecimal.valueOf(50000),
                "KSH",
                AccountStatus.ACTIVE,
                AccountType.SAVINGS
        );

        given(accountRepository.save(any(Account.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(accountMapper.toDto(any(Account.class))).willReturn(responseDto);

        // When
        AccountResponseDto result = accountService.createAccount(request, user);

        // Then
        then(accountRepository).should().findByUserAndAccountType(user, AccountType.SAVINGS);
        then(accountRepository).should().save(accountCaptor.capture());
        then(accountMapper).should().toEntity(request);
        then(accountMapper).should().toDto(any(Account.class));

        Account savedAccount = accountCaptor.getValue();

        assertThat(savedAccount.getUser()).isEqualTo(user);
        assertThat(savedAccount.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedAccount.getAccountNumber()).as("generated account number").isNotNull();

        assertThat(result).isEqualTo(responseDto);
    }

    // Exception Testing + Verification for freezeAccount
    @Test
    void givenClosedAccount_whenFreezeAccount_thenThrowsAccountStateException() {
        // Given
        String accountNumber = "ACC123";

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setStatus(AccountStatus.CLOSED);

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        // When + Then
        assertThatThrownBy(() -> accountService.freezeAccount(accountNumber))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("closed");

        // Verification - closed account is not saved again
        then(accountRepository).should(never()).save(any(Account.class));
    }

    // Given-When-Then + ArgumentCaptor for freezeAccount happy path
    @Test
    void givenActiveAccount_whenFreezeAccount_thenStatusUpdatedToFrozenAndSaved() {
        // Given
        String accountNumber = "ACC999";

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setStatus(AccountStatus.ACTIVE);

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        AccountResponseDto responseDto = new AccountResponseDto(
                UUID.randomUUID(),
                accountNumber,
                BigDecimal.ZERO,
                "KSH",
                AccountStatus.FROZEN,
                AccountType.CURRENT
        );

        given(accountRepository.save(any(Account.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(accountMapper.toDto(any(Account.class))).willReturn(responseDto);

        // When
        AccountResponseDto result = accountService.freezeAccount(accountNumber);

        // Then
        then(accountRepository).should().findByAccountNumber(accountNumber);
        then(accountRepository).should().save(accountCaptor.capture());
        then(accountMapper).should().toDto(any(Account.class));

        Account saved = accountCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.FROZEN);

        assertThat(result.accountStatus()).isEqualTo(AccountStatus.FROZEN);
        assertThat(result.accountNumber()).isEqualTo(accountNumber);
    }

    // Exception Testing for getAccountDetails
    @Test
    void givenNonExistingAccount_whenGetAccountDetails_thenThrowsResourceNotFoundException() {
        // Given
        String accountNumber = "MISSING";
        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.empty());

        // When + Then
        assertThatThrownBy(() -> accountService.getAccountDetails(accountNumber))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account")
                .hasMessageContaining("accountNumber")
                .hasMessageContaining(accountNumber);

        then(accountMapper).shouldHaveNoInteractions();
    }
}

