package com.banking_application.service;

import com.banking_application.config.JwtService;
import com.banking_application.dto.AuthenticationResponseDto;
import com.banking_application.dto.CreateUserDto;
import com.banking_application.dto.LoginUserDto;
import com.banking_application.model.User;
import com.banking_application.model.UserRole;
import com.banking_application.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
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

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

        return buildAuthResponse(user);
    }

    public AuthenticationResponseDto authenticate(LoginUserDto dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.username(), dto.password())
        );

        User user = userRepository.findByUsername(dto.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.resetFailedLoginAttempts();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

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
