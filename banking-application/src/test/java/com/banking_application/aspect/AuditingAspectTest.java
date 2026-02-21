package com.banking_application.aspect;

import com.banking_application.model.AuditStatus;
import com.banking_application.model.User;
import com.banking_application.model.UserRole;
import com.banking_application.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AuditingAspectTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AuditingAspectTest {

    @MockBean
    private AuditLogService auditLogService;

    @Autowired
    private AuditableTestService auditableTestService;

    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUser = User.builder()
                .id(1L)
                .userUuid(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .phoneNumber("+1234567890")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();
    }

    @Test
    void aspect_shouldLogSuccessWhenAnnotatedMethodCompletes() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()));

        auditableTestService.auditableMethod(testUser);

        verify(auditLogService).logAction(eq("TEST_ACTION"), eq(testUser), eq("test-resource"),
                anyString(), eq(AuditStatus.SUCCESS), isNull(), isNull());
    }

    @Test
    void aspect_shouldLogFailureWhenAnnotatedMethodThrows() {
        try {
            auditableTestService.failingMethod(testUser);
        } catch (RuntimeException ignored) {
        }

        verify(auditLogService).logFailure(eq("TEST_FAIL"), eq(testUser), eq("fail-resource"),
                eq("Test failure"), isNull());
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import(AuditingAspect.class)
    static class TestConfig {
        @Bean
        public AuditableTestService auditableTestService() {
            return new AuditableTestService();
        }
    }

    static class AuditableTestService {
        @Auditable(action = "TEST_ACTION", resource = "test-resource", details = "Test details")
        public String auditableMethod(User user) {
            return "ok";
        }

        @Auditable(action = "TEST_FAIL", resource = "fail-resource")
        public String failingMethod(User user) {
            throw new RuntimeException("Test failure");
        }
    }
}
