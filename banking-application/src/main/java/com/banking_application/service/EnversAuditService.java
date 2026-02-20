package com.banking_application.service;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.RevisionInfoDto;
import com.banking_application.dto.UserAuditDto;
import com.banking_application.mapper.AccountMapper;
import com.banking_application.model.Account;
import com.banking_application.model.Transaction;
import com.banking_application.model.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EnversAuditService {

    private final EntityManager entityManager;
    private final AccountMapper accountMapper;

    @Transactional(readOnly = true)
    public List<RevisionInfoDto> getAccountRevisions(Long accountId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        @SuppressWarnings("unchecked")
        List<Object[]> results = reader.createQuery()
                .forRevisionsOfEntity(Account.class, false, true)
                .add(AuditEntity.id().eq(accountId))
                .getResultList();

        return results.stream()
                .map(row -> {
                    Number rev = (Number) row[1];
                    Object revEntity = row[2];
                    Instant ts = getRevisionTimestamp(revEntity);
                    return new RevisionInfoDto(rev, ts);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AccountResponseDto> getAccountAtRevision(Long accountId, Number revisionNumber) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        try {
            Account account = reader.find(Account.class, accountId, revisionNumber);
            if (account == null) return Optional.empty();
            return Optional.of(accountMapper.toDto(account));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public List<RevisionInfoDto> getTransactionRevisions(Long transactionId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        @SuppressWarnings("unchecked")
        List<Object[]> results = reader.createQuery()
                .forRevisionsOfEntity(Transaction.class, false, true)
                .add(AuditEntity.id().eq(transactionId))
                .getResultList();

        return results.stream()
                .map(row -> {
                    Number rev = (Number) row[1];
                    Object revEntity = row[2];
                    Instant ts = getRevisionTimestamp(revEntity);
                    return new RevisionInfoDto(rev, ts);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Transaction> getTransactionAtRevision(Long transactionId, Number revisionNumber) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        try {
            Transaction tx = reader.find(Transaction.class, transactionId, revisionNumber);
            return Optional.ofNullable(tx);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public List<RevisionInfoDto> getUserRevisions(Long userId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        @SuppressWarnings("unchecked")
        List<Object[]> results = reader.createQuery()
                .forRevisionsOfEntity(User.class, false, true)
                .add(AuditEntity.id().eq(userId))
                .getResultList();

        return results.stream()
                .map(row -> {
                    Number rev = (Number) row[1];
                    Object revEntity = row[2];
                    Instant ts = getRevisionTimestamp(revEntity);
                    return new RevisionInfoDto(rev, ts);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserAuditDto> getUserAtRevision(Long userId, Number revisionNumber) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        try {
            User user = reader.find(User.class, userId, revisionNumber);
            if (user == null) return Optional.empty();
            return Optional.of(new UserAuditDto(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getIsActive()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Instant getRevisionTimestamp(Object revEntity) {
        if (revEntity == null) return Instant.now();
        try {
            Long ts = (Long) revEntity.getClass().getMethod("getTimestamp").invoke(revEntity);
            return ts != null ? Instant.ofEpochMilli(ts) : Instant.now();
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
