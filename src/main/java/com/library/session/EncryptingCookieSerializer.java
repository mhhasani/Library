package com.library.session;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Encrypts the session cookie's content with AES-256-GCM (random IV per cookie). The
 * browser only ever holds ciphertext; a tampered or forged cookie fails authentication
 * and is ignored. Cookie attributes (HttpOnly, Secure, SameSite, no Max-Age, i.e. never
 * persisted to disk) come from the wrapped {@link DefaultCookieSerializer}.
 */
@Slf4j
public class EncryptingCookieSerializer implements CookieSerializer {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final DefaultCookieSerializer delegate;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public EncryptingCookieSerializer(DefaultCookieSerializer delegate, SecretKey key) {
        this.delegate = delegate;
        this.key = key;
        delegate.setUseBase64Encoding(false);
    }

    @Override
    public void writeCookieValue(CookieValue cookieValue) {
        String value = cookieValue.getCookieValue();
        String encrypted = value == null || value.isEmpty() ? "" : encrypt(value);
        CookieValue out = new CookieValue(cookieValue.getRequest(), cookieValue.getResponse(), encrypted);
        out.setCookieMaxAge(cookieValue.getCookieMaxAge());
        delegate.writeCookieValue(out);
    }

    @Override
    public List<String> readCookieValues(HttpServletRequest request) {
        List<String> plain = new ArrayList<>();
        for (String value : delegate.readCookieValues(request)) {
            try {
                plain.add(decrypt(value));
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                log.warn("Ignoring a session cookie that failed decryption from {}", request.getRemoteAddr());
            }
        }
        return plain;
    }

    String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_BYTES + cipherText.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(cipherText, 0, out, IV_BYTES, cipherText.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Session cookie encryption failed", e);
        }
    }

    String decrypt(String encoded) throws GeneralSecurityException {
        byte[] data = Base64.getUrlDecoder().decode(encoded);
        if (data.length <= IV_BYTES) {
            throw new GeneralSecurityException("cookie too short");
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
        return new String(cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES), StandardCharsets.UTF_8);
    }
}
