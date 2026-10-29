package com.markethub.exception;

/** The caller sent data that failed validation (HTTP 400). */
public class ValidationException extends MarketHubException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message, 400);
    }
}
