package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.audit.AuditService;
import com.library.config.SessionProperties;
import com.library.entity.SecuritySettings;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.repository.UserRepository;
import com.library.settings.SecuritySettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Session security filter: binding, idle timeout, live account state, notice gate")
class SessionSecurityFilterTest {

    private static final String IP = "10.0.0.5";
    private static final String AGENT = "Mozilla/5.0 test";

    private final UserRepository users = mock(UserRepository.class);
    private final SecuritySettingsService settings = mock(SecuritySettingsService.class);
    private final AuditService audit = mock(AuditService.class);
    private final SessionSecurityFilter filter = new SessionSecurityFilter(users, settings,
            new SessionProperties("LIBSESSION", true, "Strict", null, true), audit, new ObjectMapper());

    private User user;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        user = User.builder().id(7L).email("u@x.ir").firstName("F").lastName("L")
                .systemRole(SystemRole.SYSTEM_ADMIN).accountStatus(AccountStatus.ACTIVE).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(settings.get()).thenReturn(SecuritySettings.builder().sessionIdleMinutes(15).build());

        session = new MockHttpSession();
        session.setAttribute(SessionAttributes.USER_ID, 7L);
        session.setAttribute(SessionAttributes.BOUND_IP, IP);
        session.setAttribute(SessionAttributes.BOUND_USER_AGENT, SessionSecurityFilter.userAgentOf(request("/v1/books")));
        session.setAttribute(SessionAttributes.LAST_ACTIVITY, System.currentTimeMillis());
        session.setAttribute(SessionAttributes.NOTICE_ACKNOWLEDGED, true);

        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "kc-7", "email", "u@x.ir"));
        AppOidcUser principal = new AppOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, null, 7L);
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "keycloak"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A valid session is authenticated with the role currently stored in the database")
    void validSessionUsesLiveRole() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("/v1/books"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isInstanceOf(AppUserDetails.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_SYSTEM_ADMIN");
    }

    @Test
    @DisplayName("A request from another IP address ends the session")
    void ipChangeEndsSession() throws Exception {
        MockHttpServletRequest request = request("/v1/books");
        request.setRemoteAddr("10.9.9.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("SESSION_INVALID");
        assertThat(session.isInvalid()).isTrue();
        verify(audit).record(any());
    }

    @Test
    @DisplayName("A different browser (user agent) cannot reuse the session")
    void userAgentChangeEndsSession() throws Exception {
        MockHttpServletRequest request = request("/v1/books");
        request.removeHeader("User-Agent");
        request.addHeader("User-Agent", "curl/8.0");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("The session ends after the configured inactivity")
    void idleSessionExpires() throws Exception {
        session.setAttribute(SessionAttributes.LAST_ACTIVITY, System.currentTimeMillis() - 16 * 60_000L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/v1/books"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("SESSION_EXPIRED");
    }

    @Test
    @DisplayName("Background polling does not count as activity")
    void backgroundRequestsDoNotExtendTheSession() throws Exception {
        long before = System.currentTimeMillis() - 5 * 60_000L;
        session.setAttribute(SessionAttributes.LAST_ACTIVITY, before);
        MockHttpServletRequest request = request("/v1/notifications/unread-count");
        request.addHeader(SessionSecurityFilter.BACKGROUND_HEADER, "true");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(session.getAttribute(SessionAttributes.LAST_ACTIVITY)).isEqualTo(before);
    }

    @Test
    @DisplayName("A suspended account loses access on its next request")
    void suspendedAccountIsCutOff() throws Exception {
        user.setAccountStatus(AccountStatus.SUSPENDED);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/v1/books"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("Data is withheld until the security notice is acknowledged")
    void noticeGate() throws Exception {
        session.setAttribute(SessionAttributes.NOTICE_ACKNOWLEDGED, false);

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("/v1/books"), blocked, new MockFilterChain());
        assertThat(blocked.getStatus()).isEqualTo(403);
        assertThat(blocked.getContentAsString()).contains("NOTICE_REQUIRED");

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("/v1/auth/session"), new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api" + path);
        request.setContextPath("/api");
        request.setRemoteAddr(IP);
        request.addHeader("User-Agent", AGENT);
        request.setSession(session);
        return request;
    }
}
