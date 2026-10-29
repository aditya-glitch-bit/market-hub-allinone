package com.markethub.service.dashboard;

import com.markethub.dao.OrderDao;
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

/** A shopper's order history and spending. */
public class BuyerDashboardProvider implements DashboardProvider {

    private final OrderDao orders;

    public BuyerDashboardProvider(OrderDao orders) {
        this.orders = orders;
    }

    @Override
    public Role role() { return Role.BUYER; }

    @Override
    public Map<String, Object> build(User buyer) throws MarketHubException {
        List<Order> mine = orders.findByBuyer(buyer.getId());

        List<Order> counted = mine.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());
        BigDecimal totalSpent = Collect.sum(counted, Order::getTotal);
        long inProgress = mine.stream()
                .filter(o -> o.getStatus() == OrderStatus.PROCESSING || o.getStatus() == OrderStatus.SHIPPED)
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalOrders", mine.size());
        summary.put("ordersInProgress", inProgress);
        summary.put("totalSpent", totalSpent);
        summary.put("orders", Collect.topN(mine, Comparator.comparing(Order::getId).reversed(), 20)
                .stream().map(o -> OrderViews.forViewer(buyer, o)).collect(Collectors.toList()));
        return summary;
    }
}
