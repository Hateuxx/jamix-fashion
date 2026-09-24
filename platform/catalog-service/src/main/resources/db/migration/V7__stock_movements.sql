-- Stock ledger (kardex): every change to a product's stock is a movement with who, when, why.
-- quantity is the signed change (+3 received, -1 sold); stock_after is the stock right after it.

CREATE TABLE stock_movements (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    product_id   BIGINT       NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    quantity     INT          NOT NULL,
    stock_after  INT          NOT NULL,
    note         VARCHAR(200),
    username     VARCHAR(40)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX ix_stock_movements_product ON stock_movements (product_id, created_at);
CREATE INDEX ix_stock_movements_created ON stock_movements (created_at);
