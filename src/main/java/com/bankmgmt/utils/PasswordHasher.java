package com.bankmgmt.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * SHA-256 with per-user salt (resume-oriented “hash simulation”; upgrade path: PBKDF2/Argon2).
 */
public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String generateSaltHex(int bytes) {
        byte[] salt = new byte[bytes];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    public static String hash(String plain, String saltHex) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((saltHex + plain).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String plain, String saltHex, String storedHash) {
        return hash(plain, saltHex).equalsIgnoreCase(storedHash);
    }

    private static String bytesToHex(byte[] raw) {
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
