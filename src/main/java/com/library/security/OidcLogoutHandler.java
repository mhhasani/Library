package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.config.OidcProperties;
import com.library.dto.ApiResponse;
import com.library.entity.enums.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Explicit logout: the application session is invalidated by Spring Security, and the SPA
 * receives Keycloak's end-session URL so the identity-provider session ends too.
 */
@Component
public class OidcLogoutHandler implements LogoutHandler, LogoutSuccessHandler {

    private static final String LOGOUT_URL_ATTRIBUTE = OidcLogoutHandler.class.getName() + ".logoutUrl";

    private final OidcProperties properties;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public OidcLogoutHandler(OidcProperties properties, AuditService auditService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /** Runs before the session is invalidated: records the logout and keeps the ID token hint. */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Long userId = (Long) session.getAttribute(SessionAttributes.USER_ID);
        if (userId != null) {
            auditService.record(AuditEntry.success(AuditAction.LOGOUT).actorId(userId).entityType("USER")
                    .entityId(userId).build());
        }
        String idToken = (String) session.getAttribute(SessionAttributes.ID_TOKEN);
        request.setAttribute(LOGOUT_URL_ATTRIBUTE, endSessionUrl(idToken));
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        String logoutUrl = (String) request.getAttribute(LOGOUT_URL_ATTRIBUTE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.success("Logged out",
                Map.of("logoutUrl", logoutUrl != null ? logoutUrl : "/")));
    }

    private String endSessionUrl(String idToken) {
        UriComponentsBuilder url = UriComponentsBuilder
                .fromHttpUrl(properties.issuerUri() + "/protocol/openid-connect/logout")
                .queryParam("client_id", properties.clientId())
                .queryParam("post_logout_redirect_uri", properties.publicUrl() + "/");
        if (idToken != null) {
            url.queryParam("id_token_hint", idToken);
        }
        return url.encode().build().toUriString();
    }
}
