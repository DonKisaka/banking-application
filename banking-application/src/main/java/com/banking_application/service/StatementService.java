package com.banking_application.service;

import com.banking_application.dto.StatementResponseDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.model.Account;
import com.banking_application.model.AccountStatement;
import com.banking_application.model.User;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.AccountStatementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatementService {

    private final AccountRepository accountRepository;
    private final AccountStatementRepository accountStatementRepository;

    public StatementService(AccountRepository accountRepository,
                            AccountStatementRepository accountStatementRepository) {
        this.accountRepository = accountRepository;
        this.accountStatementRepository = accountStatementRepository;
    }


    @Transactional(readOnly = true)
    public List<StatementResponseDto> getStatementsForAccount(String accountNumber, User user, int limit) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        }

        return accountStatementRepository
                .findByAccountOrderByPeriodEndDesc(account, PageRequest.of(0, limit))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private StatementResponseDto toDto(AccountStatement s) {
        return new StatementResponseDto(
                s.getStatementUuid(),
                s.getPeriodStart(),
                s.getPeriodEnd(),
                s.getOpeningBalance(),
                s.getClosingBalance(),
                s.getTransactionCount(),
                s.getGeneratedAt()
        );
    }
}
