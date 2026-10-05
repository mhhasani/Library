package com.library.config;

import com.library.session.EncryptingCookieSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** Session cookie: encrypted, HttpOnly, Secure, SameSite, never persisted (no Max-Age). */
@Slf4j
@Configuration
@EnableConfigurationProperties({SessionProperties.class, OidcProperties.class})
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer(SessionProperties properties) {
        DefaultCookieSerializer cookie = new DefaultCookieSerializer();
        cookie.setCookieName(properties.cookieName());
        cookie.setCookiePath("/api/");
        cookie.setUseHttpOnlyCookie(true);
        cookie.setUseSecureCookie(properties.cookieSecure());
        cookie.setSameSite(properties.cookieSameSite());
        cookie.setCookieMaxAge(-1);
        return new EncryptingCookieSerializer(cookie, encryptionKey(properties));
    }

    private static SecretKey encryptionKey(SessionProperties properties) {
        String configured = properties.encryptionKey();
        if (configured != null && !configured.isBlank()) {
            byte[] raw = Base64.getDecoder().decode(configured.trim());
            if (raw.length != 32) {
                throw new IllegalStateException("SESSION_COOKIE_KEY must be 32 bytes (base64-encoded)");
            }
            return new SecretKeySpec(raw, "AES");
        }
        log.warn("SESSION_COOKIE_KEY is not set: using a random key, sessions will not survive a restart");
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
