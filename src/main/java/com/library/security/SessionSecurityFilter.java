package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.config.SessionProperties;
import com.library.dto.ApiResponse;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.AuditAction;
import com.library.repository.UserRepository;
import com.library.settings.SecuritySettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Enforces the session rules on every request of a logged-in user, before authorization:
 * <ol>
 *   <li>the session stays bound to the client IP (configurable) and user agent it was created for;</li>
 *   <li>it ends after the configured inactivity (background polling does not count as activity);</li>
 *   <li>the account must still exist and be active — role and status are re-read from the
 *       database, so a suspension or role change applies to the very next request;</li>
 *   <li>no data is served until the post-login security notice has been acknowledged.</li>
 * </ol>
 * Requests not authenticated through the identity provider are left untouched.
 */
public class SessionSecurityFilter extends OncePerRequestFilter {

    /** Sent by the SPA on automatic polling requests, which must not keep a session alive. */
    public static final String BACKGROUND_HEADER = "X-Background-Request";

    private static final String API_PATH = "/v1/";
    private static final String AUTH_PATH = "/v1/auth/";

    private final UserRepository userRepository;
    private final SecuritySettingsService settingsService;
    private final SessionProperties sessionProperties;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public SessionSecurityFilter(UserRepository userRepository, SecuritySettingsService settingsService,
                                 SessionProperties sessionProperties, AuditService auditService,
                                 ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.settingsService = settingsService;
        this.sessionProperties = sessionProperties;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession(false);
        if (!(authentication instanceof OAuth2AuthenticationToken) || session == null) {
            chain.doFilter(request, response);
            return;
        }

        Long userId = (Long) session.getAttribute(SessionAttributes.USER_ID);
        if (isHijackSuspected(request, session)) {
            reject(request, response, session, userId, AuditAction.SESSION_BINDING_MISMATCH,
                    "request from another client " + request.getRemoteAddr(), "SESSION_INVALID");
            return;
        }
        if (isIdleExpired(session)) {
            reject(request, response, session, userId, AuditAction.SESSION_EXPIRED,
                    "inactivity", "SESSION_EXPIRED");
            return;
        }
        Optional<User> user = userId == null ? Optional.empty() : userRepository.findById(userId);
        if (user.isEmpty() || user.get().getAccountStatus() != AccountStatus.ACTIVE) {
            reject(request, response, session, userId, AuditAction.SESSION_TERMINATED,
                    "account no longer active", "SESSION_INVALID");
            return;
        }

        if (!"true".equalsIgnoreCase(request.getHeader(BACKGROUND_HEADER))) {
            session.setAttribute(SessionAttributes.LAST_ACTIVITY, System.currentTimeMillis());
        }
        authenticateRequest(user.get());

        if (!Boolean.TRUE.equals(session.getAttribute(SessionAttributes.NOTICE_ACKNOWLEDGED))
                && isDataRequest(pathOf(request))) {
            write(response, HttpStatus.FORBIDDEN, ApiResponse.error("تأیید اطلاعیه‌ی امنیتی لازم است",
                    "ابتدا اطلاعیه‌ی امنیتی را مطالعه و تأیید کنید", "NOTICE_REQUIRED"));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isHijackSuspected(HttpServletRequest request, HttpSession session) {
        boolean ipChanged = sessionProperties.bindClientIp()
                && !Objects.equals(session.getAttribute(SessionAttributes.BOUND_IP), request.getRemoteAddr());
        boolean agentChanged = !Objects.equals(session.getAttribute(SessionAttributes.BOUND_USER_AGENT),
                userAgentOf(request));
        return ipChanged || agentChanged;
    }

    private boolean isIdleExpired(HttpSession session) {
        Long last = (Long) session.getAttribute(SessionAttributes.LAST_ACTIVITY);
        long idleMillis = settingsService.get().getSessionIdleMinutes() * 60_000L;
        return last == null || System.currentTimeMillis() - last > idleMillis;
    }

    /**
     * The session holds only the identity; authorities come from the current database row,
     * for this request only (the stored security context is not modified).
     */
    private static void authenticateRequest(User user) {
        AppUserDetails principal = new AppUserDetails(user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                        Long userId, AuditAction action, String reason, String code) throws IOException {
        auditService.record(AuditEntry.failure(action).actorId(userId).entityType("SESSION")
                .details(reason + " | " + request.getMethod() + " " + request.getRequestURI()).build());
        session.invalidate();
        SecurityContextHolder.clearContext();
        write(response, HttpStatus.UNAUTHORIZED,
                ApiResponse.error("نشست شما پایان یافته است", "لطفاً دوباره وارد شوید", code));
    }

    private void write(HttpServletResponse response, HttpStatus status, ApiResponse<Object> body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /** Application data endpoints; session and login (OIDC) endpoints stay reachable. */
    private static boolean isDataRequest(String path) {
        return path.startsWith(API_PATH) && !path.startsWith(AUTH_PATH);
    }

    private static String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** Fingerprint of the User-Agent header (stored hashed, compared exactly). */
    static String userAgentOf(HttpServletRequest request) {
        String agent = Objects.toString(request.getHeader(HttpHeaders.USER_AGENT), "");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(agent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
