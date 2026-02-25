package com.banking_application.service;

import com.banking_application.dto.AuditLogResponseDto;
import com.banking_application.mapper.AuditLogMapper;
import com.banking_application.model.AuditLog;
import com.banking_application.model.AuditStatus;
import com.banking_application.model.User;
import com.banking_application.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    // Given-When-Then + ArgumentCaptor + Verification
    @Test
    void givenActionDetails_whenLogAction_thenAuditLogSavedWithOutcome() {
        // Given
        User user = User.builder().username("donald").build();
        String action = "LOGIN_SUCCESS";
        String resource = "auth";
        String details = "User logged in";

        // When
        auditLogService.logAction(action, user, resource, details, AuditStatus.SUCCESS, "127.0.0.1", "JUnit");

        // Then
        then(auditLogRepository).should().save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getAffectedResource()).isEqualTo(resource);
        assertThat(saved.getDetails()).isEqualTo(details);
        assertThat(saved.getOutcome()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("JUnit");
    }

    // Given-When-Then + ArgumentCaptor + Verification for failures
    @Test
    void givenFailureDetails_whenLogFailure_thenAuditLogSavedWithFailureOutcome() {
        // Given
        User user = User.builder().username("donald").build();
        String action = "LOGIN_FAILED";
        String resource = "auth";
        String errorMessage = "Bad credentials";

        // When
        auditLogService.logFailure(action, user, resource, errorMessage, "127.0.0.1");

        // Then
        then(auditLogRepository).should().save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getAffectedResource()).isEqualTo(resource);
        assertThat(saved.getOutcome()).isEqualTo(AuditStatus.FAILURE);
        assertThat(saved.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
    }

    // Given-When-Then for paged retrieval
    @Test
    void givenUserAndPageable_whenGetUserAuditLogs_thenRepositoryAndMapperUsed() {
        // Given
        User user = User.builder().username("ted").build();
        var pageable = PageRequest.of(0, 10);

        AuditLog log = AuditLog.builder()
                .id(1L)
                .user(user)
                .action("LOGIN_SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);
        given(auditLogRepository.findByUserOrderByCreatedAtDesc(user, pageable))
                .willReturn(page);

        AuditLogResponseDto dto = new AuditLogResponseDto(
                1L, "ted", "LOGIN_SUCCESS", "auth", "127.0.0.1", AuditStatus.SUCCESS, "User logged in"
        );
        given(auditLogMapper.toDto(log)).willReturn(dto);

        // When
        Page<AuditLogResponseDto> result = auditLogService.getUserAuditLogs(user, pageable);

        // Then
        then(auditLogRepository).should().findByUserOrderByCreatedAtDesc(user, pageable);
        then(auditLogMapper).should().toDto(log);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(dto);
    }

    // Given-When-Then for counting failed logins
    @Test
    void givenIpAndSince_whenCountFailedLoginsByIp_thenDelegatesToRepository() {
        // Given
        String ipAddress = "192.168.1.1";
        LocalDateTime since = LocalDateTime.now().minusHours(1);

        given(auditLogRepository.countFailedLoginsByIpSince(ipAddress, since))
                .willReturn(5L);

        // When
        long result = auditLogService.countFailedLoginsByIp(ipAddress, since);

        // Then
        then(auditLogRepository).should().countFailedLoginsByIpSince(eq(ipAddress), eq(since));
        assertThat(result).isEqualTo(5L);
    }
}

