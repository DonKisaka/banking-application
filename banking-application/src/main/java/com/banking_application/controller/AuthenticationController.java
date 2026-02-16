package com.banking_application.controller;

import com.banking_application.dto.AuthenticationResponseDto;
import com.banking_application.dto.CreateUserDto;
import com.banking_application.dto.LoginUserDto;
import com.banking_application.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthenticationResponseDto> signup(@Valid @RequestBody CreateUserDto dto) {
        try {
            AuthenticationResponseDto response = authenticationService.signup(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDto> login(@Valid @RequestBody LoginUserDto dto) {
        AuthenticationResponseDto response = authenticationService.authenticate(dto);
        return ResponseEntity.ok(response);
    }
}
