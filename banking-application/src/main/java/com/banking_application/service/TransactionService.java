package com.banking_application.service;

import com.banking_application.dto.*;
import com.banking_application.exception.AccountStateException;
import com.banking_application.exception.InvalidTransactionException;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.mapper.TransactionMapper;
import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final FraudDetectionService fraudDetectionService;
    private final AuditLogService auditLogService;

    public TransactionService(AccountService accountService,
                              AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              TransactionMapper transactionMapper,
                              FraudDetectionService fraudDetectionService,
                              AuditLogService auditLogService) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.fraudDetectionService = fraudDetectionService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequestDto dto, User initiatedBy) {
        Account account = accountRepository.findByAccountNumberWithLock(dto.accountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", dto.accountNumber()));

        if (!account.isActive()) {
            throw new AccountStateException(dto.accountNumber(), "Account is not active");
        }

        account.credit(dto.amount());

        Transaction transaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .targetAccount(account)
                .amount(dto.amount())
                .currency(account.getCurrency())
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.SUCCESS)
                .description(dto.description())
                .initiatedBy(initiatedBy)
                .completedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        fraudDetectionService.evaluateTransaction(saved);
        auditLogService.logAction("DEPOSIT", initiatedBy, "account:" + dto.accountNumber(),
                "Deposited " + dto.amount() + " " + account.getCurrency(),
                AuditStatus.SUCCESS, null, null);

        return transactionMapper.toDto(saved);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawalRequestDto dto, User initiatedBy) {
        Account account = accountRepository.findByAccountNumberWithLock(dto.accountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", dto.accountNumber()));

        if (!account.isActive()) {
            throw new AccountStateException(dto.accountNumber(), "Account is not active");
        }

        account.debit(dto.amount());

        Transaction transaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .sourceAccount(account)
                .amount(dto.amount())
                .currency(account.getCurrency())
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionStatus(TransactionStatus.SUCCESS)
                .description(dto.description())
                .initiatedBy(initiatedBy)
                .completedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        fraudDetectionService.evaluateTransaction(saved);
        auditLogService.logAction("WITHDRAWAL", initiatedBy, "account:" + dto.accountNumber(),
                "Withdrew " + dto.amount() + " " + account.getCurrency(),
                AuditStatus.SUCCESS, null, null);

        return transactionMapper.toDto(saved);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequestDto dto, User initiatedBy) {
        if (dto.sourceAccountNumber().equals(dto.targetAccountNumber())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }

        String firstLock;
        String secondLock;
        if (dto.sourceAccountNumber().compareTo(dto.targetAccountNumber()) < 0) {
            firstLock = dto.sourceAccountNumber();
            secondLock = dto.targetAccountNumber();
        } else {
            firstLock = dto.targetAccountNumber();
            secondLock = dto.sourceAccountNumber();
        }

        Account firstAccount = accountRepository.findByAccountNumberWithLock(firstLock)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", firstLock));
        Account secondAccount = accountRepository.findByAccountNumberWithLock(secondLock)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", secondLock));

        Account sourceAccount = firstLock.equals(dto.sourceAccountNumber()) ? firstAccount : secondAccount;
        Account targetAccount = firstLock.equals(dto.targetAccountNumber()) ? firstAccount : secondAccount;

        if (!sourceAccount.isActive()) {
            throw new AccountStateException(dto.sourceAccountNumber(), "Source account is not active");
        }
        if (!targetAccount.isActive()) {
            throw new AccountStateException(dto.targetAccountNumber(), "Target account is not active");
        }
        if (!sourceAccount.getCurrency().equals(targetAccount.getCurrency())) {
            throw new InvalidTransactionException("Currency mismatch between source and target accounts");
        }

        sourceAccount.debit(dto.amount());
        targetAccount.credit(dto.amount());

        Transaction transaction = Transaction.builder()
                .transactionUuid(UUID.randomUUID())
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(dto.amount())
                .currency(sourceAccount.getCurrency())
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESS)
                .description(dto.description())
                .initiatedBy(initiatedBy)
                .completedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        fraudDetectionService.evaluateTransaction(saved);
        auditLogService.logAction("TRANSFER", initiatedBy,
                "account:" + dto.sourceAccountNumber() + " -> account:" + dto.targetAccountNumber(),
                "Transferred " + dto.amount() + " " + sourceAccount.getCurrency(),
                AuditStatus.SUCCESS, null, null);

        return transactionMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        List<Transaction> transactions = transactionRepository
                .findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(account, account);

        return transactionMapper.toDto(transactions);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String reference) {
        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "reference", reference));

        return transactionMapper.toDto(transaction);
    }
}
