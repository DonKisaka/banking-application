package com.banking_application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequestDto(
        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
        BigDecimal amount,

        @NotBlank(message = "Transaction source is required")
        String transactionSource,

        String description

) {}
