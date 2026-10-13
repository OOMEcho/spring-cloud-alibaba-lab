# syntax=docker/dockerfile:1
# TC 使用 MySQL 存储时必须提供 JDBC 驱动；业务服务的 Maven 依赖不会进入 TC 镜像。
FROM apache/seata-server:2.5.0

# 官方入口脚本会设置 loader.path；显式指向真实 libs 目录，避免默认落到 /lib。
ENV LOADER_PATH=/seata-server/libs \
    LOG_HOME=/seata-server/logs

# 固定版本与 SHA-256；由 Seata 2.5 的 MySQL 驱动加载器扫描 libs/jdbc。
# 从 Maven Central 下载，构建时校验；不向仓库提交第三方 JAR。
ADD --checksum=sha256:e2a3b2fc726a1ac64e998585db86b30fa8bf3f706195b78bb77c5f99bf877bd9 \
    https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar \
    /seata-server/libs/jdbc/mysql-connector-j-8.0.33.jar
