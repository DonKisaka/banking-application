package com.banking_application.service;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.CreateAccountRequestDto;
import com.banking_application.mapper.AccountMapper;
import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService underTest;

    private User testUser;
    private Account activeAccount;
    private AccountResponseDto responseDto;

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

        activeAccount = Account.builder()
                .id(1L)
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("ACC123456789")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();

        responseDto = new AccountResponseDto(
                activeAccount.getAccountUuid(),
                activeAccount.getAccountNumber(),
                activeAccount.getBalance(),
                activeAccount.getCurrency(),
                AccountStatus.ACTIVE,
                AccountType.SAVINGS
        );
    }

    // --- getUserAccounts tests ---

    @Test
    void getUserAccounts_shouldReturnAllAccountsForUser() {
        // given
        Account secondAccount = Account.builder()
                .id(2L)
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("ACC987654321")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("10000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        AccountResponseDto secondDto = new AccountResponseDto(
                secondAccount.getAccountUuid(),
                secondAccount.getAccountNumber(),
                secondAccount.getBalance(),
                secondAccount.getCurrency(),
                AccountStatus.ACTIVE,
                AccountType.CURRENT
        );

        when(accountRepository.findByUser(testUser)).thenReturn(List.of(activeAccount, secondAccount));
        when(accountMapper.toDto(activeAccount)).thenReturn(responseDto);
        when(accountMapper.toDto(secondAccount)).thenReturn(secondDto);

        // when
        List<AccountResponseDto> result = underTest.getUserAccounts(testUser);

        // then
        assertThat(result).hasSize(2);
        verify(accountRepository).findByUser(testUser);
    }

    @Test
    void getUserAccounts_shouldReturnEmptyListWhenNoAccounts() {
        // given
        when(accountRepository.findByUser(testUser)).thenReturn(List.of());

        // when
        List<AccountResponseDto> result = underTest.getUserAccounts(testUser);

        // then
        assertThat(result).isEmpty();
    }

    // --- freezeAccount tests ---

    @Test
    void freezeAccount_shouldSetStatusToFrozen() {
        // given
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);
        when(accountMapper.toDto(any(Account.class))).thenReturn(responseDto);

        // when
        underTest.freezeAccount("ACC123456789");

        // then
        assertThat(activeAccount.getStatus()).isEqualTo(AccountStatus.FROZEN);
        verify(accountRepository).save(activeAccount);
    }

    @Test
    void freezeAccount_shouldThrowWhenAccountNotFound() {
        // given
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.freezeAccount("NONEXISTENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void freezeAccount_shouldThrowWhenAccountAlreadyClosed() {
        // given
        activeAccount.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));

        // when & then
        assertThatThrownBy(() -> underTest.freezeAccount("ACC123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot freeze");
    }

    // --- closeAccount tests ---

    @Test
    void closeAccount_shouldSetStatusToClosedWhenBalanceIsZero() {
        // given
        activeAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);
        when(accountMapper.toDto(any(Account.class))).thenReturn(responseDto);

        // when
        underTest.closeAccount("ACC123456789");

        // then
        assertThat(activeAccount.getStatus()).isEqualTo(AccountStatus.CLOSED);
        verify(accountRepository).save(activeAccount);
    }

    @Test
    void closeAccount_shouldThrowWhenBalanceIsNotZero() {
        // given
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));

        // when & then
        assertThatThrownBy(() -> underTest.closeAccount("ACC123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("balance");
    }

    @Test
    void closeAccount_shouldThrowWhenAccountNotFound() {
        // given
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.closeAccount("NONEXISTENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void closeAccount_shouldThrowWhenAccountAlreadyClosed() {
        // given
        activeAccount.setStatus(AccountStatus.CLOSED);
        activeAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));

        // when & then
        assertThatThrownBy(() -> underTest.closeAccount("ACC123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    // --- reactivateAccount tests ---

    @Test
    void reactivateAccount_shouldSetStatusToActiveWhenFrozen() {
        // given
        activeAccount.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);
        when(accountMapper.toDto(any(Account.class))).thenReturn(responseDto);

        // when
        underTest.reactivateAccount("ACC123456789");

        // then
        assertThat(activeAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountRepository).save(activeAccount);
    }

    @Test
    void reactivateAccount_shouldSetStatusToActiveWhenDormant() {
        // given
        activeAccount.setStatus(AccountStatus.DORMANT);
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);
        when(accountMapper.toDto(any(Account.class))).thenReturn(responseDto);

        // when
        underTest.reactivateAccount("ACC123456789");

        // then
        assertThat(activeAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void reactivateAccount_shouldThrowWhenAccountIsClosed() {
        // given
        activeAccount.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));

        // when & then
        assertThatThrownBy(() -> underTest.reactivateAccount("ACC123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reactivate");
    }

    @Test
    void reactivateAccount_shouldThrowWhenAccountAlreadyActive() {
        // given
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(activeAccount));

        // when & then
        assertThatThrownBy(() -> underTest.reactivateAccount("ACC123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");
    }

    // --- getAccountWithLock tests ---

    @Test
    void getAccountWithLock_shouldReturnAccountEntity() {
        // given
        when(accountRepository.findByAccountNumberWithLock("ACC123456789")).thenReturn(Optional.of(activeAccount));

        // when
        Account result = underTest.getAccountWithLock("ACC123456789");

        // then
        assertThat(result).isEqualTo(activeAccount);
        verify(accountRepository).findByAccountNumberWithLock("ACC123456789");
    }

    @Test
    void getAccountWithLock_shouldThrowWhenAccountNotFound() {
        // given
        when(accountRepository.findByAccountNumberWithLock("NONEXISTENT")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.getAccountWithLock("NONEXISTENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");
    }

    // --- createAccount tests ---

    @Test
    void createAccount_shouldCreateSuccessfully() {
        // given
        CreateAccountRequestDto dto = new CreateAccountRequestDto(
                AccountType.CURRENT, new BigDecimal("1000.00"), "USD"
        );
        Account newAccount = Account.builder()
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        when(accountMapper.toEntity(dto)).thenReturn(newAccount);
        when(accountRepository.save(any(Account.class))).thenReturn(newAccount);
        when(accountMapper.toDto(any(Account.class))).thenReturn(responseDto);

        // when
        AccountResponseDto result = underTest.createAccount(dto, testUser);

        // then
        assertThat(result).isNotNull();
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_shouldThrowWhenDuplicateSavingsAccount() {
        // given
        CreateAccountRequestDto dto = new CreateAccountRequestDto(
                AccountType.SAVINGS, new BigDecimal("1000.00"), "USD"
        );
        when(accountRepository.findByUserAndAccountType(testUser, AccountType.SAVINGS))
                .thenReturn(Optional.of(activeAccount));

        // when & then
        assertThatThrownBy(() -> underTest.createAccount(dto, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("savings account");
    }
}
