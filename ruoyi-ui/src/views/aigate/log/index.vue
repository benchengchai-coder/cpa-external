<template>
  <div class="app-container log-page">
    <div class="toolbar">
      <div class="mb8 toolbar-row">
        <el-form
          :model="queryParams"
          ref="queryRef"
          :inline="true"
          v-show="showSearch"
          label-width="80px"
          class="log-search-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="queryParams.username"
              placeholder="请输入用户名"
              clearable
              class="search-control"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="供应商" prop="provider">
            <el-input
              v-model="queryParams.provider"
              placeholder="请输入供应商"
              clearable
              class="search-control"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="模型" prop="modelName">
            <el-select
              v-model="queryParams.modelName"
              placeholder="请选择模型"
              clearable
              filterable
              class="search-control"
            >
              <el-option v-for="item in modelOptions" :key="item.modelId" :label="item.modelName" :value="item.modelName" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="failed">
            <el-select v-model="queryParams.failed" placeholder="请选择状态" clearable class="search-control">
              <el-option v-for="item in logStatusOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item class="search-button-item">
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList">
          <el-tooltip :content="autoRefresh ? '关闭自动刷新' : '开启自动刷新，每5秒刷新一次'" placement="top">
            <el-button
              circle
              icon="SwitchButton"
              class="auto-refresh-button"
              :class="{ 'is-auto-refresh-active': autoRefresh }"
              :type="autoRefresh ? 'primary' : 'info'"
              :plain="!autoRefresh"
              :aria-label="autoRefresh ? '关闭自动刷新' : '开启自动刷新'"
              :aria-pressed="autoRefresh"
              @click="toggleAutoRefresh"
            />
          </el-tooltip>
        </right-toolbar>
      </div>
    </div>

    <el-table stripe v-loading="loading" :data="logList">
      <el-table-column label="供应商" align="center" prop="provider" width="150" :show-overflow-tooltip="true">
        <template #default="scope">{{ providerDisplayText(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="请求时间" align="center" prop="requestTime" width="180">
        <template #default="scope">{{ formatRequestTime(scope.row.requestTime) }}</template>
      </el-table-column>
      <el-table-column label="端点" align="center" prop="endpoint" width="220" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ endpointDisplayText(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户" align="center" prop="username" width="120" :show-overflow-tooltip="true">
        <template #default="scope">
          <span
            v-if="scope.row.userId && checkPermi(['aigate:log:query'])"
            class="user-link"
            @click="handleUserSummaryClick(scope.row)"
          >
            {{ scope.row.username || scope.row.userId }}
          </span>
          <span v-else>{{ scope.row.username || scope.row.userId || '-' }}</span>
        </template>
      </el-table-column>
    
      <el-table-column label="客户端IP" align="center" prop="ip" width="150" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.ip || '-' }}</template>
      </el-table-column>
      <el-table-column label="模型" align="center" prop="modelName" width="200" :show-overflow-tooltip="true">
        <template #default="scope">
          <div class="model-with-effort">
            <span class="model-with-effort__tag-slot">
              <el-tag
                v-if="scope.row.reasoningEffort"
                class="model-with-effort__tag"
                :type="reasoningEffortTagType(scope.row.reasoningEffort)"
                effect="plain"
                size="small"
              >
                {{ reasoningEffortLabel(scope.row.reasoningEffort) }}
              </el-tag>
            </span>
            <span class="model-with-effort__name">{{ scope.row.modelName || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用户倍率" align="center" prop="billingMultiplier" width="100">
        <template #default="scope">{{ formatMultiplier(scope.row.billingMultiplier) }}</template>
      </el-table-column>
      <el-table-column label="请求类型" align="center" prop="isStream" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.isStream === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.isStream === 1 ? '流式' : '非流式' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
            {{ scope.row.status === '0' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" align="center" width="120">
        <template #default="scope">
          <div class="latency-stack">
            <div class="latency-stack__row">
              <span class="latency-stack__label">首T</span>
              <span class="latency-stack__value">{{ formatMilliseconds(scope.row.ttft) }}</span>
            </div>
            <div class="latency-stack__row">
              <span class="latency-stack__label">总耗时</span>
              <span class="latency-stack__value">{{ formatMilliseconds(scope.row.duration, true) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Token用量" align="center" width="450">
        <template #default="scope">
          <el-popover
            trigger="hover"
            placement="right"
            width="280"
            popper-class="token-usage-popover"
            :show-after="80"
            :hide-after="0"
            transition=""
            :popper-options="{ modifiers: [{ name: 'flip', options: { fallbackPlacements: ['right', 'left'] } }] }"
          >
            <template #default>
              <div class="token-usage-panel">
                <div class="token-usage-panel__title">Token 用量详情</div>
                <div class="token-usage-panel__rows">
                  <div
                    v-for="item in tokenUsageDetailItems(scope.row)"
                    :key="item.key"
                    class="token-usage-panel__row"
                  >
                    <span class="token-usage-panel__label">
                      <span class="token-usage-panel__badge" :class="item.badgeClass">{{ item.shortLabel }}</span>
                      <span>{{ item.label }}</span>
                    </span>
                    <span class="token-usage-panel__value">{{ item.value }}</span>
                  </div>
                </div>
              </div>
            </template>
            <template #reference>
              <div class="token-usage">
                <div class="token-usage__detail">
                  <span class="token-usage__item">
                    <span class="token-usage__label">I</span>
                    <span class="token-usage__value">{{ formatTokens(scope.row.promptTokens) }}</span>
                  </span>
                  <span class="token-usage__item">
                    <span class="token-usage__label">CR</span>
                    <span class="token-usage__value">{{ formatTokens(scope.row.cacheReadTokens) }}</span>
                  </span>
                  <span class="token-usage__item">
                    <span class="token-usage__label">CW</span>
                    <span class="token-usage__value">{{ formatTokens(scope.row.cacheWriteTokens) }}</span>
                  </span>

                  <span class="token-usage__item">
                    <span class="token-usage__label">O</span>
                    <span class="token-usage__value">{{ formatTokens(scope.row.completionTokens) }}</span>
                  </span>
                  <span class="token-usage__item">
                    <span class="token-usage__label">RO</span>
                    <span class="token-usage__value">{{ formatTokens(scope.row.reasoningOutputTokens) }}</span>
                  </span>
                  <span class="token-usage__item">
                    <span class="token-usage__label">HR</span>
                    <span class="token-usage__value">{{
                      formatCacheHitRate(scope.row.cacheReadTokens, scope.row.promptTokens)
                    }}</span>
                  </span>
                </div>
                <div class="token-usage__total">
                  <span class="token-usage__label">Total</span>
                  <span class="token-usage__total-value">{{ tokenTotal(scope.row) }}</span>
                </div>
              </div>
            </template>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="计算费用" align="center" prop="cost" min-width="100">
        <template #default="scope">{{ formatCurrency(scope.row.cost, 6) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['aigate:log:query']"
            >详情</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <AiUserSummaryDialog ref="userSummaryDialogRef" />

    <el-dialog
      v-model="detailOpen"
      width="min(1080px, 94vw)"
      top="4vh"
      append-to-body
      destroy-on-close
      class="log-detail-dialog"
    >
      <template #header>
        <div class="log-detail-dialog__header">
          <div class="log-detail-dialog__heading">
            <div class="log-detail-dialog__title">全局使用记录详情</div>
            <div class="log-detail-dialog__subtitle">
              <span>{{ detail.model || detail.modelName || '未知模型' }}</span>
              <span class="log-detail-dialog__separator"></span>
              <span>{{ formatRequestTime(detail.requestTime) }}</span>
            </div>
          </div>
          <el-tag :type="detail.status === '0' ? 'success' : 'danger'" effect="light" round>
            {{ detail.status === '0' ? '调用成功' : '调用失败' }}
          </el-tag>
        </div>
      </template>
      <div class="log-detail-dialog__body">
        <section class="detail-section">
          <div class="detail-section__header">
            <span class="detail-section__title">调用概览</span>
          </div>
          <div class="detail-info-grid">
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">请求 ID</span>
              <span class="detail-field__value detail-field__value--mono">{{ detail.requestId || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">API Key</span>
              <span class="detail-field__value">{{ detail.apiKey || detail.keyName || detail.keyId || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">模型</span>
              <span class="detail-field__value">{{ detail.modelName || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">端点</span>
              <span class="detail-field__value">
                <span>{{ detail.endpoint || detail.relayMode || '-' }}</span>
              </span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">调用类型</span>
              <span class="detail-field__value">{{ requestTypeLabel(detail.type) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">请求方式</span>
              <span class="detail-field__value">{{ detail.isStream === 1 ? '流式' : '非流式' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">推理等级</span>
              <span class="detail-field__value">{{ reasoningEffortLabel(detail.reasoningEffort || '') || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">来源</span>
              <span class="detail-field__value">{{ detail.source || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">认证类型</span>
              <span class="detail-field__value">{{ detail.authType || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">认证索引</span>
              <span class="detail-field__value">{{ detail.authIndex || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">用户</span>
              <span class="detail-field__value">{{ detail.username || detail.userId || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">IP 地址</span>
              <span class="detail-field__value">{{ detail.ip || '-' }}</span>
            </div>
            <div class="detail-field detail-field--span-2">
              <span class="detail-field__label">供应商 / 别名</span>
              <span class="detail-field__value">{{ providerDisplayText(detail) }} / {{ detail.alias || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">用户倍率</span>
              <span class="detail-field__value">{{ formatMultiplier(detail.billingMultiplier) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">执行器</span>
              <span class="detail-field__value">{{ detail.executorType || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">服务层级</span>
              <span class="detail-field__value">{{ detail.responseServiceTier || detail.serviceTier || '-' }}</span>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">会话 ID</span>
              <span class="detail-field__value detail-field__value--mono">{{ detail.sessionId || '-' }}</span>
            </div>
            <div v-if="detail.parentSessionId" class="detail-field detail-field--full">
              <span class="detail-field__label">父会话 ID</span>
              <span class="detail-field__value detail-field__value--mono">{{ detail.parentSessionId }}</span>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">User-Agent</span>
              <span class="detail-field__value">{{ detail.userAgent || '-' }}</span>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">X-Forwarded-For</span>
              <span class="detail-field__value">{{ detail.xForwardedFor || '-' }}</span>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <div class="detail-section__header">
            <span class="detail-section__title">Token 用量</span>
            <span class="detail-section__summary">
              输入 + 输出 <strong>{{ formatTokenDetail(tokenTotalValue(detail)) }}</strong>
            </span>
          </div>
          <div class="token-metric-grid">
            <div class="token-metric token-metric--prompt">
              <span class="token-metric__label">输入 Token</span>
              <strong class="token-metric__value">{{ formatTokenDetail(detail.promptTokens) }}</strong>
            </div>
            <div class="token-metric token-metric--completion">
              <span class="token-metric__label">输出 Token</span>
              <strong class="token-metric__value">{{ formatTokenDetail(detail.completionTokens) }}</strong>
            </div>
            <div class="token-metric token-metric--reasoning">
              <span class="token-metric__label">推理 Token</span>
              <strong class="token-metric__value">{{ formatTokenDetail(detail.reasoningOutputTokens) }}</strong>
            </div>
            <div class="token-metric token-metric--cache-read">
              <span class="token-metric__label">缓存命中 Token</span>
              <strong class="token-metric__value">{{ formatTokenDetail(detail.cacheReadTokens) }}</strong>
            </div>
            <div class="token-metric token-metric--cache-write">
              <span class="token-metric__label">缓存写入 Token</span>
              <strong class="token-metric__value">{{ formatTokenDetail(detail.cacheWriteTokens) }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <div class="detail-section__header">
            <span class="detail-section__title">性能与费用</span>
          </div>
          <div class="performance-grid">
            <div class="performance-metric performance-metric--cost">
              <span class="performance-metric__label">计算费用</span>
              <strong class="performance-metric__value">{{ formatCurrency(detail.cost, 6, true) }}</strong>
            </div>
            <div class="performance-metric">
              <span class="performance-metric__label">总耗时</span>
              <strong class="performance-metric__value">{{ formatMilliseconds(detail.duration, true) }}</strong>
            </div>
            <div class="performance-metric">
              <span class="performance-metric__label">首 Token</span>
              <strong class="performance-metric__value">{{ formatMilliseconds(detail.ttft) }}</strong>
            </div>
          </div>
        </section>

        <section v-if="detail.errorMessage" class="detail-section detail-section--error">
          <div class="detail-section__header">
            <span class="detail-section__title">错误信息</span>
          </div>
          <pre class="detail-error-message">{{ formatErrorMessage(detail.errorMessage) }}</pre>
        </section>

        <section v-if="hasFailDetail(detail.fail)" class="detail-section detail-section--error">
          <div class="detail-section__header">
            <span class="detail-section__title">失败详情</span>
          </div>
          <pre class="detail-error-message">{{ failSummary(detail.fail) }}</pre>
        </section>

        <section v-if="hasTokenBreakdown(detail.tokenBreakdown)" class="detail-section">
          <div class="detail-section__header">
            <span class="detail-section__title">Token 计费明细</span>
            <span class="detail-section__summary">
              v{{ detail.tokenBreakdown?.schema_version }} · {{ detail.tokenBreakdown?.quality || '-' }}
            </span>
          </div>
          <pre class="detail-error-message">{{ prettyJson(detail.tokenBreakdown) }}</pre>
        </section>

        <section class="detail-section">
          <div class="detail-section__header">
            <span class="detail-section__title">账单信息</span>
          </div>
          <div v-if="!billingDetail" class="billing-empty">
            <span class="billing-empty__indicator"></span>
            <span>本次调用未形成账单</span>
          </div>
          <div v-else class="detail-info-grid detail-info-grid--billing">
            <div class="detail-field">
              <span class="detail-field__label">账单状态</span>
              <span class="detail-field__value">
                <el-tag :type="billingStatusTagType(billingDetail.status)" effect="plain" size="small">
                  {{ billingStatusLabel(billingDetail.status) }}
                </el-tag>
              </span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">计费模式</span>
              <span class="detail-field__value">
                <el-tag :type="billingModeTagType(billingDetail.billingSource)" effect="plain" size="small">
                  {{ billingModeLabel(billingDetail.billingSource) }}
                </el-tag>
              </span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">结算时间</span>
              <span class="detail-field__value">{{ parseTime(billingDetail.updateTime) || '-' }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">应计金额</span>
              <span class="detail-field__value">{{ formatCurrency(billingDetail.amount, 6, true) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">实际资金扣款</span>
              <span class="detail-field__value">{{ formatCurrency(billingDetail.userChargedAmount, 6, true) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">未覆盖金额</span>
              <span class="detail-field__value">{{ formatCurrency(billingDetail.uncoveredAmount, 6, true) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">钱包实扣</span>
              <span class="detail-field__value">{{ formatCurrency(billingDetail.walletChargedAmount, 6, true) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">订阅实扣</span>
              <span class="detail-field__value">{{ formatCurrency(billingDetail.subscriptionChargedAmount, 6, true) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">API Key 额度扣减</span>
              <span class="detail-field__value">{{ formatCurrency(billingDetail.keyChargedAmount, 6, true) }}</span>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">订阅套餐</span>
              <span class="detail-field__value">{{ billingDetail.subscriptionPlanTitle || '-' }}</span>
            </div>
          </div>
        </section>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AiLog">
import { getAdminBillingRecordByLogId } from '@/api/aigate/billingRecord'
import { getAiLog, listAiLog } from '@/api/aigate/log'
import { optionselectAiModel } from '@/api/aigate/model'
import { usePersistentAutoRefresh } from '@/composables/usePersistentAutoRefresh'
import type {
  AiBillingRecordDetail,
  AiLog,
  AiLogFailDetail,
  AiLogQueryParams,
  AiLogTokenBreakdown,
  AiModel
} from '@/types'
import { checkPermi } from '@/utils/permission'
import AiUserSummaryDialog from '../components/AiUserSummaryDialog.vue'
import {
  billingModeLabel,
  billingModeTagType,
  billingStatusLabel,
  billingStatusTagType,
  formatCacheHitRate,
  formatCurrency,
  formatMilliseconds,
  formatMultiplier,
  formatRequestTime,
  formatTokenCount,
  reasoningEffortLabel,
  reasoningEffortTagType
} from '../common'

const { proxy } = getCurrentInstance() as any

// 状态筛选直接绑定后端 failed 布尔字段；不传查全部（status 是响应兼容字段，不作为查询条件）
const logStatusOptions = [
  { label: '成功', value: false },
  { label: '失败', value: true }
]
const AUTO_REFRESH_INTERVAL = 5000

const modelOptions = ref<AiModel[]>([])
const logList = ref<AiLog[]>([])
const detail = ref<AiLog>({})
const billingDetail = ref<AiBillingRecordDetail | null>(null)
const detailOpen = ref(false)
const userSummaryDialogRef = ref<InstanceType<typeof AiUserSummaryDialog> | null>(null)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    username: undefined,
    provider: undefined,
    modelName: undefined,
    failed: undefined
  } as AiLogQueryParams
})

const { queryParams } = toRefs(data)

function tokenTotal(row: AiLog) {
  return formatTokenCount(row.totalTokens ?? (row.promptTokens || 0) + (row.completionTokens || 0))
}

function tokenTotalValue(row: AiLog) {
  return row.totalTokens ?? (row.promptTokens || 0) + (row.completionTokens || 0)
}

function formatTokens(value: number | undefined): string {
  return formatTokenCount(value)
}

function formatTokenDetail(value: number | undefined): string {
  const tokens = Number(value || 0)
  if (!Number.isFinite(tokens)) {
    return '0'
  }
  return tokens.toLocaleString('en-US')
}

function formatErrorMessage(value: string | undefined): string {
  if (!value) {
    return '-'
  }

  const bodyMarker = 'body='
  const bodyIndex = value.indexOf(bodyMarker)
  if (bodyIndex >= 0) {
    const prefix = value.slice(0, bodyIndex + bodyMarker.length)
    const body = value.slice(bodyIndex + bodyMarker.length).trim()
    try {
      return `${prefix}\n${JSON.stringify(JSON.parse(body), null, 2)}`
    } catch {
      return value
    }
  }

  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function hasFailDetail(fail?: AiLogFailDetail): boolean {
  if (!fail) {
    return false
  }
  return (fail.status_code != null && fail.status_code >= 400) || !!fail.body
}

function failSummary(fail?: AiLogFailDetail): string {
  if (!fail) {
    return ''
  }
  const lines: string[] = []
  if (fail.status_code != null) {
    lines.push(`状态码：${fail.status_code}`)
  }
  if (fail.body) {
    lines.push(fail.body)
  }
  return lines.join('\n')
}

function hasTokenBreakdown(breakdown?: AiLogTokenBreakdown): boolean {
  return breakdown != null && breakdown.total_tokens != null
}

function prettyJson(value: unknown): string {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function tokenUsageDetailItems(row: AiLog) {
  return [
    {
      key: 'input',
      shortLabel: 'I',
      label: '输入 Token',
      value: formatTokenDetail(row.promptTokens),
      badgeClass: 'token-usage-panel__badge--input'
    },
    {
      key: 'inputCache',
      shortLabel: 'CR',
      label: '缓存命中 Token',
      value: formatTokenDetail(row.cacheReadTokens),
      badgeClass: 'token-usage-panel__badge--input-cache'
    },
    {
      key: 'cacheHitRate',
      shortLabel: 'HR',
      label: '缓存命中率',
      value: formatCacheHitRate(row.cacheReadTokens, row.promptTokens),
      badgeClass: 'token-usage-panel__badge--hit-rate'
    },
       
    {
      key: 'cacheWrite',
      shortLabel: 'CW',
      label: '缓存写入 Token',
      value: formatTokenDetail(row.cacheWriteTokens),
      badgeClass: 'token-usage-panel__badge--output-cache'
    },
    {
      key: 'output',
      shortLabel: 'O',
      label: '输出 Token',
      value: formatTokenDetail(row.completionTokens),
      badgeClass: 'token-usage-panel__badge--output'
    },

    {
      key: 'reasoning',
      shortLabel: 'RO',
      label: '推理 Token',
      value: formatTokenDetail(row.reasoningOutputTokens),
      badgeClass: 'token-usage-panel__badge--reasoning'
    },
    {
      key: 'total',
      shortLabel: 'T',
      label: 'Total',
      value: formatTokenDetail(tokenTotalValue(row)),
      badgeClass: 'token-usage-panel__badge--total'
    }
  ]
}

const RELAY_MODE_LABELS: Record<string, string> = {
  CHAT_COMPLETIONS: 'Chat Completions',
  RESPONSES: 'Responses',
  RESPONSES_COMPACT: 'Responses Compact'
}

const RELAY_MODE_TAG_TYPES: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  CHAT_COMPLETIONS: 'success',
  RESPONSES: 'info',
  RESPONSES_COMPACT: 'warning'
}

const REQUEST_TYPE_LABELS: Record<number, string> = {
  0: '聊天',
  1: '聊天',
  2: '补全',
  3: '向量',
  4: '图片',
  5: '音频',
  6: '视频'
}

function requestTypeLabel(type: number | undefined): string {
  if (type === undefined || type === null) {
    return '-'
  }
  return REQUEST_TYPE_LABELS[type] || String(type)
}

function relayModeLabel(mode: string): string {
  return RELAY_MODE_LABELS[mode] || mode
}

function relayModeTagType(mode: string): '' | 'success' | 'info' | 'warning' | 'danger' {
  return RELAY_MODE_TAG_TYPES[mode] || ''
}

function providerDisplayText(row: AiLog): string {
  const authType = (row.authType || '').toLowerCase().replace(/[\s_-]/g, '')
  if (authType.includes('oauth')) {
    return row.source || '-'
  }
  // CLIProxyAPI 对 openai-compatibility 配置段的自定义供应商，上报的 provider 是「openai-compatible-名称」内部 key，展示时剥掉前缀
  const provider = row.provider || ''
  const prefix = 'openai-compatible-'
  if (provider.toLowerCase().startsWith(prefix)) {
    const name = provider.slice(prefix.length)
    return name || provider
  }
  return provider || '-'
}

function endpointDisplayText(row: AiLog): string {
  const endpoint = row.endpoint || ''
  if (!endpoint) {
    return '-'
  }
  const methodSeparatorIndex = endpoint.indexOf(' ')
  return methodSeparatorIndex >= 0 ? endpoint.slice(methodSeparatorIndex + 1) : endpoint
}

function getList(options: { silent?: boolean } = {}) {
  const silent = options.silent === true
  if (!silent) {
    loading.value = true
  }
  return listAiLog(queryParams.value).then((response) => {
    logList.value = response.rows
    total.value = response.total
  }).finally(() => {
    if (!silent) {
      loading.value = false
    }
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

let detailRequestSequence = 0

function handleDetail(row: AiLog) {
  const logId = row.logId!
  const currentRequest = ++detailRequestSequence
  getAiLog(logId).then((logResponse) => {
    if (currentRequest !== detailRequestSequence) {
      return
    }
    detail.value = logResponse.data || {}
    billingDetail.value = null
    detailOpen.value = true
    getAdminBillingRecordByLogId(logId)
      .then((billingResponse) => {
        if (currentRequest === detailRequestSequence) {
          billingDetail.value = billingResponse.data || null
        }
      })
      .catch(() => {
        // 账单查询失败时保持空态，不阻塞日志详情展示
        if (currentRequest === detailRequestSequence) {
          billingDetail.value = null
        }
      })
  })
}

function handleUserSummaryClick(row: AiLog): void {
  const selectedText = window.getSelection()?.toString().trim()
  if (selectedText) {
    return
  }
  if (!row.userId) {
    return
  }
  userSummaryDialogRef.value?.open(row.userId, row.username)
}

const { autoRefresh, toggleAutoRefresh } = usePersistentAutoRefresh({
  storageKeyPrefix: 'aigate:log:auto-refresh',
  intervalMs: AUTO_REFRESH_INTERVAL,
  refresh: () => getList({ silent: true })
})

optionselectAiModel().then((response) => {
  modelOptions.value = response.data || []
})
getList()
</script>

<style scoped>
:deep(.el-table) {
  --el-table-border: none;
}

:deep(.el-table .el-table__cell) {
  padding: 0;
}

.toolbar {
  --aigate-filter-width: 180px;
  padding: 0;
}

.toolbar-row {
  display: flex;
  width: 100%;
  align-items: flex-start;
}

.log-search-form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  row-gap: 8px;
}

.log-search-form :deep(.el-form-item) {
  margin-right: 10px;
  margin-bottom: 0;
}

.log-search-form :deep(.el-form-item:last-child) {
  margin-right: 0;
}

.log-search-form :deep(.search-control) {
  width: var(--aigate-filter-width);
}

.search-button-item :deep(.el-form-item__content) {
  display: flex;
  width: var(--aigate-filter-width);
  gap: 10px;
}

.search-button-item :deep(.el-button) {
  flex: 1;
  min-width: 0;
}

.search-button-item :deep(.el-button + .el-button) {
  margin-left: 0;
}

.toolbar :deep(.top-right-btn) {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh-button {
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.auto-refresh-button.is-auto-refresh-active {
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--el-color-primary) 24%, transparent);
}

:deep(.log-detail-dialog .el-dialog__header) {
  padding: 20px 24px 16px;
  margin-right: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

:deep(.log-detail-dialog .el-dialog__headerbtn) {
  top: 14px;
  right: 16px;
}

:deep(.log-detail-dialog .el-dialog__body) {
  padding: 16px 20px 20px;
}

:deep(.log-detail-dialog .el-dialog__footer) {
  padding: 14px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.log-detail-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  padding-right: 40px;
  gap: 16px;
}

.log-detail-dialog__heading {
  min-width: 0;
}

.log-detail-dialog__title {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.log-detail-dialog__subtitle {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
  gap: 8px;
}

.log-detail-dialog__separator {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
}

.log-detail-dialog__body {
  display: flex;
  flex-direction: column;
  max-height: calc(92vh - 150px);
  overflow-y: auto;
  padding-right: 4px;
  gap: 16px;
}

.detail-section {
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
}

.detail-section__header {
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-extra-light);
  gap: 12px;
}

.detail-section__title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.detail-section__summary {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.detail-section__summary strong {
  margin-left: 4px;
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.detail-info-grid {
  display: grid;
  padding: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 28px;
  row-gap: 18px;
}

.detail-field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
}

.detail-field--span-2 {
  grid-column: span 2;
}

.detail-field--full {
  grid-column: 1 / -1;
}

.detail-field__label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.3;
}

.detail-field__value {
  min-width: 0;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.detail-field__value--mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 13px;
  font-weight: 400;
  user-select: all;
}

.token-metric-grid {
  display: grid;
  padding: 14px 16px 16px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.token-metric {
  --token-metric-color: var(--el-color-primary);
  display: flex;
  min-width: 0;
  min-height: 82px;
  flex-direction: column;
  justify-content: space-between;
  padding: 13px 14px;
  border: 1px solid color-mix(in srgb, var(--token-metric-color) 24%, var(--el-border-color-lighter));
  border-radius: 8px;
  background: color-mix(in srgb, var(--token-metric-color) 7%, var(--el-bg-color));
  gap: 10px;
}

.token-metric--completion {
  --token-metric-color: var(--el-color-success);
}

.token-metric--reasoning {
  --token-metric-color: var(--el-color-warning);
}

.token-metric--cache-read {
  --token-metric-color: #8b5cf6;
}

.token-metric--cache-write {
  --token-metric-color: var(--el-color-info);
}

.token-metric__label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.35;
}

.token-metric__value {
  color: var(--token-metric-color);
  font-size: 22px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.performance-grid {
  display: grid;
  padding: 14px 16px 16px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.performance-metric {
  display: flex;
  min-width: 0;
  min-height: 72px;
  flex-direction: column;
  justify-content: center;
  padding: 12px 14px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  gap: 8px;
}

.performance-metric__label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.performance-metric__value {
  color: var(--el-text-color-primary);
  font-size: 17px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.performance-metric--cost .performance-metric__value {
  color: var(--el-color-success);
}

.detail-section--error {
  border-color: color-mix(in srgb, var(--el-color-danger) 34%, var(--el-border-color-lighter));
}

.detail-section--error .detail-section__header {
  background: color-mix(in srgb, var(--el-color-danger) 8%, var(--el-bg-color));
}

.detail-section--error .detail-section__title {
  color: var(--el-color-danger);
}

.detail-error-message {
  max-height: 260px;
  padding: 16px;
  margin: 0;
  overflow: auto;
  background: var(--el-fill-color-extra-light);
  color: var(--el-text-color-regular);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.65;
  overflow-wrap: anywhere;
  tab-size: 2;
  white-space: pre-wrap;
  word-break: break-word;
}

.billing-empty {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  gap: 10px;
}

.billing-empty__indicator {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-color-info);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--el-color-info) 14%, transparent);
}

@media (max-width: 920px) {
  .detail-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .token-metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .performance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  :deep(.log-detail-dialog .el-dialog__header) {
    padding: 16px 18px 14px;
  }

  :deep(.log-detail-dialog .el-dialog__body) {
    padding: 12px 14px 16px;
  }

  :deep(.log-detail-dialog .el-dialog__footer) {
    padding: 12px 14px;
  }

  .log-detail-dialog__header {
    align-items: flex-start;
  }

  .detail-info-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }

  .detail-field--span-2,
  .detail-field--full {
    grid-column: auto;
  }

  .token-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar-row {
    flex-wrap: wrap;
    row-gap: 8px;
  }

  .log-search-form {
    flex: 1 0 100%;
    width: 100%;
  }

  .log-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    margin-right: 0;
  }

  .log-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .log-search-form :deep(.search-control) {
    width: 100%;
  }

  .search-button-item :deep(.el-form-item__content) {
    width: 100%;
  }

  .toolbar :deep(.top-right-btn) {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }
}

@media (max-width: 520px) {
  .log-detail-dialog__subtitle {
    display: none;
  }

  .detail-section__summary {
    display: none;
  }

  .token-metric-grid,
  .performance-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.model-with-effort {
  display: inline-flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  gap: 6px;
}

.model-with-effort__tag-slot {
  display: inline-flex;
  flex: 0 0 50px;
  align-items: center;
  justify-content: center;
  width: 50px;
  min-width: 50px;
}

.model-with-effort__tag {
  justify-content: center;
  width: 50px;
  min-width: 50px;
  padding: 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-with-effort__name {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-regular);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.latency-stack {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
  gap: 3px;
}

.latency-stack__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  line-height: 1.3;
}

.latency-stack__label {
  color: var(--el-text-color-secondary);
  font-size: 11px;
  white-space: nowrap;
}

.latency-stack__value {
  color: var(--el-text-color-regular);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.token-usage {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 2px 0;
  cursor: default;
}

.token-usage__detail {
  display: grid;
  grid-template-columns: repeat(3, minmax(64px, 1fr));
  row-gap: 4px;
  column-gap: 10px;
  text-align: left;
}

.token-usage__item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  min-width: 70px;
}

.token-usage__label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.token-usage__detail .token-usage__item:nth-child(1) .token-usage__label {
  background-color: #e6f4ff;
  color: #1677ff;
}

.token-usage__detail .token-usage__item:nth-child(2) .token-usage__label {
  background-color: #f6ffed;
  color: #52c41a;
}

.token-usage__detail .token-usage__item:nth-child(3) .token-usage__label {
  background-color: #fff7e6;
  color: #fa8c16;
}

.token-usage__detail .token-usage__item:nth-child(4) .token-usage__label {
  background-color: #e6fffb;
  color: #13c2c2;
}

.token-usage__detail .token-usage__item:nth-child(5) .token-usage__label {
  background-color: #f9f0ff;
  color: #722ed1;
}

.user-link {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: var(--el-color-primary);
  cursor: pointer;
  text-overflow: ellipsis;
  user-select: text;
  vertical-align: middle;
  white-space: nowrap;
}

.user-link:hover {
  text-decoration: underline;
}


.token-usage__detail .token-usage__item:nth-child(6) .token-usage__label {
  background-color: #f0f5ff;
  color: #2f54eb;
}

.token-usage__value {
  color: var(--el-text-color-regular);
  font-variant-numeric: tabular-nums;
}

.token-usage__total {
  display: flex;
  align-items: center;
  border-left: 1px solid var(--el-border-color-light);
  padding-left: 12px;
  width: 100px;
  gap: 12px;
}

.token-usage__total-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

:global(.token-usage-popover) {
  padding: 0 !important;
}

.token-usage-panel {
  padding: 10px 12px;
}

.token-usage-panel__title {
  padding-bottom: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.token-usage-panel__rows {
  display: grid;
  gap: 7px;
}

.token-usage-panel__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-width: 0;
}

.token-usage-panel__label {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.token-usage-panel__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 18px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.token-usage-panel__value {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.token-usage-panel__badge--input {
  background-color: #e6f4ff;
  color: #1677ff;
}

.token-usage-panel__badge--output {
  background-color: #f6ffed;
  color: #52c41a;
}

.token-usage-panel__badge--input-cache {
  background-color: #fff7e6;
  color: #fa8c16;
}

.token-usage-panel__badge--hit-rate {
  background-color: #f0f5ff;
  color: #2f54eb;
}

.token-usage-panel__badge--output-cache {
  background-color: #e6fffb;
  color: #13c2c2;
}

.token-usage-panel__badge--reasoning {
  background-color: #f9f0ff;
  color: #722ed1;
}

.token-usage-panel__badge--total {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}
</style>
