package com.markethub.db;

import com.markethub.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Minimal blocking JDBC connection pool.
 *
 * <p>Opening a database connection is slow, and servlet threads run in parallel, so connections
 * are reused. A {@link Semaphore} caps how many may be checked out at once: when all are busy
 * a caller waits (up to a timeout) instead of opening an unbounded number of connections.
 * Idle connections wait in a {@link BlockingQueue}.</p>
 */
public class ConnectionPool implements AutoCloseable {

    private static final long BORROW_TIMEOUT_SECONDS = 10;

    private final String url;
    private final String user;
    private final String password;
    private final Semaphore permits;
    private final BlockingQueue<Connection> idle = new LinkedBlockingQueue<>();
    private volatile boolean closed;

    public ConnectionPool(String url, String user, String password, int maxConnections) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.permits = new Semaphore(maxConnections);
    }

    /** Checks out a connection; the caller must hand it back with {@link #giveBack(Connection)}. */
    public Connection borrow() throws DataAccessException {
        if (closed) {
            throw new DataAccessException("Connection pool is closed", null);
        }
        try {
            if (!permits.tryAcquire(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new DataAccessException("Timed out waiting for a database connection", null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataAccessException("Interrupted while waiting for a database connection", e);
        }
        try {
            Connection connection = idle.poll();
            if (connection != null && !isUsable(connection)) {
                closeQuietly(connection);
                connection = null;
            }
            return connection != null ? connection : DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            permits.release();
            throw new DataAccessException("Could not open a database connection", e);
        }
    }

    /** Returns a connection to the pool after clearing any half-finished transaction. */
    public void giveBack(Connection connection) {
        try {
            if (!closed && !connection.isClosed()) {
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
                idle.offer(connection);
            } else {
                closeQuietly(connection);
            }
        } catch (SQLException e) {
            closeQuietly(connection);
        } finally {
            permits.release();
        }
    }

    @Override
    public void close() {
        closed = true;
        Connection connection;
        while ((connection = idle.poll()) != null) {
            closeQuietly(connection);
        }
    }

    private static boolean isUsable(Connection connection) {
        try {
            return !connection.isClosed() && connection.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // nothing useful to do
        }
    }
}
