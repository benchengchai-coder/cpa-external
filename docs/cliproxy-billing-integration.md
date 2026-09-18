# CLIProxyAPI 计费集成契约

本项目作为独立账务服务，仅接收 CLIProxyAPI 已完成请求产生的 usage 并结算，不提供请求前预占或冻结接口。
本项目不转发任何 AI 请求；请求入口由 CLIProxyAPI 自行承担，预占调用方是
CLIProxyAPI 的插件（`request.intercept_before` 拦截器 + `host.http.do` 回调，
参考官方示例 `examples/plugin/request-lifecycle`）。

## 数据流

```
客户端 → CLIProxyAPI
           ↓ 请求前（intercept_before）
         （不再调用请求前预占接口）
           ↓ allowed=false → CLIProxyAPI 直接拒绝（建议 402/429）
           ↓ allowed=true  → CLIProxyAPI 处理 AI 请求
           ↓ 请求完成
         Redis usage（SUBSCRIBE usage）       ← CLIProxyAPI 发布，每执行尝试一条
           ↓
         本项目 CpaUsageSubscriber → ai_log 落库（重试场景非失败记录覆盖失败记录）
           ↓ 事务提交后
         延迟结算任务（默认 5s 窗口）
           ↓ Worker 领取时重读 ai_log.cost
         三源结算（允许实扣超过预占）→ success / partial
```

## 关键约定

- **request_id**：即 CLIProxyAPI 的 8 位 hex TraceID（响应头 `X-CPA-TRACE-ID`）。
  预占、usage 记录、账单全部以该值关联。注意 usage 记录中的 `request_id` 与插件
  拦截阶段可见的 TraceID 一致；插件侧每次模型执行的 RequestID（UUID）不参与计费关联。
- **多条 usage**：CLIProxyAPI 凭据重试时同一 request_id 会产生多条 usage，首条常是
  失败尝试。本项目用"延迟结算窗口（`cpa.billing.settle-delay-seconds`，默认 5s）+
  ai_log 择优覆盖（非失败记录覆盖失败记录）"保证按最终成功尝试计费；全失败的请求
  在窗口后按零费结算并释放预占。
- **失败请求不收费**：失败尝试已产生的 token 不计入费用（对齐旧系统对失败请求的
  释放语义）。
- **usage 即结算依据**：无需预占调用；无法解析用户归属的 usage 只记日志并跳过扣费。

## 公开 HTTP API

基础路径：`http://<后端>/aigate/billing`，三个路径均已在 SecurityConfig 放行。
鉴权：请求头 `X-Billing-Token: <CPA_BILLING_API_TOKEN>`（常量时间比较）。
**Token 未配置时三个接口一律拒绝**，防止裸奔部署。
使用独立的 `X-Billing-Token` 头而不是 `Authorization`，避免网关 JWT 过滤器
把计费 Token 当登录 Token 解析而刷错误日志。

### 请求前预占接口（已移除）

请求前额度预占（幂等）：

```json
{
  "request_id": "1a2b3c4d",
  "api_key": "sk-xxxx"
}
```

响应（HTTP 200）：

```json
{
  "allowed": true,
  "request_id": "1a2b3c4d",
  "billing_id": 600,
  "user_id": 100,
  "key_id": 5,
  "subscription_id": 12,
  "billing_preference": "subscription_first",
  "reserved_amount": 0.01,
  "wallet_reserved_amount": 0.01,
  "subscription_reserved_amount": 0,
  "key_reserved_amount": 0.01,
  "concurrency_limit": 100,
  "active_request_count": 1,
  "reason": null
}
```

`concurrency_limit` 为该用户并发上限（0 表示禁用）；`active_request_count` 为诊断参考值
（事务开始时的快照计数），权威判定以 `allowed` 为准。

`allowed=false` 时 `reason` 给出原因（`API Key无效`、`API Key已停用`、`API Key可用额度不足`、
`单一资金源可用额度不足，需完整预占 0.01`、`AI并发数已达上限(100)`、
`AI并发上限为0，用户已被禁用AI访问`、`request_id已存在，无法重复预占` 等），
CLIProxyAPI 应拒绝该请求，建议映射：余额/额度类 → 402，Key 无效 → 401/403，
并发超限/禁用 → 429。

幂等语义：同一 request_id 重复调用时，若账单处于 reserved / pending_settlement /
success / partial 状态则回放既有预占结果（allowed=true）；终态释放后的重复调用会被拒绝。

### 请求释放接口（已移除）

请求被拒绝或未产生任何用量时主动释放预占（幂等，仅 reserved 状态生效；
已产生 usage 的请求无需调用，结算会自动释放多余冻结）：

```json
{ "request_id": "1a2b3c4d", "reason": "rejected_by_gate" }
```

