package com.mycompany.logistic.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            byte[] derived = derive(password, salt, ITERATIONS);
            return "pbkdf2$" + ITERATIONS + "$" + HexFormat.of().formatHex(salt) + "$" + HexFormat.of().formatHex(derived);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash password", ex);
        }
    }

    public static boolean matches(String password, String hash) {
        try {
            String[] parts = hash.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 1_000_000) {
                return false;
            }
            byte[] expected = HexFormat.of().parseHex(parts[3]);
            byte[] actual = derive(password, HexFormat.of().parseHex(parts[2]), iterations);
            return MessageDigest.isEqual(actual, expected);
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
