package com.banking_application.dto;

import com.banking_application.model.AuditStatus;

public record AuditLogResponseDto(
        Long id,
        String username,
        String action,
        String affectedResource,
        String ipAddress,
        AuditStatus outcome,
        String details
) {}
