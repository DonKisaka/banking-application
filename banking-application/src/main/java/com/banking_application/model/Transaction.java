package com.banking_application.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions",
 indexes = {
         @Index(name = "idx_transaction_reference", columnList = "transaction_reference"),
         @Index(name = "idx_transaction_status", columnList = "transaction_status"),
         @Index(name = "idx_transaction_created_at", columnList = "created_at"),
         @Index(name = "idx_source_account", columnList = "source_account_id"),
         @Index(name = "idx_target_account", columnList = "target_account_id")
 })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_uuid", unique = true, nullable = false)
    private UUID transactionUuid;

    @Column(name = "transaction_reference", unique = true, nullable = false, updatable = false)
    private String transactionReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false)
    @Builder.Default
    private TransactionStatus transactionStatus = TransactionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "location")
    private String location;

    @Column(name = "device_info")
    private String deviceInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by")
    private User initiatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;


    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (transactionUuid == null) {
            transactionUuid = UUID.randomUUID();
        }
        if (transactionReference == null) {
            this.transactionReference = "HDFC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();;
        }
    }




}
