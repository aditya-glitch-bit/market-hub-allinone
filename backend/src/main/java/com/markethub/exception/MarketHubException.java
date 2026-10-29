package com.markethub.exception;

/**
 * Root of the application's <em>checked</em> exception hierarchy.
 *
 * <p>Every business or data failure extends this class and carries the HTTP status the servlet
 * layer should answer with. The servlet base class catches {@code MarketHubException} in one
 * place, so individual servlets never build error responses by hand.</p>
 */
public class MarketHubException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;

    public MarketHubException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public MarketHubException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /** HTTP status code that best describes this failure. */
    public int getHttpStatus() {
        return httpStatus;
    }
}
