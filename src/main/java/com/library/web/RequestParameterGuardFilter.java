package com.library.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.ApiResponse;
import com.library.validation.SafeTextValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Central validation of request parameters (query string and form fields) on every endpoint,
 * before any controller runs:
 * <ul>
 *   <li>"page" must be a non-negative integer, "size"/"limit" within [1, max-page-size],
 *       so a single request cannot load whole tables into memory;</li>
 *   <li>every value is bounded in length and rejected if it carries markup, script patterns
 *       or control characters (same rules as {@code @SafeText}).</li>
 * </ul>
 * JSON bodies are covered by Bean Validation and {@code InputSanitizationConfig}.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestParameterGuardFilter extends OncePerRequestFilter {

    static final int MAX_PARAMETER_LENGTH = 2000;

    private final int maxPageSize;
    private final ObjectMapper objectMapper;

    public RequestParameterGuardFilter(@Value("${app.security.max-page-size:100}") int maxPageSize,
                                       ObjectMapper objectMapper) {
        this.maxPageSize = maxPageSize;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String error = checkPaging(request);
        if (error == null) error = checkValues(request.getParameterMap());
        if (error != null) {
            log.warn("Rejected request parameters on {}: {}", request.getRequestURI(), error);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(),
                    ApiResponse.error("درخواست نامعتبر", "پارامترهای درخواست نامعتبر است"));
            return;
        }
        chain.doFilter(request, response);
    }

    private String checkPaging(HttpServletRequest request) {
        String error = checkRange(request.getParameter("page"), 0, Integer.MAX_VALUE, "page");
        if (error == null) error = checkRange(request.getParameter("size"), 1, maxPageSize, "size");
        if (error == null) error = checkRange(request.getParameter("limit"), 1, maxPageSize, "limit");
        return error;
    }

    private static String checkValues(Map<String, String[]> parameters) {
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            for (String value : entry.getValue()) {
                if (value.length() > MAX_PARAMETER_LENGTH) {
                    return entry.getKey() + " too long";
                }
                if (!SafeTextValidator.isSafe(value)) {
                    return entry.getKey() + " contains disallowed content";
                }
            }
        }
        return null;
    }

    private static String checkRange(String raw, int min, int max, String name) {
        if (raw == null) return null;
        try {
            int value = Integer.parseInt(raw.trim());
            return (value < min || value > max) ? name + " out of range" : null;
        } catch (NumberFormatException e) {
            return name + " not a number";
        }
    }
}
