package com.markethub.dao.jdbc;

import com.markethub.dao.OrderDao;
import com.markethub.db.Database;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.model.Order;
import com.markethub.model.OrderItem;
import com.markethub.model.OrderStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** JDBC implementation of {@link OrderDao}; an order and its lines are always saved together. */
public class JdbcOrderDao extends AbstractJdbcDao<Order> implements OrderDao {

    public JdbcOrderDao(Database db) {
        super(db);
    }

    @Override
    protected String baseSelect() {
        return "SELECT o.id, o.order_number, o.buyer_id, u.name AS buyer_name, o.status, o.created_at "
                + "FROM orders o JOIN users u ON u.id = o.buyer_id";
    }

    @Override
    protected String idColumn() { return "o.id"; }

    @Override
    protected String tableName() { return "orders"; }

    /** Maps the order header; the lines are attached afterwards by {@link #afterLoad}. */
    @Override
    protected Order mapRow(ResultSet rs) throws SQLException {
        return new Order(
                rs.getLong("id"),
                rs.getInt("order_number"),
                rs.getLong("buyer_id"),
                rs.getString("buyer_name"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                new ArrayList<>());
    }

    /** Loads the lines of all returned orders with a single query (avoids one query per order). */
    @Override
    protected void afterLoad(Connection connection, List<Order> orders) throws SQLException {
        if (orders.isEmpty()) {
            return;
        }
        Map<Long, List<OrderItem>> byOrder = new HashMap<>();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < orders.size(); i++) {
            placeholders.append(i == 0 ? "?" : ", ?");
            byOrder.put(orders.get(i).getId(), new ArrayList<>());
        }
        String sql = "SELECT oi.order_id, oi.product_id, p.seller_id, oi.product_name, oi.unit_price, oi.quantity "
                + "FROM order_items oi JOIN products p ON p.id = oi.product_id "
                + "WHERE oi.order_id IN (" + placeholders + ") ORDER BY oi.id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < orders.size(); i++) {
                ps.setLong(i + 1, orders.get(i).getId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byOrder.get(rs.getLong("order_id")).add(new OrderItem(
                            rs.getLong("product_id"),
                            rs.getLong("seller_id"),
                            rs.getString("product_name"),
                            rs.getBigDecimal("unit_price"),
                            rs.getInt("quantity")));
                }
            }
        }
        for (Order order : orders) {
            order.setItems(byOrder.get(order.getId()));
        }
    }

    /**
     * New order: inserts the header and all lines in one transaction (all-or-nothing).
     * Existing order: only the status is written.
     */
    @Override
    public Order save(Order order) throws MarketHubException {
        if (order.getId() != null) {
            int changed = update("UPDATE orders SET status = ? WHERE id = ?", ps -> {
                ps.setString(1, order.getStatus().name());
                ps.setLong(2, order.getId());
            });
            if (changed == 0) {
                throw new NotFoundException("Order", order.getId());
            }
            return order;
        }
        long id = db.inTransaction(() -> execute(connection -> {
            LocalDateTime created = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
            long orderId;
            String insertOrder = "INSERT INTO orders (order_number, buyer_id, status, created_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, order.getOrderNumber());
                ps.setLong(2, order.getBuyerId());
                ps.setString(3, order.getStatus().name());
                ps.setTimestamp(4, Timestamp.valueOf(created));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getLong(1);
                }
            }
            String insertItem = "INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity) "
                    + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertItem)) {
                for (OrderItem item : order.getItems()) {
                    ps.setLong(1, orderId);
                    ps.setLong(2, item.getProductId());
                    ps.setString(3, item.getProductName());
                    ps.setBigDecimal(4, item.getUnitPrice());
                    ps.setInt(5, item.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return orderId;
        }));
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted order vanished"));
    }

    @Override
    public List<Order> findByBuyer(long buyerId) throws MarketHubException {
        return query(baseSelect() + " WHERE o.buyer_id = ? ORDER BY o.id DESC", ps -> ps.setLong(1, buyerId));
    }

    @Override
    public List<Order> findBySeller(long sellerId) throws MarketHubException {
        return query(baseSelect() + " WHERE o.id IN (SELECT oi.order_id FROM order_items oi "
                + "JOIN products p ON p.id = oi.product_id WHERE p.seller_id = ?) ORDER BY o.id DESC",
                ps -> ps.setLong(1, sellerId));
    }

    @Override
    public boolean updateStatus(long orderId, OrderStatus expected, OrderStatus target) throws MarketHubException {
        return update("UPDATE orders SET status = ? WHERE id = ? AND status = ?", ps -> {
            ps.setString(1, target.name());
            ps.setLong(2, orderId);
            ps.setString(3, expected.name());
        }) == 1;
    }

    @Override
    public int maxOrderNumber() throws MarketHubException {
        return execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COALESCE(MAX(order_number), 0) FROM orders");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }
}
