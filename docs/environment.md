# 环境准备与配置

项目支持两种环境：复用已有中间件，或者使用仓库提供的 Compose 创建独立中间件。Java 服务均在宿主机 / IDEA 中运行。Compose 不接管已经安装的容器。

## 复用已有中间件

1. 使用 Java 21、Maven 3.9.x，确认 MySQL 8、Redis 7、Nacos 3.0.3、RabbitMQ 4.1.8、Seata 2.5.0 可访问。Sentinel Dashboard 1.8.9 用于治理实验。
2. 执行 [业务库初始化 SQL](../sql/mysql/00-create-databases.sql)，创建项目账号并只授予五个业务库的权限。业务表和种子数据由各服务的 Flyway 执行。
3. RabbitMQ 创建独立 vhost `/cloud-lab` 及可配置、写入、读取该 vhost 的账号。
4. 将 [Nacos 配置样例](nacos) 发布到 public 命名空间、`LAB_GROUP`，Data ID 与文件名一致：六个业务配置使用 YAML，两个 Sentinel 规则及一个网关路由使用 JSON。已存在配置由使用者判断是否更新。
5. Seata TC 按自身部署方式准备存储，服务端注册到 Nacos 的 `seata-server` / `SEATA_GROUP` / public / `default`；订单和库存通过同一注册信息发现 TC，`lab_tx_group` 映射该集群。
6. 首次复制根 `.env.example` 为 `.env` 并填写实际参数；已有 `.env` 时保留并编辑。示例默认端口为 3306、6379、8848、5672、8858、8091。

选择已有环境时无需执行 Compose 命令。服务启动和接口验证步骤见 README。

## 使用独立 Compose 环境

在项目根目录操作：

```bash
cp .env.compose.example .env.compose
# 填写密码、JWT 密钥和实际 SEATA_ADVERTISED_IP 后再启动；已有文件时不要覆盖。
docker compose --env-file .env.compose config --quiet
docker compose --env-file .env.compose up -d --build
docker compose --env-file .env.compose ps -a
docker compose --env-file .env.compose logs nacos-init
```

`config --quiet` 只检查配置。`up` 才实际创建容器；首次需下载镜像，从 Sentinel 官方 1.8.9 JAR 构建 Dashboard 镜像，并基于官方 Seata 2.5.0 镜像构建含 MySQL 驱动的 TC 镜像。当前配置面向本地开发，不包含 Java 应用镜像。

2026-10-13 已使用独立项目名、全新数据卷和不冲突端口完成真实首次启动验收；LOCAL / GLOBAL 下单、消息消费、库存不足回滚通过。中间件同时重启后，等待健康状态及 TC 注册恢复，再启动 Java 服务；不能把容器处于 running 当成依赖已就绪。

### Seata MySQL JDBC 驱动

TC 使用 `store.mode=db` 和 MySQL 时，需要单独提供 JDBC 驱动；业务服务中的 Maven 依赖不能替代容器内的驱动文件。缺少驱动时可能出现 `cannot find jdbc folder` 或驱动类找不到的启动错误。

[Seata Dockerfile](../docker/seata/Dockerfile) 从 Maven Central 下载固定版本 `mysql-connector-j-8.0.33.jar`，通过 SHA-256 校验后放入 `/seata-server/libs/jdbc/`；镜像显式设置 `LOADER_PATH=/seata-server/libs`，供 Seata 2.5 的驱动加载器扫描。官方入口脚本默认路径可能落到 `/lib`，只加入 JAR 而不修正搜索路径仍会失败。首次构建需要访问 Docker 镜像源和 Maven Central，后续可复用构建缓存。驱动 JAR 不放入 Git 仓库；如调整驱动版本，必须同时更新下载地址、校验值与镜像标签。

