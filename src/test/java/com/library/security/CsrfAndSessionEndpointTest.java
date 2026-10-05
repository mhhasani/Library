package com.library.security;

import com.library.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("CSRF protection and session endpoints")
class CsrfAndSessionEndpointTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    @DisplayName("A cookie-authenticated write without the CSRF token is refused")
    void writeWithSessionCookieNeedsCsrfToken() throws Exception {
        mockMvc.perform(post("/v1/me/favorites/1").cookie(new Cookie("LIBSESSION", "x")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("The same write with the CSRF token passes the CSRF check")
    void writeWithCsrfTokenPassesCheck() throws Exception {
        mockMvc.perform(post("/v1/me/favorites/1").cookie(new Cookie("LIBSESSION", "x")).with(csrf()))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(403));
    }

    @Test
    @DisplayName("Anonymous callers get an unauthenticated session description, not an error")
    void anonymousSession() throws Exception {
        mockMvc.perform(get("/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }

    @Test
    @DisplayName("Login starts at Keycloak with PKCE, and the CSRF cookie is issued")
    void loginRedirectsToKeycloakWithPkce() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(
                        startsWith("https://library.test/auth/realms/library/protocol/openid-connect/auth"),
                        containsString("code_challenge_method=S256"),
                        containsString("redirect_uri=https://library.test/api/login/oauth2/code/keycloak"))));
    }

    @Test
    @DisplayName("Account actions map to Keycloak application-initiated actions")
    void accountActionsMapToKeycloak() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak").param("action", "password"))
                .andExpect(header().string("Location", containsString("kc_action=UPDATE_PASSWORD")));
        mockMvc.perform(get("/oauth2/authorization/keycloak").param("action", "unknown"))
                .andExpect(header().string("Location", not(containsString("kc_action"))));
    }

    @Test
    @DisplayName("Re-authentication forces a fresh login at Keycloak")
    void reauthenticationForcesLogin() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/keycloak").param("reauth", "true"))
                .andExpect(header().string("Location", allOf(
                        containsString("prompt=login"),
                        containsString("max_age=0"))));
    }
}
