package com.bankmgmt.utils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Central validation helpers for Swing forms. */
public final class FormValidator {

    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private FormValidator() {
    }

    public static boolean isUsername(String s) {
        return s != null && USERNAME.matcher(s.trim()).matches();
    }

    public static boolean isEmail(String s) {
        return s != null && EMAIL.matcher(s.trim()).matches();
    }

    public static boolean isNonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static boolean isStrongEnoughPassword(String p) {
        if (p == null || p.length() < 8) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char c : p.toCharArray()) {
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                special = true;
            }
        }
        return upper && lower && digit && special;
    }

    public static BigDecimal parsePositiveMoney(String s) throws IllegalArgumentException {
        if (!isNonBlank(s)) {
            throw new IllegalArgumentException("Amount required");
        }
        BigDecimal v = new BigDecimal(s.trim());
        if (v.compareTo(BigDecimal.ZERO) <= 0 || v.scale() > 2) {
            throw new IllegalArgumentException("Amount must be positive with max 2 decimal places");
        }
        return v;
    }
}
