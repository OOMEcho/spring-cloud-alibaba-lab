#!/bin/sh
# 初始化本 Compose 的 Nacos 管理员，并补齐 public 命名空间的项目配置。
# 已存在的配置保持原值，重复启动不会覆盖使用者在控制台做的修改。
set -eu
base=http://nacos:8848/nacos
curl -sS --connect-timeout 5 --max-time 15 -X POST "$base/v3/auth/user/admin" \
  --data-urlencode "password=$NACOS_PASSWORD" >/dev/null
login=$(curl -fsS --connect-timeout 5 --max-time 15 -X POST "$base/v3/auth/user/login" \
  --data-urlencode username=nacos --data-urlencode "password=$NACOS_PASSWORD")
token=$(printf '%s' "$login" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
if [ -z "$token" ]; then echo 'Nacos 登录失败，请核对管理员密码；未输出凭据' >&2; exit 1; fi
for config in /opt/lab/config/cloud-*.yaml /opt/lab/config/cloud-*.json; do
  # 未匹配到某类文件时 shell 保留字面通配符，直接跳过。
  [ -f "$config" ] || continue
  name=${config##*/}
  case "$name" in
    *.yaml) config_type=yaml ;;
    *.json) config_type=json ;;
  esac
  status=$(curl -sS --connect-timeout 5 --max-time 15 -o /tmp/lab-nacos-response -w '%{http_code}' \
    --get "$base/v1/cs/configs" --data-urlencode "accessToken=$token" \
    --data-urlencode "dataId=$name" --data-urlencode "group=$NACOS_GROUP")
  case "$status" in
    200) printf '保留已有配置：%s\n' "$name" ;;
    404)
      result=$(curl -fsS --connect-timeout 5 --max-time 15 -X POST "$base/v1/cs/configs" \
        --data-urlencode "accessToken=$token" --data-urlencode "dataId=$name" \
        --data-urlencode "group=$NACOS_GROUP" --data-urlencode "type=$config_type" --data-urlencode "content@$config")
      if [ "$result" != true ]; then echo "配置初始化失败：$name" >&2; exit 1; fi
      printf '已创建配置：%s\n' "$name" ;;
    *) echo "配置查询失败：$name，HTTP $status" >&2; exit 1 ;;
  esac
done
