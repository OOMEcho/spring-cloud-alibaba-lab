CREATE TABLE lab_product
(
    id      BIGINT PRIMARY KEY,
    name    VARCHAR(100)   NOT NULL,
    price   DECIMAL(12, 2) NOT NULL,
    enabled BOOLEAN        NOT NULL,
    version INT            NOT NULL DEFAULT 0,
    CONSTRAINT ck_product_price CHECK (price > 0)
);
INSERT INTO lab_product (id, name, price, enabled, version)
VALUES (1001, '学习笔记本', 29.90, TRUE, 0),
       (1002, '保温杯', 49.90, TRUE, 0);
