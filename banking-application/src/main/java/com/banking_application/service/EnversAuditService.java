package com.banking_application.service;

import com.banking_application.dto.RevisionInfoDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.model.Account;
import com.banking_application.model.Transaction;
import com.banking_application.model.User;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.TransactionRepository;
import com.banking_application.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnversAuditService {

    private final EntityManager entityManager;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public EnversAuditService(EntityManager entityManager,
                              AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.entityManager = entityManager;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private AuditReader getAuditReader() {
        return AuditReaderFactory.get(entityManager);
    }

    @Transactional(readOnly = true)
    public List<RevisionInfoDto> getAccountRevisions(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        List<Number> revisions = getAuditReader().getRevisions(Account.class, account.getId());
        return toRevisionInfoDtos(getAuditReader(), revisions);
    }

    @Transactional(readOnly = true)
    public List<RevisionInfoDto> getAccountRevisionsByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
        return getAccountRevisions(account.getId());
    }

    @Transactional(readOnly = true)
    public Account getAccountAtRevision(Long accountId, Number revisionNumber) {
        Account account = getAuditReader().find(Account.class, accountId, revisionNumber);
        if (account == null) {
            throw new ResourceNotFoundException("Account", "revision", revisionNumber);
        }
        return account;
    }

    @Transactional(readOnly = true)
    public List<RevisionInfoDto> getTransactionRevisions(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        List<Number> revisions = getAuditReader().getRevisions(Transaction.class, transaction.getId());
        return toRevisionInfoDtos(getAuditReader(), revisions);
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionAtRevision(Long transactionId, Number revisionNumber) {
        Transaction transaction = getAuditReader().find(Transaction.class, transactionId, revisionNumber);
        if (transaction == null) {
            throw new ResourceNotFoundException("Transaction", "revision", revisionNumber);
        }
        return transaction;
    }

    @Transactional(readOnly = true)
    public List<RevisionInfoDto> getUserRevisions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<Number> revisions = getAuditReader().getRevisions(User.class, user.getId());
        return toRevisionInfoDtos(getAuditReader(), revisions);
    }

    @Transactional(readOnly = true)
    public User getUserAtRevision(Long userId, Number revisionNumber) {
        User user = getAuditReader().find(User.class, userId, revisionNumber);
        if (user == null) {
            throw new ResourceNotFoundException("User", "revision", revisionNumber);
        }
        return user;
    }

    private List<RevisionInfoDto> toRevisionInfoDtos(AuditReader reader, List<Number> revisions) {
        return revisions.stream()
                .map(rev -> {
                    java.util.Date date = reader.getRevisionDate(rev);
                    Instant timestamp = date != null ? date.toInstant() : Instant.EPOCH;
                    return new RevisionInfoDto(rev, timestamp);
                })
                .collect(Collectors.toList());
    }
}
