package com.banking_application.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record TransferRequestDto(
        @NotBlank
        String sourceAccountNumber,

        @NotBlank
        String targetAccountNumber,

        BigDecimal amount,

        String description
) {}
