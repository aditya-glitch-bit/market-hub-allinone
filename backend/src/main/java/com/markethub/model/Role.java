package com.markethub.model;

import com.markethub.exception.ValidationException;

import java.util.Locale;

/** The three kinds of account in MarketHub. */
public enum Role {
    ADMIN, SELLER, BUYER;

    /** Case-insensitive lookup that reports bad input as a {@link ValidationException}. */
    public static Role parse(String text) throws ValidationException {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Role is required");
        }
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown role: " + text);
        }
    }
}
