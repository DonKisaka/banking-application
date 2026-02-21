package com.banking_application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StatementResponseDto(
        UUID statementUuid,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        Integer transactionCount,
        LocalDateTime generatedAt
) {}
