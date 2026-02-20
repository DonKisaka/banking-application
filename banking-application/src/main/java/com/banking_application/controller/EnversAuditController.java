package com.banking_application.controller;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.RevisionInfoDto;
import com.banking_application.dto.UserAuditDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.model.Transaction;
import com.banking_application.service.EnversAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/envers")
public class EnversAuditController {

    private final EnversAuditService enversAuditService;

    public EnversAuditController(EnversAuditService enversAuditService) {
        this.enversAuditService = enversAuditService;
    }

    @GetMapping("/accounts/{accountId}/revisions")
    public List<RevisionInfoDto> getAccountRevisions(@PathVariable Long accountId) {
        return enversAuditService.getAccountRevisions(accountId);
    }

    @GetMapping("/accounts/{accountId}/revisions/{revisionNumber}")
    public ResponseEntity<AccountResponseDto> getAccountAtRevision(
            @PathVariable Long accountId,
            @PathVariable Long revisionNumber) {
        return enversAuditService.getAccountAtRevision(accountId, revisionNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Account revision", "id", accountId + "@" + revisionNumber));
    }

    @GetMapping("/transactions/{transactionId}/revisions")
    public List<RevisionInfoDto> getTransactionRevisions(@PathVariable Long transactionId) {
        return enversAuditService.getTransactionRevisions(transactionId);
    }

    @GetMapping("/transactions/{transactionId}/revisions/{revisionNumber}")
    public ResponseEntity<Transaction> getTransactionAtRevision(
            @PathVariable Long transactionId,
            @PathVariable Long revisionNumber) {
        return enversAuditService.getTransactionAtRevision(transactionId, revisionNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction revision", "id", transactionId + "@" + revisionNumber));
    }

    @GetMapping("/users/{userId}/revisions")
    public List<RevisionInfoDto> getUserRevisions(@PathVariable Long userId) {
        return enversAuditService.getUserRevisions(userId);
    }

    @GetMapping("/users/{userId}/revisions/{revisionNumber}")
    public ResponseEntity<UserAuditDto> getUserAtRevision(
            @PathVariable Long userId,
            @PathVariable Long revisionNumber) {
        return enversAuditService.getUserAtRevision(userId, revisionNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("User revision", "id", userId + "@" + revisionNumber));
    }
}
