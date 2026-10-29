package com.markethub.dao;

import com.markethub.exception.MarketHubException;

/**
 * Runs several DAO calls as one all-or-nothing unit.
 *
 * <p>Services depend on this interface instead of on JDBC, so unit tests can supply a
 * pass-through implementation with in-memory DAOs.</p>
 */
public interface TransactionManager {

    /**
     * Executes {@code work}; commits if it returns normally and rolls back if it throws.
     * Calling this from inside another transaction joins the outer one.
     */
    <T> T inTransaction(TransactionWork<T> work) throws MarketHubException;
}
