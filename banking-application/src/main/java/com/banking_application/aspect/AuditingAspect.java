package com.banking_application.aspect;

import com.banking_application.model.AuditStatus;
import com.banking_application.model.User;
import com.banking_application.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;


@Aspect
@Component
public class AuditingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditingAspect.class);
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private final AuditLogService auditLogService;

    public AuditingAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(com.banking_application.aspect.Auditable) && @annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String action = auditable.action().isEmpty()
                ? pjp.getSignature().getName()
                : auditable.action();

        Object[] args = pjp.getArgs();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] paramNames = signature.getParameterNames();
        EvaluationContext evalContext = createEvaluationContext(signature.getMethod().getName(), args, paramNames);

        User user = resolveUser(args);
        String resource = evaluateSpel(auditable.resource(), evalContext, args);
        String details = evaluateSpel(auditable.details(), evalContext, args);
        if (details == null || details.isEmpty()) {
            details = pjp.getSignature().toShortString();
        }

        String ipAddress = getClientIp();
        String userAgent = getUserAgent();

        try {
            Object result = pjp.proceed();
            auditLogService.logAction(action, user, resource, details, AuditStatus.SUCCESS, ipAddress, userAgent);
            return result;
        } catch (Throwable ex) {
            String errorDetails = details + " | Exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
            auditLogService.logFailure(action, user, resource, ex.getMessage(), ipAddress);
            log.debug("Audited failure for {}: {}", action, ex.getMessage());
            throw ex;
        }
    }

    private User resolveUser(Object[] args) {
        User fromSecurity = getCurrentUser();
        if (fromSecurity != null) return fromSecurity;

        for (Object arg : args) {
            if (arg instanceof User u) return u;
        }
        return null;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        return principal instanceof User u ? u : null;
    }

    private EvaluationContext createEvaluationContext(String methodName, Object[] args, String[] paramNames) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            if (paramNames != null && i < paramNames.length) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        context.setVariable("methodName", methodName);
        return context;
    }

    private String evaluateSpel(String expression, EvaluationContext context, Object[] args) {
        if (expression == null || expression.isBlank()) return "";
        try {
            Object value = SPEL_PARSER.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.trace("SpEL evaluation failed for '{}': {}", expression, e.getMessage());
            return "";
        }
    }

    private String getClientIp() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(a -> ((ServletRequestAttributes) a).getRequest())
                .map(this::extractClientIp)
                .orElse(null);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(a -> ((ServletRequestAttributes) a).getRequest())
                .map(r -> r.getHeader("User-Agent"))
                .orElse(null);
    }
}
