package com.banking_application.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditlogs",
  indexes = {
          @Index(name = "idx_audit_user_id", columnList = "user_id"),
          @Index(name = "idx_audit_action", columnList = "action"),
          @Index(name = "idx_audit_created_at", columnList = "created_at"),
          @Index(name = "idx_audit_outcome", columnList = "outcome")
  })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false)
    private String userAgent;

    @Column(name = "affected_resource", updatable = false)
    private String affectedResource;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuditStatus outcome = AuditStatus.SUCCESS;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "error_message", updatable = false)
    private String errorMessage;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
