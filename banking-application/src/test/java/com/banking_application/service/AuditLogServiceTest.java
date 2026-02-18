package com.banking_application.service;

import com.banking_application.dto.AuditLogResponseDto;
import com.banking_application.mapper.AuditLogMapper;
import com.banking_application.model.*;
import com.banking_application.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogService underTest;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();
    }

    // --- logAction tests ---

    @Test
    void logAction_shouldCreateAuditLogEntry() {
        // given
        AuditLog savedLog = AuditLog.builder()
                .id(1L)
                .user(testUser)
                .action("LOGIN_SUCCESS")
                .affectedResource("auth")
                .details("User logged in")
                .outcome(AuditStatus.SUCCESS)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);

        // when
        underTest.logAction("LOGIN_SUCCESS", testUser, "auth", "User logged in",
                AuditStatus.SUCCESS, "127.0.0.1", "Mozilla/5.0");

        // then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertThat(captured.getAction()).isEqualTo("LOGIN_SUCCESS");
        assertThat(captured.getUser()).isEqualTo(testUser);
        assertThat(captured.getAffectedResource()).isEqualTo("auth");
        assertThat(captured.getDetails()).isEqualTo("User logged in");
        assertThat(captured.getOutcome()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(captured.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(captured.getUserAgent()).isEqualTo("Mozilla/5.0");
    }

    // --- logFailure tests ---

    @Test
    void logFailure_shouldCreateFailureAuditLog() {
        // given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(AuditLog.builder().build());

        // when
        underTest.logFailure("LOGIN_FAILED", testUser, "auth", "Bad credentials", "192.168.1.1");

        // then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertThat(captured.getAction()).isEqualTo("LOGIN_FAILED");
        assertThat(captured.getOutcome()).isEqualTo(AuditStatus.FAILURE);
        assertThat(captured.getErrorMessage()).isEqualTo("Bad credentials");
        assertThat(captured.getIpAddress()).isEqualTo("192.168.1.1");
    }

    // --- getUserAuditLogs tests ---

    @Test
    void getUserAuditLogs_shouldReturnPaginatedResults() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = AuditLog.builder()
                .id(1L)
                .user(testUser)
                .action("DEPOSIT")
                .outcome(AuditStatus.SUCCESS)
                .build();

        AuditLogResponseDto dto = new AuditLogResponseDto(
                1L, "testuser", "DEPOSIT", null, null, AuditStatus.SUCCESS, null
        );

        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(page);
        when(auditLogMapper.toDto(log)).thenReturn(dto);

        // when
        Page<AuditLogResponseDto> result = underTest.getUserAuditLogs(testUser, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).action()).isEqualTo("DEPOSIT");
    }

    // --- getAuditLogsByAction tests ---

    @Test
    void getAuditLogsByAction_shouldReturnFilteredResults() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = AuditLog.builder()
                .id(1L)
                .user(testUser)
                .action("TRANSFER")
                .outcome(AuditStatus.SUCCESS)
                .build();

        AuditLogResponseDto dto = new AuditLogResponseDto(
                1L, "testuser", "TRANSFER", null, null, AuditStatus.SUCCESS, null
        );

        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByAction("TRANSFER", pageable)).thenReturn(page);
        when(auditLogMapper.toDto(log)).thenReturn(dto);

        // when
        Page<AuditLogResponseDto> result = underTest.getAuditLogsByAction("TRANSFER", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).action()).isEqualTo("TRANSFER");
    }

    // --- getRecentUserActivity tests ---

    @Test
    void getRecentUserActivity_shouldReturnTop10() {
        // given
        AuditLog log1 = AuditLog.builder().id(1L).user(testUser).action("DEPOSIT").outcome(AuditStatus.SUCCESS).build();
        AuditLog log2 = AuditLog.builder().id(2L).user(testUser).action("WITHDRAWAL").outcome(AuditStatus.SUCCESS).build();

        AuditLogResponseDto dto1 = new AuditLogResponseDto(1L, "testuser", "DEPOSIT", null, null, AuditStatus.SUCCESS, null);
        AuditLogResponseDto dto2 = new AuditLogResponseDto(2L, "testuser", "WITHDRAWAL", null, null, AuditStatus.SUCCESS, null);

        when(auditLogRepository.findTop10ByUserOrderByCreatedAtDesc(testUser)).thenReturn(List.of(log1, log2));
        when(auditLogMapper.toDto(List.of(log1, log2))).thenReturn(List.of(dto1, dto2));

        // when
        List<AuditLogResponseDto> result = underTest.getRecentUserActivity(testUser);

        // then
        assertThat(result).hasSize(2);
        verify(auditLogRepository).findTop10ByUserOrderByCreatedAtDesc(testUser);
    }

    // --- countFailedLoginsByIp tests ---

    @Test
    void countFailedLoginsByIp_shouldReturnCount() {
        // given
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        when(auditLogRepository.countFailedLoginsByIpSince("10.0.0.1", since)).thenReturn(3L);

        // when
        long count = underTest.countFailedLoginsByIp("10.0.0.1", since);

        // then
        assertThat(count).isEqualTo(3L);
    }
}
