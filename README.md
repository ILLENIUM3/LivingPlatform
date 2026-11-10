# 直播平台

阶段 03 登录注册与会话代码已完成：本地模拟验证码、手机号唯一约束与自动注册、Redis Token、当前用户查询、固定过期及退出。学习与启动入口见 [登录注册与会话](docs/07-阶段03-登录注册与会话.md)，请求样例见 `scripts/auth.http`。已有数据库需补执行 `sql/003-user-phone.sql`；Provider 需 `local` profile 和 `AUTH_MOCK_ENABLED=true` 才会回显模拟验证码，API 无需开启 local。下一阶段：Gateway 与登录页面。

热点缓存重建锁已实现：按用户加锁、二次检查、有限等待、Lua 安全解锁及持锁回填。学习入口见 `docs/06-热点缓存重建锁.md`；它只用于减少重复查库，不保证数据库与缓存强一致。

用户缓存基础代码已接入：Cache Aside、短期空值缓存、TTL 抖动、事务提交后删除缓存，以及 Redis 故障时回退数据库。配置与一致性边界见 `docs/05-用户缓存基础设计.md`；部署时需启动新增 Redis 服务并重新打包、启动 Provider。

参考 qiyu-live 的模块设计，逐步实现自己的个人直播项目。阶段 01 已完成；阶段 02 用户基础业务代码已完成并通过编译检查，待执行新增 SQL 和 Maven 打包后使用。

用户创建、查询、修改、版本冲突处理和本地调试接口见 `docs/04-阶段02-用户基础业务.md`，请求示例见 `scripts/users.http`。已有数据库需执行 `sql/002-user.sql`；用户缓存阶段已重新打包，当前 JAR 包含用户基础业务与缓存代码；数据库 SQL 仍需自行执行。

## 当前能力

```text
GET /api/system/user-service
  → live-api
  → Dubbo（Nacos 服务发现）
  → live-user-provider
  → MySQL：读取 platform_bootstrap 中的初始化记录
```

探针接口不返回硬编码的数据库成功状态，每次请求都会访问数据库，依赖不可用时返回 HTTP 503。当前已实现验证码登录后自动注册、`GET /api/users/me` 与 `POST /api/auth/logout`；直播间业务仍在后续阶段开发。

## 工程结构

| 模块 / 目录 | 职责 |
| --- | --- |
| `backend/live-common` | HTTP 统一响应 |
| `backend/live-user-interface` | 用户服务 RPC 接口和可序列化 DTO；不依赖 Spring |
| `backend/live-user-provider` | 用户业务、缓存、验证码与会话、数据库探针、RPC 实现 |
| `backend/live-api` | HTTP 入口、RPC 调用和异常响应 |
| `deploy` | 本地 MySQL、Nacos、Redis 的 Compose 配置 |
| `sql` | 编号数据库脚本 |
| `scripts` | 验收脚本、HTTP 请求样例 |
| `docs` | 开发路线与阶段记录 |

API 只依赖用户服务的接口包，不依赖 Provider 实现。两个可启动模块各生成一个可执行 JAR。

## 版本与端口

| 组件 | 锁定版本 / 要求 | 本地端口 |
| --- | --- | --- |
| Java | 编译目标 17；本轮使用 JDK 21 | — |
| Maven | 3.6.3+；本轮使用 3.9.12 | — |
| Spring Boot | 3.5.16 | API 8080，用户服务健康检查 8081 |
| Dubbo | 3.3.7 | 用户 RPC 20880 |
| MySQL | Docker 镜像 `mysql:8.4.7` | 13306 → 容器 3306 |
| Nacos | Docker 镜像 `nacos/nacos-server:v2.5.1` | 18848 → 8848，19848 → 9848 |
| Redis | Docker 镜像 `redis:7.4.2-alpine` | 16379 → 6379 |

Nacos 客户端的 gRPC 端口是配置的 HTTP 端口加 1000，两者都要可连接。Docker 配置只将端口发布到本机。Nacos 使用单机内嵌存储，本地关闭鉴权；这份 Compose 用于本机学习。

工程采用 Dubbo 核心 Starter 和 Nacos Starter，用户缓存阶段已接入 Redis；Gateway、Kafka 和分表组件后续按阶段加入。MySQL JDBC 驱动版本由 Spring Boot 依赖管理统一控制。

## 启动步骤（Windows PowerShell）

先启动 Docker Desktop，确认 `java -version`、`mvn -version`、`docker version` 可用。在项目根目录执行：

```powershell
Set-Location D:\LivingPlatformByMyself
if (-not (Test-Path deploy/.env)) { Copy-Item deploy/.env.example deploy/.env }
# 首次配置时执行上面的复制；修改 deploy/.env 中的本地开发密码。
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --wait
mvn -f backend/pom.xml clean verify
```

