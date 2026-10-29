package com.markethub.dao;

import com.markethub.exception.MarketHubException;

/**
 * A block of work to run inside one database transaction.
 *
 * @param <T> result type
 */
@FunctionalInterface
public interface TransactionWork<T> {

    T run() throws MarketHubException;
}
