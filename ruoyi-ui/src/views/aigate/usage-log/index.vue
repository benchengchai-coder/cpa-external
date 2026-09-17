<template>
  <div class="app-container usage-log-page">
    <div class="toolbar">
      <div class="mb8 toolbar-row">
        <el-form
          :model="queryParams"
          ref="queryRef"
          :inline="true"
          v-show="showSearch"
          label-width="80px"
          class="usage-search-form"
        >
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
      <el-table-column label="请求时间" align="center" prop="requestTime" width="180">
        <template #default="scope">{{ formatRequestTime(scope.row.requestTime) }}</template>
      </el-table-column>
      <el-table-column label="端点" align="center" prop="relayMode" min-width="180" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-tag v-if="scope.row.relayMode" :type="relayModeTagType(scope.row.relayMode)" effect="plain">
            {{ relayModeLabel(scope.row.relayMode) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="客户端IP" align="center" prop="ip" width="150" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.ip || '-' }}</template>
      </el-table-column>
      <el-table-column label="模型" align="center" prop="modelName" width="250" :show-overflow-tooltip="true">
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
            {{ scope.row.status === '0' ? '成功' : '失败' }}</el-tag
          >
        </template>
      </el-table-column>
      <el-table-column label="耗时" align="center" width="120">
        <template #default="scope">
          <div class="latency-stack">
            <div class="latency-stack__row">
              <span class="latency-stack__label">首Token</span>
              <span class="latency-stack__value">{{ formatMilliseconds(scope.row.ttft) }}</span>
            </div>
            <div class="latency-stack__row">
              <span class="latency-stack__label">总耗时</span>
              <span class="latency-stack__value">{{ formatMilliseconds(scope.row.duration, true) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="Token用量" align="center" min-width="390">
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
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-drawer title="个人使用记录详情" v-model="detailOpen" direction="rtl" :size="drawerSize" append-to-body>
      <el-divider content-position="left">调用信息</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="请求ID">{{ detail.requestId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="API Key">{{ detail.keyName || detail.keyId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ detail.modelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提供方">{{ detail.channelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="端点">{{ detail.relayMode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.source || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认证索引">{{ detail.authIndex || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认证类型">{{ detail.authType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型别名">{{ detail.alias || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === '0' ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="Prompt Tokens">{{ detail.promptTokens ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="Completion Tokens">{{ detail.completionTokens ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="Reasoning Tokens">{{ detail.reasoningOutputTokens ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="Cache Read">{{ detail.cacheReadTokens ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="Total Tokens">{{ tokenTotalValue(detail) }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ formatMilliseconds(detail.duration, true) }}</el-descriptions-item>
        <el-descriptions-item label="首Token延迟">{{ formatMilliseconds(detail.ttft) }}</el-descriptions-item>
        <el-descriptions-item label="Cache Write">{{ detail.cacheWriteTokens ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="推理等级">{{ detail.reasoningEffort || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务层级">{{ detail.responseServiceTier || detail.serviceTier || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求时间">{{ formatRequestTime(detail.requestTime) }}</el-descriptions-item>
        <el-descriptions-item label="会话ID" :span="2">{{ detail.sessionId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">{{ detail.userAgent || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="UsageLog">
import { listUsageLog, getUsageLog } from '@/api/aigate/usageLog'
import { optionselectAiModel } from '@/api/aigate/model'
import { usePersistentAutoRefresh } from '@/composables/usePersistentAutoRefresh'
import type { AiLog, AiLogQueryParams, AiModel } from '@/types'
import {
  formatCacheHitRate,
  formatCurrency,
  formatMilliseconds,
  formatMultiplier,
  formatRequestTime,
  formatTokenCount,
  reasoningEffortLabel,
  reasoningEffortTagType,
  useAiDrawerSize
} from '../common'

const { proxy } = getCurrentInstance() as any

// 状态筛选直接绑定后端 failed 布尔字段；不传查全部（status 是响应兼容字段，不作为查询条件）
const logStatusOptions = [
  { label: '成功', value: false },
  { label: '失败', value: true }
]
const AUTO_REFRESH_INTERVAL = 5000

const drawerSize = useAiDrawerSize()
const modelOptions = ref<AiModel[]>([])
const logList = ref<AiLog[]>([])
const detail = ref<AiLog>({})
const detailOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    modelName: undefined,
    failed: undefined
  } as AiLogQueryParams
})

const { queryParams } = toRefs(data)

function tokenTotal(row: AiLog) {
  return formatTokenCount(tokenTotalValue(row))
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
      key: 'output',
      shortLabel: 'O',
      label: '输出 Token',
      value: formatTokenDetail(row.completionTokens),
      badgeClass: 'token-usage-panel__badge--output'
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

function relayModeLabel(mode: string): string {
  return RELAY_MODE_LABELS[mode] || mode
}

function relayModeTagType(mode: string): '' | 'success' | 'info' | 'warning' | 'danger' {
  return RELAY_MODE_TAG_TYPES[mode] || ''
}

function getList(options: { silent?: boolean } = {}) {
  const silent = options.silent === true
  if (!silent) {
    loading.value = true
  }
  return listUsageLog(queryParams.value).then((response) => {
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

function handleDetail(row: AiLog) {
  const logId = row.logId!
  getUsageLog(logId).then((logResponse) => {
    detail.value = logResponse.data || {}
    detailOpen.value = true
  })
}

const { autoRefresh, toggleAutoRefresh } = usePersistentAutoRefresh({
  storageKeyPrefix: 'aigate:usage-log:auto-refresh',
  intervalMs: AUTO_REFRESH_INTERVAL,
  refresh: () => getList({ silent: true })
})

optionselectAiModel().then((response) => {
  modelOptions.value = response.data || []
})
getList()
</script>

<style scoped>
:deep(.el-table .el-table__cell){
  padding: 0 ;
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

.usage-search-form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  row-gap: 8px;
}

.usage-search-form :deep(.el-form-item) {
  margin-right: 10px;
  margin-bottom: 0;
}

.usage-search-form :deep(.el-form-item:last-child) {
  margin-right: 0;
}

.usage-search-form :deep(.search-control) {
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

@media (max-width: 768px) {
  .toolbar-row {
    flex-wrap: wrap;
    row-gap: 8px;
  }

  .usage-search-form {
    flex: 1 0 100%;
    width: 100%;
    max-width: 100%;
  }

  .usage-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    margin-right: 0;
  }

  .usage-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .usage-search-form :deep(.search-control) {
    width: 100%;
  }

  .search-button-item :deep(.el-form-item__content) {
    width: 100%;
  }

  .usage-search-form :deep(.search-button-item) {
    flex: 1 0 100%;
    width: 100%;
    padding-left: 0;
  }

  .toolbar :deep(.top-right-btn) {
    width: 100%;
    justify-content: flex-end;
    box-sizing: border-box;
    margin-right: 0 !important;
    margin-left: 0;
  }
}

.latency-stack {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 4px 0;
  gap: 3px;
}

.latency-stack__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
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
  /* width: 400px; */
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
  min-width: 60px;
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
  background-color: #fff7e6;
  color: #fa8c16;
}

.token-usage__detail .token-usage__item:nth-child(3) .token-usage__label {
  background-color: #f6ffed;
  color: #52c41a;
}

.token-usage__detail .token-usage__item:nth-child(4) .token-usage__label {
  background-color: #f9f0ff;
  color: #722ed1;
}

.token-usage__detail .token-usage__item:nth-child(5) .token-usage__label {
  background-color: #f0f5ff;
  color: #2f54eb;
}

.token-usage__value {
  color: var(--el-text-color-regular);
  font-variant-numeric: tabular-nums;
}

.token-usage__total {
  display: flex;
  /* flex-direction: column; */
  align-items: center;
  /* justify-content: center; */
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

.token-usage-panel__badge--reasoning {
  background-color: #f9f0ff;
  color: #722ed1;
}

.token-usage-panel__badge--total {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}

/* 模型列与全局使用记录保持一致：左侧推理等级标签，右侧模型名 */
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

</style>
