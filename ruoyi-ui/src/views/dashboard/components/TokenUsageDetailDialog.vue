<template>
  <el-dialog
    v-model="visible"
    class="token-usage-dialog"
    title="Token 用量详情"
    width="min(640px, calc(100vw - 24px))"
    append-to-body
    destroy-on-close
  >
    <div class="token-detail-toolbar">
      <div class="token-detail-summary">
        <span>{{ rangeLabel }}合计</span>
        <strong>{{ formatTokenCount(rangeTotalTokens) }}</strong>
      </div>
      <el-segmented v-model="range" :options="rangeOptions" @change="fetchData" />
    </div>

    <el-table
      v-loading="loading"
      :data="dailyUsage"
      stripe
      max-height="min(480px, calc(100vh - 220px))"
      empty-text="暂无 Token 用量"
    >
      <el-table-column prop="date" label="日期" min-width="130" />
      <el-table-column prop="totalTokens" label="Token 合计" min-width="130" align="right">
        <template #default="{ row }">
          <span class="token-total-value">{{ formatTokenCount(row.totalTokens) }}</span>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { getUserTrend } from '@/api/aigate/dashboard'
import type { DashboardTrend, DashboardTrendRange } from '@/types'
import { formatTokenCount } from '../../aigate/common'

type TokenUsageRange = Exclude<DashboardTrendRange, 'today'>

const rangeOptions: Array<{ label: string; value: TokenUsageRange }> = [
  { label: '近7天', value: '7' },
  { label: '近30天', value: '30' }
]

const visible = ref(false)
const loading = ref(false)
const range = ref<TokenUsageRange>('30')
const trendData = ref<DashboardTrend[]>([])

const rangeLabel = computed(() => rangeOptions.find((item) => item.value === range.value)?.label || '')
const dailyUsage = computed(() => fillDailyUsage(trendData.value, Number(range.value)).reverse())
const rangeTotalTokens = computed(() => dailyUsage.value.reduce(
  (total: number, item: DashboardTrend) => total + item.totalTokens,
  0
))

function padNumber(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

function formatLocalDate(date: Date): string {
  return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())}`
}

function fillDailyUsage(data: DashboardTrend[], days: number): DashboardTrend[] {
  const dataMap = new Map(data.map((item) => [item.date, item]))
  const result: DashboardTrend[] = []
  const today = new Date()

  for (let index = days - 1; index >= 0; index--)
  {
    const date = new Date(today)
    date.setDate(date.getDate() - index)
    const dateText = formatLocalDate(date)
    result.push(dataMap.get(dateText) || {
      date: dateText,
      requestCount: 0,
      totalTokens: 0,
      cost: 0
    })
  }

  return result
}

async function fetchData(): Promise<void> {
  loading.value = true
  try
  {
    const response = await getUserTrend(range.value)
    trendData.value = response.data || []
  }
  finally
  {
    loading.value = false
  }
}

function open(): void {
  visible.value = true
  fetchData()
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.token-detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.token-detail-summary {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;

  strong {
    color: var(--el-text-color-primary);
    font-size: 20px;
  }
}

.token-total-value {
  color: var(--el-color-primary);
  font-weight: 600;
}

@media (max-width: 767px) {
  :global(.token-usage-dialog) {
    margin-top: 8px;
    margin-bottom: 8px;
  }

  :global(.token-usage-dialog .el-dialog__header) {
    padding: 14px 16px 8px;
  }

  :global(.token-usage-dialog .el-dialog__body) {
    padding: 8px 12px 14px;
  }

  .token-detail-toolbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 12px;
  }

  .token-detail-toolbar :deep(.el-segmented) {
    width: 100%;
  }
}
</style>
