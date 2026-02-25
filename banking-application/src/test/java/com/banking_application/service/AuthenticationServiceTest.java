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
import com.banking_application.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
    private AuthenticationService authenticationService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    // Given-When-Then + Exception Testing + Verification
    @Test
    void givenExistingUser_whenSignup_thenThrowsDuplicateResourceException() {
        // Given
        CreateUserDto dto = new CreateUserDto(
                "donaldkisaka",
                "kisakadonald@example.com",
                "delaand2004##",
                "0707412258"
        );

        given(userRepository.existsByUsernameOrEmail(dto.username(), dto.email()))
                .willReturn(true);

        // When + Then
        assertThatThrownBy(() -> authenticationService.signup(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("username/email");

        then(userRepository).should(never()).save(any(User.class));
        then(auditLogService).shouldHaveNoInteractions();
    }

    // Given-When-Then + Mocking + ArgumentCaptor + Verification
    @Test
    void givenValidSignup_whenSignup_thenUserSavedAuditLoggedAndTokenReturned() {
        // Given
        CreateUserDto dto = new CreateUserDto(
                "tom",
                "tomevans@example.com",
                "chelsea123456789",
                "0712569002"
        );

        given(userRepository.existsByUsernameOrEmail(dto.username(), dto.email()))
                .willReturn(false);
        given(passwordEncoder.encode(dto.password())).willReturn("encoded-password");
        given(jwtService.generateToken(any(User.class))).willReturn("jwt-token");

        // When
        AuthenticationResponseDto response = authenticationService.signup(dto);

        // Then
        then(userRepository).should().save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo(dto.username());
        assertThat(savedUser.getEmail()).isEqualTo(dto.email());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(dto.phoneNumber());
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getIsActive()).isTrue();

        then(auditLogService).should().logAction(
                eq("SIGNUP"),
                eq(savedUser),
                eq("auth"),
                anyString(),
                eq(AuditStatus.SUCCESS),
                isNull(),
                isNull()
        );

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo(dto.username());
        assertThat(response.email()).isEqualTo(dto.email());
    }

    // Exception Testing for locked account
    @Test
    void givenLockedUser_whenAuthenticate_thenThrowsAccountLockedException() {
        // Given
        LoginUserDto dto = new LoginUserDto("lockedUser", "password123");

        User lockedUser = User.builder()
                .username("lockedUser")
                .accountLockedUntil(LocalDateTime.now().plusMinutes(10))
                .build();

        given(userRepository.findByUsername(dto.username()))
                .willReturn(Optional.of(lockedUser));

        // When + Then
        assertThatThrownBy(() -> authenticationService.authenticate(dto))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("lockedUser");

        then(authenticationManager).shouldHaveNoInteractions();
    }

    // Given-When-Then + Exception Testing + Verification for bad credentials
    @Test
    void givenBadCredentials_whenAuthenticate_thenFailedAttemptsIncrementedAndAuditLogged() {
        // Given
        LoginUserDto dto = new LoginUserDto("melissa", "wrongPassword");

        User user = User.builder()
                .id(1L)
                .username("lisa")
                .email("lisamukoya@example.com")
                .build();

        given(userRepository.findByUsername(dto.username()))
                .willReturn(Optional.of(user));

        BadCredentialsException authException = new BadCredentialsException("Bad credentials");
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(authException);

        // When + Then
        assertThatThrownBy(() -> authenticationService.authenticate(dto))
                .isInstanceOf(BadCredentialsException.class);

        then(userRepository).should().save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(1);

        then(auditLogService).should().logFailure(
                eq("LOGIN_FAILED"),
                eq(user),
                eq("auth"),
                anyString(),
                isNull()
        );
    }

    // Given-When-Then for successful authentication
    @Test
    void givenValidCredentials_whenAuthenticate_thenLastLoginUpdatedAndTokenReturned() {
        // Given
        LoginUserDto dto = new LoginUserDto("alice", "correctPassword");

        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .failedLoginAttempts(2)
                .build();

        given(userRepository.findByUsername(dto.username()))
                .willReturn(Optional.of(user));

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(new UsernamePasswordAuthenticationToken(dto.username(), dto.password()));

        given(jwtService.generateToken(user)).willReturn("jwt-token");

        // When
        AuthenticationResponseDto response = authenticationService.authenticate(dto);

        // Then
        then(userRepository).should().save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getFailedLoginAttempts()).isZero();
        assertThat(savedUser.getLastLogin()).isNotNull();

        then(auditLogService).should().logAction(
                eq("LOGIN_SUCCESS"),
                eq(user),
                eq("auth"),
                anyString(),
                eq(AuditStatus.SUCCESS),
                isNull(),
                isNull()
        );

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("alice");
    }
}

