package com.markethub.service.dashboard;

import com.markethub.concurrent.LowStockMonitor;
import com.markethub.dao.OrderDao;
import com.markethub.dao.ProductDao;
import com.markethub.exception.MarketHubException;
import com.markethub.model.Order;
import com.markethub.model.OrderItem;
import com.markethub.model.OrderStatus;
import com.markethub.model.Product;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.service.OrderViews;
import com.markethub.util.Collect;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** One store's inventory, sales, best sellers and low-stock alerts. */
public class SellerDashboardProvider implements DashboardProvider {

    private final ProductDao products;
    private final OrderDao orders;
    private final LowStockMonitor lowStock;

    public SellerDashboardProvider(ProductDao products, OrderDao orders, LowStockMonitor lowStock) {
        this.products = products;
        this.orders = orders;
        this.lowStock = lowStock;
    }

    @Override
    public Role role() { return Role.SELLER; }

    @Override
    public Map<String, Object> build(User seller) throws MarketHubException {
        long sellerId = seller.getId();
        List<Product> mine = products.findBySeller(sellerId);
        List<Order> myOrders = orders.findBySeller(sellerId);

        Map<Long, Product> productById = new HashMap<>();
        for (Product product : mine) {
            productById.put(product.getId(), product);
        }

        // Only this seller's lines of orders that were not cancelled count as sales.
        List<OrderItem> soldLines = new ArrayList<>();
        for (Order order : myOrders) {
            if (order.getStatus() != OrderStatus.CANCELLED) {
                soldLines.addAll(order.itemsOf(sellerId));
            }
        }
        BigDecimal revenue = Collect.sum(soldLines, OrderItem::getLineTotal);

        Map<String, BigDecimal> salesByCategory = new LinkedHashMap<>(Collect.sumBy(soldLines,
                line -> productById.containsKey(line.getProductId())
                        ? productById.get(line.getProductId()).getCategory() : "Other",
                OrderItem::getLineTotal));

        Map<String, Integer> unitsByProduct = new LinkedHashMap<>();
        for (OrderItem line : soldLines) {
            unitsByProduct.merge(line.getProductName(), line.getQuantity(), Integer::sum);
        }
        List<Map<String, Object>> bestSellers = Collect.topN(unitsByProduct.entrySet(),
                Map.Entry.<String, Integer>comparingByValue().reversed(), 5).stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("product", entry.getKey());
                    row.put("unitsSold", entry.getValue());
                    return row;
                }).collect(Collectors.toList());

        int unitsInStock = 0;
        BigDecimal stockValue = BigDecimal.ZERO;
        int outOfStock = 0;
        for (Product product : mine) {
            unitsInStock += product.getStock();
            stockValue = stockValue.add(product.getPrice().multiply(BigDecimal.valueOf(product.getStock())));
            if (product.getStock() == 0) {
                outOfStock++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalProducts", mine.size());
        summary.put("unitsInStock", unitsInStock);
        summary.put("stockValue", stockValue);
        summary.put("outOfStock", outOfStock);
        summary.put("totalOrders", myOrders.size());
        summary.put("revenue", revenue);
        summary.put("lowStockThreshold", lowStock.getThreshold());
        summary.put("lowStockAlerts", lowStock.alertsForSeller(sellerId).stream()
                .map(Product::toView).collect(Collectors.toList()));
        summary.put("salesByCategory", salesByCategory);
        summary.put("bestSellers", bestSellers);
        summary.put("recentOrders", Collect.topN(myOrders, Comparator.comparing(Order::getId).reversed(), 10)
                .stream().map(o -> OrderViews.forViewer(seller, o)).collect(Collectors.toList()));
        return summary;
    }
}
