package com.markethub.exception;

/** The request clashes with the current state, e.g. duplicate e-mail or illegal status change (HTTP 409). */
public class ConflictException extends MarketHubException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message, 409);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, 409, cause);
    }
}
