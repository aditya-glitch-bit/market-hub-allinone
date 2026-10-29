package com.markethub.dao.jdbc;

import com.markethub.dao.ProductDao;
import com.markethub.db.Database;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.model.Product;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** JDBC implementation of {@link ProductDao}. */
public class JdbcProductDao extends AbstractJdbcDao<Product> implements ProductDao {

    public JdbcProductDao(Database db) {
        super(db);
    }

    @Override
    protected String baseSelect() {
        return "SELECT p.id, p.seller_id, u.name AS seller_name, p.name, p.category, p.price, p.old_price, "
                + "p.rating, p.reviews, p.image, p.badge, p.stock, p.created_at "
                + "FROM products p JOIN users u ON u.id = p.seller_id";
    }

    @Override
    protected String idColumn() { return "p.id"; }

    @Override
    protected String tableName() { return "products"; }

    @Override
    protected Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getLong("seller_id"),
                rs.getString("seller_name"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("old_price"),
                rs.getDouble("rating"),
                rs.getInt("reviews"),
                rs.getString("image"),
                rs.getString("badge"),
                rs.getInt("stock"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    @Override
    public Product save(Product product) throws MarketHubException {
        if (product.getId() == null) {
            long id = execute(connection -> {
                String sql = "INSERT INTO products (seller_id, name, category, price, old_price, rating, "
                        + "reviews, image, badge, stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, product.getSellerId());
                    ps.setString(2, product.getName());
                    ps.setString(3, product.getCategory());
                    ps.setBigDecimal(4, product.getPrice());
                    ps.setBigDecimal(5, product.getOldPrice());
                    ps.setDouble(6, product.getRating());
                    ps.setInt(7, product.getReviews());
                    ps.setString(8, product.getImage());
                    ps.setString(9, product.getBadge());
                    ps.setInt(10, product.getStock());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        return keys.getLong(1);
                    }
                }
            });
            return findById(id).orElseThrow(() -> new IllegalStateException("Inserted product vanished"));
        }
        int changed = update("UPDATE products SET name = ?, category = ?, price = ?, old_price = ?, "
                        + "image = ?, badge = ?, stock = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, product.getName());
                    ps.setString(2, product.getCategory());
                    ps.setBigDecimal(3, product.getPrice());
                    ps.setBigDecimal(4, product.getOldPrice());
                    ps.setString(5, product.getImage());
                    ps.setString(6, product.getBadge());
                    ps.setInt(7, product.getStock());
                    ps.setLong(8, product.getId());
                });
        if (changed == 0) {
            throw new NotFoundException("Product", product.getId());
        }
        return product;
    }

    @Override
    public List<Product> search(String text, String category) throws MarketHubException {
        StringBuilder sql = new StringBuilder(baseSelect()).append(" WHERE 1 = 1");
        List<String> parameters = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
            sql.append(" AND (LOWER(p.name) LIKE ? OR LOWER(p.category) LIKE ? OR LOWER(u.name) LIKE ?)");
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND p.category = ?");
            parameters.add(category.trim());
        }
        sql.append(" ORDER BY p.id");
        return query(sql.toString(), ps -> {
            for (int i = 0; i < parameters.size(); i++) {
                ps.setString(i + 1, parameters.get(i));
            }
        });
    }

    @Override
    public List<Product> findBySeller(long sellerId) throws MarketHubException {
        return query(baseSelect() + " WHERE p.seller_id = ? ORDER BY p.id", ps -> ps.setLong(1, sellerId));
    }

    @Override
    public List<Product> findLowStock(int threshold) throws MarketHubException {
        return query(baseSelect() + " WHERE p.stock > 0 AND p.stock <= ? ORDER BY p.stock, p.id",
                ps -> ps.setInt(1, threshold));
    }

    /**
     * One conditional UPDATE does the check and the change together, so two buyers racing for
     * the last unit cannot both succeed: the database serialises the row update.
     */
    @Override
    public boolean tryDecrementStock(long productId, int quantity) throws MarketHubException {
        return update("UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?", ps -> {
            ps.setInt(1, quantity);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
        }) == 1;
    }

    @Override
    public void increaseStock(long productId, int quantity) throws MarketHubException {
        int changed = update("UPDATE products SET stock = stock + ? WHERE id = ?", ps -> {
            ps.setInt(1, quantity);
            ps.setLong(2, productId);
        });
        if (changed == 0) {
            throw new NotFoundException("Product", productId);
        }
    }
}
