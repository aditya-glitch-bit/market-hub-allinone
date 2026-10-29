package com.markethub.exception;

/** The caller is signed in but not allowed to do this (HTTP 403). */
public class ForbiddenException extends MarketHubException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message, 403);
    }
}
