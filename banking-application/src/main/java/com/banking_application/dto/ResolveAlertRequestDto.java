package com.banking_application.dto;

import com.banking_application.model.FraudStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolveAlertRequestDto(
        @NotBlank(message = "Admin remarks are required")
        String adminRemarks,

        @NotBlank(message = "Action taken is required")
        String actionTaken,

        @NotNull(message = "New status is required")
        FraudStatus newStatus
) {}
