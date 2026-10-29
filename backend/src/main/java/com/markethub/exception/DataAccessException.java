package com.markethub.exception;

/**
 * A database call failed (HTTP 500). Wraps {@link java.sql.SQLException} so the lower-level
 * JDBC type never leaks into service or servlet code.
 */
public class DataAccessException extends MarketHubException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, 500, cause);
    }
}
