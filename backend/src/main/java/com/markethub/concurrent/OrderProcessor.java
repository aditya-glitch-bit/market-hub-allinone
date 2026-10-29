package com.markethub.concurrent;

import com.markethub.dao.OrderDao;
import com.markethub.exception.MarketHubException;
import com.markethub.model.OrderStatus;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background fulfilment: moves new orders from PROCESSING to SHIPPED after a delay.
 *
 * <h3>Producer / consumer</h3>
 * Request threads (<em>producers</em>) call {@link #submit(long)} right after an order is saved.
 * A small pool of worker threads (<em>consumers</em>) wait on a {@link DelayQueue}, which only
 * releases a task once its delay has passed, so no thread has to sleep per order.
 *
 * <h3>Synchronisation</h3>
 * The queue is thread-safe. The status change itself uses
 * {@link OrderDao#updateStatus(long, OrderStatus, OrderStatus)}, a compare-and-set, so if a
 * seller or buyer changes the order first, the worker's update simply does nothing.
 */
public class OrderProcessor implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(OrderProcessor.class.getName());

    /** An order waiting to be shipped at a given moment. */
    private static final class ShipTask implements Delayed {
        private final long orderId;
        private final long dueAtNanos;

        ShipTask(long orderId, long delayMillis) {
            this.orderId = orderId;
            this.dueAtNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMillis);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(dueAtNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(dueAtNanos, ((ShipTask) other).dueAtNanos);
        }
    }

    private final OrderDao orders;
    private final ActivityLog activity;
    private final long delayMillis;
    private final DelayQueue<ShipTask> queue = new DelayQueue<>();
    private final ExecutorService workers;
    /** {@code volatile}: written by {@link #close()}, read by every worker thread. */
    private volatile boolean running = true;

    /**
     * @param delayMillis time before an order ships; a negative value disables automatic shipping
     * @param threads     number of worker threads
     */
    public OrderProcessor(OrderDao orders, ActivityLog activity, long delayMillis, int threads) {
        this.orders = orders;
        this.activity = activity;
        this.delayMillis = delayMillis;
        if (delayMillis < 0) {
            this.workers = null;
            return;
        }
        this.workers = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "order-processor");
            thread.setDaemon(true);
            return thread;
        });
        for (int i = 0; i < threads; i++) {
            workers.execute(this::workLoop);
        }
    }

    /** Queues an order for automatic shipping. Safe to call from any thread. */
    public void submit(long orderId) {
        if (delayMillis >= 0) {
            queue.offer(new ShipTask(orderId, delayMillis));
        }
    }

    /** @return number of orders still waiting to ship */
    public int pending() {
        return queue.size();
    }

    private void workLoop() {
        while (running) {
            try {
                ShipTask task = queue.poll(250, TimeUnit.MILLISECONDS);
                if (task != null) {
                    ship(task.orderId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Order worker failed; continuing", e);
            }
        }
    }

    private void ship(long orderId) {
        try {
            if (orders.updateStatus(orderId, OrderStatus.PROCESSING, OrderStatus.SHIPPED)) {
                activity.record("system", "Order #" + orderId + " shipped automatically");
            }
        } catch (MarketHubException e) {
            LOG.log(Level.WARNING, "Could not ship order " + orderId, e);
        }
    }

    @Override
    public void close() {
        running = false;
        if (workers != null) {
            workers.shutdownNow();
            try {
                workers.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
