package com.banking_application.service;

import com.banking_application.model.*;
import com.banking_application.repository.AccountRepository;
import com.banking_application.repository.AccountStatementRepository;
import com.banking_application.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;


@Service
public class ScheduledJobService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountStatementRepository accountStatementRepository;

    public ScheduledJobService(AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               AccountStatementRepository accountStatementRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountStatementRepository = accountStatementRepository;
    }

    /**
     * Apply monthly interest to SAVINGS accounts.
     * Runs at 00:05 on the 1st of each month (cron: 5 0 1 * * *)
     * Formula: monthly interest = balance * (annualRate/100) / 12
     */
    @Caching(evict = {
            @CacheEvict(value = "accountDetails", allEntries = true),
            @CacheEvict(value = "transactionHistory", allEntries = true)
    })
    @Scheduled(cron = "${scheduling.interest.cron:0 5 0 1 * *}")
    @Transactional
    public void applyMonthlyInterest() {
        log.info("Starting monthly interest application");
        List<Account> savingsAccounts = accountRepository.findByStatusAndAccountType(
                AccountStatus.ACTIVE, AccountType.SAVINGS);

        int applied = 0;
        for (Account account : savingsAccounts) {
            try {
                Account locked = accountRepository.findByAccountNumberWithLock(account.getAccountNumber())
                        .orElse(null);
                if (locked == null || locked.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (locked.getBalance().compareTo(locked.getMinimumBalance()) < 0) {
                    log.debug("Skipping account {} - below minimum balance", locked.getAccountNumber());
                    continue;
                }

                BigDecimal annualRate = locked.getInterestRate() != null ? locked.getInterestRate() : BigDecimal.ZERO;
                if (annualRate.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal monthlyInterest = locked.getBalance()
                        .multiply(annualRate)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_EVEN)
                        .divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_EVEN);

                if (monthlyInterest.compareTo(BigDecimal.ZERO) <= 0) continue;

                locked.credit(monthlyInterest);

                Transaction interestTx = Transaction.builder()
                        .transactionUuid(UUID.randomUUID())
                        .targetAccount(locked)
                        .amount(monthlyInterest)
                        .currency(locked.getCurrency())
                        .transactionType(TransactionType.INTEREST)
                        .transactionStatus(TransactionStatus.SUCCESS)
                        .description("Monthly interest " + annualRate + "% p.a.")
                        .initiatedBy(null)
                        .completedAt(LocalDateTime.now())
                        .build();
                transactionRepository.save(interestTx);
                accountRepository.save(locked);
                applied++;
                log.debug("Applied interest {} to account {}", monthlyInterest, locked.getAccountNumber());
            } catch (Exception e) {
                log.error("Failed to apply interest to account {}: {}", account.getAccountNumber(), e.getMessage());
            }
        }
        log.info("Monthly interest application completed. Applied to {} accounts", applied);
    }

    /**
     * Generate monthly statements for all active accounts.
     * Runs at 01:00 on the 1st of each month (cron: 0 0 1 1 * *)
     * Generates statements for the previous calendar month.
     */
    @Scheduled(cron = "${scheduling.statements.cron:0 0 1 1 * *}")
    @Transactional
    public void generateMonthlyStatements() {
        LocalDate today = LocalDate.now();
        LocalDate periodEnd = today.minusMonths(1).withDayOfMonth(
                today.minusMonths(1).lengthOfMonth());
        LocalDate periodStart = periodEnd.withDayOfMonth(1);

        log.info("Starting monthly statement generation for period {} to {}", periodStart, periodEnd);

        List<Account> accounts = accountRepository.findAll();
        LocalDateTime from = periodStart.atStartOfDay();
        LocalDateTime to = periodEnd.atTime(LocalTime.MAX).plusNanos(1);

        int generated = 0;
        for (Account account : accounts) {
            try {
                if (accountStatementRepository.findByAccountAndPeriodStartAndPeriodEnd(
                        account, periodStart, periodEnd).isPresent()) {
                    log.debug("Statement already exists for account {} period {}", account.getAccountNumber(), periodStart);
                    continue;
                }

                List<Transaction> transactions = transactionRepository.findByAccountAndCreatedAtBetween(account, from, to);

                BigDecimal credits = BigDecimal.ZERO;
                BigDecimal debits = BigDecimal.ZERO;
                for (Transaction t : transactions) {
                    BigDecimal amount = t.getAmount();
                    if (account.equals(t.getTargetAccount())) {
                        credits = credits.add(amount);
                    }
                    if (account.equals(t.getSourceAccount())) {
                        debits = debits.add(amount);
                    }
                }
                BigDecimal netChange = credits.subtract(debits);
                BigDecimal closingBalance = account.getBalance();
                BigDecimal openingBalance = closingBalance.subtract(netChange);

                AccountStatement statement = AccountStatement.builder()
                        .account(account)
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .openingBalance(openingBalance)
                        .closingBalance(closingBalance)
                        .transactionCount(transactions.size())
                        .generatedAt(LocalDateTime.now())
                        .build();
                accountStatementRepository.save(statement);
                generated++;
                log.debug("Generated statement for account {}", account.getAccountNumber());
            } catch (Exception e) {
                log.error("Failed to generate statement for account {}: {}", account.getAccountNumber(), e.getMessage());
            }
        }
        log.info("Monthly statement generation completed. Generated {} statements", generated);
    }
}
