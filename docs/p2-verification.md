# P2：网关、负载均衡与 Sentinel

## 前置条件与请求路径

按 [环境说明](environment.md) 准备实验环境，使用完整业务配置及默认 `nacos` profile。

```text
调用方 → Gateway :8100 → 订单 :8102 → Feign → 用户、商品、库存
调用方 → Gateway :8100 → 用户 :8101 / 第二实例
```

Gateway 通过 `lb://` 服务名路由，公开用户、订单、商品、库存和通知的 `/api` 业务路径，移除 `/api` 前缀。不转发 Swagger、Actuator 或 `/internal/**`。上游业务状态和 JSON 保持原样；连接失败返回 50300，响应超时返回 50400。

## 可选：多实例负载分配

复制用户 IDEA 运行配置，第二实例设置 `SERVER_PORT=8111`，服务名、命名空间和 Group 保持一致。检查 Nacos 有两个健康实例，重复 `GET /api/users/1` 并观察 `X-Service-Instance`。

随后请求订单预览，用同一个请求 ID 对照两个用户实例日志，验证 Feign 也能分配请求。实例退出和发现缓存更新之间可能有短暂延迟，写请求不自动重试。

## 可选：Dashboard 双向通信

订单服务通过 `SENTINEL_DASHBOARD` 上报心跳，Dashboard 还需要反向访问订单客户端数字 IP 和 `SENTINEL_CLIENT_PORT`（默认 8719）。多个订单实例使用不同命令端口。

`SENTINEL_CLIENT_IP` 留空时由 SDK 选择；如需指定，必须填写容器可访问的宿主机数字 IP，不能填 `host.docker.internal` 域名。必须实际读取或下发规则成功，才能认为双向通信通过。客户端日志在 `.runtime/cloud-order/logs/sentinel/`。

## 可选：限流实验

在 Nacos 发布 `cloud-order-flow.json`，格式 JSON，命名空间与 Group 与订单业务配置一致。初始 [样例](nacos/cloud-order-flow.json) 对 `order:preview` 使用单机 QPS 20、直接快速失败；将 `count` 改为 1 后连续请求订单预览，超限应返回 HTTP 429 / 42901。

规则按具体实例生效，不是集群总 QPS。对 `/orders/preview` 的 Web URL 资源设置限流也应返回统一 JSON。结束后恢复样例；停用全部流控规则可发布 `[]`，不要删除 Data ID。

## 可选：熔断实验

在 Nacos 的 `cloud-order-degrade.json` 为 `user:query` 设置异常数策略：`grade=2`、`count=1`、`minRequestAmount=2`、`statIntervalMs=10000`、`timeWindow=3`。初始 [样例](nacos/cloud-order-degrade.json) 使用异常比例策略：10 秒窗口内至少 5 次请求，异常比例超过 0.5 时熔断 10 秒。

停止全部实验用户实例并连续预览：真实调用失败返回 50301，熔断打开后返回 50302，订单日志应显示熔断期间不再请求用户服务。恢复用户实例，等待恢复窗口与注册发现更新，探测成功后恢复正常。

用户不存在等业务 4xx 不计入服务故障；不能用不存在用户制造熔断。实验后恢复规则样例并停止第二实例。

## 规则持久化与重启恢复

订单通过 `config/sentinel.yml` 的两个 Nacos 数据源读取上述 JSON，沿用 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_GROUP`、`NACOS_USERNAME` 和 `NACOS_PASSWORD`。独立 Compose 自动补齐样例，已有 Data ID 不会被重复初始化覆盖；已有环境需自行发布这两个 JSON。

将流控 `count` 暂设为 0，确认预览返回 429；重启订单服务后仍应返回 429。再恢复 20，确认不用重启即可返回 200。限流针对订单预览资源，不代表正式下单端点也设置了同一规则。

Nacos 是持久规则的来源。Dashboard 可观察规则，但官方默认控制台只改客户端内存，不自动回写 Nacos；这些临时修改可能在下次 Nacos 推送或客户端重启时被覆盖。需要持久保存的修改应在 Nacos 发布。本项目未改造 Dashboard 的规则写入链路，见 [Sentinel 官方 FAQ](https://github.com/alibaba/Sentinel/wiki/FAQ)。

规则 JSON 应为完整数组：`[]` 用于明确清空对应规则类型。初始化前先确认 Data ID、Group、命名空间及 JSON 有效；客户端可启动不代表规则已成功加载，需核对数据源日志、Dashboard 与实际请求。

## 动态路由

Gateway 从独立 JSON 加载路由并监听变化，格式、边界与验证步骤见 [动态路由说明](dynamic-routing.md)。

## 验证边界

通过实际 HTTP 请求、响应头及日志验证网关转发和错误语义；Dashboard 下发、多实例发现及熔断恢复按上述步骤执行。

规则在 Nacos 持久保存，客户端内存用于执行规则；当前支持流控和熔断，尚未加入热点参数规则及 Dashboard 回写 Nacos。

## 历史验收

2026-10-12 已完成真实 Dashboard 双向通信、规则下发、两层负载分配和熔断恢复。接口验证入口见 [接口验证说明](verification.md)。

2026-10-13 在独立 Compose 中完成新 Dashboard 登录、心跳与反向读取规则，以及鉴权规则加载、Nacos 热更新、订单重启恢复、重复初始化保留自定义规则，以及真实异常熔断和恢复；动态路由验收见对应文档。
