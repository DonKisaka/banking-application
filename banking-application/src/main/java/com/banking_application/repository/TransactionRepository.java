package com.banking_application.repository;

import com.banking_application.model.Account;
import com.banking_application.model.Transaction;
import com.banking_application.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(Account source, Account target);

    Optional<Transaction> findByTransactionReference(String transactionReference);

    @Query("SELECT t FROM Transaction t WHERE t.amount > :threshold AND t.createdAt > :since")
    List<Transaction> findHighValueTransactions(@Param("threshold") BigDecimal threshold, @Param("since") LocalDateTime since);
}
