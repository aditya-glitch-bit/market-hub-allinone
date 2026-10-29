package com.markethub.service.dashboard;

import com.markethub.dao.OrderDao;
import com.markethub.exception.ForbiddenException;
import com.markethub.exception.MarketHubException;
import com.markethub.model.Order;
import com.markethub.model.OrderItem;
import com.markethub.model.Role;
import com.markethub.model.User;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Picks the dashboard provider that matches the user's role and wraps its data. */
public class DashboardService {

    private final Map<Role, DashboardProvider> providers = new EnumMap<>(Role.class);
    private final OrderDao orders;

    public DashboardService(Collection<? extends DashboardProvider> providerList, OrderDao orders) {
        for (DashboardProvider provider : providerList) {
            providers.put(provider.role(), provider);
        }
        this.orders = orders;
    }

    public Map<String, Object> build(User user) throws MarketHubException {
        DashboardProvider provider = providers.get(user.getRole());
        if (provider == null) {
            throw new ForbiddenException("No dashboard for role " + user.getRole());
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("title", user.getDashboardTitle());
        response.put("user", user.toView());
        response.put("summary", provider.build(user));
        return response;
    }

    /** Seller sales export (the storefront's "export sales CSV" button): one row per sold line. */
    public String salesCsv(User seller) throws MarketHubException {
        if (seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only sellers can export sales");
        }
        StringBuilder csv = new StringBuilder("Order,Date,Customer,Product,Quantity,Unit price,Line total,Status\n");
        for (Order order : orders.findBySeller(seller.getId())) {
            for (OrderItem line : order.itemsOf(seller.getId())) {
                csv.append(cell(order.getCode())).append(',')
                        .append(cell(String.valueOf(order.getCreatedAt()))).append(',')
                        .append(cell(order.getBuyerName())).append(',')
                        .append(cell(line.getProductName())).append(',')
                        .append(line.getQuantity()).append(',')
                        .append(line.getUnitPrice()).append(',')
                        .append(line.getLineTotal()).append(',')
                        .append(cell(order.getStatus().getLabel())).append('\n');
            }
        }
        return csv.toString();
    }

    /** Quotes a CSV field and neutralises spreadsheet formulas (a leading = + - @ becomes text). */
    static String cell(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
