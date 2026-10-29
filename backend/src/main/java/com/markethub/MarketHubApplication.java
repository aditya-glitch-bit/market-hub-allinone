package com.markethub;

import com.markethub.config.AppConfig;
import com.markethub.servlet.AuthServlet;
import com.markethub.servlet.CartServlet;
import com.markethub.servlet.CorsFilter;
import com.markethub.servlet.DashboardServlet;
import com.markethub.servlet.HealthServlet;
import com.markethub.servlet.OrderServlet;
import com.markethub.servlet.ProductServlet;
import com.markethub.servlet.UserServlet;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

import java.util.EnumSet;

/**
 * Program entry point: loads configuration, opens the database, registers the servlets with an
 * embedded Jetty server and starts listening.
 *
 * <pre>
 *   mvn exec:java                      (development)
 *   java -jar target/markethub-backend.jar
 * </pre>
 */
public final class MarketHubApplication {

    private MarketHubApplication() { }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnvironment();
        AppContext context = AppContext.start(config);
        Server server = buildServer(context, config);

        // Stop cleanly on Ctrl+C: close HTTP first, then background threads and the database.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (Exception ignored) {
                // shutting down anyway
            }
            context.close();
        }, "shutdown"));

        server.start();
        System.out.println("MarketHub API running on http://localhost:" + config.getPort() + "/api/health");
        server.join();
    }

    /** Registers every servlet under {@code /api/...}; kept separate from {@code main} so tests can reuse it. */
    static Server buildServer(AppContext ctx, AppConfig config) {
        Server server = new Server(config.getPort());

        ServletContextHandler web = new ServletContextHandler(ServletContextHandler.SESSIONS);
        web.setContextPath("/");
        web.getSessionHandler().setHttpOnly(true);          // cookie not readable from JavaScript
        web.getSessionHandler().setMaxInactiveInterval(60 * 60);

        web.addFilter(new FilterHolder(new CorsFilter(config.getCorsOrigin())), "/*",
                EnumSet.of(DispatcherType.REQUEST));

        web.addServlet(new ServletHolder(new HealthServlet(ctx)), "/api/health");
        web.addServlet(new ServletHolder(new AuthServlet(ctx)), "/api/auth/*");
        web.addServlet(new ServletHolder(new ProductServlet(ctx)), "/api/products/*");
        web.addServlet(new ServletHolder(new CartServlet(ctx)), "/api/cart/*");
        web.addServlet(new ServletHolder(new OrderServlet(ctx)), "/api/orders/*");
        web.addServlet(new ServletHolder(new DashboardServlet(ctx)), "/api/dashboard/*");
        web.addServlet(new ServletHolder(new UserServlet(ctx)), "/api/users/*");

        server.setHandler(web);
        return server;
    }
}
