package com.library.keycloak.captcha;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Self-contained image captcha (no external service, works offline).
 *
 * Characters are drawn as vector strokes on a 4x6 grid instead of with fonts, so the
 * image can be rendered in a minimal headless JVM without any installed font. Characters
 * that are easy to confuse (0/O, 1/I, 2/Z, 5/S, 8/B, 6/G) are not used.
 */
public final class CaptchaImage {

    static final String ALPHABET = "234679ACDEFHKMNPRTUVWXY";
    private static final int LENGTH = 5;
    private static final int WIDTH = 220;
    private static final int HEIGHT = 72;

    /** Strokes per character: {x1, y1, x2, y2} on a 4 (wide) x 6 (high) grid. */
    private static final Map<Character, double[][]> GLYPHS = Map.ofEntries(
            Map.entry('2', new double[][]{{0, 0, 4, 0}, {4, 0, 4, 3}, {4, 3, 0, 3}, {0, 3, 0, 6}, {0, 6, 4, 6}}),
            Map.entry('3', new double[][]{{0, 0, 4, 0}, {4, 0, 4, 6}, {1, 3, 4, 3}, {0, 6, 4, 6}}),
            Map.entry('4', new double[][]{{0, 0, 0, 3}, {0, 3, 4, 3}, {4, 0, 4, 6}}),
            Map.entry('6', new double[][]{{4, 0, 0, 0}, {0, 0, 0, 6}, {0, 6, 4, 6}, {4, 6, 4, 3}, {4, 3, 0, 3}}),
            Map.entry('7', new double[][]{{0, 0, 4, 0}, {4, 0, 1, 6}}),
            Map.entry('9', new double[][]{{4, 3, 0, 3}, {0, 3, 0, 0}, {0, 0, 4, 0}, {4, 0, 4, 6}, {4, 6, 0, 6}}),
            Map.entry('A', new double[][]{{0, 6, 2, 0}, {2, 0, 4, 6}, {1, 3, 3, 3}}),
            Map.entry('C', new double[][]{{4, 0, 0, 0}, {0, 0, 0, 6}, {0, 6, 4, 6}}),
            Map.entry('D', new double[][]{{0, 0, 0, 6}, {0, 0, 2.5, 0}, {2.5, 0, 4, 2}, {4, 2, 4, 4}, {4, 4, 2.5, 6}, {2.5, 6, 0, 6}}),
            Map.entry('E', new double[][]{{4, 0, 0, 0}, {0, 0, 0, 6}, {0, 6, 4, 6}, {0, 3, 3, 3}}),
            Map.entry('F', new double[][]{{0, 0, 0, 6}, {0, 0, 4, 0}, {0, 3, 3, 3}}),
            Map.entry('H', new double[][]{{0, 0, 0, 6}, {4, 0, 4, 6}, {0, 3, 4, 3}}),
            Map.entry('K', new double[][]{{0, 0, 0, 6}, {4, 0, 0, 3}, {0, 3, 4, 6}}),
            Map.entry('M', new double[][]{{0, 6, 0, 0}, {0, 0, 2, 3}, {2, 3, 4, 0}, {4, 0, 4, 6}}),
            Map.entry('N', new double[][]{{0, 6, 0, 0}, {0, 0, 4, 6}, {4, 6, 4, 0}}),
            Map.entry('P', new double[][]{{0, 6, 0, 0}, {0, 0, 4, 0}, {4, 0, 4, 3}, {4, 3, 0, 3}}),
            Map.entry('R', new double[][]{{0, 6, 0, 0}, {0, 0, 4, 0}, {4, 0, 4, 3}, {4, 3, 0, 3}, {1.5, 3, 4, 6}}),
            Map.entry('T', new double[][]{{0, 0, 4, 0}, {2, 0, 2, 6}}),
            Map.entry('U', new double[][]{{0, 0, 0, 6}, {0, 6, 4, 6}, {4, 6, 4, 0}}),
            Map.entry('V', new double[][]{{0, 0, 2, 6}, {2, 6, 4, 0}}),
            Map.entry('W', new double[][]{{0, 0, 1, 6}, {1, 6, 2, 3}, {2, 3, 3, 6}, {3, 6, 4, 0}}),
            Map.entry('X', new double[][]{{0, 0, 4, 6}, {4, 0, 0, 6}}),
            Map.entry('Y', new double[][]{{0, 0, 2, 3}, {4, 0, 2, 3}, {2, 3, 2, 6}})
    );

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String answer;
    private final String dataUri;

    private CaptchaImage(String answer, String dataUri) {
        this.answer = answer;
        this.dataUri = dataUri;
    }

    public static CaptchaImage generate() {
        StringBuilder answer = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            answer.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return new CaptchaImage(answer.toString(), toDataUri(render(answer.toString())));
    }

    public String answer() {
        return answer;
    }

    /** PNG as a data: URI, ready for an {@code <img src>}. */
    public String dataUri() {
        return dataUri;
    }

    /** Case-insensitive, whitespace-tolerant comparison. */
    public static boolean matches(String expected, String given) {
        if (expected == null || given == null) return false;
        String normalized = given.replaceAll("\\s+", "").toUpperCase();
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII), normalized.getBytes(StandardCharsets.US_ASCII));
    }

    static BufferedImage render(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(240, 243, 248));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            drawNoise(g, 6, 1.2f);

            double cell = (WIDTH - 20) / (double) text.length();
            for (int i = 0; i < text.length(); i++) {
                drawGlyph(g, text.charAt(i), 10 + i * cell + cell / 2, HEIGHT / 2.0);
            }
            drawNoise(g, 4, 1.6f);
            for (int i = 0; i < 260; i++) {
                g.setColor(randomColor(90, 200));
                g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 1, 1);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void drawGlyph(Graphics2D g, char c, double centerX, double centerY) {
        double scale = 6.5 + RANDOM.nextDouble() * 1.8;
        AffineTransform saved = g.getTransform();
        g.translate(centerX + RANDOM.nextGaussian() * 2, centerY + RANDOM.nextGaussian() * 3);
        g.rotate((RANDOM.nextDouble() - 0.5) * 0.7);
        g.shear((RANDOM.nextDouble() - 0.5) * 0.4, 0);
        g.setStroke(new BasicStroke(2.6f + RANDOM.nextFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(randomColor(10, 90));
        for (double[] s : GLYPHS.get(c)) {
            g.draw(new Line2D.Double((s[0] - 2) * scale, (s[1] - 3) * scale, (s[2] - 2) * scale, (s[3] - 3) * scale));
        }
        g.setTransform(saved);
    }

    private static void drawNoise(Graphics2D g, int lines, float width) {
        g.setStroke(new BasicStroke(width));
        for (int i = 0; i < lines; i++) {
            g.setColor(randomColor(100, 190));
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }
    }

    private static Color randomColor(int min, int max) {
        int span = max - min;
        return new Color(min + RANDOM.nextInt(span), min + RANDOM.nextInt(span), min + RANDOM.nextInt(span));
    }

    private static String toDataUri(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
