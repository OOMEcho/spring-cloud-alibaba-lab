-- 与订单确认状态在同一个事务写入；不保存 RabbitMQ 凭据或内部异常。
CREATE TABLE lab_order_outbox
(
    event_id        VARCHAR(36) PRIMARY KEY,
    order_no        VARCHAR(64) NOT NULL,
    event_type      VARCHAR(40) NOT NULL,
    payload         TEXT        NOT NULL,
    request_id      VARCHAR(64),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts        INT         NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    lease_token     VARCHAR(36),
    lease_until     DATETIME(6),
    last_error      VARCHAR(200),
    created_at      DATETIME(6) NOT NULL,
    sent_at         DATETIME(6),
    CONSTRAINT uk_order_event UNIQUE (order_no, event_type),
    INDEX           ix_outbox_due (status, next_attempt_at),
    INDEX           ix_outbox_lease (status, lease_until)
);
