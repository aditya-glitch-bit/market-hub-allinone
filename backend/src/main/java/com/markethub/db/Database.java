package com.markethub.db;

import com.markethub.config.AppConfig;
import com.markethub.dao.TransactionManager;
import com.markethub.dao.TransactionWork;
import com.markethub.exception.DataAccessException;
import com.markethub.exception.MarketHubException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC entry point: owns the connection pool, creates the schema and runs transactions.
 *
 * <h3>How connections reach the DAOs</h3>
 * DAOs call {@link #acquire()} / {@link #release(Connection)}. Normally that borrows a fresh
 * connection from the pool for one statement. Inside {@link #inTransaction}, the current
 * <em>thread</em> is bound to a single connection (a {@link ThreadLocal}), so every DAO call in
 * the block shares it and commits or rolls back together.
 */
public class Database implements TransactionManager, AutoCloseable {

    private final ConnectionPool pool;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    public Database(AppConfig config) {
        this.pool = new ConnectionPool(config.getDbUrl(), config.getDbUser(), config.getDbPassword(), 8);
    }

    /** Creates all tables if they do not exist yet (script: {@code /schema.sql}). */
    public void initSchema() throws DataAccessException {
        // Drop "-- comment" lines first so a semicolon inside a comment cannot split a statement.
        StringBuilder cleaned = new StringBuilder();
        for (String line : readResource("/schema.sql").split("\\R")) {
            if (!line.trim().startsWith("--")) {
                cleaned.append(line).append('\n');
            }
        }
        String script = cleaned.toString();
        Connection connection = acquire();
        try (Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql.trim());
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the database schema", e);
        } finally {
            release(connection);
        }
    }

    /** @return the transaction's connection if one is active on this thread, else a pooled one */
    public Connection acquire() throws DataAccessException {
        Connection bound = transactionConnection.get();
        return bound != null ? bound : pool.borrow();
    }

    /** Hands a connection back; does nothing while a transaction still owns it. */
    public void release(Connection connection) {
        if (connection != transactionConnection.get()) {
            pool.giveBack(connection);
        }
    }

    @Override
    public <T> T inTransaction(TransactionWork<T> work) throws MarketHubException {
        if (transactionConnection.get() != null) {
            return work.run(); // join the surrounding transaction
        }
        Connection connection = pool.borrow();
        transactionConnection.set(connection);
        try {
            connection.setAutoCommit(false);
            T result = work.run();
            connection.commit();
            return result;
        } catch (MarketHubException | RuntimeException e) {
            rollbackQuietly(connection);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new DataAccessException("Transaction failed", e);
        } finally {
            transactionConnection.remove();
            pool.giveBack(connection);
        }
    }

    @Override
    public void close() {
        pool.close();
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // the pool will discard the connection if it is broken
        }
    }

    private static String readResource(String path) throws DataAccessException {
        try (InputStream in = Database.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new DataAccessException("Missing resource " + path, null);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + path, e);
        }
    }
}
