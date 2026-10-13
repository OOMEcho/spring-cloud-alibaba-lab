CREATE TABLE lab_stock
(
    product_id BIGINT PRIMARY KEY,
    available  INT NOT NULL,
    CONSTRAINT ck_stock_available CHECK (available >= 0)
);
CREATE TABLE lab_stock_reservation
(
    order_no   VARCHAR(64) PRIMARY KEY,
    product_id BIGINT      NOT NULL,
    quantity   INT         NOT NULL,
    status     VARCHAR(20) NOT NULL,
    CONSTRAINT ck_reservation_quantity CHECK (quantity > 0)
);
INSERT INTO lab_stock (product_id, available)
VALUES (1001, 20),
       (1002, 5);
