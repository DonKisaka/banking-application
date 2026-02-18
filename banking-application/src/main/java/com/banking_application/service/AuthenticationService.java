package com.banking_application.service;

import com.banking_application.config.JwtService;
import com.banking_application.dto.AuthenticationResponseDto;
import com.banking_application.dto.CreateUserDto;
import com.banking_application.dto.LoginUserDto;
import com.banking_application.model.AuditStatus;
import com.banking_application.model.User;
import com.banking_application.model.UserRole;
import com.banking_application.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    public AuthenticationResponseDto signup(CreateUserDto dto) {
        if (userRepository.existsByUsernameOrEmail(dto.username(), dto.email())) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        User user = User.builder()
                .username(dto.username())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .phoneNumber(dto.phoneNumber())
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        auditLogService.logAction("SIGNUP", user, "auth",
                "New user registered: " + user.getUsername(),
                AuditStatus.SUCCESS, null, null);

        return buildAuthResponse(user);
    }

    public AuthenticationResponseDto authenticate(LoginUserDto dto) {
        User user = userRepository.findByUsername(dto.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isAccountNonLocked()) {
            throw new IllegalStateException("Account is locked until " + user.getAccountLockedUntil()
                    + ". Please try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.username(), dto.password())
            );
        } catch (BadCredentialsException e) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);

            auditLogService.logFailure("LOGIN_FAILED", user, "auth",
                    "Failed login attempt #" + user.getFailedLoginAttempts(), null);

            throw e;
        }

        user.resetFailedLoginAttempts();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        auditLogService.logAction("LOGIN_SUCCESS", user, "auth",
                "User logged in successfully", AuditStatus.SUCCESS, null, null);

        return buildAuthResponse(user);
    }

    public AuthenticationResponseDto buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);

        return new AuthenticationResponseDto(
                token,
                user.getUserUuid(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isMfaEnabled()
        );
    }
}
