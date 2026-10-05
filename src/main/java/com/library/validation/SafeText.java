package com.library.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Free text that will be stored and shown to other users: rejects markup/script
 * patterns and control characters (null is valid; combine with @NotBlank when required).
 */
@Documented
@Constraint(validatedBy = SafeTextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeText {

    String message() default "متن واردشده شامل نویسه‌ها یا الگوهای غیرمجاز است";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
