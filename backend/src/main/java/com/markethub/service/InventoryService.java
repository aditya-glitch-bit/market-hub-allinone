package com.markethub.service;

import com.markethub.dao.ProductDao;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.exception.OutOfStockException;
import com.markethub.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Reserves and releases stock safely when many buyers check out at the same time.
 *
 * <h3>Two layers of protection</h3>
 * <ol>
 *   <li><b>In the JVM</b> – one {@link ReentrantLock} per product, so threads buying the
 *       <em>same</em> product take turns while threads buying different products run in parallel.</li>
 *   <li><b>In the database</b> – {@link ProductDao#tryDecrementStock} is a single conditional
 *       {@code UPDATE ... WHERE stock >= ?}, so even a second server instance cannot oversell.</li>
 * </ol>
 *
 * <h3>Deadlock avoidance</h3>
 * An order may contain several products. If thread A locked product 1 then 2 while thread B
 * locked 2 then 1, both would wait forever. Locks are therefore always taken in ascending
 * product-id order, which makes a circular wait impossible.
 */
public class InventoryService {

    private final ProductDao products;
    private final ConcurrentMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public InventoryService(ProductDao products) {
        this.products = products;
    }

    /**
     * Takes stock for every line, or for none of them.
     *
     * @param quantities product id to units wanted
     * @throws OutOfStockException if any product has too few units left; stock already taken for
     *                             earlier lines is given back before the exception propagates
     */
    public void reserve(Map<Long, Integer> quantities) throws MarketHubException {
        List<Long> ordered = new ArrayList<>(new TreeSet<>(quantities.keySet()));
        List<ReentrantLock> held = new ArrayList<>();
        List<Long> taken = new ArrayList<>();
        try {
            for (Long productId : ordered) {
                ReentrantLock lock = locks.computeIfAbsent(productId, id -> new ReentrantLock());
                lock.lock();
                held.add(lock);
            }
            for (Long productId : ordered) {
                int wanted = quantities.get(productId);
                if (!products.tryDecrementStock(productId, wanted)) {
                    Product product = products.findById(productId)
                            .orElseThrow(() -> new NotFoundException("Product", productId));
                    throw new OutOfStockException(productId, product.getName());
                }
                taken.add(productId);
            }
        } catch (MarketHubException | RuntimeException e) {
            for (Long productId : taken) {
                products.increaseStock(productId, quantities.get(productId)); // undo partial reservation
            }
            throw e;
        } finally {
            for (int i = held.size() - 1; i >= 0; i--) {
                held.get(i).unlock();
            }
        }
    }

    /** Puts units back (cancelled order or failed checkout). */
    public void release(Map<Long, Integer> quantities) throws MarketHubException {
        for (Map.Entry<Long, Integer> line : new java.util.TreeMap<>(quantities).entrySet()) {
            ReentrantLock lock = locks.computeIfAbsent(line.getKey(), id -> new ReentrantLock());
            lock.lock();
            try {
                products.increaseStock(line.getKey(), line.getValue());
            } finally {
                lock.unlock();
            }
        }
    }
}
