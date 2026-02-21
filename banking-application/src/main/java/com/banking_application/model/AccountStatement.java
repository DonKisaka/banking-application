package com.banking_application.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_statements",
        indexes = {@Index(name = "idx_statement_account_period", columnList = "account_id, period_start, period_end")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_uuid", unique = true, nullable = false, updatable = false)
    private UUID statementUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "opening_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal closingBalance;

    @Column(name = "transaction_count", nullable = false)
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        if (statementUuid == null) statementUuid = UUID.randomUUID();
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }
}
