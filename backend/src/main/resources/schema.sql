-- MarketHub database schema.
-- Written to run unchanged on H2 (MODE=MySQL) and MySQL 8.
-- Statements are separated by semicolons; do not put a semicolon inside a string literal.

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_id   BIGINT        NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    category    VARCHAR(60)   NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    old_price   DECIMAL(10,2) NOT NULL DEFAULT 0,
    rating      DECIMAL(2,1)  NOT NULL DEFAULT 0,
    reviews     INT           NOT NULL DEFAULT 0,
    image       VARCHAR(150)  NOT NULL DEFAULT '',
    badge       VARCHAR(40)   NOT NULL DEFAULT '',
    stock       INT           NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT ck_products_stock CHECK (stock >= 0),
    CONSTRAINT ck_products_price CHECK (price > 0)
);

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_number INT         NOT NULL UNIQUE,
    buyer_id     BIGINT      NOT NULL,
    status       VARCHAR(12) NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(150)  NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL,
    quantity     INT           NOT NULL,
    CONSTRAINT fk_items_order   FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_items_quantity CHECK (quantity > 0)
)
-- Note: the FOREIGN KEY constraints above already create indexes on seller_id, buyer_id,
-- order_id and product_id in both H2 and MySQL, so no CREATE INDEX statements are needed
-- (and CREATE INDEX has no IF NOT EXISTS in MySQL, which would break a second start-up).
