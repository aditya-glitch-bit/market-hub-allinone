package com.markethub.util;

import com.markethub.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Input checks shared by the service layer. Every failure is a {@link ValidationException}. */
public final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validators() { }

    /** Trims and checks length; returns the cleaned text. */
    public static String requireText(String field, String value, int min, int max) throws ValidationException {
        String text = value == null ? "" : value.trim();
        if (text.length() < min || text.length() > max) {
            throw new ValidationException(field + " must be between " + min + " and " + max + " characters");
        }
        return text;
    }

    /** Returns the lower-cased e-mail so "Olivia@Example.com" and "olivia@example.com" match. */
    public static String requireEmail(String value) throws ValidationException {
        String email = value == null ? "" : value.trim().toLowerCase();
        if (email.length() > 150 || !EMAIL.matcher(email).matches()) {
            throw new ValidationException("Enter a valid e-mail address");
        }
        return email;
    }

    /** At least 8 characters with at least one letter and one digit. */
    public static void requirePassword(String value) throws ValidationException {
        if (value == null || value.length() < 8 || value.length() > 100
                || !value.matches(".*[A-Za-z].*") || !value.matches(".*\\d.*")) {
            throw new ValidationException("Password must be 8+ characters and include a letter and a number");
        }
    }

    /** Rounds to 2 decimals and requires a value above zero. */
    public static BigDecimal requirePrice(String field, BigDecimal value) throws ValidationException {
        if (value == null || value.signum() <= 0 || value.compareTo(new BigDecimal("1000000")) > 0) {
            throw new ValidationException(field + " must be greater than 0");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Optional money value: null becomes zero, negatives are rejected. */
    public static BigDecimal optionalPrice(String field, BigDecimal value) throws ValidationException {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (value.signum() < 0) {
            throw new ValidationException(field + " cannot be negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static int requireRange(String field, int value, int min, int max) throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException(field + " must be between " + min + " and " + max);
        }
        return value;
    }
}
