package com.markethub.dao.jdbc;

import com.markethub.dao.UserDao;
import com.markethub.db.Database;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.model.UserFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** JDBC implementation of {@link UserDao}. */
public class JdbcUserDao extends AbstractJdbcDao<User> implements UserDao {

    public JdbcUserDao(Database db) {
        super(db);
    }

    @Override
    protected String baseSelect() {
        return "SELECT u.id, u.name, u.email, u.password_hash, u.role, u.active, u.created_at FROM users u";
    }

    @Override
    protected String idColumn() { return "u.id"; }

    @Override
    protected String tableName() { return "users"; }

    /** Builds the right subclass (Admin / Seller / Buyer) for the stored role. */
    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        return UserFactory.create(
                Role.valueOf(rs.getString("role")),
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    /**
     * Inserts a new account. For an existing account only the {@code active} flag is updated:
     * name, e-mail, role and password are fixed once created.
     */
    @Override
    public User save(User user) throws MarketHubException {
        if (user.getId() == null) {
            long id = execute(connection -> {
                String sql = "INSERT INTO users (name, email, password_hash, role, active) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, user.getName());
                    ps.setString(2, user.getEmail());
                    ps.setString(3, user.getPasswordHash());
                    ps.setString(4, user.getRole().name());
                    ps.setBoolean(5, user.isActive());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        return keys.getLong(1);
                    }
                }
            });
            return findById(id).orElseThrow(() -> new IllegalStateException("Inserted user vanished"));
        }
        int changed = update("UPDATE users SET active = ? WHERE id = ?", ps -> {
            ps.setBoolean(1, user.isActive());
            ps.setLong(2, user.getId());
        });
        if (changed == 0) {
            throw new NotFoundException("User", user.getId());
        }
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) throws MarketHubException {
        List<User> rows = query(baseSelect() + " WHERE LOWER(u.email) = ?",
                ps -> ps.setString(1, email.trim().toLowerCase(Locale.ROOT)));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<User> findByRole(Role role) throws MarketHubException {
        return query(baseSelect() + " WHERE u.role = ? ORDER BY u.id", ps -> ps.setString(1, role.name()));
    }
}
