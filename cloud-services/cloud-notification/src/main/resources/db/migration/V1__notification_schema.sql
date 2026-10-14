-- 消费去重和通知业务记录必须同一个本地事务完成。
CREATE TABLE lab_message_inbox (
    event_id VARCHAR(36) PRIMARY KEY,
    payload_hash VARCHAR(64) NOT NULL,
    processed_at DATETIME(6) NOT NULL
);
CREATE TABLE lab_order_notification (
    event_id VARCHAR(36) PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    total_amount DECIMAL(16,2) NOT NULL,
    created_at DATETIME(6) NOT NULL
);
