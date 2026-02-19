package com.banking_application.service;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.mapper.AccountMapper;
import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AccountServiceCacheTest.TestConfig.class)
class AccountServiceCacheTest {

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
        public AccountMapper accountMapper() {
            return mock(AccountMapper.class);
        }

        @Bean
        public AccountService accountService(AccountRepository accountRepository, AccountMapper accountMapper) {
            return new AccountService(accountRepository, accountMapper);
        }
    }

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private CacheManager cacheManager;

    private Account testAccount;
    private AccountResponseDto testDto;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear()
        );
        reset(accountRepository, accountMapper);

        testAccount = Account.builder()
                .id(1L)
                .accountUuid(UUID.randomUUID())
                .accountNumber("ACC123456789")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();

        testDto = new AccountResponseDto(
                testAccount.getAccountUuid(),
                "ACC123456789",
                new BigDecimal("5000.0000"),
                "USD",
                AccountStatus.ACTIVE,
                AccountType.SAVINGS
        );
    }

    @Test
    void getAccountDetails_shouldCacheResultOnFirstCallAndReturnCachedOnSecond() {
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(testAccount));
        when(accountMapper.toDto(testAccount)).thenReturn(testDto);

        AccountResponseDto first = accountService.getAccountDetails("ACC123456789");
        AccountResponseDto second = accountService.getAccountDetails("ACC123456789");

        assertThat(first).isEqualTo(second);
        verify(accountRepository, times(1)).findByAccountNumber("ACC123456789");
    }

    @Test
    void getAccountDetails_shouldReturnDifferentCacheEntriesForDifferentAccounts() {
        Account otherAccount = Account.builder()
                .id(2L)
                .accountUuid(UUID.randomUUID())
                .accountNumber("ACC987654321")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("10000.0000"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();

        AccountResponseDto otherDto = new AccountResponseDto(
                otherAccount.getAccountUuid(),
                "ACC987654321",
                new BigDecimal("10000.0000"),
                "USD",
                AccountStatus.ACTIVE,
                AccountType.CURRENT
        );

        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(testAccount));
        when(accountMapper.toDto(testAccount)).thenReturn(testDto);
        when(accountRepository.findByAccountNumber("ACC987654321")).thenReturn(Optional.of(otherAccount));
        when(accountMapper.toDto(otherAccount)).thenReturn(otherDto);

        AccountResponseDto first = accountService.getAccountDetails("ACC123456789");
        AccountResponseDto second = accountService.getAccountDetails("ACC987654321");

        assertThat(first.accountNumber()).isEqualTo("ACC123456789");
        assertThat(second.accountNumber()).isEqualTo("ACC987654321");
        verify(accountRepository).findByAccountNumber("ACC123456789");
        verify(accountRepository).findByAccountNumber("ACC987654321");
    }

    @Test
    void freezeAccount_shouldEvictAccountDetailsCache() {
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(testAccount));
        when(accountMapper.toDto(any(Account.class))).thenReturn(testDto);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.getAccountDetails("ACC123456789");
        verify(accountRepository, times(1)).findByAccountNumber("ACC123456789");

        accountService.freezeAccount("ACC123456789");

        accountService.getAccountDetails("ACC123456789");
        verify(accountRepository, times(3)).findByAccountNumber("ACC123456789");
    }

    @Test
    void closeAccount_shouldEvictAccountDetailsCache() {
        testAccount.setBalance(BigDecimal.ZERO);

        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(testAccount));
        when(accountMapper.toDto(any(Account.class))).thenReturn(testDto);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.getAccountDetails("ACC123456789");
        verify(accountRepository, times(1)).findByAccountNumber("ACC123456789");

        accountService.closeAccount("ACC123456789");

        accountService.getAccountDetails("ACC123456789");
        verify(accountRepository, times(3)).findByAccountNumber("ACC123456789");
    }

    @Test
    void reactivateAccount_shouldEvictAccountDetailsCache() {
        testAccount.setStatus(AccountStatus.FROZEN);

        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(testAccount));
        when(accountMapper.toDto(any(Account.class))).thenReturn(testDto);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.getAccountDetails("ACC123456789");
        verify(accountRepository, times(1)).findByAccountNumber("ACC123456789");

        accountService.reactivateAccount("ACC123456789");

        accountService.getAccountDetails("ACC123456789");
        verify(accountRepository, times(3)).findByAccountNumber("ACC123456789");
    }

    @Test
    void getAccountDetails_cacheEntryShouldBeStoredInCorrectCache() {
        when(accountRepository.findByAccountNumber("ACC123456789")).thenReturn(Optional.of(testAccount));
        when(accountMapper.toDto(testAccount)).thenReturn(testDto);

        accountService.getAccountDetails("ACC123456789");

        assertThat(cacheManager.getCache("accountDetails").get("ACC123456789")).isNotNull();
    }
}
