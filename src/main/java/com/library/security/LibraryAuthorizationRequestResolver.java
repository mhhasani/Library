package com.library.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the redirect to Keycloak (always with PKCE) and maps the SPA's intents to
 * Keycloak parameters:
 * <ul>
 *   <li>{@code ?register=true} → registration page ({@code prompt=create})</li>
 *   <li>{@code ?reauth=true} → force a fresh password + second factor ({@code prompt=login, max_age=0})</li>
 *   <li>{@code ?action=password} / {@code ?action=otp} → change password / enroll an authenticator</li>
 *   <li>{@code ?returnTo=/path} → in-app path to land on afterwards (relative paths only)</li>
 * </ul>
 */
public class LibraryAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    private static final Map<String, String> KEYCLOAK_ACTIONS = Map.of(
            "password", "UPDATE_PASSWORD",
            "otp", "CONFIGURE_TOTP");

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public LibraryAuthorizationRequestResolver(ClientRegistrationRepository registrations) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, AUTHORIZATION_BASE_URI);
        this.delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(request, delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(HttpServletRequest request, OAuth2AuthorizationRequest original) {
        if (original == null) {
            return null;
        }
        Map<String, Object> extra = new HashMap<>(original.getAdditionalParameters());
        boolean reauth = "true".equals(request.getParameter("reauth"));
        if ("true".equals(request.getParameter("register"))) {
            extra.put("prompt", "create");
        }
        if (reauth) {
            extra.put("prompt", "login");
            extra.put("max_age", "0");
        }
        String action = request.getParameter("action");
        if (action != null && KEYCLOAK_ACTIONS.containsKey(action)) {
            extra.put("kc_action", KEYCLOAK_ACTIONS.get(action));
        }

        var session = request.getSession();
        // Re-authentication and account actions continue the current session (if any)
        session.setAttribute(SessionAttributes.REAUTH_PENDING, reauth || action != null);
        String returnTo = safeReturnPath(request.getParameter("returnTo"));
        if (returnTo != null) {
            session.setAttribute(SessionAttributes.RETURN_TO, returnTo);
        } else {
            session.removeAttribute(SessionAttributes.RETURN_TO);
        }
        return OAuth2AuthorizationRequest.from(original).additionalParameters(extra).build();
    }

    /** Only same-application paths: "/x/y", never "//host", "/\\host" or absolute URLs. */
    static String safeReturnPath(String path) {
        if (path == null || path.length() > 512 || !path.startsWith("/")
                || path.startsWith("//") || path.startsWith("/\\") || path.contains("\r") || path.contains("\n")) {
            return null;
        }
        return path;
    }
}