已自行部署 TC 的环境也要准备服务端驱动；仅填写 `driver-class-name` 不会下载驱动。目录选择依据 [Seata 2.5 驱动加载源码](https://github.com/apache/incubator-seata/blob/v2.5.0/core/src/main/java/org/apache/seata/core/store/db/AbstractDataSourceProvider.java)。

除 Seata TC 绑定到配置的宿主机 IP 外，其余宿主机端口绑定 `127.0.0.1`，默认端口与常用的已有环境错开：

| 服务 | Compose 宿主机端口 | 容器端口 |
| --- | --- | --- |
| MySQL | 13306 | 3306 |
| Redis | 16379 | 6379 |
| Nacos API / gRPC / 控制台 | 18848 / 19848 / 18080 | 8848 / 9848 / 8080 |
| RabbitMQ AMQP / 管理端 | 15673 / 25672 | 5672 / 15672 |
| Sentinel Dashboard | 18858 | 8858 |
| Seata TC | 18091 | 18091 |

修改 Nacos API 端口时，gRPC 宿主机端口必须同时设置为 API 端口 + 1000。Seata 的内部监听、对外公布及宿主机映射端口保持一致，避免 XID 中的地址指向另一套 TC。

### 凭据与初始化

`.env.compose.example` 是模板，所有占位密码需要替换。MySQL 初始化中的业务与 Seata 账号密码只允许字母、数字、`_`、`-`，可分别使用 `openssl rand -hex 24` 生成。Nacos JWT 密钥使用 `openssl rand -base64 48` 生成；Nacos 管理员用户名为 `nacos`。

- MySQL 空数据卷首次启动：执行 `docker/mysql/init.sh`，创建五个业务库、Seata 存储库，以及分别限权的账号。业务表仍由 Flyway 管理。
- RabbitMQ 空数据卷首次启动：通过镜像默认用户和 vhost 参数准备项目消息环境。
- Nacos：单机内嵌存储，开启客户端鉴权，数据保存在独立卷。`nacos-init` 初始化管理员并补齐六个业务 YAML 和三个规则/路由 JSON Data ID；已有配置保持原值。该任务退出码必须为 0，再启动 Java 服务。
- Seata：等待 MySQL 可用和 Nacos 初始化成功后启动，通过 Nacos 注册 TC；镜像补齐 Connector/J 并明确驱动路径；TCP 健康检查确认监听端口，数据库恢复后可自动重启；数据库存储使用 `cloud_seata` 专用账号与逻辑库；官方 SQL 来源与许可保留在 [初始化文件](../sql/seata/01-server-schema.sql) 中。

修改环境文件不会自动改变已初始化数据库或 RabbitMQ 中的账号密码，应通过对应管理工具修改。MySQL 初始化失败时先排查日志；已有数据卷不会自动重新执行首次初始化，修复后应明确补齐缺失资源。不要通过删卷来处理需要保留的数据。

停止本项目独立环境：

```bash
docker compose --env-file .env.compose down
```

数据保留在项目命名卷中。`down -v` 会删除这些卷内的数据，只在明确重建该实验环境时使用。

### Java 服务连接这套环境

Java 服务的编译、环境变量加载与启动步骤统一见 [README 启动指南](../README.md#启动指南)。选择独立 Compose 环境时，使用 `.env.compose` 中的连接参数，保留已有 `.env`。

只传递服务实际需要的变量。`MYSQL_ROOT_PASSWORD`、`SEATA_DB_PASSWORD` 等管理 / 存储凭据不用于业务服务数据库连接。

Dashboard 在容器内，需要反向访问宿主机的客户端端口 8719。`SENTINEL_CLIENT_IP` 应为容器可访问的宿主机数字 IP；不能填 `host.docker.internal` 域名。是否可达需要在目标机器实际下发规则验证。

## Seata 注册发现

Seata 使用本地文件配置（`config.type=file`）和 Nacos 注册发现（`registry.type=nacos`）。订单、库存客户端沿用 `NACOS_SERVER_ADDR` 及 Nacos 鉴权变量，TC 注册信息由以下变量控制：

| 变量 | 默认值 | 对应关系 |
| --- | --- | --- |
| `SEATA_NACOS_APPLICATION` | seata-server | TC 在 Nacos 中的服务名 |
| `SEATA_NACOS_GROUP` | SEATA_GROUP | TC 注册组，与业务服务的 LAB_GROUP 独立 |
| `SEATA_NACOS_NAMESPACE` | 空 ID（public） | 客户端与 TC 必须使用同一命名空间 ID |
| `SEATA_NACOS_CLUSTER` | default | TC 注册集群，同时是 lab_tx_group 的映射值 |

复用现有 TC 时按实际注册信息填写，无需覆盖已有 `.env`。自定义命名空间需先在 Nacos 创建；配置模板默认 public。不要把业务服务的 `NACOS_GROUP` 当作 TC 注册组。

Compose 中 TC 使用 `nacos:8848` 注册，宿主机客户端使用映射后的 Nacos 地址。TC 公布的 IP / 端口必须能被客户端连接：Compose 的 `SEATA_ADVERTISED_IP` 必须填写客户端可达的实际宿主机 IPv4（模板 `192.0.2.1` 仅为示例），TC 的端口绑定与公布地址保持一致，默认端口 18091。Seata 2.5 不使用回环地址生成 XID，因此不能填写 `127.0.0.1` 或 `0.0.0.0`，否则可能自动选择容器内部地址，见 [Seata 2.5 地址选择源码](https://github.com/apache/incubator-seata/blob/v2.5.0/server/src/main/java/org/apache/seata/server/Server.java)。若以后客户端部署到其他机器或容器，应同时调整公布地址、端口绑定与网络，而不是仅检查 Nacos 是否显示健康。

启动客户端前，在 Nacos 确认 TC 服务名、注册组和集群正确，且公布地址可从客户端访问。客户端不配置固定 TC 地址，注册中心不可用或匹配不到实例时不自动改为直连。

## 配置文件职责

| 位置 | 职责 |
| --- | --- |
| 各服务 `application.yml` | 服务名、端口、数据库、框架行为与组件配置导入 |
| 各服务 `application-nacos.yml` | 正常运行的 Nacos 注册发现与必需远程配置导入 |
| 订单 / 通知 `config/messaging.yml` | RabbitMQ 及项目消息参数，按需显式导入 |
| 订单 / 库存 `config/seata.yml` | AT 客户端参数，按需显式导入 |
| 订单 `config/sentinel.yml` | 流控与熔断的 Nacos JSON 数据源 |
| `docs/nacos/cloud-gateway-routes.json` | 动态业务路由，手工发布或由 Compose 补齐 |
| 根 `.env` / `.env.compose` | 本机实际参数，被 Git 忽略 |

正常运行默认使用 `nacos` profile；没有无数据库的业务运行模式。Nacos Data ID 缺失或不可用时，应用启动失败，不自动使用内存用户替代数据库。环境变量占位符不会让 Java 自动读取 `.env`，需由终端或 IDEA 明确传入。

Compose 读取 `.env.compose` 用于配置替换，再通过各服务的 `environment` 传入容器。容器内的中间件地址使用服务名（例如 Seata 连接 `mysql:3306`），宿主机 Java 使用映射后的地址。

## 验证范围

2026-10-13：独立项目名、全新数据卷、鉴权开启的 Compose 已完成镜像构建、首次初始化、六服务启动、LOCAL / GLOBAL 幂等下单、通知消费、库存不足回滚及中间件重启保留数据。Seata TC 通过 Nacos 公布宿主机 IP 与匹配端口，订单返回的 XID 与该独立 TC 一致。

新 Sentinel Dashboard 登录、心跳与反向读取客户端规则通过；Nacos 规则加载、热更新、订单重启恢复和熔断恢复通过；网关动态路由新增、删除、上游修改、无效更新保留及重启加载通过。验收在本地单机 OrbStack 完成，没有验证生产部署或多节点容灾。

参考：[Docker Compose 启动依赖](https://docs.docker.com/compose/how-tos/startup-order/)、[Nacos 3 Docker 部署](https://www.nacos.io/en/docs/v3.0/quickstart/quick-start-docker/)、[Seata 2.5 Docker 部署](https://seata.apache.org/zh-cn/docs/v2.5/ops/deploy-by-docker/)。
