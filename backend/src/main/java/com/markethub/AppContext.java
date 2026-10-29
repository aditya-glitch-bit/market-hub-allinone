package com.markethub;

import com.markethub.concurrent.ActivityLog;
import com.markethub.concurrent.LowStockMonitor;
import com.markethub.concurrent.OrderProcessor;
import com.markethub.config.AppConfig;
import com.markethub.dao.OrderDao;
import com.markethub.dao.ProductDao;
import com.markethub.dao.UserDao;
import com.markethub.dao.jdbc.JdbcOrderDao;
import com.markethub.dao.jdbc.JdbcProductDao;
import com.markethub.dao.jdbc.JdbcUserDao;
import com.markethub.db.Database;
import com.markethub.exception.MarketHubException;
import com.markethub.service.AccountService;
import com.markethub.service.CartService;
import com.markethub.service.InventoryService;
import com.markethub.service.OrderService;
import com.markethub.service.ProductService;
import com.markethub.service.dashboard.AdminDashboardProvider;
import com.markethub.service.dashboard.BuyerDashboardProvider;
import com.markethub.service.dashboard.DashboardService;
import com.markethub.service.dashboard.SellerDashboardProvider;

import java.util.Arrays;

/**
 * Composition root: builds every object once and wires them together by constructor injection.
 * Servlets receive this context and ask it for the services they need.
 *
 * <pre>
 *   Servlets --> Services --> DAO interfaces --> JDBC DAOs --> Database (pool) --> H2 / MySQL
 *                   |
 *                   +--> concurrent helpers (inventory locks, order shipper, stock monitor)
 * </pre>
 */
public final class AppContext implements AutoCloseable {

    private final AppConfig config;
    private final Database database;
    private final ActivityLog activity;
    private final LowStockMonitor lowStockMonitor;
    private final OrderProcessor orderProcessor;
    private final AccountService accounts;
    private final ProductService products;
    private final CartService carts;
    private final OrderService orders;
    private final DashboardService dashboards;

    private AppContext(AppConfig config) throws MarketHubException {
        this.config = config;

        // --- data layer ---
        this.database = new Database(config);
        database.initSchema();
        UserDao userDao = new JdbcUserDao(database);
        ProductDao productDao = new JdbcProductDao(database);
        OrderDao orderDao = new JdbcOrderDao(database);
        if (config.isSeedDemoData()) {
            DemoData.seedIfEmpty(userDao, productDao, orderDao);
        }

        // --- background workers ---
        this.activity = new ActivityLog();
        this.lowStockMonitor = new LowStockMonitor(productDao, config.getLowStockThreshold());
        lowStockMonitor.scanNow();
        lowStockMonitor.start(30);
        long shipDelayMillis = config.getShipDelaySeconds() < 0 ? -1 : config.getShipDelaySeconds() * 1000;
        this.orderProcessor = new OrderProcessor(orderDao, activity, shipDelayMillis, 2);

        // --- business layer ---
        InventoryService inventory = new InventoryService(productDao);
        this.accounts = new AccountService(userDao, activity);
        this.products = new ProductService(productDao, userDao, activity);
        this.carts = new CartService(products);
        this.orders = new OrderService(orderDao, productDao, inventory, database, orderProcessor, activity);
        this.dashboards = new DashboardService(Arrays.asList(
                new AdminDashboardProvider(userDao, productDao, orderDao, activity),
                new SellerDashboardProvider(productDao, orderDao, lowStockMonitor),
                new BuyerDashboardProvider(orderDao)), orderDao);
    }

    public static AppContext start(AppConfig config) throws MarketHubException {
        return new AppContext(config);
    }

    public AppConfig config() { return config; }
    public AccountService accounts() { return accounts; }
    public ProductService products() { return products; }
    public CartService carts() { return carts; }
    public OrderService orders() { return orders; }
    public DashboardService dashboards() { return dashboards; }

    /** Stops background threads and closes database connections. */
    @Override
    public void close() {
        orderProcessor.close();
        lowStockMonitor.close();
        database.close();
    }
}
