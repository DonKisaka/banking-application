package com.banking_application.dto;

import com.banking_application.model.TransactionStatus;
import com.banking_application.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
   String transactionReference,
   BigDecimal amount,
   String currency,
   TransactionType type,
   TransactionStatus status,
   LocalDateTime timestamp,
   String description,
   String targetAccountName
) {}
