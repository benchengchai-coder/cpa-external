import type { BaseEntity, PageDomain } from "./common";

export type AiStatus = "0" | "1" | "2";
export type AiSwitch = 0 | 1;
export type AiPlatform = "openai" | "azure" | "anthropic" | "gemini" | "deepseek" | "ollama" | "custom" | "zhipu" | "aliyun";

export interface AiPlatformRecordQueryParams extends PageDomain {
  platformVal?: string;
  platformName?: string;
  status?: AiStatus;
}

export interface AiPlatformRecord {
  platformId?: number;
  platformVal?: string;
  platformName?: string;
  status?: AiStatus;
  remark?: string;
}

export interface AiModelQueryParams extends PageDomain {
  modelName?: string;
  platform?: AiPlatform;
  status?: AiStatus;
}

export interface AiModel extends BaseEntity {
  modelId?: number;
  modelName?: string;
  description?: string;
  icon?: string;
  platform?: AiPlatform;
  officialInputPrice?: number;
  officialOutputPrice?: number;
  officialCacheReadPrice?: number;
  officialCacheWritePrice?: number;
  status?: AiStatus;
}

export interface AiApiKeyQueryParams extends PageDomain {
  userId?: number;
  keyName?: string;
  status?: AiStatus;
}

/** 列表行的 CPA 同步状态标记（后端与 CLIProxyAPI 实时对比，列表场景走短缓存） */
export type AiApiKeyCpaSyncStatus = "synced" | "missing" | "residual" | "unknown";

export interface AiApiKey extends BaseEntity {
  keyId?: number;
  userId?: number;
  keyName?: string;
  apiKey?: string;
  status?: AiStatus;
  accessedTime?: string;
  cpaSyncStatus?: AiApiKeyCpaSyncStatus;
}

/** 平台与 CLIProxyAPI 的 api-keys 实时同步差异条目（不含明文） */
export interface AiApiKeySyncDiffItem {
  keyId?: number;
  keyName?: string;
  userId?: number;
  status?: AiStatus;
}

/** 平台与 CLIProxyAPI 的 api-keys 实时同步状态 */
export interface AiApiKeySyncStatus {
  pushEnabled?: boolean;
  cpaKeyCount?: number;
  platformKeyCount?: number;
  syncedCount?: number;
  missingInCpa?: AiApiKeySyncDiffItem[];
  disabledResidual?: AiApiKeySyncDiffItem[];
  orphanInCpa?: string[];
}

export interface AiApiKeySecret {
  keyId?: number;
  keyName?: string;
  apiKey: string;
}

export interface AiApiKeyModelQueryParams extends PageDomain {
  keyId?: number;
  modelId?: number;
}

export interface AiApiKeyModel extends BaseEntity {
  id?: number;
  keyId?: number;
  modelId?: number;
}

export interface AiLogQueryParams extends PageDomain {
  requestId?: string;
  username?: string;
  keyId?: number;
  provider?: string;
  modelName?: string;
  /** 是否失败筛选；不传查全部（status 是响应兼容字段，不作为查询条件） */
  failed?: boolean;
  channelId?: number;
}

/** AI 日志失败详情（CLIProxyAPI fail 对象） */
export interface AiLogFailDetail {
  status_code?: number;
  body?: string;
}

/** CLIProxyAPI 上游失败事件查询参数（errors 通道事件，仅实时推送无补收） */
export interface UpstreamFailureQueryParams extends PageDomain {
  provider?: string;
  model?: string;
  authId?: string;
  authIndex?: string;
  statusCode?: number;
  retryable?: boolean;
  authUnavailable?: boolean;
  quotaExceeded?: boolean;
}

/** 失败事件携带的配额状态快照 */
export interface UpstreamFailureQuotaStatus {
  exceeded?: boolean;
  reason?: string;
  next_recover_at?: string;
  backoff_level?: number;
}

/** 失败事件中的模型级凭证状态（CLIProxyAPI 按模型独立冷却时存在） */
export interface UpstreamFailureModelStatus {
  name?: string;
  status?: string;
  status_message?: string;
  unavailable?: boolean;
  next_retry_after?: string;
  quota?: UpstreamFailureQuotaStatus;
}

/** 失败事件携带的凭证状态快照（auth_status_snapshot JSON 列） */
export interface UpstreamFailureAuthStatusSnapshot {
  status?: string;
  status_message?: string;
  disabled?: boolean;
  unavailable?: boolean;
  next_retry_after?: string;
  quota?: UpstreamFailureQuotaStatus;
  model?: UpstreamFailureModelStatus;
}

