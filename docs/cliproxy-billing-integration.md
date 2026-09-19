# CLIProxyAPI 计费集成契约

本项目作为独立账务服务，仅接收 CLIProxyAPI 已完成请求产生的 usage 并结算，不提供请求前预占或冻结接口。
本项目不转发任何 AI 请求；请求入口由 CLIProxyAPI 自行承担，请求前预检（可选的
`/check` 调用）方是 CLIProxyAPI 的插件（`request.intercept_before` 拦截器 +
`host.http.do` 回调，参考官方示例 `examples/plugin/request-lifecycle`）。

## 数据流

```
客户端 → CLIProxyAPI
           ↓ 请求前（intercept_before，可选预检）
         POST /aigate/billing/check   ← 校验 Key/用户、按模型估算费用并做余额预检，不产生计费副作用
           ↓ allowed=false → CLIProxyAPI 直接拒绝（建议 402/403）
           ↓ allowed=true  → CLIProxyAPI 处理 AI 请求
           ↓ 请求完成
         Redis usage（SUBSCRIBE usage）       ← CLIProxyAPI 发布，每执行尝试一条
           ↓
         本项目 CpaUsageSubscriber → ai_log 落库（重试场景非失败记录覆盖失败记录）
           ↓ 事务提交后
         创建 pending_settlement 账单 + 延迟结算任务（默认 5s 窗口）
           ↓ Worker 领取时重读 ai_log.cost
         三源结算（订阅优先/钱包，按实际费用）→ success / partial
```

## 关键约定

- **request_id**：即 CLIProxyAPI 的 8 位 hex TraceID（响应头 `X-CPA-TRACE-ID`）。
  usage 记录、账单全部以该值关联。注意 usage 记录中的 `request_id` 与插件
  拦截阶段可见的 TraceID 一致；插件侧每次模型执行的 RequestID（UUID）不参与计费关联。
- **多条 usage**：CLIProxyAPI 凭据重试时同一 request_id 会产生多条 usage，首条常是
  失败尝试。本项目用"延迟结算窗口（`cpa.billing.settle-delay-seconds`，默认 5s）+
  ai_log 择优覆盖（非失败记录覆盖失败记录）"保证按最终成功尝试计费；全失败的请求
  在窗口后按零费结算。
- **失败请求不收费**：失败尝试已产生的 token 不计入费用（对齐旧系统对失败请求的
  释放语义）。
- **usage 即结算依据**：无法解析用户归属的 usage 只记日志并跳过扣费。

## 公开 HTTP API

基础路径：`http://<后端>/aigate/billing`，唯一路径 `/check` 已在 SecurityConfig 放行。
鉴权：请求头 `X-Billing-Token: <CPA_BILLING_API_TOKEN>`（常量时间比较）。
**Token 未配置时该接口一律拒绝**，防止裸奔部署。
使用独立的 `X-Billing-Token` 头而不是 `Authorization`，避免网关 JWT 过滤器
把计费 Token 当登录 Token 解析而刷错误日志。

> 历史接口 `/reserve`（请求前预占）与 `/release`（主动释放预占）已随预占功能移除，
> 旧路径不再注册。仍在调用它们的插件（billing-guard 1.x）会被 fail-closed 拦下全部
> 请求，需升级到 billing-guard 2.0.0+。

### POST /aigate/billing/check

余额/订阅只读校验与请求费用预估，不产生任何计费副作用，可用于诊断或请求前预检：

```json
{ "api_key": "sk-xxxx", "model": "gpt-5.6-sol" }
```

响应：`allowed`、`user_id`、`key_id`、`billing_preference`、`wallet{balance,
frozen_balance,available_balance}`、`subscription{subscription_id,plan_title,
amount_total,amount_used,frozen_balance,available_amount}`（amount_total=0 表示不限量）、
`concurrency{limit,active}`（AI并发上限与历史在途计数，纯诊断展示，不参与判定）、
`key{unlimited_balance,remain_balance,frozen_balance,available_balance}`。
Key 级配额已下线，`key` 子对象为兼容旧插件保留结构，恒返回
`unlimited_balance=true, remain_balance=0, frozen_balance=0, available_balance=null`。

`allowed=false` 出现两类场景（`reason` 给出原因）：

