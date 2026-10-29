# MarketHub Backend (Java · Servlets · JDBC)

Java 17 backend for the MarketHub multi-vendor marketplace (Admin / Seller / Buyer).
It exposes a JSON REST API built on **Jakarta Servlets** and stores everything through **JDBC**
(embedded **H2** by default, **MySQL** optional). An embedded **Jetty** server is used, so no
separate Tomcat install is needed.

## Requirements

- JDK 17 or newer
- Maven 3.8+
- (optional) MySQL 8 if you do not want the embedded H2 database

## Run

```bash
cd backend
mvn package                       # builds target/markethub-backend.jar
java -jar target/markethub-backend.jar
# or, during development:
mvn exec:java
```

The API listens on `http://localhost:8080`. Check it with `curl http://localhost:8080/api/health`.
On first start an empty database is created from `src/main/resources/schema.sql` and filled with
demo data (set `MARKETHUB_SEED=false` to disable). The React frontend (`npm run dev`, port 5173)
is allowed through CORS by default.

### Docker

```bash
docker build -t markethub-api backend
docker run -p 8080:8080 markethub-api
```

Hosting platforms that set a plain `PORT` variable (Render, Railway) work without changes. All demo accounts use the password `Demo@1234` (for example `olivia@example.com` buyer, `hello@example.com` seller, `admin@example.com` admin).

### Configuration (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `MARKETHUB_PORT` (or `PORT`) | `8080` | HTTP port |
| `MARKETHUB_DB_URL` | embedded H2 file in `./data` | JDBC URL, e.g. `jdbc:mysql://localhost:3306/markethub` |
| `MARKETHUB_DB_USER` / `MARKETHUB_DB_PASSWORD` | `sa` / empty | Database credentials |
| `MARKETHUB_CORS_ORIGIN` | `http://localhost:5173` | Allowed frontend origin |
| `MARKETHUB_SEED` | `true` | Load demo data when the database is empty |
| `MARKETHUB_SHIP_DELAY_SECONDS` | `30` | Demo delay before a new order is auto-shipped (`-1` = off) |
| `MARKETHUB_LOW_STOCK` | `5` | Low-stock alert threshold |

## API overview

| Path | Purpose |
|---|---|
| `/api/health` | Liveness check |
| `/api/auth/*` | `register`, `login`, `logout` |
| `/api/products/*` | Search, filter, sort, paginate; sellers create / edit / delete listings |
| `/api/cart/*` | Per-user cart |
| `/api/orders/*` | Place orders, order history, status updates |
| `/api/dashboard/*` | Role-specific dashboard data (admin / seller / buyer) |
| `/api/users/*` | Admin user management |

## Architecture

```
HTTP  →  servlet (ApiServlet subclasses)      request/response, JSON, error mapping
         →  service (Product/Cart/Order/Account/Inventory/Dashboard)   business rules
            →  dao (interfaces)  →  dao.jdbc (JDBC implementations)    SQL only
               →  db (Database, ConnectionPool)                         connections, transactions
concurrent: OrderProcessor, LowStockMonitor, ActivityLog   background work
model: User ▸ Admin / Seller / Buyer, Product, Cart, Order, OrderItem
```

## Rubric mapping

### Java GUI-based project rubric (backend side)

| Criterion | Where it is implemented |
|---|---|
| **OOP** – inheritance, polymorphism, interfaces, exceptions | `model/`: abstract `User` extended by `Admin`, `Seller`, `Buyer`; interfaces `Identifiable<ID>`, `Discountable`; `UserFactory`. `service/dashboard/`: `DashboardProvider` interface with three implementations chosen at run time (polymorphism). `exception/`: checked hierarchy rooted at `MarketHubException` (`NotFound`, `Validation`, `Conflict` ▸ `OutOfStock`, `Authentication`, `Forbidden`, `DataAccess`, …) |
| **Collections & Generics** | `util/Collect` (generic `groupBy`, `sumBy`, `countBy`, `topN` with PECS wildcards), `util/LruCache<K,V>`, `util/Page<T>`, `dao/GenericDao<T extends Identifiable<ID>, ID>`, `Cart` (map of items), `Product implements Comparable<Product>` |
| **Multithreading & synchronization** | `concurrent/OrderProcessor` (producer/consumer on a `DelayQueue`, worker pool, compare-and-set status update), `LowStockMonitor` (scheduled executor, immutable snapshot published safely), `ActivityLog` (concurrent queue), `db/ConnectionPool` (blocking pool), `service/InventoryService` (atomic stock decrement) |
| **Database operation classes** | `dao/` interfaces and `dao/jdbc/` classes: `AbstractJdbcDao`, `JdbcProductDao`, `JdbcUserDao`, `JdbcOrderDao`, `TransactionManager` / `TransactionWork` for transactions |
| **JDBC connectivity** | `db/Database`, `db/ConnectionPool`, `config/AppConfig`; `PreparedStatement` everywhere (`Binder`, `SqlFunction`), H2 or MySQL by changing one URL |

### Java web-based project rubric

| Criterion | Where it is implemented |
|---|---|
| **Problem understanding & solution design** | Multi-vendor marketplace with three roles; layered design above; schema in `resources/schema.sql` (`users`, `products`, `orders`, `order_items`) |
| **Core Java concepts** | Same as the OOP / collections / concurrency rows above, plus `util/Validators`, `util/PasswordHasher`, `util/Json` |
| **Database integration (JDBC)** | DAO layer, connection pool, transactions for order placement (stock check + order insert commit or roll back together) |
| **Servlets & web integration** | `servlet/`: `ApiServlet` base class (JSON, error → HTTP status mapping) with `Auth`, `Product`, `Cart`, `Order`, `Dashboard`, `User`, `Health` servlets, `CorsFilter`; embedded Jetty bootstrap in `MarketHubApplication` |

## Notes

- Passwords are stored as salted hashes (`PasswordHasher`), never in plain text.
- All SQL uses `PreparedStatement`, so user input is never concatenated into queries.
- Demo data lives in `DemoData.java` and is clearly fictional.
