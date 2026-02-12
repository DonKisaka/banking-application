package com.banking_application.dto;

import com.banking_application.model.AccountStatus;
import com.banking_application.model.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponseDto(
  UUID accountUuid,
  String accountNumber,
  BigDecimal balance,
  String currency,
  AccountStatus accountStatus,
  AccountType accountType
){}
