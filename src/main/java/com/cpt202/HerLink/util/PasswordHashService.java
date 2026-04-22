package com.cpt202.HerLink.util;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

@Component
public class PasswordHashService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] digest = pbkdf2(rawPassword, salt, ITERATIONS, KEY_LENGTH);
        return ITERATIONS + ":" + HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(digest);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }

        byte[] expected;
        byte[] actual;

        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = HexFormat.of().parseHex(parts[1]);
            expected = HexFormat.of().parseHex(parts[2]);
            actual = pbkdf2(rawPassword, salt, iterations, expected.length * 8);
        } catch (RuntimeException exception) {
            return false;
        }

        if (expected.length != actual.length) {
            return false;
        }

        int diff = 0;
        for (int i = 0; i < expected.length; i++) {
            diff |= expected[i] ^ actual[i];
        }
        return diff == 0;
    }

    private byte[] pbkdf2(String rawPassword, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to hash password.", exception);
        }
    }
}
