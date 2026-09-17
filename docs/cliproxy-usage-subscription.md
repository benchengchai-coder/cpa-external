# CLIProxyAPI 用量订阅

后端通过 CLIProxyAPI 提供的最小 Redis RESP 接口订阅 `usage` 通道，将每条已完成请求的用量记录保存到 `ai_log`。该集成只采集统计数据，不参与 AI 请求转发。

相关地址：

- CLIProxyAPI 项目：<https://github.com/router-for-me/CLIProxyAPI>
- Management API：<https://help.router-for.me/cn/management/api>
- Redis 用量队列：<https://help.router-for.me/cn/management/redis-usage-queue>

## CLIProxyAPI 配置

1. 配置可用的 Management Key。
2. 设置 `usage-statistics-enabled: true`。
3. Java 后端与 CLIProxyAPI 不在同一网络命名空间时，设置 `remote-management.allow-remote: true`，并通过防火墙限制该端口的访问来源。
4. 确认 Java 后端能够访问 CLIProxyAPI 的监听地址。RESP 与 HTTP API 复用同一个 TCP 端口，默认是 `8317`；CLIProxyAPI 启用 TLS 时，RESP 连接也必须使用 TLS。

典型配置如下：

```yaml
remote-management:
  allow-remote: true
  secret-key: <Management Key>
usage-statistics-enabled: true
```

## Java 后端配置

本地运行可以设置以下环境变量：

```text
CPA_CLIPROXY_USAGE_SUBSCRIPTION_ENABLED=true
CPA_CLIPROXY_USAGE_HOST=127.0.0.1
CPA_CLIPROXY_USAGE_PORT=8317
CPA_CLIPROXY_MANAGEMENT_KEY=<CLIProxyAPI Management Key>
CPA_CLIPROXY_USAGE_TLS=false
```

Docker 部署时，如果 CLIProxyAPI 运行在同一台宿主机，可保留 `CPA_CLIPROXY_USAGE_HOST=host.docker.internal`；如果它也是 Compose 服务，应填写同一 Docker 网络中的服务名。

订阅连接会定期发送 `PING` 保活，并在普通网络故障后自动重连。建立实时订阅后，后端还会补收订阅建立前暂存在 FIFO 队列中的记录。Management Key 认证失败时不会反复重试，以免触发 CLIProxyAPI 的 IP 封禁；修正密钥后需要重启 Java 后端。

## 日志时间入库口径

CLIProxyAPI 上报的 `timestamp` 是 ISO-8601 字符串，秒的小数位可能到纳秒并带时区偏移（例如 `2026-09-10T10:59:53.738095654+08:00`）。后端在 `CpaLogTimestampParser` 中把它解析成时间对象，按 `ai_log.timestamp`（`datetime(3)`）保存：

- 时区偏移会换算成绝对时间点，纳秒截断到毫秒；
- 上报值缺失或格式无法识别时回退为入库时间，并打一条 WARN 日志，不会丢掉整条用量记录；
- 存量数据的列类型变更见 `sql/migration/20260911_ai_log_timestamp_datetime.sql`，迁移后不要再运行会写入 ISO 字符串的旧版后端。
