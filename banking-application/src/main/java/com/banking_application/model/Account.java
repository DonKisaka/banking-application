package com.banking_application.model;

import com.banking_application.converter.EncryptionConverter;
import com.banking_application.exception.InsufficientFundsException;
import com.banking_application.exception.InvalidAmountException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_uuid", unique = true, nullable = false, updatable = false)
    private UUID accountUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = EncryptionConverter.class)
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "account_type")
    private AccountType accountType;

    @Column(precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    private String currency;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestRate = new BigDecimal("4.00");

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal overDraftLimit = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    protected void onCreate(){
        if (accountUuid == null) accountUuid = UUID.randomUUID();
    }

    public boolean hasSufficientBalance(BigDecimal amount){
        return balance.compareTo(amount) >= 0;
    }

    public void credit(BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Credit amount must be positive");
        }

        BigDecimal scaledAmount = amount.setScale(4, RoundingMode.HALF_EVEN);
        this.balance = this.balance.add(scaledAmount);
    }

    public void debit(BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Debit amount must be positive");
        }

        if (!hasSufficientBalance(amount)) {
            throw new InsufficientFundsException(accountNumber);
        }

        BigDecimal scaledAmount = amount.setScale(4, RoundingMode.HALF_EVEN);
        this.balance = this.balance.subtract(scaledAmount);
    }

    public boolean isActive(){
        return status == AccountStatus.ACTIVE;
    }

    public boolean isBelowMinimumBalance(){
        return balance.compareTo(minimumBalance) < 0;
    }


}
