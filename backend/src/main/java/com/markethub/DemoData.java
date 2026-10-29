package com.markethub;

import com.markethub.dao.OrderDao;
import com.markethub.dao.ProductDao;
import com.markethub.dao.UserDao;
import com.markethub.exception.MarketHubException;
import com.markethub.model.Order;
import com.markethub.model.OrderItem;
import com.markethub.model.OrderStatus;
import com.markethub.model.Product;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.model.UserFactory;
import com.markethub.util.PasswordHasher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the same fictional marketplace the React frontend shows ({@code src/data/demo.ts}):
 * 8 products, 6 stores, 4 buyers, 1 admin and 4 orders. Runs only when the users table is empty.
 *
 * <p><b>Demo accounts only.</b> Every seeded account uses the password {@value #DEMO_PASSWORD}.
 * Turn seeding off with {@code MARKETHUB_SEED=false} for any real deployment.</p>
 */
public final class DemoData {

    public static final String DEMO_PASSWORD = "Demo@1234";

    private DemoData() { }

    /** @return {@code true} if data was inserted */
    public static boolean seedIfEmpty(UserDao users, ProductDao products, OrderDao orders) throws MarketHubException {
        if (users.count() > 0) {
            return false;
        }

        Map<String, User> byEmail = new HashMap<>();
        String[][] accounts = {
                // role, name, email
                {"ADMIN", "MarketHub Admin", "admin@example.com"},
                {"SELLER", "Sound Collective", "hello@example.com"},
                {"SELLER", "The Living Edit", "living@example.com"},
                {"SELLER", "Urban Step", "urbanstep@example.com"},
                {"SELLER", "Roam Supply", "roam@example.com"},
                {"SELLER", "Minute Studio", "minute@example.com"},
                {"SELLER", "Botanical House", "botanical@example.com"},
                {"BUYER", "Olivia Martin", "olivia@example.com"},
                {"BUYER", "James Wilson", "james@example.com"},
                {"BUYER", "Emma Thompson", "emma@example.com"},
                {"BUYER", "Noah Davis", "noah@example.com"},
        };
        for (String[] account : accounts) {
            User saved = users.save(UserFactory.create(Role.valueOf(account[0]), null, account[1], account[2],
                    PasswordHasher.hash(DEMO_PASSWORD), true, null));
            byEmail.put(account[2], saved);
        }

        Map<String, Product> byName = new HashMap<>();
        Object[][] catalogue = {
                // seller e-mail, name, category, price, old price, rating, reviews, image, badge, stock
                {"hello@example.com", "Studio Wireless Headphones", "Electronics", "129", "179", 4.8, 128, "product-1.jpg", "Best seller", 24},
                {"urbanstep@example.com", "Everyday Leather Sneakers", "Fashion", "89", "110", 4.7, 86, "product-2.jpg", "20% off", 18},
                {"living@example.com", "Sage Ceramic Vase", "Home & Living", "38", "0", 4.9, 64, "product-3.jpg", "New arrival", 4},
                {"roam@example.com", "The Weekend Backpack", "Sports & Outdoors", "65", "85", 4.6, 92, "product-4.jpg", "", 31},
                {"minute@example.com", "Classic Silver Watch", "Fashion", "149", "189", 4.8, 57, "product-5.jpg", "", 12},
                {"botanical@example.com", "Amber Eau de Parfum", "Beauty & Care", "72", "0", 4.9, 43, "product-6.jpg", "New arrival", 3},
                {"hello@example.com", "Portable Mini Speaker", "Electronics", "49", "69", 4.7, 105, "product-7.jpg", "", 28},
                {"urbanstep@example.com", "Essential Sun Frames", "Fashion", "45", "0", 4.6, 38, "product-8.jpg", "", 16},
        };
        for (Object[] row : catalogue) {
            User seller = byEmail.get((String) row[0]);
            Product saved = products.save(new Product(null, seller.getId(), seller.getName(), (String) row[1],
                    (String) row[2], new BigDecimal((String) row[3]), new BigDecimal((String) row[4]),
                    (Double) row[5], (Integer) row[6], (String) row[7], (String) row[8], (Integer) row[9], null));
            byName.put(saved.getName(), saved);
        }

        // order number, buyer e-mail, product, status, date
        seedOrder(orders, byEmail, byName, 1045, "noah@example.com", "The Weekend Backpack", OrderStatus.DELIVERED, 6);
        seedOrder(orders, byEmail, byName, 1046, "emma@example.com", "Sage Ceramic Vase", OrderStatus.DELIVERED, 7);
        seedOrder(orders, byEmail, byName, 1047, "james@example.com", "Everyday Leather Sneakers", OrderStatus.PROCESSING, 8);
        seedOrder(orders, byEmail, byName, 1048, "olivia@example.com", "Studio Wireless Headphones", OrderStatus.SHIPPED, 9);
        return true;
    }

    private static void seedOrder(OrderDao orders, Map<String, User> users, Map<String, Product> catalogue,
                                  int number, String buyerEmail, String productName, OrderStatus status,
                                  int dayOfOctober) throws MarketHubException {
        User buyer = users.get(buyerEmail);
        Product product = catalogue.get(productName);
        OrderItem line = new OrderItem(product.getId(), product.getSellerId(), product.getName(), product.getPrice(), 1);
        orders.save(new Order(null, number, buyer.getId(), buyer.getName(), status,
                LocalDateTime.of(2026, 10, dayOfOctober, 10, 30), Collections.singletonList(line)));
    }
}
