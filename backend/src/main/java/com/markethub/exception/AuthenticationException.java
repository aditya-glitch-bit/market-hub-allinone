package com.markethub.exception;

/** The caller is not signed in, or the credentials were wrong (HTTP 401). */
public class AuthenticationException extends MarketHubException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message, 401);
    }
}
