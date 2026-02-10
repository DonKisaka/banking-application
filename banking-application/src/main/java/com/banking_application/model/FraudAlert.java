package com.banking_application.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts",
  indexes = {
          @Index(name = "idx_fraud_status", columnList = "status"),
          @Index(name = "idx_fraud_risk_score", columnList = "risk_score"),
          @Index(name = "idx_fraud_transaction_id", columnList = "transaction_id"),
          @Index(name = "idx_fraud_user_id", columnList = "user_id"),
  })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(nullable = false)
    private String reasonCode;

    private String detectionLogic;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FraudStatus status = FraudStatus.PENDING_REVIEW;

    @Column(name = "admin_remarks")
    private String adminRemarks;

    @Column(name = "action_taken")
    private String actionTaken;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
