package com.library.util;

import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Validates an upload by its actual leading bytes ("magic number") instead of trusting the
 * client-supplied Content-Type, so e.g. an HTML/SVG file renamed to .png cannot be stored and
 * later served back as active content.
 */
@UtilityClass
public class FileSignatures {

    public static boolean isImage(MultipartFile file) {
        byte[] h = head(file, 12);
        return startsWith(h, 0xFF, 0xD8, 0xFF)                                   // JPEG
                || startsWith(h, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)   // PNG
                || startsWith(h, 'G', 'I', 'F', '8')                             // GIF
                || (startsWith(h, 'R', 'I', 'F', 'F') && h.length >= 12
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P'); // WEBP
    }

    public static boolean isPdf(MultipartFile file) {
        return startsWith(head(file, 5), '%', 'P', 'D', 'F', '-');
    }

    private static byte[] head(MultipartFile file, int n) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(n);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != (prefix[i] & 0xFF)) return false;
        }
        return true;
    }
}
