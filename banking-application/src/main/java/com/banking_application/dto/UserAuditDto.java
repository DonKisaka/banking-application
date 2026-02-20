package com.banking_application.dto;

import com.banking_application.model.UserRole;

public record UserAuditDto(
        Long id,
        String username,
        String email,
        UserRole role,
        Boolean isActive
) {}
