package com.markethub.exception;

/** The requested record does not exist (HTTP 404). */
public class NotFoundException extends MarketHubException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String entity, Object id) {
        super(entity + " " + id + " was not found", 404);
    }

    public NotFoundException(String message) {
        super(message, 404);
    }
}
