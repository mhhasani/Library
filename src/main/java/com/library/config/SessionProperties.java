package com.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP session hardening.
 *
 * @param cookieName      session cookie name
 * @param cookieSecure    send the cookie over HTTPS only
 * @param cookieSameSite  SameSite attribute (Strict when Keycloak is on the same site)
 * @param encryptionKey   base64 AES-256 key used to encrypt the cookie value
 * @param bindClientIp    end the session when a request arrives from another IP address
 */
@ConfigurationProperties(prefix = "app.session")
public record SessionProperties(
        String cookieName,
        boolean cookieSecure,
        String cookieSameSite,
        String encryptionKey,
        boolean bindClientIp) {
}
