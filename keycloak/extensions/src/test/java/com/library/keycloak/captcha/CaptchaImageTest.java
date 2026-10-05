package com.library.keycloak.captcha;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CaptchaImageTest {

    @Test
    void generatesReadablePngWithoutFonts() throws Exception {
        CaptchaImage captcha = CaptchaImage.generate();

        assertEquals(5, captcha.answer().length());
        assertTrue(captcha.answer().chars().allMatch(c -> CaptchaImage.ALPHABET.indexOf(c) >= 0));
        assertTrue(captcha.dataUri().startsWith("data:image/png;base64,"));
        byte[] png = Base64.getDecoder().decode(captcha.dataUri().substring("data:image/png;base64,".length()));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image);
        assertTrue(image.getWidth() > 100 && image.getHeight() > 40);
    }

    @Test
    void everyAlphabetCharacterHasAGlyph() {
        for (char c : CaptchaImage.ALPHABET.toCharArray()) {
            assertDoesNotThrow(() -> CaptchaImage.render(String.valueOf(c)), "glyph " + c);
        }
    }

    @Test
    void answersAreRandom() {
        Set<String> answers = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            answers.add(CaptchaImage.generate().answer());
        }
        assertTrue(answers.size() > 45);
    }

    @Test
    void matchingIsCaseInsensitiveAndStrict() {
        assertTrue(CaptchaImage.matches("AB3CD", "ab3cd"));
        assertTrue(CaptchaImage.matches("AB3CD", " AB 3CD "));
        assertFalse(CaptchaImage.matches("AB3CD", "AB3C"));
        assertFalse(CaptchaImage.matches(null, "AB3CD"));
        assertFalse(CaptchaImage.matches("AB3CD", null));
    }
}