/** CLIProxyAPI 上游失败事件记录（ai_upstream_failure 表） */
export interface UpstreamFailureRecord extends BaseEntity {
  failureId?: number;
  eventTime?: string;
  provider?: string;
  model?: string;
  authId?: string;
  authIndex?: string;
  statusCode?: number;
  body?: string;
  code?: string;
  retryable?: boolean;
  authStatus?: string;
  authDisabled?: boolean;
  authUnavailable?: boolean;
  authNextRetryAt?: string;
  quotaExceeded?: boolean;
  quotaReason?: string;
  /** 后端以原始 JSON 对象输出（@JsonRawValue） */
  authStatusSnapshot?: UpstreamFailureAuthStatusSnapshot;
}

/** AI 日志 Token 计费明细（CLIProxyAPI token_breakdown 对象） */
export interface AiLogTokenBreakdown {
  schema_version?: number;
  quality?: string;
  total_tokens?: number;
  input?: {
    total_tokens?: number;
    uncached_tokens?: number;
    cache_read_tokens?: number;
    cache_write_tokens?: number;
  };
  output?: {
    total_tokens?: number;
    non_reasoning_tokens?: number;
    reasoning_tokens?: number;
  };
  unclassified_tokens?: number;
}

export interface AiLog extends BaseEntity {
  logId?: number;
  requestId?: string;
  upstreamRequestId?: string;
  userId?: number;
  username?: string;
  keyId?: number;
  keyName?: string;
  type?: number;
  /** 中继模式：CHAT_COMPLETIONS / RESPONSES / RESPONSES_COMPACT */
  relayMode?: string;
  modelName?: string;
  isStream?: AiSwitch;
  /** 推理等级：minimal / low / medium / high */
  reasoningEffort?: string;
  channelId?: number;
  channelName?: string;
  ip?: string;
  promptTokens?: number;
  completionTokens?: number;
  cacheReadTokens?: number;
  cacheWriteTokens?: number;
  reasoningOutputTokens?: number;
  cost?: number;
  /** 入库时用户计费倍率快照，未归属用户时为空 */
  billingMultiplier?: number;
  requestTime?: string;
  duration?: number;
  /** 首次成功写入客户端耗时（毫秒） */
  ttft?: number;
  status?: AiStatus;
  errorMessage?: string;
  timestamp?: string;
  latencyMs?: number;
  source?: string;
  authIndex?: string;
  inputTokens?: number;
  outputTokens?: number;
  reasoningTokens?: number;
  cachedTokens?: number;
  totalTokens?: number;
  failed?: boolean;
  provider?: string;
  model?: string;
  alias?: string;
  endpoint?: string;
  authType?: string;
  apiKey?: string;
  responseHeaders?: Record<string, string[]>;
  ttftMs?: number;
  accessTokenSha256?: string;
  clientIp?: string;
  xForwardedFor?: string;
  userAgent?: string;
  generate?: boolean;
  stream?: boolean;
  cacheCreationTokens?: number;
  cacheReadTokensPresent?: boolean;
  fail?: AiLogFailDetail;
  accountingVersion?: number;
  tokenBreakdown?: AiLogTokenBreakdown;
  executorType?: string;
  sessionId?: string;
  parentSessionId?: string;
  serviceTier?: string;
  responseServiceTier?: string;
}

