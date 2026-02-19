package com.banking_application.controller;

import com.banking_application.dto.*;
import com.banking_application.model.User;
import com.banking_application.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(@Valid @RequestBody DepositRequestDto dto,
                                       @AuthenticationPrincipal User user) {
        return transactionService.deposit(dto, user);
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(@Valid @RequestBody WithdrawalRequestDto dto,
                                        @AuthenticationPrincipal User user) {
        return transactionService.withdraw(dto, user);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@Valid @RequestBody TransferRequestDto dto,
                                        @AuthenticationPrincipal User user) {
        return transactionService.transfer(dto, user);
    }

    @GetMapping("/account/{accountNumber}")
    public List<TransactionResponse> getTransactionHistory(@PathVariable String accountNumber) {
        return transactionService.getTransactionHistory(accountNumber);
    }

    @GetMapping("/{reference}")
    public TransactionResponse getTransactionByReference(@PathVariable String reference) {
        return transactionService.getTransactionByReference(reference);
    }
}
