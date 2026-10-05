package com.library.session;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer.CookieValue;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Encrypted session cookie")
class EncryptingCookieSerializerTest {

    private static final String SESSION_ID = "4f8d1c2a-0b7e-4c55-9a51-2f3e6d7c8b90";

    private final EncryptingCookieSerializer serializer = serializer(new byte[32]);

    @Test
    @DisplayName("The cookie never contains the session id in clear and carries hardened attributes")
    void cookieIsEncryptedAndHardened() {
        MockHttpServletResponse response = write(serializer, SESSION_ID);
        String header = response.getHeader("Set-Cookie");

        assertThat(header).doesNotContain(SESSION_ID);
        assertThat(header).contains("HttpOnly").contains("Secure").contains("SameSite=Strict");
        assertThat(header).doesNotContain("Max-Age").doesNotContain("Expires");
    }

    @Test
    @DisplayName("The encrypted cookie reads back to the session id")
    void roundTrip() {
        Cookie cookie = cookieFrom(write(serializer, SESSION_ID));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookie);

        assertThat(serializer.readCookieValues(request)).containsExactly(SESSION_ID);
    }

    @Test
    @DisplayName("Tampered or foreign cookies are ignored")
    void tamperedCookieIgnored() {
        Cookie cookie = cookieFrom(write(serializer, SESSION_ID));
        char[] value = cookie.getValue().toCharArray();
        value[value.length - 2] = value[value.length - 2] == 'A' ? 'B' : 'A';
        MockHttpServletRequest tampered = new MockHttpServletRequest();
        tampered.setCookies(new Cookie("LIBSESSION", new String(value)));
        assertThat(serializer.readCookieValues(tampered)).isEmpty();

        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        MockHttpServletRequest foreign = new MockHttpServletRequest();
        foreign.setCookies(cookieFrom(write(serializer(otherKey), SESSION_ID)));
        assertThat(serializer.readCookieValues(foreign)).isEmpty();

        MockHttpServletRequest plain = new MockHttpServletRequest();
        plain.setCookies(new Cookie("LIBSESSION", SESSION_ID));
        assertThat(serializer.readCookieValues(plain)).isEmpty();
    }

    private static EncryptingCookieSerializer serializer(byte[] key) {
        DefaultCookieSerializer cookie = new DefaultCookieSerializer();
        cookie.setCookieName("LIBSESSION");
        cookie.setUseSecureCookie(true);
        cookie.setSameSite("Strict");
        return new EncryptingCookieSerializer(cookie, new SecretKeySpec(key, "AES"));
    }

    private static MockHttpServletResponse write(EncryptingCookieSerializer serializer, String value) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieValue(new MockHttpServletRequest(), response, value));
        return response;
    }

    private static Cookie cookieFrom(MockHttpServletResponse response) {
        String header = response.getHeader("Set-Cookie");
        String value = header.substring("LIBSESSION=".length(), header.indexOf(';'));
        return new Cookie("LIBSESSION", value);
    }
}
