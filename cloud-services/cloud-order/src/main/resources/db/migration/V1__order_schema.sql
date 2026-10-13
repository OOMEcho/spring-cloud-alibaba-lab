CREATE TABLE lab_order
(
    order_no     VARCHAR(64) PRIMARY KEY,
    user_id      BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(100)   NOT NULL,
    unit_price   DECIMAL(12, 2) NOT NULL,
    quantity     INT            NOT NULL,
    total_amount DECIMAL(16, 2) NOT NULL,
    status       VARCHAR(32)    NOT NULL,
    last_error   VARCHAR(200),
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_order_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_amount CHECK (total_amount > 0)
);
