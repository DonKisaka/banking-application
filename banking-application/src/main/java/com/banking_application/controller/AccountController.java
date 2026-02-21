package com.banking_application.controller;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.CreateAccountRequestDto;
import com.banking_application.dto.StatementResponseDto;
import com.banking_application.model.User;
import com.banking_application.service.AccountService;
import com.banking_application.service.StatementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final StatementService statementService;

    public AccountController(AccountService accountService, StatementService statementService) {
        this.accountService = accountService;
        this.statementService = statementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponseDto createAccount(@Valid @RequestBody CreateAccountRequestDto dto,
                                            @AuthenticationPrincipal User user) {
        return accountService.createAccount(dto, user);
    }

    @GetMapping
    public List<AccountResponseDto> getUserAccounts(@AuthenticationPrincipal User user) {
        return accountService.getUserAccounts(user);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponseDto getAccountDetails(@PathVariable String accountNumber) {
        return accountService.getAccountDetails(accountNumber);
    }

    @GetMapping("/{accountNumber}/statements")
    public List<StatementResponseDto> getStatements(@PathVariable String accountNumber,
                                                     @RequestParam(defaultValue = "12") int limit,
                                                     @AuthenticationPrincipal User user) {
        return statementService.getStatementsForAccount(accountNumber, user, Math.min(limit, 24));
    }

    @PatchMapping("/{accountNumber}/freeze")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public AccountResponseDto freezeAccount(@PathVariable String accountNumber) {
        return accountService.freezeAccount(accountNumber);
    }

    @PatchMapping("/{accountNumber}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public AccountResponseDto closeAccount(@PathVariable String accountNumber) {
        return accountService.closeAccount(accountNumber);
    }

    @PatchMapping("/{accountNumber}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public AccountResponseDto reactivateAccount(@PathVariable String accountNumber) {
        return accountService.reactivateAccount(accountNumber);
    }
}
