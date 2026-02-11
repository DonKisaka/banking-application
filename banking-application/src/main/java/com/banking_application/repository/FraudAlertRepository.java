package com.banking_application.repository;

import com.banking_application.model.FraudAlert;
import com.banking_application.model.FraudStatus;
import com.banking_application.model.Transaction;
import com.banking_application.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FraudAlertRepository extends JpaRepository <FraudAlert, Long> {
    List<FraudAlert> findByStatus(FraudStatus status);

    List<FraudAlert> findByRiskScoreGreaterThanEqual(Integer riskScore);

    List<FraudAlert> findByStatusOrderByRiskScoreDesc(FraudStatus status);

    Optional<FraudAlert> findByTransaction(Transaction transaction);

    Page<FraudAlert> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    boolean existsByUserAndStatus(User user, FraudStatus status);
}
