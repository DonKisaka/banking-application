package com.banking_application.repository;

import com.banking_application.model.AuditLog;
import com.banking_application.model.AuditStatus;
import com.banking_application.model.User;
import com.banking_application.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository underTest;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("joynkirote")
                .email("joynkirote@example.com")
                .password("password123")
                .phoneNumber("+254712345678")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void itShouldFindAuditLogsByUserOrderedByCreatedAtDesc() {
        // given
        AuditLog log1 = AuditLog.builder()
                .user(testUser)
                .action("LOGIN")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .outcome(AuditStatus.SUCCESS)
                .details("User logged in successfully")
                .build();

        AuditLog log2 = AuditLog.builder()
                .user(testUser)
                .action("TRANSFER")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .outcome(AuditStatus.SUCCESS)
                .details("Transfer completed")
                .build();

        underTest.save(log1);
        underTest.save(log2);

        // when
        Page<AuditLog> logsPage = underTest.findByUserOrderByCreatedAtDesc(testUser, PageRequest.of(0, 10));

        // then
        assertThat(logsPage.getContent()).hasSize(2);
        assertThat(logsPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    void itShouldReturnEmptyPageWhenUserHasNoAuditLogs() {
        // given
        User newUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("kimberly")
                .email("kimberly@example.com")
                .password("password456")
                .phoneNumber("+254700000000")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        newUser = userRepository.save(newUser);

        // when
        Page<AuditLog> logsPage = underTest.findByUserOrderByCreatedAtDesc(newUser, PageRequest.of(0, 10));

        // then
        assertThat(logsPage.getContent()).isEmpty();
    }

    @Test
    void itShouldFindAuditLogsByAction() {
        // given
        AuditLog loginLog = AuditLog.builder()
                .user(testUser)
                .action("LOGIN")
                .ipAddress("192.168.1.1")
                .outcome(AuditStatus.SUCCESS)
                .build();

        AuditLog transferLog = AuditLog.builder()
                .user(testUser)
                .action("TRANSFER")
                .ipAddress("192.168.1.1")
                .outcome(AuditStatus.SUCCESS)
                .build();

        underTest.save(loginLog);
        underTest.save(transferLog);

        // when
        Page<AuditLog> loginLogs = underTest.findByAction("LOGIN", PageRequest.of(0, 10));

        // then
        assertThat(loginLogs.getContent()).hasSize(1);
        assertThat(loginLogs.getContent().get(0).getAction()).isEqualTo("LOGIN");
    }

    @Test
    void itShouldReturnEmptyPageWhenNoAuditLogsWithAction() {
        // given
        String action = "NONEXISTENT_ACTION";

        // when
        Page<AuditLog> logs = underTest.findByAction(action, PageRequest.of(0, 10));

        // then
        assertThat(logs.getContent()).isEmpty();
    }

    @Test
    void itShouldFindTop10AuditLogsByUserOrderedByCreatedAtDesc() {
        // given - create 12 audit logs
        for (int i = 1; i <= 12; i++) {
            AuditLog log = AuditLog.builder()
                    .user(testUser)
                    .action("ACTION_" + i)
                    .ipAddress("192.168.1." + i)
                    .outcome(AuditStatus.SUCCESS)
                    .details("Log entry " + i)
                    .build();
            underTest.save(log);
        }

        // when
        List<AuditLog> top10Logs = underTest.findTop10ByUserOrderByCreatedAtDesc(testUser);

        // then
        assertThat(top10Logs).hasSize(10);
    }

    @Test
    void itShouldReturnLessThan10WhenUserHasFewerAuditLogs() {
        // given
        AuditLog log1 = AuditLog.builder()
                .user(testUser)
                .action("LOGIN")
                .ipAddress("192.168.1.1")
                .outcome(AuditStatus.SUCCESS)
                .build();

        AuditLog log2 = AuditLog.builder()
                .user(testUser)
                .action("LOGOUT")
                .ipAddress("192.168.1.1")
                .outcome(AuditStatus.SUCCESS)
                .build();

        underTest.save(log1);
        underTest.save(log2);

        // when
        List<AuditLog> logs = underTest.findTop10ByUserOrderByCreatedAtDesc(testUser);

        // then
        assertThat(logs).hasSize(2);
    }

    @Test
    void itShouldFindAuditLogsByActionAndOutcome() {
        // given
        AuditLog successLogin = AuditLog.builder()
                .user(testUser)
                .action("LOGIN")
                .ipAddress("192.168.1.1")
                .outcome(AuditStatus.SUCCESS)
                .build();

        AuditLog failedLogin = AuditLog.builder()
                .user(testUser)
                .action("LOGIN")
                .ipAddress("192.168.1.2")
                .outcome(AuditStatus.FAILURE)
                .errorMessage("Invalid credentials")
                .build();

        underTest.save(successLogin);
        underTest.save(failedLogin);

        // when
        Page<AuditLog> successfulLogins = underTest.findByActionAndOutcome("LOGIN", AuditStatus.SUCCESS, PageRequest.of(0, 10));
        Page<AuditLog> failedLogins = underTest.findByActionAndOutcome("LOGIN", AuditStatus.FAILURE, PageRequest.of(0, 10));

        // then
        assertThat(successfulLogins.getContent()).hasSize(1);
        assertThat(successfulLogins.getContent().get(0).getOutcome()).isEqualTo(AuditStatus.SUCCESS);

        assertThat(failedLogins.getContent()).hasSize(1);
        assertThat(failedLogins.getContent().get(0).getOutcome()).isEqualTo(AuditStatus.FAILURE);
    }

    @Test
    void itShouldReturnEmptyPageWhenNoAuditLogsWithActionAndOutcome() {
        // given
        AuditLog successLogin = AuditLog.builder()
                .user(testUser)
                .action("LOGIN")
                .ipAddress("192.168.1.1")
                .outcome(AuditStatus.SUCCESS)
                .build();
        underTest.save(successLogin);

        // when
        Page<AuditLog> failedLogins = underTest.findByActionAndOutcome("LOGIN", AuditStatus.FAILURE, PageRequest.of(0, 10));

        // then
        assertThat(failedLogins.getContent()).isEmpty();
    }

    @Test
    void itShouldCountFailedLoginsByIpAddressSince() {
        // given
        String ipAddress = "192.168.1.100";
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        AuditLog failedLogin1 = AuditLog.builder()
                .user(testUser)
                .action("LOGIN_FAILED")
                .ipAddress(ipAddress)
                .outcome(AuditStatus.FAILURE)
                .errorMessage("Invalid password")
                .build();

        AuditLog failedLogin2 = AuditLog.builder()
                .user(testUser)
                .action("LOGIN_FAILED")
                .ipAddress(ipAddress)
                .outcome(AuditStatus.FAILURE)
                .errorMessage("Invalid password")
                .build();

        AuditLog failedLogin3 = AuditLog.builder()
                .user(testUser)
                .action("LOGIN_FAILED")
                .ipAddress(ipAddress)
                .outcome(AuditStatus.FAILURE)
                .errorMessage("Invalid password")
                .build();

        underTest.save(failedLogin1);
        underTest.save(failedLogin2);
        underTest.save(failedLogin3);

        // when
        long count = underTest.countFailedLoginsByIpSince(ipAddress, oneHourAgo);

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void itShouldReturnZeroWhenNoFailedLoginsFromIpAddress() {
        // given
        String ipAddress = "10.0.0.1";
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // when
        long count = underTest.countFailedLoginsByIpSince(ipAddress, oneHourAgo);

        // then
        assertThat(count).isZero();
    }

    @Test
    void itShouldNotCountFailedLoginsFromDifferentIpAddress() {
        // given
        String targetIpAddress = "192.168.1.100";
        String otherIpAddress = "192.168.1.200";
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        AuditLog failedLoginFromOtherIp = AuditLog.builder()
                .user(testUser)
                .action("LOGIN_FAILED")
                .ipAddress(otherIpAddress)
                .outcome(AuditStatus.FAILURE)
                .errorMessage("Invalid password")
                .build();
        underTest.save(failedLoginFromOtherIp);

        // when
        long count = underTest.countFailedLoginsByIpSince(targetIpAddress, oneHourAgo);

        // then
        assertThat(count).isZero();
    }

    @Test
    void itShouldNotCountFailedLoginsBeforeSinceDate() {
        // given
        String ipAddress = "192.168.1.100";
        LocalDateTime futureDate = LocalDateTime.now().plusHours(1);

        AuditLog failedLogin = AuditLog.builder()
                .user(testUser)
                .action("LOGIN_FAILED")
                .ipAddress(ipAddress)
                .outcome(AuditStatus.FAILURE)
                .errorMessage("Invalid password")
                .build();
        underTest.save(failedLogin);

        // when
        long count = underTest.countFailedLoginsByIpSince(ipAddress, futureDate);

        // then
        assertThat(count).isZero();
    }

    @Test
    void itShouldSaveAuditLogWithAllFields() {
        // given
        AuditLog auditLog = AuditLog.builder()
                .user(testUser)
                .action("ACCOUNT_UPDATE")
                .ipAddress("192.168.1.50")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .affectedResource("Account:12345")
                .outcome(AuditStatus.SUCCESS)
                .details("Updated account settings")
                .build();

        // when
        AuditLog saved = underTest.save(auditLog);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAction()).isEqualTo("ACCOUNT_UPDATE");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.50");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        assertThat(saved.getAffectedResource()).isEqualTo("Account:12345");
        assertThat(saved.getOutcome()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(saved.getDetails()).isEqualTo("Updated account settings");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
