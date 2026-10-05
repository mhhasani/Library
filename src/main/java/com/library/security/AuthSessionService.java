package com.library.security;

import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.dto.SessionInfoDTO;
import com.library.entity.User;
import com.library.entity.enums.AuditAction;
import com.library.keycloak.KeycloakAdminClient;
import com.library.keycloak.KeycloakAdminException;
import com.library.repository.UserRepository;
import com.library.service.UserService;
import com.library.settings.SecuritySettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Session information for the SPA and acknowledgement of the post-login security notice. */
@Slf4j
@Service
public class AuthSessionService {

    private static final String FAILED_LOGINS_CACHE = "library.failedLoginsSinceLastLogin";

    private final UserRepository userRepository;
    private final UserService userService;
    private final KeycloakAdminClient keycloak;
    private final SecuritySettingsService settingsService;
    private final AuditService auditService;
    private final String organizationName;

    public AuthSessionService(UserRepository userRepository, UserService userService, KeycloakAdminClient keycloak,
                              SecuritySettingsService settingsService, AuditService auditService,
                              @Value("${app.organization-name}") String organizationName) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.keycloak = keycloak;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.organizationName = organizationName;
    }

    public SessionInfoDTO describe(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long userId = session == null ? null : (Long) session.getAttribute(SessionAttributes.USER_ID);
        if (userId == null) {
            return SessionInfoDTO.anonymous();
        }
        String previousAt = (String) session.getAttribute(SessionAttributes.PREVIOUS_LOGIN_AT);
        return new SessionInfoDTO(
                true,
                userService.getCurrentUserProfile(),
                Boolean.TRUE.equals(session.getAttribute(SessionAttributes.NOTICE_ACKNOWLEDGED)),
                organizationName,
                (String) session.getAttribute(SessionAttributes.BOUND_IP),
                previousAt,
                (String) session.getAttribute(SessionAttributes.PREVIOUS_LOGIN_IP),
                failedLoginsSince(session, userId, previousAt),
                settingsService.get().getSessionIdleMinutes());
    }

    public void acknowledgeNotice(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long userId = session == null ? null : (Long) session.getAttribute(SessionAttributes.USER_ID);
        if (userId == null) {
            return;
        }
        session.setAttribute(SessionAttributes.NOTICE_ACKNOWLEDGED, true);
        auditService.record(AuditEntry.success(AuditAction.SECURITY_NOTICE_ACKNOWLEDGED)
                .actorId(userId).entityType("USER").entityId(userId).build());
    }

    /** Asked once per session from Keycloak's login events; unknown if Keycloak is unreachable. */
    private Integer failedLoginsSince(HttpSession session, Long userId, String previousAt) {
        Integer cached = (Integer) session.getAttribute(FAILED_LOGINS_CACHE);
        if (cached != null || previousAt == null) {
            return cached;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getKeycloakSubject() == null) {
            return null;
        }
        try {
            int failures = keycloak.countLoginFailures(user.getKeycloakSubject(),
                    LocalDateTime.parse(previousAt).atZone(ZoneId.systemDefault()).toInstant());
            session.setAttribute(FAILED_LOGINS_CACHE, failures);
            return failures;
        } catch (KeycloakAdminException e) {
            log.warn("Could not read failed logins from the identity provider: {}", e.getMessage());
            return null;
        }
    }
}
