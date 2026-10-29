package com.markethub.dao;

import com.markethub.exception.MarketHubException;
import com.markethub.model.Product;

import java.util.List;

/** Database operations for products. */
public interface ProductDao extends GenericDao<Product, Long> {

    /**
     * Filters the catalogue.
     *
     * @param text     case-insensitive match on name, category or seller; {@code null}/blank = any
     * @param category exact category; {@code null}/blank = any
     */
    List<Product> search(String text, String category) throws MarketHubException;

    List<Product> findBySeller(long sellerId) throws MarketHubException;

    /** Products with stock above zero but at or below {@code threshold}. */
    List<Product> findLowStock(int threshold) throws MarketHubException;

    /**
     * Atomically removes units, but only if enough remain.
     *
     * @return {@code true} if the stock was reduced; {@code false} if there was not enough
     */
    boolean tryDecrementStock(long productId, int quantity) throws MarketHubException;

    /** Atomically adds units back (cancelled order, failed checkout, restock). */
    void increaseStock(long productId, int quantity) throws MarketHubException;
}
