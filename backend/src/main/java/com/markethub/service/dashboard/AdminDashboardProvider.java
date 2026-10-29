package com.markethub.service.dashboard;

import com.markethub.concurrent.ActivityLog;
import com.markethub.dao.OrderDao;
import com.markethub.dao.ProductDao;
import com.markethub.dao.UserDao;
import com.markethub.exception.MarketHubException;
import com.markethub.model.Order;
import com.markethub.model.OrderStatus;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.service.OrderViews;
import com.markethub.util.Collect;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Marketplace-wide numbers: users, catalogue size, orders, revenue and recent activity. */
public class AdminDashboardProvider implements DashboardProvider {

    private final UserDao users;
    private final ProductDao products;
    private final OrderDao orders;
    private final ActivityLog activity;

    public AdminDashboardProvider(UserDao users, ProductDao products, OrderDao orders, ActivityLog activity) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.activity = activity;
    }

    @Override
    public Role role() { return Role.ADMIN; }

    @Override
    public Map<String, Object> build(User admin) throws MarketHubException {
        List<User> allUsers = users.findAll();
        List<Order> allOrders = orders.findAll();

        Map<String, Integer> usersByRole = new LinkedHashMap<>();
        Collect.countBy(allUsers, User::getRole).forEach((role, count) -> usersByRole.put(role.name(), count));

        Map<String, Integer> ordersByStatus = new LinkedHashMap<>();
        Collect.countBy(allOrders, Order::getStatus).forEach((status, count) -> ordersByStatus.put(status.getLabel(), count));

        // Cancelled orders do not count as revenue.
        List<Order> counted = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());
        BigDecimal revenue = Collect.sum(counted, Order::getTotal);

        List<Map<String, Object>> recentOrders = Collect.topN(allOrders, Comparator.comparing(Order::getId).reversed(), 5)
                .stream().map(o -> OrderViews.forViewer(admin, o)).collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", allUsers.size());
        summary.put("usersByRole", usersByRole);
        summary.put("totalProducts", products.count());
        summary.put("totalOrders", allOrders.size());
        summary.put("ordersByStatus", ordersByStatus);
        summary.put("revenue", revenue);
        summary.put("recentOrders", recentOrders);
        summary.put("recentActivity", activity.recent(10).stream()
                .map(ActivityLog.Event::toView).collect(Collectors.toList()));
        return summary;
    }
}
