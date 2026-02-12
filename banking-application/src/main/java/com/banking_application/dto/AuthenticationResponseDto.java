package com.banking_application.dto;

import java.util.UUID;

public record AuthenticationResponseDto(
        String token,
        UUID userUuid,
        String username,
        String email,
        String role,
        boolean mfaEnabled
) {}
