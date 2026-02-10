package com.banking_application.repository;

import com.banking_application.model.FraudAlert;
import com.banking_application.model.FraudStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FraudAlertRepository extends JpaRepository <FraudAlert, Long> {
    List<FraudAlert> findByStatus(FraudStatus status);

    @Query("SELECT f FROM FraudAlert f WHERE f.riskScore >= :minScore")
    List<FraudAlert> findHighRiskAlerts(@Param("minScore") Integer minScore);
}
