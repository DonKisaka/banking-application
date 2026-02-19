package com.banking_application.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultExceptionHandlerTest {

    private DefaultExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new DefaultExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleResourceNotFound_shouldReturn404WithProblemDetail() {
        var ex = new ResourceNotFoundException("Account", "accountNumber", "ACC123");

        ProblemDetail pd = handler.handleResourceNotFound(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Resource Not Found");
        assertThat(pd.getDetail()).contains("Account");
        assertThat(pd.getProperties()).containsKey("resource");
    }

    @Test
    void handleDuplicateResource_shouldReturn409WithProblemDetail() {
        var ex = new DuplicateResourceException("User", "email", "test@email.com");

        ProblemDetail pd = handler.handleDuplicateResource(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getTitle()).isEqualTo("Duplicate Resource");
        assertThat(pd.getDetail()).contains("User");
    }

    @Test
    void handleAccountState_shouldReturn409WithProblemDetail() {
        var ex = new AccountStateException("ACC123", "Cannot freeze a closed account");

        ProblemDetail pd = handler.handleAccountState(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getTitle()).isEqualTo("Account State Error");
        assertThat(pd.getDetail()).contains("Cannot freeze a closed account");
        assertThat(pd.getProperties()).containsEntry("accountNumber", "ACC123");
    }

    @Test
    void handleInsufficientFunds_shouldReturn422WithProblemDetail() {
        var ex = new InsufficientFundsException("ACC123");

        ProblemDetail pd = handler.handleInsufficientFunds(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(pd.getTitle()).isEqualTo("Insufficient Funds");
        assertThat(pd.getDetail()).contains("ACC123");
    }

    @Test
    void handleInvalidTransaction_shouldReturn422WithProblemDetail() {
        var ex = new InvalidTransactionException("Cannot transfer to the same account");

        ProblemDetail pd = handler.handleInvalidTransaction(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(pd.getTitle()).isEqualTo("Invalid Transaction");
        assertThat(pd.getDetail()).contains("Cannot transfer to the same account");
    }

    @Test
    void handleAccountLocked_shouldReturn423WithProblemDetail() {
        var ex = new AccountLockedException("testuser", "2026-01-01T12:00:00");

        ProblemDetail pd = handler.handleAccountLocked(ex, request);

        assertThat(pd.getStatus()).isEqualTo(423);
        assertThat(pd.getTitle()).isEqualTo("Account Locked");
        assertThat(pd.getDetail()).contains("testuser");
        assertThat(pd.getProperties()).containsEntry("lockedUntil", "2026-01-01T12:00:00");
    }

    @Test
    void handleInvalidAmount_shouldReturn400WithProblemDetail() {
        var ex = new InvalidAmountException("Credit amount must be positive");

        ProblemDetail pd = handler.handleInvalidAmount(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getTitle()).isEqualTo("Invalid Amount");
        assertThat(pd.getDetail()).contains("positive");
    }

    @Test
    void handleBadCredentials_shouldReturn401WithProblemDetail() {
        var ex = new BadCredentialsException("Bad credentials");

        ProblemDetail pd = handler.handleBadCredentials(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(pd.getTitle()).isEqualTo("Authentication Failed");
        assertThat(pd.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleValidation_shouldReturn400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("dto", "email", "must not be blank"),
                new FieldError("dto", "password", "size must be between 8 and 100")
        ));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail pd = handler.handleValidation(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getTitle()).isEqualTo("Validation Error");
        assertThat(pd.getProperties()).containsKey("fieldErrors");

        Map<String, String> fieldErrors = (Map<String, String>) pd.getProperties().get("fieldErrors");
        assertThat(fieldErrors).containsEntry("email", "must not be blank");
        assertThat(fieldErrors).containsEntry("password", "size must be between 8 and 100");
    }

    @Test
    void handleEncryption_shouldReturn500WithProblemDetail() {
        var ex = new EncryptionException("Error encrypting field", new RuntimeException("AES failure"));

        ProblemDetail pd = handler.handleEncryption(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
        assertThat(pd.getDetail()).doesNotContain("AES failure");
    }

    @Test
    void handleGenericException_shouldReturn500WithProblemDetail() {
        var ex = new RuntimeException("Something unexpected happened");

        ProblemDetail pd = handler.handleGenericException(ex, request);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
        assertThat(pd.getDetail()).isEqualTo("An unexpected error occurred. Please try again later.");
    }
}
