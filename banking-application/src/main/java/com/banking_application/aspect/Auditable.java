package com.banking_application.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for AOP-based audit logging. Audit logic is applied as a cross-cutting concern
 * without cluttering business logic. Supports SpEL expressions for dynamic resource and details.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Audit action identifier (e.g. SIGNUP, LOGIN_SUCCESS, DEPOSIT). */
    String action();

    /** Resource being audited. Use SpEL for dynamic values, e.g. "'account:' + #dto.accountNumber()". */
    String resource() default "auth";

    /** Human-readable details. Use SpEL for dynamic values. */
    String details() default "";

    /** Action to use when an exception is thrown. If empty, no failure audit is logged. */
    String failureAction() default "";

    /** Parameter name containing the User for audit context. If empty, inferred from args or result. */
    String userParam() default "";
}
