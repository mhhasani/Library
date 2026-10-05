package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.entity.SecuritySettings;
import com.library.entity.enums.SensitiveOperation;
import com.library.settings.SecuritySettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Re-authentication for sensitive operations")
class RecentAuthenticationInterceptorTest {

    private final SecuritySettingsService settingsService = mock(SecuritySettingsService.class);
    private final RecentAuthenticationInterceptor interceptor =
            new RecentAuthenticationInterceptor(settingsService, new ObjectMapper());
    private SecuritySettings settings;

    static class Endpoints {
        @RequiresRecentAuthentication(SensitiveOperation.USER_ROLE_CHANGE)
        public void changeRole() {
        }

        public void ordinary() {
        }
    }

    @BeforeEach
    void setUp() {
        settings = SecuritySettings.builder().reauthWindowMinutes(5).build();
        settings.setSensitiveOperationSet(Set.of(SensitiveOperation.USER_ROLE_CHANGE));
        when(settingsService.get()).thenReturn(settings);
    }

    @Test
    @DisplayName("A stale authentication is refused with REAUTH_REQUIRED")
    void staleAuthenticationRefused() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(request(10), response, handler("changeRole"));

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("REAUTH_REQUIRED");
    }

    @Test
    @DisplayName("A fresh authentication is accepted")
    void freshAuthenticationAccepted() throws Exception {
        assertThat(interceptor.preHandle(request(1), new MockHttpServletResponse(), handler("changeRole"))).isTrue();
    }

    @Test
    @DisplayName("Operations the administrator has not marked sensitive are not gated")
    void unmarkedOperationNotGated() throws Exception {
        settings.setSensitiveOperationSet(Set.of());
        assertThat(interceptor.preHandle(request(10), new MockHttpServletResponse(), handler("changeRole"))).isTrue();
        assertThat(interceptor.preHandle(request(10), new MockHttpServletResponse(), handler("ordinary"))).isTrue();
    }

    private static MockHttpServletRequest request(int minutesSinceAuthentication) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(SessionAttributes.AUTH_TIME,
                System.currentTimeMillis() - minutesSinceAuthentication * 60_000L);
        return request;
    }

    private static HandlerMethod handler(String method) throws NoSuchMethodException {
        return new HandlerMethod(new Endpoints(), Endpoints.class.getMethod(method));
    }
}
