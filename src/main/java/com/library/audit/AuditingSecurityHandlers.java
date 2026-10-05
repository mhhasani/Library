package com.library.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.ApiResponse;
import com.library.entity.enums.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security-filter-level rejections (no/invalid credentials, forbidden URL) are recorded as
 * attempts to reach protected resources and answered with a generic JSON body.
 */
@Component
public class AuditingSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditingSecurityHandlers(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        auditService.record(AuditEntry.failure(AuditAction.AUTHENTICATION_REQUIRED)
                .entityType("API").details(request.getMethod() + " " + request.getRequestURI()).build());
        write(response, HttpStatus.UNAUTHORIZED, "احراز هویت لازم است", "لطفاً وارد سامانه شوید");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        auditService.record(AuditEntry.failure(AuditAction.ACCESS_DENIED)
                .entityType("API").details(request.getMethod() + " " + request.getRequestURI()).build());
        write(response, HttpStatus.FORBIDDEN, "دسترسی رد شد", "شما اجازه‌ی دسترسی به این بخش را ندارید");
    }

    private void write(HttpServletResponse response, HttpStatus status, String message, String error)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message, error));
    }
}
