package com.library.util;

import java.nio.charset.StandardCharsets;

/** Byte fixtures that carry real file signatures, so uploads pass magic-byte validation. */
public final class TestFiles {

    private TestFiles() {
    }

    public static byte[] jpeg(String body) {
        return withPrefix(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, body);
    }

    public static byte[] png(String body) {
        return withPrefix(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, body);
    }

    public static byte[] pdf(String body) {
        return withPrefix("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII), body);
    }

    private static byte[] withPrefix(byte[] prefix, String body) {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[prefix.length + content.length];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        System.arraycopy(content, 0, out, prefix.length, content.length);
        return out;
    }
}
