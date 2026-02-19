package com.banking_application.controller;

import com.banking_application.dto.AuditLogResponseDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.model.User;
import com.banking_application.repository.UserRepository;
import com.banking_application.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AuditLogController(AuditLogService auditLogService,
                              UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @GetMapping("/user/{userUuid}")
    public Page<AuditLogResponseDto> getUserAuditLogs(@PathVariable UUID userUuid,
                                                      Pageable pageable) {
        User user = findUserOrThrow(userUuid);
        return auditLogService.getUserAuditLogs(user, pageable);
    }

    @GetMapping
    public Page<AuditLogResponseDto> getAuditLogsByAction(@RequestParam String action,
                                                          Pageable pageable) {
        return auditLogService.getAuditLogsByAction(action, pageable);
    }

    @GetMapping("/user/{userUuid}/recent")
    public List<AuditLogResponseDto> getRecentUserActivity(@PathVariable UUID userUuid) {
        User user = findUserOrThrow(userUuid);
        return auditLogService.getRecentUserActivity(user);
    }

    private User findUserOrThrow(UUID userUuid) {
        return userRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userUuid", userUuid));
    }
}
