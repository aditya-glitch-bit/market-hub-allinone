package com.markethub.config;

/**
 * Immutable application settings.
 *
 * <p>Values come from environment variables (or JVM system properties of the same name) so the
 * same jar runs unchanged on a laptop (H2 file database) and on a server (MySQL).</p>
 *
 * <pre>
 *   MARKETHUB_PORT                 HTTP port                           (default 8080)
 *   MARKETHUB_DB_URL               JDBC url                            (default embedded H2 file)
 *   MARKETHUB_DB_USER / _PASSWORD  database credentials                (default sa / empty)
 *   MARKETHUB_CORS_ORIGIN          allowed browser origin (frontend)   (default http://localhost:5173)
 *   MARKETHUB_SEED                 load demo data when DB is empty     (default true)
 *   MARKETHUB_SHIP_DELAY_SECONDS   simulated fulfilment delay, -1 off  (default 30)
 *   MARKETHUB_LOW_STOCK            low-stock alert threshold           (default 5)
 * </pre>
 */
public final class AppConfig {

    /** Embedded H2 database stored in ./data, using MySQL compatibility mode. */
    public static final String DEFAULT_DB_URL = "jdbc:h2:file:./data/markethub;MODE=MySQL;DB_CLOSE_DELAY=-1";

    private final int port;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String corsOrigin;
    private final boolean seedDemoData;
    private final long shipDelaySeconds;
    private final int lowStockThreshold;

    public AppConfig(int port, String dbUrl, String dbUser, String dbPassword, String corsOrigin,
                     boolean seedDemoData, long shipDelaySeconds, int lowStockThreshold) {
        this.port = port;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.corsOrigin = corsOrigin;
        this.seedDemoData = seedDemoData;
        this.shipDelaySeconds = shipDelaySeconds;
        this.lowStockThreshold = lowStockThreshold;
    }

    /** Reads configuration from the environment, falling back to development defaults. */
    public static AppConfig fromEnvironment() {
        return new AppConfig(
                Integer.parseInt(read("MARKETHUB_PORT", "8080")),
                read("MARKETHUB_DB_URL", DEFAULT_DB_URL),
                read("MARKETHUB_DB_USER", "sa"),
                read("MARKETHUB_DB_PASSWORD", ""),
                read("MARKETHUB_CORS_ORIGIN", "http://localhost:5173"),
                Boolean.parseBoolean(read("MARKETHUB_SEED", "true")),
                Long.parseLong(read("MARKETHUB_SHIP_DELAY_SECONDS", "30")),
                Integer.parseInt(read("MARKETHUB_LOW_STOCK", "5")));
    }

    /** Settings for automated tests: private in-memory database, no demo delay. */
    public static AppConfig forTests(String databaseName) {
        return new AppConfig(0,
                "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", "", "http://localhost:5173", true, -1, 5);
    }

    private static String read(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    public int getPort() { return port; }
    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public String getCorsOrigin() { return corsOrigin; }
    public boolean isSeedDemoData() { return seedDemoData; }
    public long getShipDelaySeconds() { return shipDelaySeconds; }
    public int getLowStockThreshold() { return lowStockThreshold; }
}
