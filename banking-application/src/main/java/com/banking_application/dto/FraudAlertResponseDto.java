package com.banking_application.dto;

import com.banking_application.model.FraudStatus;

public record FraudAlertResponseDto(
     Long id,
     String transactionReference,
     String username,
     Integer riskScore,
     String reasonCode,
     FraudStatus status
) {}
