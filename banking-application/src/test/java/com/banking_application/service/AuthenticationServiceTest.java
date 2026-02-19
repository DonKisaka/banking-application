package com.banking_application.service;

import com.banking_application.config.JwtService;
import com.banking_application.dto.AuthenticationResponseDto;
import com.banking_application.dto.CreateUserDto;
import com.banking_application.dto.LoginUserDto;
import com.banking_application.exception.AccountLockedException;
import com.banking_application.exception.DuplicateResourceException;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.model.AuditStatus;
import com.banking_application.model.User;
import com.banking_application.model.UserRole;
import com.banking_application.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthenticationService underTest;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .phoneNumber("+1234567890")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- signup tests ---

    @Test
    void signup_shouldCreateNewUserAndReturnToken() {
        // given
        CreateUserDto dto = new CreateUserDto("newuser", "new@example.com", "password123", "+9876543210");
        when(userRepository.existsByUsernameOrEmail("newuser", "new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-123");

        // when
        AuthenticationResponseDto result = underTest.signup(dto);

        // then
        assertThat(result.token()).isEqualTo("jwt-token-123");
        verify(userRepository).save(any(User.class));
        verify(auditLogService).logAction(eq("SIGNUP"), any(User.class), eq("auth"),
                anyString(), eq(AuditStatus.SUCCESS), any(), any());
    }

    @Test
    void signup_shouldThrowWhenUsernameOrEmailExists() {
        // given
        CreateUserDto dto = new CreateUserDto("testuser", "test@example.com", "password123", "+1234567890");
        when(userRepository.existsByUsernameOrEmail("testuser", "test@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> underTest.signup(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    // --- authenticate tests ---

    @Test
    void authenticate_shouldReturnTokenOnSuccess() {
        // given
        LoginUserDto dto = new LoginUserDto("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("jwt-token-456");

        // when
        AuthenticationResponseDto result = underTest.authenticate(dto);

        // then
        assertThat(result.token()).isEqualTo("jwt-token-456");
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        assertThat(testUser.getLastLogin()).isNotNull();
        verify(auditLogService).logAction(eq("LOGIN_SUCCESS"), eq(testUser), eq("auth"),
                anyString(), eq(AuditStatus.SUCCESS), any(), any());
    }

    @Test
    void authenticate_shouldThrowWhenAccountIsLocked() {
        // given
        testUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));
        LoginUserDto dto = new LoginUserDto("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> underTest.authenticate(dto))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void authenticate_shouldIncrementFailedAttemptsOnBadCredentials() {
        // given
        LoginUserDto dto = new LoginUserDto("testuser", "wrongpassword");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // when & then
        assertThatThrownBy(() -> underTest.authenticate(dto))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(testUser);
        verify(auditLogService).logFailure(eq("LOGIN_FAILED"), eq(testUser), eq("auth"),
                anyString(), any());
    }

    @Test
    void authenticate_shouldLockAccountAfterFiveFailedAttempts() {
        // given
        testUser.setFailedLoginAttempts(4);
        LoginUserDto dto = new LoginUserDto("testuser", "wrongpassword");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // when & then
        assertThatThrownBy(() -> underTest.authenticate(dto))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(testUser.getAccountLockedUntil()).isNotNull();
        assertThat(testUser.getAccountLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void authenticate_shouldThrowWhenUserNotFound() {
        // given
        LoginUserDto dto = new LoginUserDto("unknown", "password123");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> underTest.authenticate(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
