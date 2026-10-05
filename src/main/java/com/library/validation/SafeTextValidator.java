package com.library.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Deny-list for stored free text. Output is additionally escaped by the UI, so this is a
 * second line of defense against stored XSS and log/console injection.
 */
public class SafeTextValidator implements ConstraintValidator<SafeText, String> {

    /** Anything that opens an HTML/XML tag, comment or processing instruction. */
    private static final Pattern MARKUP = Pattern.compile("<[a-zA-Z!/?]");
    /** Script URLs and inline event-handler attributes. */
    private static final Pattern SCRIPT = Pattern.compile(
            "(?i)(javascript|vbscript|data\\s*:\\s*text/html)\\s*:|\\bon[a-z]+\\s*=");
    /** Control characters other than tab, line feed and carriage return. */
    private static final Pattern CONTROL = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || isSafe(value);
    }

    public static boolean isSafe(String value) {
        return !MARKUP.matcher(value).find()
                && !SCRIPT.matcher(value).find()
                && !CONTROL.matcher(value).find();
    }
}
