package com.banking_application.controller;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.RevisionInfoDto;
import com.banking_application.dto.TransactionResponse;
import com.banking_application.dto.UserAuditDto;
import com.banking_application.mapper.AccountMapper;
import com.banking_application.mapper.TransactionMapper;
import com.banking_application.model.Account;
import com.banking_application.model.Transaction;
import com.banking_application.model.User;
import com.banking_application.service.EnversAuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/envers")
public class EnversAuditController {

    private final EnversAuditService enversAuditService;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;

    public EnversAuditController(EnversAuditService enversAuditService,
                                 AccountMapper accountMapper,
                                 TransactionMapper transactionMapper) {
        this.enversAuditService = enversAuditService;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("/accounts/{accountId}/revisions")
    public List<RevisionInfoDto> getAccountRevisions(@PathVariable Long accountId) {
        return enversAuditService.getAccountRevisions(accountId);
    }

    @GetMapping("/accounts/by-number/{accountNumber}/revisions")
    public List<RevisionInfoDto> getAccountRevisionsByNumber(@PathVariable String accountNumber) {
        return enversAuditService.getAccountRevisionsByAccountNumber(accountNumber);
    }

    @GetMapping("/accounts/{accountId}/revisions/{revisionNumber}")
    public AccountResponseDto getAccountAtRevision(@PathVariable Long accountId,
                                                    @PathVariable Long revisionNumber) {
        Account account = enversAuditService.getAccountAtRevision(accountId, revisionNumber);
        return accountMapper.toDto(account);
    }

    @GetMapping("/transactions/{transactionId}/revisions")
    public List<RevisionInfoDto> getTransactionRevisions(@PathVariable Long transactionId) {
        return enversAuditService.getTransactionRevisions(transactionId);
    }

    @GetMapping("/transactions/{transactionId}/revisions/{revisionNumber}")
    public TransactionResponse getTransactionAtRevision(@PathVariable Long transactionId,
                                                       @PathVariable Long revisionNumber) {
        Transaction transaction = enversAuditService.getTransactionAtRevision(transactionId, revisionNumber);
        return transactionMapper.toDto(transaction);
    }

    @GetMapping("/users/{userId}/revisions")
    public List<RevisionInfoDto> getUserRevisions(@PathVariable Long userId) {
        return enversAuditService.getUserRevisions(userId);
    }

    @GetMapping("/users/{userId}/revisions/{revisionNumber}")
    public UserAuditDto getUserAtRevision(@PathVariable Long userId,
                                          @PathVariable Long revisionNumber) {
        User user = enversAuditService.getUserAtRevision(userId, revisionNumber);
        return new UserAuditDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive()
        );
    }
}
