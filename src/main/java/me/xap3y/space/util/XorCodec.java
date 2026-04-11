package me.xap3y.space.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class XorCodec {

    private static byte[] xor(byte[] data, byte[] key) {
        if (key.length == 0) throw new IllegalArgumentException("empty_key");
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return out;
    }

    public static String encodeToBase64Url(String plainText, String secret) {
        byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        byte[] x = xor(data, key);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(x);
    }

    public static String decodeFromBase64Url(String token, String secret) {
        byte[] x = Base64.getUrlDecoder().decode(token);
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        byte[] data = xor(x, key);
        return new String(data, StandardCharsets.UTF_8);
    }

    private XorCodec() {}
}