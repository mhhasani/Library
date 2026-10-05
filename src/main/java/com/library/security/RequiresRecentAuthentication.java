package com.library.security;

import com.library.entity.enums.SensitiveOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint as a sensitive operation. When the administrator has enabled the
 * operation in the security settings, the user must have authenticated (password and
 * second factor) within the configured window, otherwise the request is refused with
 * code REAUTH_REQUIRED and the client sends the user through Keycloak again.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRecentAuthentication {

    SensitiveOperation value();
}
