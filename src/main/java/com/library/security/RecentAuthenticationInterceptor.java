package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.ApiResponse;
import com.library.entity.SecuritySettings;
import com.library.settings.SecuritySettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/** Enforces {@link RequiresRecentAuthentication} for sessions created by an OIDC login. */
@Component
public class RecentAuthenticationInterceptor implements HandlerInterceptor {

    private final SecuritySettingsService settingsService;
    private final ObjectMapper objectMapper;

    public RecentAuthenticationInterceptor(SecuritySettingsService settingsService, ObjectMapper objectMapper) {
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequiresRecentAuthentication marker = method.getMethodAnnotation(RequiresRecentAuthentication.class);
        HttpSession session = request.getSession(false);
        Long authTime = session == null ? null : (Long) session.getAttribute(SessionAttributes.AUTH_TIME);
        if (marker == null || authTime == null) {
            return true;
        }
        SecuritySettings settings = settingsService.get();
        if (!settings.sensitiveOperationSet().contains(marker.value())) {
            return true;
        }
        long windowMillis = settings.getReauthWindowMinutes() * 60_000L;
        if (System.currentTimeMillis() - authTime <= windowMillis) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(
                "احراز هویت مجدد لازم است",
                "برای انجام این عملیات حساس، لطفاً دوباره هویت خود را تأیید کنید",
                "REAUTH_REQUIRED"));
        return false;
    }
}
