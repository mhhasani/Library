package com.library.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret",
                "test-secret-key-change-this-in-production-at-least-32-characters-long");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpiration", 604800000L);
    }

    @Test
    @DisplayName("generateAccessToken produces a valid, non-expired token with correct email and userId")
    void generateAccessToken_isValid() {
        String token = tokenProvider.generateAccessToken("user@example.com", 7L);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.isTokenValid(token)).isTrue();
        assertThat(tokenProvider.isTokenExpired(token)).isFalse();
        assertThat(tokenProvider.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(tokenProvider.extractUserId(token)).isEqualTo(7L);
    }

    @Test
    @DisplayName("generateRefreshToken produces a valid token")
    void generateRefreshToken_isValid() {
        String token = tokenProvider.generateRefreshToken("user@example.com", 7L);

        assertThat(tokenProvider.isTokenValid(token)).isTrue();
        assertThat(tokenProvider.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(tokenProvider.extractUserId(token)).isEqualTo(7L);
    }

    @Test
    @DisplayName("isTokenValid returns false for a malformed token")
    void isTokenValid_malformedToken() {
        assertThat(tokenProvider.isTokenValid("not-a-real-jwt")).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired returns true for a malformed token")
    void isTokenExpired_malformedToken() {
        assertThat(tokenProvider.isTokenExpired("not-a-real-jwt")).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for a token signed with a different secret")
    void isTokenValid_wrongSignature() {
        String token = tokenProvider.generateAccessToken("user@example.com", 1L);

        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherProvider, "jwtSecret",
                "a-completely-different-secret-key-also-at-least-32-chars-long");
        ReflectionTestUtils.setField(otherProvider, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(otherProvider, "refreshExpiration", 604800000L);

        assertThat(otherProvider.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired returns true for an already-expired token")
    void isTokenExpired_expiredToken() {
        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", -1000L);
        String token = tokenProvider.generateAccessToken("user@example.com", 1L);

        assertThat(tokenProvider.isTokenExpired(token)).isTrue();
    }

    @Test
    @DisplayName("extractClaims throws for an invalid token")
    void extractClaims_invalidToken() {
        assertThatThrownBy(() -> tokenProvider.extractClaims("garbage"))
                .isInstanceOf(RuntimeException.class);
    }
}
