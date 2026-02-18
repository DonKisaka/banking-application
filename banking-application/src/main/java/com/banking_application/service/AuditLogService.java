package com.banking_application.service;

import com.banking_application.dto.AuditLogResponseDto;
import com.banking_application.mapper.AuditLogMapper;
import com.banking_application.model.AuditLog;
import com.banking_application.model.AuditStatus;
import com.banking_application.model.User;
import com.banking_application.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional
    public void logAction(String action, User user, String resource, String details,
                          AuditStatus outcome, String ipAddress, String userAgent) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .affectedResource(resource)
                .details(details)
                .outcome(outcome)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional
    public void logFailure(String action, User user, String resource,
                           String errorMessage, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .affectedResource(resource)
                .outcome(AuditStatus.FAILURE)
                .errorMessage(errorMessage)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getUserAuditLogs(User user, Pageable pageable) {
        return auditLogRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(auditLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable)
                .map(auditLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getRecentUserActivity(User user) {
        List<AuditLog> logs = auditLogRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        return auditLogMapper.toDto(logs);
    }

    @Transactional(readOnly = true)
    public long countFailedLoginsByIp(String ipAddress, LocalDateTime since) {
        return auditLogRepository.countFailedLoginsByIpSince(ipAddress, since);
    }
}
