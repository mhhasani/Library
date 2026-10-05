package com.library.security;

import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.entity.User;
import com.library.entity.enums.AuditAction;
import com.library.repository.UserRepository;
import com.library.settings.SecuritySettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Runs after Keycloak has authenticated the user (the session id has already been
 * changed, preventing session fixation):
 * <ul>
 *   <li>binds the session to the client IP and user agent and sets the idle timeout;</li>
 *   <li>keeps a single session per account — any other session of the user is ended;</li>
 *   <li>remembers the previous login (time and IP) for the security notice;</li>
 *   <li>requires the security notice to be acknowledged before any data is served.</li>
 * </ul>
 */
@Slf4j
@Component
public class OidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final SecuritySettingsService settingsService;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;
    private final AuditService auditService;

    public OidcLoginSuccessHandler(UserRepository userRepository, SecuritySettingsService settingsService,
                                   FindByIndexNameSessionRepository<? extends Session> sessions,
                                   AuditService auditService) {
        this.userRepository = userRepository;
        this.settingsService = settingsService;
        this.sessions = sessions;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        AppOidcUser principal = (AppOidcUser) authentication.getPrincipal();
        HttpSession session = request.getSession();
        boolean reauthentication = Boolean.TRUE.equals(session.getAttribute(SessionAttributes.REAUTH_PENDING));
        long now = System.currentTimeMillis();
        Instant authTime = principal.getIdToken().getAuthenticatedAt();

        session.setAttribute(SessionAttributes.USER_ID, principal.getUserId());
        session.setAttribute(SessionAttributes.BOUND_IP, request.getRemoteAddr());
        session.setAttribute(SessionAttributes.BOUND_USER_AGENT, SessionSecurityFilter.userAgentOf(request));
        session.setAttribute(SessionAttributes.AUTH_TIME, authTime != null ? authTime.toEpochMilli() : now);
        session.setAttribute(SessionAttributes.LAST_ACTIVITY, now);
        session.setAttribute(SessionAttributes.ID_TOKEN, principal.getIdToken().getTokenValue());
        session.setMaxInactiveInterval(settingsService.get().getSessionIdleMinutes() * 60);
        session.removeAttribute(SessionAttributes.REAUTH_PENDING);

        if (reauthentication) {
            auditService.record(AuditEntry.success(AuditAction.REAUTHENTICATION)
                    .entityType("USER").entityId(principal.getUserId()).build());
        } else {
            session.setAttribute(SessionAttributes.NOTICE_ACKNOWLEDGED, false);
            recordLogin(principal.getUserId(), request.getRemoteAddr(), session);
            endOtherSessions(authentication.getName(), session.getId(), principal.getUserId());
        }

        String returnTo = (String) session.getAttribute(SessionAttributes.RETURN_TO);
        session.removeAttribute(SessionAttributes.RETURN_TO);
        response.sendRedirect(returnTo != null ? returnTo : "/");
    }

    private void recordLogin(Long userId, String ip, HttpSession session) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getLastLoginAt() != null) {
            session.setAttribute(SessionAttributes.PREVIOUS_LOGIN_AT, user.getLastLoginAt().toString());
            session.setAttribute(SessionAttributes.PREVIOUS_LOGIN_IP, user.getLastLoginIp());
        }
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userRepository.save(user);
    }

    /** One identity, one session: a new login ends the user's other sessions. */
    private void endOtherSessions(String principalName, String currentSessionId, Long userId) {
        for (String sessionId : sessions.findByPrincipalName(principalName).keySet()) {
            if (!sessionId.equals(currentSessionId)) {
                sessions.deleteById(sessionId);
                auditService.record(AuditEntry.success(AuditAction.SESSION_TERMINATED)
                        .entityType("USER").entityId(userId)
                        .details("previous session ended by a new login").build());
            }
        }
    }
}