MySQL 首次使用空数据卷启动时依次执行 `sql/001-bootstrap.sql`、`sql/002-user.sql`、`sql/003-user-phone.sql`。已有数据卷不会重新执行初始化脚本，需单独补执行尚未应用的 SQL。

在终端 A 启动用户服务（密码应与 `deploy/.env` 中的 MYSQL_PASSWORD 一致）：

```powershell
Set-Location D:\LivingPlatformByMyself
$env:MYSQL_PASSWORD = '填写 deploy/.env 中的 MYSQL_PASSWORD'
java -jar backend/live-user-provider/target/live-user-provider-0.1.0-SNAPSHOT.jar
```

用户服务启动后，在终端 B 启动 API：

```powershell
Set-Location D:\LivingPlatformByMyself
java -jar backend/live-api/target/live-api-0.1.0-SNAPSHOT.jar
```

在终端 C 验收：

```powershell
Set-Location D:\LivingPlatformByMyself
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

也可以用 IDEA 打开 `backend/pom.xml`，配置用户服务的 `MYSQL_PASSWORD` 环境变量，依次运行 `UserProviderApplication` 和 `LiveApiApplication`。源文件使用 Java 17 语法。

WSL/Linux 下 Maven 和 JAR 命令相同，环境变量使用 `export MYSQL_PASSWORD='...'`。工程路径为 `/mnt/d/LivingPlatformByMyself`。若服务和依赖跨 Windows/WSL 运行，先确认端口可达；本项目 RPC 默认广播 `127.0.0.1`，两个 Java 进程应在同一侧运行。此次实测 Windows PowerShell 无法直接访问 WSL Java 的 localhost 端口，WSL 内使用 `python3 scripts/smoke_test.py` 验收。

## 验收接口

| 请求 | 用途 |
| --- | --- |
| `GET http://127.0.0.1:8080/actuator/health` | API 进程健康，不代表完整 RPC 链路已通 |
| `GET http://127.0.0.1:8081/actuator/health` | 用户服务健康，包含数据库连接检查 |
| `GET http://127.0.0.1:8080/api/system/user-service` | RPC 与初始化表的实际读取 |

完整链路成功返回示例（数据库版本和 requestId 以实际返回为准）：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "serviceName": "live-user-provider",
    "database": "live_platform",
    "databaseProduct": "MySQL",
    "databaseVersion": "8.4.7",
    "probeValue": "stage01-ready"
  },
  "requestId": "每次响应的唯一标识"
}
```

依赖失败返回 HTTP 503、`code=DEPENDENCY_UNAVAILABLE`、`data=null`，可以通过返回的 requestId 在 API 日志查找原因。此诊断接口仅用于当前本地开发阶段。

## 配置与停止

- 用户服务必须提供 `MYSQL_PASSWORD`；默认库名 `live_platform`，用户名 `live_app`。
- 可通过 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USER` 覆盖数据库配置。
- 可通过 `NACOS_HOST`、`NACOS_PORT`、`NACOS_USERNAME`、`NACOS_PASSWORD` 覆盖注册中心配置。
- `API_PORT`、`USER_HTTP_PORT`、`USER_RPC_PORT`、`DUBBO_HOST` 可以调整应用端口和 RPC 广播地址。
- 两个 Java 终端分别按 Ctrl+C 停止。`docker compose --env-file deploy/.env -f deploy/compose.yaml stop` 停止项目依赖，保留数据。
- 本地 `deploy/.env`、日志、`target` 和 `.runtime` 不纳入版本管理。

## 验证与学习入口

`mvn -f backend/pom.xml clean verify` 运行现有自动化测试，包括初始六个探针测试、缓存锁测试和登录会话测试。数据库单元测试使用仅在 test 范围引入的 H2；实际链路另用 MySQL 和 Nacos 验收。登录阶段的聚焦验证结果见 `docs/07-阶段03-登录注册与会话.md`。

开发路线见 `docs/01-直播平台开发流程.md`，本轮操作与验收结果见 `docs/02-阶段01-工程搭建与验收.md`。

版本依据：[Spring Boot 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)、[Dubbo Spring Boot 集成](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/config/spring/spring-boot/)、[Dubbo Nacos 注册中心](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/registry/nacos/)。

当前四个模块已通过构建和六个测试，真实 Nacos / MySQL 链路及依赖故障恢复已验证。验证用 Java 进程已经停止，按上文命令即可启动。技术设计与后续实现顺序见 `docs/03-核心技术设计与实施顺序.md`。