export interface AiBillingRecordDetail {
  billingId?: number;
  requestId?: string;
  logId?: number;
  subscriptionId?: number;
  subscriptionPlanTitle?: string;
  /** 应计金额 */
  amount?: number;
  /** 钱包与订阅合计实扣 */
  userChargedAmount?: number;
  walletChargedAmount?: number;
  subscriptionChargedAmount?: number;
  /** API Key额度扣减，不计入用户资金实扣 */
  keyChargedAmount?: number;
  uncoveredAmount?: number;
  billingSource?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface AiBillingSettlementFailedQuery extends PageDomain {
  requestId?: string;
  username?: string;
}

export interface AiBillingSettlementFailed {
  taskId: number;
  billingId: number;
  requestId: string;
  userId: number;
  username?: string;
  keyId?: number;
  keyName?: string;
  amount: number;
  walletReservedAmount: number;
  subscriptionReservedAmount: number;
  keyReservedAmount: number;
  retryCount: number;
  errorMessage?: string;
  failedTime?: string;
}

export interface AiLogUserSummary {
  userId?: number;
  username?: string;
  nickName?: string;
  email?: string;
  remark?: string;
  status?: AiStatus;
  deptName?: string;
  createTime?: string;
  loginDate?: string;
  balance?: number;
  frozenBalance?: number;
  availableBalance?: number;
  usedBalance?: number;
  totalChargedAmount?: number;
  totalUncoveredAmount?: number;
  requestCount?: number;
  billingPreference?: AiBillingPreference;
  aiConcurrencyLimit?: number;
  subscriptions: AiUserSubscription[];
}

export interface AiWalletInfo {
  balance: number;
  frozenBalance: number;
  availableBalance: number;
  usedBalance: number;
  totalChargedAmount: number;
  totalUncoveredAmount: number;
  requestCount: number;
  aiConcurrencyLimit?: number;
  /** AI在途并发占用（等于reserved状态账单数，随刷新更新） */
  activeRequestCount?: number;
  billingMultiplier?: number;
}

export interface AiRedemptionCodeQueryParams extends PageDomain {
  codeName?: string;
  redemptionKey?: string;
  status?: AiStatus;
  usedUserId?: number;
}

export interface AiRedemptionCode extends BaseEntity {
  codeId?: number;
  codeName?: string;
  quota?: number;
  redemptionKey?: string;
  status?: AiStatus;
  expiredTime?: string;
  usedUserId?: number;
  usedUsername?: string;
  redeemedTime?: string;
  batchCount?: number;
}

export interface AiRechargeRecord extends BaseEntity {
  recordId?: number;
  userId?: number;
  username?: string;
  type?: string; // '1'兑换码 '2'在线支付 '3'管理员调整 '4'新用户注册 '5'每日签到 '6'邀请返利
  amount?: number;
  sourceId?: number;
  sourceName?: string;
  status?: string; // '0'成功 '1'失败
}

export interface CheckinCalendar {
  enabled: boolean;
  month: string;
  today: string;
  signedDates: string[];
  todaySigned: boolean;
}

export interface CheckinResult {
  checkinDate: string;
  rewardAmount: number;
  balance: number;
  signedDates: string[];
  todaySigned: boolean;
}

export type AiBillingPreference = "subscription_first" | "wallet_first" | "subscription_only" | "wallet_only";

export interface AiSubscriptionPlanQueryParams extends PageDomain {
  title?: string;
  status?: AiStatus;
  allowBalancePurchase?: AiSwitch;
}

export interface AiSubscriptionPlan extends BaseEntity {
  planId?: number;
  title?: string;
  subTitle?: string;
  priceAmount?: number;
  durationUnit?: "day" | "week" | "month" | "year" | "custom";
  durationValue?: number;
  customSeconds?: number;
  amountTotal?: number;
  quotaResetPeriod?: "none" | "day" | "week" | "month" | "custom";
  quotaResetCustomSeconds?: number;
  status?: AiStatus;
  sortOrder?: number;
  maxPurchasePerUser?: number;
  allowBalancePurchase?: AiSwitch;
}

export interface AiUserSubscription extends BaseEntity {
  subscriptionId?: number;
  userId?: number;
  username?: string;
  planId?: number;
  planTitle?: string;
  planSubTitle?: string;
  priceAmount?: number;
  startTime?: string;
  endTime?: string;
  status?: "active" | "cancelled" | "expired";
  amountTotal?: number;
  amountUsed?: number;
  frozenBalance?: number;
  availableAmount?: number | null;
  quotaResetPeriod?: string;
  quotaResetCustomSeconds?: number;
  lastResetTime?: string;
  nextResetTime?: string;
  sourceType?: "grant" | "balance_purchase" | "register_trial";
}

export interface AiSubscriptionRecord extends BaseEntity {
  recordId?: number;
  userId?: number;
  username?: string;
  planId?: number;
  planTitle?: string;
  userSubscriptionId?: number;
  type?: string;
  amount?: number;
  sourceName?: string;
  operatorName?: string;
  status?: string;
}

export interface AiSubscriptionSelf {
  billingPreference: AiBillingPreference;
  subscriptions: AiUserSubscription[];
}

// ========== Dashboard 仪表盘 ==========

export type DashboardTrendRange = "today" | "7" | "30";
export type DashboardRankRange = DashboardTrendRange | "90" | "all";

export interface DashboardOverview {
  channelCount: number;
  modelCount: number;
  userCount: number;
  todayNewUsers: number;
  apiKeyCount: number;
  todayRequests: number;
  requestsPerMinute: number;
  todayTokens: number;
  totalTokens: number;
  todayCost: number;
  todayChargedAmount: number;
  failedSettlementCount: number;
  averageResponseTime: number;
  averageFirstTokenTime: number;
}

export interface DashboardTrend {
  date: string;
  requestCount: number;
  totalTokens: number;
  cost: number;
}

export interface DashboardUserRank {
  userId: number;
  username: string;
  requestCount: number;
  totalTokens: number;
  totalChargedAmount: number;
}

export interface UserDashboard {
  balance: number;
  frozenBalance: number;
  availableBalance: number;
  usedBalance: number;
  totalChargedAmount: number;
  totalUncoveredAmount: number;
  requestCount: number;
  todayRequests: number;
  totalTokens: number;
  apiKeyCount: number;
  averageResponseTime: number;
  averageFirstTokenTime: number;
  recentLogs: AiLog[];
}
