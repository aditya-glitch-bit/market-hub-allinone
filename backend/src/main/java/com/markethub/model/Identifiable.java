package com.markethub.model;

/**
 * Anything stored in the database with a primary key.
 *
 * @param <ID> type of the primary key
 */
public interface Identifiable<ID> {

    /** @return the primary key, or {@code null} if the object has not been saved yet */
    ID getId();
}
