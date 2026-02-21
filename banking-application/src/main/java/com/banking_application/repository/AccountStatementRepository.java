package com.banking_application.repository;

import com.banking_application.model.Account;
import com.banking_application.model.AccountStatement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountStatementRepository extends JpaRepository<AccountStatement, Long> {
    Optional<AccountStatement> findByAccountAndPeriodStartAndPeriodEnd(Account account, LocalDate periodStart, LocalDate periodEnd);

    List<AccountStatement> findByAccountOrderByPeriodEndDesc(Account account, Pageable pageable);
}
