-- AT 分支恢复日志；结构对应 Seata 2.5 官方客户端 MySQL undo_log。
CREATE TABLE undo_log
(
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6) NOT NULL,
    log_modified  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_undo_xid_branch (xid, branch_id),
    INDEX         ix_undo_created (log_created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE lab_order
    ADD COLUMN transaction_mode VARCHAR(10) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN global_xid VARCHAR(128);
ALTER TABLE lab_order_outbox
    ADD COLUMN global_xid VARCHAR(128);
