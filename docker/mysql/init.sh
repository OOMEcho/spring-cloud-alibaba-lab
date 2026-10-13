#!/bin/sh
# MySQL 官方镜像在空数据卷首次初始化时执行；只创建本项目资源，不重置业务数据。
set -eu

# 初始化 SQL 使用这些值创建账号；限制字符以避免拼接 SQL 的歧义。
lab_check_value() {
  case "$2" in
    ''|*[!A-Za-z0-9_-]*) echo "初始化失败：$1 只允许字母、数字、下划线、短横线" >&2; exit 1 ;;
  esac
}
lab_check_value DB_USERNAME "$DB_USERNAME"
case "$DB_USERNAME" in
  root|cloud_seata) echo '业务账号不能与管理或 Seata 账号同名' >&2; exit 1 ;;
esac
lab_check_value DB_PASSWORD "$DB_PASSWORD"
lab_check_value SEATA_DB_PASSWORD "$SEATA_DB_PASSWORD"
export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
mysql -uroot < /opt/lab/sql/mysql/00-create-databases.sql
mysql -uroot -e 'CREATE DATABASE IF NOT EXISTS cloud_seata CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
mysql -uroot cloud_seata < /opt/lab/sql/seata/01-server-schema.sql
{
  printf "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s';\n" "$DB_USERNAME" "$DB_PASSWORD"
  for lab_schema in cloud_user cloud_product cloud_stock cloud_order cloud_notification; do
    printf "GRANT ALL PRIVILEGES ON \`%s\`.* TO '%s'@'%%';\n" "$lab_schema" "$DB_USERNAME"
  done
  printf "CREATE USER IF NOT EXISTS 'cloud_seata'@'%%' IDENTIFIED BY '%s';\n" "$SEATA_DB_PASSWORD"
  printf "GRANT ALL PRIVILEGES ON cloud_seata.* TO 'cloud_seata'@'%%';\n"
} | mysql -uroot
unset MYSQL_PWD
printf '项目业务库与 Seata 存储已初始化。\n'
