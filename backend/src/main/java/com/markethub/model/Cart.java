package com.markethub.model;

import com.markethub.exception.ValidationException;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A shopper's bag, kept in the HTTP session.
 *
 * <p>Several requests from the same browser may arrive at once on different container threads,
 * so every method is {@code synchronized}. Insertion order is preserved so the bag lists items
 * in the order they were added.</p>
 */
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Upper bound per line, a sanity limit rather than a business rule. */
    public static final int MAX_QUANTITY = 99;

    private final Map<Long, Integer> lines = new LinkedHashMap<>();

    /** Adds {@code quantity} units, creating the line if needed. */
    public synchronized void add(long productId, int quantity) throws ValidationException {
        checkQuantity(quantity);
        int updated = lines.getOrDefault(productId, 0) + quantity;
        if (updated > MAX_QUANTITY) {
            throw new ValidationException("A bag line can hold at most " + MAX_QUANTITY + " units");
        }
        lines.put(productId, updated);
    }

    /** Sets an exact quantity; zero removes the line. */
    public synchronized void set(long productId, int quantity) throws ValidationException {
        if (quantity == 0) {
            lines.remove(productId);
            return;
        }
        checkQuantity(quantity);
        lines.put(productId, quantity);
    }

    public synchronized void remove(long productId) {
        lines.remove(productId);
    }

    public synchronized void clear() {
        lines.clear();
    }

    public synchronized boolean isEmpty() {
        return lines.isEmpty();
    }

    /** @return an independent copy (productId to quantity) that is safe to iterate */
    public synchronized Map<Long, Integer> snapshot() {
        return new LinkedHashMap<>(lines);
    }

    private static void checkQuantity(int quantity) throws ValidationException {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new ValidationException("Quantity must be between 1 and " + MAX_QUANTITY);
        }
    }
}
