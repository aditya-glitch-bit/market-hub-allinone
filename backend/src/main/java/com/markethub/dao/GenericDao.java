package com.markethub.dao;

import com.markethub.exception.MarketHubException;
import com.markethub.model.Identifiable;

import java.util.List;
import java.util.Optional;

/**
 * CRUD operations common to every table (generic interface).
 *
 * @param <T>  entity type
 * @param <ID> primary-key type
 */
public interface GenericDao<T extends Identifiable<ID>, ID> {

    Optional<T> findById(ID id) throws MarketHubException;

    List<T> findAll() throws MarketHubException;

    /** Inserts when {@code entity.getId()} is null, otherwise updates. Returns the saved entity with its id. */
    T save(T entity) throws MarketHubException;

    /** @return {@code true} if a row was deleted */
    boolean deleteById(ID id) throws MarketHubException;

    long count() throws MarketHubException;
}
