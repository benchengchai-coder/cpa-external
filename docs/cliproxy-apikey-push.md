# CLIProxyAPI API Key 推送同步

平台侧（本项目）`ai_apikey` 表是入口 API Key 的权威存储；CLIProxyAPI（CPA）只校验自身
配置中的 `api-keys` 列表。本文说明平台 Key 变更后如何自动写入 CPA 并即时生效。

## 功能开关

```yaml
cpa:
  cli-proxy:
    management:
      # 连接配置（host/port/management-key/tls/超时）与用量订阅共用同一台 CLIProxyAPI
      api-key-push-enabled: ${CPA_CLIPROXY_APIKEY_PUSH_ENABLED:false}
```

默认关闭。开启前提：

1. CPA 侧已配置 Management Key（`remote-management.secret-key`）并允许远程管理
   （`remote-management.allow-remote: true`）；
2. 本项目 `cpa.cli-proxy.management.management-key` 与之一致；
3. 注意 CPA 对连续 5 次认证失败会临时封禁约 30 分钟，Management Key 配错时推送会
   连带影响用量订阅。

## 同步机制

CLIProxyAPI 管理 API 没有单条新增接口，推送统一采用「拉取现有列表 → 本地合并 →
`PUT /v0/management/api-keys` 整体改写」模式：

- 只增删本次变更涉及的 Key，不会误删 CPA 上平台未登记的 Key（如管理员手工配置的 Key）；
- 写入后 CPA 持久化到配置文件并热加载生效，无需重启；
- 进程内加锁串行化，避免并发读改写互相覆盖。

### 覆盖的变更场景

| 平台操作 | 同步动作 |
|---------|---------|
| 新增 Key（含用户首次自动生成的默认密钥） | 推送新明文 |
| 轮换 / 管理员自定义密钥 | 旧明文替换为新明文（旧 Key 立即失效） |
| 删除 Key | 从 CPA 列表移除 |
| 停用（status → 1/2/3） | 从 CPA 列表移除 |
| 重新启用（status → 0） | 重新推送 |

推送通过 Spring 事件在**本地事务提交后、原请求线程内**同步执行：接口返回时 Key 已在
CPA 生效。推送失败不回滚本地记录（数据已落库），接口返回错误信息，可通过手动同步
入口补偿。

## 手动同步入口

后台「API 密钥管理」页面操作列提供**同步CPA**按钮（`POST /aigate/api-key/{keyId}/sync`，
仅管理员）：启用状态的 Key 立即推送到 CPA 生效，停用状态的 Key 从 CPA 移除，与自动
同步语义一致且幂等。适用场景：实时推送失败后的补偿、推送开关开启后同步存量 Key、
排查 CPA 侧配置是否生效。推送开关未开启时点击会返回开启配置的提示。

## 同步状态查询

**列表行标记**：API 密钥管理页列表自带"CPA同步"列（已同步/CPA缺失/停用残留/未知），
后端对 CPA 的 api-keys 列表做 60 秒展示缓存合并进列表接口——翻页/搜索不会每次都调
CPA；CPA 不可用时该列显示"未知"，不影响列表本身。同步动作（实时推送、手动同步按钮）
成功后缓存立即失效，列表即时反映新状态。

**CPA同步状态按钮**：页面工具栏的按钮（`GET /aigate/api-key/cpa-sync-status`，仅管理员）
绕过缓存实时拉取 CPA 的 api-keys 与本地 `ai_apikey` 对比，分类展示差异：

| 分类 | 含义 | 页面操作 |
|------|------|---------|
| 已同步 | 平台启用且 CPA 存在 | — |
| CPA缺失 | 平台启用但 CPA 没有（该 Key 无法调用 CPA） | 同步生效（复用单 Key 同步接口） |
| 停用残留 | 平台已停用但 CPA 仍存在 | 从CPA移除 |
| CPA未登记 | CPA 有但平台无记录（可能手工配置） | 仅展示脱敏明文，不自动删除 |

推送开关未开启时对话框顶部会警示"自动同步不会执行"。查询依赖 Management Key 可用，
CPA 不可连接时返回带原因的错误提示。

## 边界说明

本能力仅通过 CLIProxyAPI 公开管理接口（`/v0/management`）写其自身配置，不涉及
AI 请求转发、渠道路由或上游凭据代理，符合项目与 CLIProxyAPI 的边界约束。
