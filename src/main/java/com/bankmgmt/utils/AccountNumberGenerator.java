package com.bankmgmt.utils;

import java.security.SecureRandom;

/** Generates unique-looking domestic-style account numbers for demo UX. */
public final class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "BK";

    private AccountNumberGenerator() {
    }

    /** Produces strings like BK + 12 digits (caller ensures uniqueness via DAO). */
    public static String generateCandidate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < 12; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
