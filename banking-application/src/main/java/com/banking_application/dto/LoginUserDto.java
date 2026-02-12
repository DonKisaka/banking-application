package com.banking_application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginUserDto(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