响应：`{ "request_id": "1a2b3c4d", "released": true }`

### POST /aigate/billing/check

余额/订阅只读校验，不产生冻结，可用于诊断或预检：

```json
{ "api_key": "sk-xxxx" }
```

响应：`allowed`、`user_id`、`key_id`、`billing_preference`、`wallet{balance,
frozen_balance,available_balance}`、`subscription{subscription_id,plan_title,
amount_total,amount_used,frozen_balance,available_amount}`（amount_total=0 表示不限量）、
`concurrency{limit,active}`（AI并发上限与当前在途占用，只读参考，权威判定在 reserve）、
`key{unlimited_balance,remain_balance,frozen_balance,available_balance}`。
Key 级配额已下线，`key` 子对象为兼容旧插件保留结构，恒返回
`unlimited_balance=true, remain_balance=0, frozen_balance=0, available_balance=null`。

## 鉴权失败

- Token 未配置：HTTP 403
- Token 不匹配：HTTP 401

## CLIProxyAPI 侧要求

1. `usage-statistics-enabled: true`，配置 `remote-management.secret-key`（同时是
   RESP usage 通道的 AUTH 密码）。
2. 插件在 `request.intercept_before` 中调用 reserve：
   - 从上下文取 TraceID 作为 request_id，取入站 api_key；
   - `allowed=false` 时 `Terminate: true` 并按原因返回 402/403；
   - HTTP 超时建议 2~3 秒：预占接口不可用时是否放行由部署者自行决断
     （默认拒绝可避免免费漏单，放行则退化为纯日志模式）。
3. 插件在请求被网关拒绝、未产生任何执行时调用 release 提前释放；
   请求已执行过（可能有 usage）时不要调用 release，交由结算处理。
4. 结算数据完全来自 Redis usage，插件无需上报费用。

## 本项目侧配置

| 配置 | 默认 | 说明 |
|------|------|------|
| `cpa.billing.api-token` | 空 | 公开计费 API 的鉴权 Token（X-Billing-Token 头）；为空则拒绝所有请求 |
| `cpa.billing.settle-delay-seconds` | 5 | 延迟结算窗口，需大于凭据重试的最大间隔 |
| `cpa.billing.settlement-worker-enabled` | true | 异步结算 Worker 开关 |
| `cpa.billing.settlement-max-retries` | 10 | 自动重试上限，超限进入"结算异常"人工处置 |
| sys_config `ai.billing.minimumAmount` | 0.001 | 请求级最低计费（结算边界应用） |

## 用户并发限制

- 预占在冻结前执行"并发占位"：`sys_user.active_request_count` 条件自增
  （`active_request_count < ai_concurrency_limit`），影响 0 行即拒绝；上限为 0 视为
  禁用该用户 AI 访问。计数列与账单状态机同事务原子维护，在途并发恒等于该用户
  `status='reserved'` 的账单数。
- 槽位释放时机：请求完成后由结算 Worker 领取转 `pending_settlement` 时释放，即约
  "完成时间 + settle-delay（默认 5s）+ Worker 轮询间隔"后可复用；方向保守，不会超发。
- CPA 崩溃导致既无 usage 也无 release 的陈旧槽位，由预占超时释放兜底
  请求不再创建冻结槽位，因此无需预占超时清理。
- 上限按用户配置：管理端"余额管理 → 并发限制"（`sys_user.ai_concurrency_limit`，0～1000）。
- 每分钟补偿任务附带并发计数对账：计数列与 reserved 账单数不一致时输出 error 告警
  （仅告警不自动修复，用于发现状态出口遗漏递减的缺陷）。

## 运维

- 结算失败任务：管理端“结算异常”页（前端路由 `/aigate/billing-settlement`，查询接口
  `/aigate/billing/settlement/failed/list`），支持“重新结算”（按账单金额正常扣款）与
  “人工核销”（释放全部冻结、放弃追收）。旧库若看不到菜单，先执行
  `sql/migration/20260918_restore_billing_settlement_menu.sql`。
- 对账兜底：`pending_settlement` 无任务（含旧系统遗留）与"延迟窗口后 reserved 且
  已有日志但无任务"的账单，由补偿任务自动补建结算任务。
- 账单查询：日志详情页"账单"数据来自 `/aigate/billing/records/by-log/{logId}`。

## 状态机（ai_billing_record.status）

```
processing → reserved → pending_settlement → success | partial
                   ├→ released（主动释放）   └→ written_off（人工核销，自 pending/失败重试）
                   ├→ expired（超时释放，包含 pending_settlement 的失败/卡住任务）
                   └→ failed
```

所有状态迁移均通过 `where status = 旧值` 的 CAS 完成；账单以 request_id 全局唯一。
