package com.markethub.concurrent;

import com.markethub.dao.ProductDao;
import com.markethub.exception.MarketHubException;
import com.markethub.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically scans for products that are nearly sold out and keeps the result for the
 * seller dashboard ("low-stock alerts").
 *
 * <p>One scheduler thread writes the snapshot; many request threads read it. The snapshot is an
 * immutable list published through a {@code volatile} field, so readers always see a complete
 * list and no locking is needed.</p>
 */
public class LowStockMonitor implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(LowStockMonitor.class.getName());

    private final ProductDao products;
    private final int threshold;
    private final ScheduledExecutorService scheduler;
    private volatile List<Product> alerts = Collections.emptyList();

    public LowStockMonitor(ProductDao products, int threshold) {
        this.products = products;
        this.threshold = threshold;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "low-stock-monitor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Starts scanning now and then every {@code periodSeconds}. */
    public void start(long periodSeconds) {
        scheduler.scheduleWithFixedDelay(this::scanQuietly, 0, periodSeconds, TimeUnit.SECONDS);
    }

    /** Runs one scan immediately on the calling thread. */
    public void scanNow() throws MarketHubException {
        alerts = Collections.unmodifiableList(new ArrayList<>(products.findLowStock(threshold)));
    }

    /** @return the latest snapshot (never {@code null}, never modified after publication) */
    public List<Product> getAlerts() {
        return alerts;
    }

    /** @return the snapshot filtered to one seller's products */
    public List<Product> alertsForSeller(long sellerId) {
        List<Product> mine = new ArrayList<>();
        for (Product product : alerts) {
            if (product.getSellerId() == sellerId) {
                mine.add(product);
            }
        }
        return mine;
    }

    public int getThreshold() {
        return threshold;
    }

    private void scanQuietly() {
        try {
            scanNow();
        } catch (MarketHubException | RuntimeException e) {
            // An exception escaping a scheduled task would cancel all future runs.
            LOG.log(Level.WARNING, "Low-stock scan failed", e);
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
