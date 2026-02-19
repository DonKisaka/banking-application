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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional
    public AccountResponseDto createAccount(CreateAccountRequestDto dto, User user) {
        if (dto.accountType() == AccountType.SAVINGS) {
            boolean hasSavings = accountRepository
                    .findByUserAndAccountType(user, AccountType.SAVINGS)
                    .stream()
                    .anyMatch(a -> a.getStatus() == AccountStatus.ACTIVE);

            if (hasSavings) {
                throw new DuplicateResourceException("Account", "type", "SAVINGS");
            }
        }

        Account account = accountMapper.toEntity(dto);

        account.setUser(user);
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setStatus(AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);

        return accountMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccountDetails(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getUserAccounts(User user) {
        return accountRepository.findByUser(user)
                .stream()
                .map(accountMapper::toDto)
                .toList();
    }

    @Transactional
    public AccountResponseDto freezeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountStateException(accountNumber, "Cannot freeze a closed account");
        }
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new AccountStateException(accountNumber, "Account is already frozen");
        }

        account.setStatus(AccountStatus.FROZEN);
        Account saved = accountRepository.save(account);
        return accountMapper.toDto(saved);
    }

    @Transactional
    public AccountResponseDto closeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountStateException(accountNumber, "Account is already closed");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountStateException(accountNumber,
                    "Cannot close account with non-zero balance. Current balance: " + account.getBalance());
        }

        account.setStatus(AccountStatus.CLOSED);
        Account saved = accountRepository.save(account);
        return accountMapper.toDto(saved);
    }

    @Transactional
    public AccountResponseDto reactivateAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new AccountStateException(accountNumber, "Account is already active");
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountStateException(accountNumber, "Cannot reactivate a closed account");
        }

        account.setStatus(AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);
        return accountMapper.toDto(saved);
    }

    @Transactional
    public Account getAccountWithLock(String accountNumber) {
        return accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
    }

    private String generateUniqueAccountNumber() {
        long number = java.util.concurrent.ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L);
        String accNum = String.valueOf(number);

        if (accountRepository.existsByAccountNumber(accNum)) {
            return generateUniqueAccountNumber();
        }
        return accNum;
    }
}
