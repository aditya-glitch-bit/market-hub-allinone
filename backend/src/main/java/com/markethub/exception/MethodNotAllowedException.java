package com.markethub.exception;

/** The URL exists but does not support the HTTP method used (HTTP 405). */
public class MethodNotAllowedException extends MarketHubException {

    private static final long serialVersionUID = 1L;

    public MethodNotAllowedException(String method) {
        super("Method " + method + " is not supported here", 405);
    }
}
