-- 仅创建本项目独立逻辑库；也可以通过 DBX 手动执行。
-- 表结构和样例数据由各服务的 Flyway V1 migration 管理。
CREATE
DATABASE IF NOT EXISTS cloud_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE
DATABASE IF NOT EXISTS cloud_product CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE
DATABASE IF NOT EXISTS cloud_stock CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE
DATABASE IF NOT EXISTS cloud_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE
DATABASE IF NOT EXISTS cloud_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
