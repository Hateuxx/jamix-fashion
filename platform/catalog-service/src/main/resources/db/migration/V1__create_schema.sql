-- Jamix catalog & sales schema.
-- Flyway runs each file once and records it in flyway_schema_history. Never edit a migration that
-- has already run anywhere: add V3__..., V4__... instead.
-- Written to run on MySQL 8+ and on H2 in MySQL mode (tests).

CREATE TABLE products (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    slug                VARCHAR(80)   NOT NULL,
    name                VARCHAR(120)  NOT NULL,
    category            VARCHAR(16)   NOT NULL,
    type                VARCHAR(60),
    description         VARCHAR(500),
    price               DECIMAL(10,2) NOT NULL,
    price_max           DECIMAL(10,2),
    wholesale_price     DECIMAL(10,2),
    wholesale_price_max DECIMAL(10,2),
    image_url           VARCHAR(255),
    stock               INT           NOT NULL DEFAULT 0,
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    -- Category values are enforced by the Category enum in Java.
    CONSTRAINT ck_products_stock CHECK (stock >= 0)
);

CREATE INDEX ix_products_category ON products (category);

CREATE TABLE orders (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    customer_username VARCHAR(80)   NOT NULL,
    status            VARCHAR(16)   NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    notes             VARCHAR(500),
    total             DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

-- "My orders" lists a customer's orders newest first.
CREATE INDEX ix_orders_customer_created ON orders (customer_username, created_at);

CREATE TABLE order_lines (
    order_id     BIGINT        NOT NULL,
    line_no      INT           NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(120)  NOT NULL,
    quantity     INT           NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL,
    wholesale    BOOLEAN       NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY (order_id, line_no),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_lines_quantity CHECK (quantity > 0)
);
