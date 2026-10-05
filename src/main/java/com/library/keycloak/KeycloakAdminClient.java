package com.library.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.config.OidcProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Minimal client for the Keycloak admin REST API of the library realm, authenticated as
 * the application's service account (client credentials). Used to keep the realm in line
 * with the application's security settings, to migrate and manage user credentials, and to
 * read authentication events.
 */
@Component
public class KeycloakAdminClient {

    private static final long TOKEN_REFRESH_MARGIN_SECONDS = 30;

    private final OidcProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient http;

    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;

    public KeycloakAdminClient(OidcProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.http = builder.build();
    }

    // ── Realm ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRealm() {
        return call(() -> http.get().uri(properties.adminRealmUri())
                .headers(this::authorize).retrieve().body(Map.class));
    }

    public void updateRealm(Map<String, Object> realm) {
        call(() -> http.put().uri(properties.adminRealmUri())
                .headers(this::authorize).contentType(MediaType.APPLICATION_JSON)
                .body(realm).retrieve().toBodilessEntity());
    }

    // ── Users ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Optional<String> findUserIdByEmail(String email) {
        List<Map<String, Object>> users = call(() -> http.get()
                .uri(properties.adminRealmUri() + "/users?exact=true&email={email}", email)
                .headers(this::authorize).retrieve().body(List.class));
        return users == null ? Optional.empty() : users.stream().findFirst().map(u -> (String) u.get("id"));
    }

    /** Creates a user and returns its id (the subject of its tokens). */
    public String createUser(Map<String, Object> representation) {
        URI location = call(() -> http.post().uri(properties.adminRealmUri() + "/users")
                .headers(this::authorize).contentType(MediaType.APPLICATION_JSON)
                .body(representation).retrieve().toBodilessEntity().getHeaders().getLocation());
        if (location == null) {
            throw new KeycloakAdminException("Keycloak did not return the new user's location", 0, null);
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Sets a temporary password: the user must change it at the next login. */
    public void setTemporaryPassword(String userId, String password) {
        call(() -> http.put().uri(properties.adminRealmUri() + "/users/{id}/reset-password", userId)
                .headers(this::authorize).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", password, "temporary", true))
                .retrieve().toBodilessEntity());
    }

    /** Ends all of the user's Keycloak sessions. */
    public void logoutUser(String userId) {
        call(() -> http.post().uri(properties.adminRealmUri() + "/users/{id}/logout", userId)
                .headers(this::authorize).retrieve().toBodilessEntity());
    }

    // ── Events ─────────────────────────────────────────────────────────────

    /** Failed login attempts recorded by Keycloak for the user since the given instant. */
    @SuppressWarnings("unchecked")
    public int countLoginFailures(String userId, Instant since) {
        String dateFrom = LocalDate.ofInstant(since, ZoneOffset.UTC).toString();
        List<Map<String, Object>> events = call(() -> http.get()
                .uri(properties.adminRealmUri() + "/events?user={user}&type=LOGIN_ERROR&dateFrom={from}&max=500",
                        userId, dateFrom)
                .headers(this::authorize).retrieve().body(List.class));
        if (events == null) return 0;
        long sinceMillis = since.toEpochMilli();
        return (int) events.stream()
                .map(e -> e.get("time"))
                .filter(t -> t instanceof Number n && n.longValue() >= sinceMillis)
                .count();
    }

    // ── Plumbing ───────────────────────────────────────────────────────────

    private void authorize(HttpHeaders headers) {
        headers.setBearerAuth(serviceAccountToken());
    }

    private synchronized String serviceAccountToken() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiry)) {
            return accessToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        @SuppressWarnings("unchecked")
        Map<String, Object> token = call(() -> http.post()
                .uri(properties.backchannelIssuerUri() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(Map.class));
        if (token == null || token.get("access_token") == null) {
            throw new KeycloakAdminException("Keycloak returned no service-account token", 0, null);
        }
        accessToken = (String) token.get("access_token");
        long expiresIn = ((Number) token.getOrDefault("expires_in", 60)).longValue();
        accessTokenExpiry = Instant.now().plusSeconds(Math.max(0, expiresIn - TOKEN_REFRESH_MARGIN_SECONDS));
        return accessToken;
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new KeycloakAdminException(errorMessage(e), e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new KeycloakAdminException("Keycloak is not reachable", 0, e);
        }
    }

    /** Keycloak reports validation problems (e.g. password policy) as {"error_description": ...}. */
    private String errorMessage(RestClientResponseException e) {
        try {
            Map<?, ?> body = objectMapper.readValue(e.getResponseBodyAsString(), Map.class);
            Object description = body.get("error_description") != null ? body.get("error_description") : body.get("errorMessage");
            return description != null ? description.toString() : e.getMessage();
        } catch (Exception parseFailure) {
            return e.getMessage();
        }
    }
}
