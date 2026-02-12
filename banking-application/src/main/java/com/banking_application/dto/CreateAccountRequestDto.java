package com.banking_application.dto;

import com.banking_application.model.AccountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountRequestDto(
        @NotNull(message = "Account name is required")
        AccountType accountType,

        @NotNull(message = "Initial deposit is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Initial deposit must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
        BigDecimal initialDeposit,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        String currency
) {}
