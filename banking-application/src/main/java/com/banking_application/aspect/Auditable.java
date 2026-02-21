package com.banking_application.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic audit logging via AOP.
 * When the method executes (success or failure), an audit log entry is created.
 *
 * @param action   The audit action name (e.g. "DEPOSIT", "TRANSFER"). Defaults to method name.
 * @param resource SpEL expression for the affected resource (e.g. "#p0.accountNumber()").
 * @param details  SpEL expression for audit details. Optional; defaults to method signature.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    String action() default "";

    String resource() default "";

    String details() default "";
}
