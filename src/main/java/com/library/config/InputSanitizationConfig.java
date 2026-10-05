package com.library.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Central input sanitization: every string in every JSON request body passes through
 * here before reaching a controller. Invisible control characters (NUL, escape
 * sequences, ...) are removed; tab, line feed and carriage return are kept.
 * Field-level rules (size, format, {@code @SafeText}) are then applied by Bean Validation.
 */
@Configuration
public class InputSanitizationConfig {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    /** Registered by Spring Boot alongside its own Jackson modules (any Module bean is picked up). */
    @Bean
    public Module inputSanitizationModule() {
        SimpleModule module = new SimpleModule("input-sanitization");
        module.addDeserializer(String.class, new StringDeserializer() {
            @Override
            public String deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
                String value = super.deserialize(parser, ctx);
                return value == null ? null : CONTROL_CHARS.matcher(value).replaceAll("");
            }
        });
        return module;
    }
}
