package com.banking_application.service;

import com.banking_application.dto.FraudAlertResponseDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.mapper.FraudAlertMapper;
import com.banking_application.model.*;
import com.banking_application.repository.FraudAlertRepository;
import com.banking_application.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000");
    private static final int RAPID_TRANSACTION_WINDOW_MINUTES = 10;
    private static final int RAPID_TRANSACTION_COUNT_THRESHOLD = 3;
    private static final int ALERT_RISK_THRESHOLD = 70;

    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertMapper fraudAlertMapper;
    private final TransactionRepository transactionRepository;

    public FraudDetectionService(FraudAlertRepository fraudAlertRepository,
                                 FraudAlertMapper fraudAlertMapper,
                                 TransactionRepository transactionRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudAlertMapper = fraudAlertMapper;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public int evaluateTransaction(Transaction transaction) {
        int riskScore = 0;
        StringBuilder reasonCode = new StringBuilder();
        StringBuilder detectionLogic = new StringBuilder();

        if (transaction.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            riskScore += 40;
            reasonCode.append("HIGH_VALUE;");
            detectionLogic.append("Amount ").append(transaction.getAmount())
                    .append(" exceeds threshold ").append(HIGH_VALUE_THRESHOLD).append("; ");
        }

        Account account = transaction.getSourceAccount() != null
                ? transaction.getSourceAccount()
                : transaction.getTargetAccount();

        if (account != null) {
            List<Transaction> recentTransactions = transactionRepository
                    .findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(account, account);

            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RAPID_TRANSACTION_WINDOW_MINUTES);
            long rapidCount = recentTransactions.stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(windowStart))
                    .count();

            if (rapidCount >= RAPID_TRANSACTION_COUNT_THRESHOLD) {
                riskScore += 20;
                reasonCode.append("RAPID_TRANSACTIONS;");
                detectionLogic.append(rapidCount).append(" transactions in last ")
                        .append(RAPID_TRANSACTION_WINDOW_MINUTES).append(" minutes; ");
            }
        }

        if (transaction.getTransactionType() == TransactionType.TRANSFER
                && transaction.getSourceAccount() != null
                && transaction.getTargetAccount() != null) {
            User sourceUser = transaction.getSourceAccount().getUser();
            User targetUser = transaction.getTargetAccount().getUser();
            if (sourceUser != null && targetUser != null && !sourceUser.getId().equals(targetUser.getId())) {
                riskScore += 15;
                reasonCode.append("CROSS_USER_TRANSFER;");
                detectionLogic.append("Transfer between different users; ");
            }
        }

        if (riskScore >= ALERT_RISK_THRESHOLD) {
            FraudAlert alert = FraudAlert.builder()
                    .transaction(transaction)
                    .user(transaction.getInitiatedBy())
                    .riskScore(riskScore)
                    .reasonCode(reasonCode.toString())
                    .detectionLogic(detectionLogic.toString())
                    .status(FraudStatus.PENDING_REVIEW)
                    .build();

            fraudAlertRepository.save(alert);
        }

        return riskScore;
    }

    @Transactional(readOnly = true)
    public List<FraudAlertResponseDto> getPendingAlerts() {
        List<FraudAlert> alerts = fraudAlertRepository
                .findByStatusOrderByRiskScoreDesc(FraudStatus.PENDING_REVIEW);
        return fraudAlertMapper.toDto(alerts);
    }

    @Transactional(readOnly = true)
    public List<FraudAlertResponseDto> getAlertsByRiskScore(Integer minScore) {
        List<FraudAlert> alerts = fraudAlertRepository.findByRiskScoreGreaterThanEqual(minScore);
        return fraudAlertMapper.toDto(alerts);
    }

    @Transactional
    public FraudAlertResponseDto resolveAlert(Long alertId, String adminRemarks,
                                               String actionTaken, FraudStatus newStatus) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", "id", alertId));

        alert.setStatus(newStatus);
        alert.setAdminRemarks(adminRemarks);
        alert.setActionTaken(actionTaken);
        alert.setResolvedAt(LocalDateTime.now());

        FraudAlert saved = fraudAlertRepository.save(alert);
        return fraudAlertMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<FraudAlertResponseDto> getUserFraudHistory(User user, Pageable pageable) {
        return fraudAlertRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(fraudAlertMapper::toDto);
    }

    @Transactional(readOnly = true)
    public boolean hasUnresolvedAlerts(User user) {
        return fraudAlertRepository.existsByUserAndStatus(user, FraudStatus.PENDING_REVIEW);
    }
}
