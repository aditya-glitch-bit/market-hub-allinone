package com.markethub.service;

import com.markethub.model.Order;
import com.markethub.model.OrderItem;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.util.Collect;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds the JSON view of an order as seen by a particular viewer. */
public final class OrderViews {

    private OrderViews() { }

    /**
     * Admins and buyers see the whole order. A seller only sees (and is only totalled for) the
     * lines that belong to their own products, because other sellers' lines are not their business.
     */
    public static Map<String, Object> forViewer(User viewer, Order order) {
        Map<String, Object> view = order.toView();
        if (viewer.getRole() == Role.SELLER) {
            List<OrderItem> mine = order.itemsOf(viewer.getId());
            view.put("items", mine.stream().map(OrderItem::toView).collect(Collectors.toList()));
            BigDecimal total = Collect.sum(mine, OrderItem::getLineTotal);
            view.put("total", total);
            if (mine.size() == 1) {
                view.put("product", mine.get(0).getProductName());
            }
        } else if (order.getItems().size() == 1) {
            view.put("product", order.getItems().get(0).getProductName());
        }
        return view;
    }
}