1. Key 无效/停用，或关联用户不存在/停用 → 建议映射 403；
2. **余额预检**（`cpa.billing.balance-check-enabled`，默认开启）：按用户计费偏好判定
   是否有足够资金覆盖本次预估费用，资金口径与结算分配一致——
   - 服务端根据 `model` 查询输入单价，按 1,000,000 个输入 Token 和用户计费倍率计算预估费用；
   - 预估费用低于 `sys_config` 的 `ai.billing.minimumAmount` 时按该配置值计算；
   - `wallet_only`：钱包可用额度（余额 − 冻结）≥ 本次预估费用；
   - `subscription_only`：存在生效订阅且可用额度 ≥ 本次预估费用（不限量订阅视为可用）；
   - `wallet_first` / `subscription_first`：钱包或订阅任一额度足够；
   - 拒绝原因带"余额/额度"特征词（如"钱包余额不足且无可用订阅额度"），
     CLIProxyAPI 插件据此映射 402。
   并发不在请求阶段判定。预检是后付费模型的请求级兜底：余额足够覆盖本次预估费用
   的用户在结算延迟窗口内仍可继续消费，欠收差额由结算记为 `partial` 的
   `uncovered_amount`，不会把余额打成负数。

## 鉴权失败

- Token 未配置：HTTP 403
- Token 不匹配：HTTP 401

## CLIProxyAPI 侧要求

1. `usage-statistics-enabled: true`，配置 `remote-management.secret-key`（同时是
   RESP usage 通道的 AUTH 密码）。
2. 插件（billing-guard 2.0.0+）在 `request.intercept_before` 中调用 check 做只读预检：
   - 取入站 api_key 调用 `/check`，`allowed=false` 时 `Terminate: true` 并返回 403
     （Key 无效/停用）或 402（余额预检不通过）；
   - HTTP 超时建议 2~3 秒：check 接口不可用时是否放行由部署者自行决断
     （默认拒绝可避免绕过管理端停用 Key，放行则退化为纯后付费模式）。
3. 无需任何释放或费用上报调用：结算数据完全来自 Redis usage，失败请求零费结算、
   多余冻结释放均由本项目结算侧处理。

## 本项目侧配置

| 配置 | 默认 | 说明 |
|------|------|------|
| `cpa.billing.api-token` | 空 | 公开计费 API 的鉴权 Token（X-Billing-Token 头）；为空则拒绝所有请求 |
| `cpa.billing.balance-check-enabled` | true | `/check` 余额预检开关：按计费偏好无任何可用资金源时 `allowed=false`；关闭则退回纯后付费 |
| `cpa.billing.settle-delay-seconds` | 5 | 延迟结算窗口，需大于凭据重试的最大间隔 |
| `cpa.billing.settlement-worker-enabled` | true | 异步结算 Worker 开关 |
| `cpa.billing.settlement-max-retries` | 10 | 自动重试上限，超限进入"结算异常"人工处置 |
| sys_config `ai.billing.minimumAmount` | 0.001 | 请求级最低计费（usage 写入 `ai_log` 前应用） |

## 用户并发限制

- 请求阶段的并发占位已随预占功能一并移除：请求不再受 `sys_user.ai_concurrency_limit`
  强制拦截，`/check` 返回的 `concurrency{limit,active}` 仅为诊断展示，不参与判定。
- `sys_user.active_request_count` 不再递增；仅历史遗留 `reserved` 账单在人工核销等
  处置出口处递减，清理旧数据后可废弃该列。
- 上限仍按用户配置：管理端"余额管理 → 并发限制"（`sys_user.ai_concurrency_limit`，0～1000），
  当前仅作为展示值下发给 `/check`。

## 运维

- 结算失败任务：管理端“结算异常”页（前端路由 `/aigate/billing-settlement`，查询接口
  `/aigate/billing/settlement/failed/list`），支持“重新结算”（按账单金额正常扣款）与
  “人工核销”（释放全部冻结、放弃追收）。旧库若看不到菜单，先执行
  `sql/migration/20260918_restore_billing_settlement_menu.sql`。
- 对账兜底：随预占清理任务一并移除的自动补偿已不存在；服务层的
  `repairMissingSettlementTasks`（补建 `pending_settlement` 无任务账单）当前没有
  定时调用方。若结算意图提交失败（见 `submitUsage` 的事务分离注释），账单会停留在
  无任务状态，需人工关注或重新接入补偿调度。
- 账单查询：日志详情页"账单"数据来自 `/aigate/billing/records/by-log/{logId}`。

## 状态机（ai_billing_record.status）

```
（现行）usage 落库后直接创建 → pending_settlement → success | partial
                                                     └→ written_off（人工核销，自 pending/失败重试）

（历史遗留）processing → reserved → pending_settlement（由结算侧收编）→ …
                          ├→ released（旧主动释放）
                          └→ expired（旧超时释放）
```

新账单不再经过 `reserved`，预占相关金额列恒为 0；遗留 `reserved/processing` 账单由
结算 Worker 与补偿任务收编进 `pending_settlement` 后正常结算。
所有状态迁移均通过 `where status = 旧值` 的 CAS 完成；账单以 request_id 全局唯一。
