package com.markethub.dao.jdbc;

import com.markethub.dao.GenericDao;
import com.markethub.db.Database;
import com.markethub.exception.ConflictException;
import com.markethub.exception.DataAccessException;
import com.markethub.exception.MarketHubException;
import com.markethub.model.Identifiable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC plumbing (template-method pattern).
 *
 * <p>This class implements the generic operations once. A concrete DAO only supplies its SQL
 * ({@link #baseSelect()}, {@link #tableName()}, ...) and the row-to-object mapping
 * ({@link #mapRow}). All statements are {@link PreparedStatement}s with bound parameters, which
 * prevents SQL injection, and connections are always returned to the pool.</p>
 *
 * @param <T> entity type handled by the subclass
 */
abstract class AbstractJdbcDao<T extends Identifiable<Long>> implements GenericDao<T, Long> {

    protected final Database db;

    protected AbstractJdbcDao(Database db) {
        this.db = db;
    }

    /** SELECT with all columns and joins but no WHERE/ORDER BY clause. */
    protected abstract String baseSelect();

    /** Qualified id column used in WHERE clauses, e.g. {@code p.id}. */
    protected abstract String idColumn();

    /** Real table name used for DELETE and COUNT. */
    protected abstract String tableName();

    /** Converts the current row of the result set into an entity. */
    protected abstract T mapRow(ResultSet row) throws SQLException;

    /** Hook for subclasses that must load child rows after the main query (default: nothing). */
    protected void afterLoad(Connection connection, List<T> rows) throws SQLException {
        // no children by default
    }

    // ----------------------------------------------------------------- generic operations

    @Override
    public Optional<T> findById(Long id) throws MarketHubException {
        List<T> rows = query(baseSelect() + " WHERE " + idColumn() + " = ?", ps -> ps.setLong(1, id));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<T> findAll() throws MarketHubException {
        return query(baseSelect() + " ORDER BY " + idColumn(), Binder.NONE);
    }

    @Override
    public boolean deleteById(Long id) throws MarketHubException {
        return update("DELETE FROM " + tableName() + " WHERE id = ?", ps -> ps.setLong(1, id)) > 0;
    }

    @Override
    public long count() throws MarketHubException {
        return execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName());
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        });
    }

    // ----------------------------------------------------------------- helpers for subclasses

    /** Runs a SELECT and maps every row. */
    protected List<T> query(String sql, Binder binder) throws MarketHubException {
        return execute(connection -> {
            List<T> rows = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(mapRow(rs));
                    }
                }
            }
            afterLoad(connection, rows);
            return rows;
        });
    }

    /** Runs an INSERT/UPDATE/DELETE and returns the number of rows changed. */
    protected int update(String sql, Binder binder) throws MarketHubException {
        return execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                binder.bind(ps);
                return ps.executeUpdate();
            }
        });
    }

    /**
     * Borrows a connection, runs the work and always releases the connection.
     *
     * <p>{@link SQLException}s are translated: SQLSTATE class 23 (integrity constraint, e.g. duplicate
     * e-mail or a foreign key still in use) becomes a {@link ConflictException}; everything else
     * becomes a {@link DataAccessException}.</p>
     */
    protected <R> R execute(SqlFunction<Connection, R> work) throws MarketHubException {
        Connection connection = db.acquire();
        try {
            return work.apply(connection);
        } catch (SQLException e) {
            String state = e.getSQLState();
            if (state != null && state.startsWith("23")) {
                throw new ConflictException("The change conflicts with existing data", e);
            }
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        } finally {
            db.release(connection);
        }
    }
}
