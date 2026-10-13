# P1：注册发现、Feign 与配置刷新

## 前置条件

按 [环境说明](environment.md) 启动完整业务服务，使用默认 `nacos` profile。public / `LAB_GROUP` 中准备 `docs/nacos` 的六个业务 YAML 及三个规则/路由 JSON Data ID；实验使用独立 Group 时，将全部样例发布到该 Group 并修改各服务的 `NACOS_GROUP`。

订单预览查询真实用户、商品报价与库存快照。

## 注册发现与真实调用

1. 在 Nacos 检查六个服务均有健康实例，地址和端口能被宿主机 Java 访问。
2. 使用 [requests.http](requests.http) 查询用户，并经 Gateway 请求订单预览。
3. 响应包含用户昵称、数据库报价和库存快照；按 `X-Request-Id` 对照订单、用户、商品与库存日志。
4. 预览前后查询订单数、库存，确认不生成订单和扣减。

Feign 写请求不自动重试，连接超时 1200ms、读取超时 2500ms；超时不同于用户不存在等业务错误。

## 可选：配置刷新

记录实验 Group 中 `cloud-order.yaml` 的原内容，将其修改为：

```yaml
lab:
  message: Nacos 配置刷新实验
  order:
    max-quantity: 1
```

发布后直接查询订单服务 `GET /learning/config`，检查新文案和数量限制，无需重启。预览数量 2 应返回 HTTP 400 / 40001，数量 1 正常。实验完成恢复原配置并再次查询。

Nacos 客户端采用必需的 `spring.config.import`。Data ID 缺失或 Nacos 不可用时，正常启动不能静默退回其他模式。

## 接口检查与可选故障实验

- 用户不存在：返回 HTTP 404 / 40401；禁用用户返回 409 / 40901。
- 数量非法：在远程调用前返回 400。
- 停止实验用户服务：订单预览返回明确 503；恢复后等待发现更新，再检查正常请求。
- 各服务 `GET /v3/api-docs` 与 `/swagger-ui/index.html` 应可访问。

接口操作清单见 [接口验证说明](verification.md)，Nacos 推送需按上述步骤实际发布并观察业务配置。

## 历史验收

2026-10-12 已完成真实 Nacos 3.0.3 注册发现、Feign 调用和动态配置推送。接口验证入口见 [接口验证说明](verification.md)。
