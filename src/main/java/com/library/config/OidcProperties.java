package com.library.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Connection to the OpenID Connect provider (Keycloak).
 *
 * @param publicUrl        base URL users open (e.g. https://library.local:3000); used for redirects
 * @param issuerUri        realm issuer as seen by browsers and written into tokens
 * @param internalIssuerUri same realm reached from the backend over the internal network
 *                         (token, keys, admin API); defaults to {@code issuerUri}
 */
@Validated
@ConfigurationProperties(prefix = "app.oidc")
public record OidcProperties(
        @NotBlank String publicUrl,
        @NotBlank String issuerUri,
        String internalIssuerUri,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        String bootstrapSuperAdminEmail,
        boolean adminSyncEnabled) {

    public static final String REGISTRATION_ID = "keycloak";

    public String backchannelIssuerUri() {
        return internalIssuerUri == null || internalIssuerUri.isBlank() ? issuerUri : internalIssuerUri;
    }

    /** Admin REST base of the same realm: {@code .../auth/admin/realms/<realm>}. */
    public String adminRealmUri() {
        return backchannelIssuerUri().replace("/realms/", "/admin/realms/");
    }
}
