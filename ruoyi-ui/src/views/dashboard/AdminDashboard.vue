<template>
  <div class="dashboard-container">
    <div class="dashboard-layout">
      <el-row v-loading="overviewLoading" :gutter="16" class="overview-card-list">
        <el-col
          v-for="item in overviewStats"
          :key="item.key"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="4"
          :xl="4"
          class="overview-stat-col"
        >
          <div class="overview-stat-item" :style="{ '--overview-stat-color': item.color }">
            <el-statistic class="overview-statistic" :value="item.value" :formatter="item.formatter">
              <template #title>
                <div class="overview-stat-title">
                  <el-icon :size="14">
                    <component :is="item.icon" />
                  </el-icon>
                  <span>{{ item.title }}</span>
                </div>
              </template>
            </el-statistic>
          </div>
        </el-col>
        <el-col v-show="checkinEnabled" :xs="24" :sm="12" :md="8" :lg="4" :xl="4" class="overview-stat-col">
          <CheckinCard class="overview-stat-item" @enabled-change="checkinEnabled = $event" />
        </el-col>
      </el-row>

      <div class="dashboard-content-card">
        <div class="dashboard-content-header">
          <el-segmented
            v-model="dashboardContentView"
            :options="dashboardContentOptions"
            @change="handleDashboardContentChange"
          />
        </div>

        <div class="dashboard-content-body">
          <div v-show="dashboardContentView === 'trend'" class="trend-grid">
            <el-card v-loading="trendLoading" shadow="always" class="chart-card">
              <template #header>
                <div class="content-card-header">
                  <span class="card-title">调用趋势</span>
                  <el-segmented v-model="trendRange" :options="trendRangeOptions" @change="fetchTrend" />
                </div>
              </template>
              <div ref="trendChartRef" class="chart-container"></div>
            </el-card>

            <el-card v-loading="tokenTrendLoading" shadow="always" class="chart-card">
              <template #header>
                <div class="content-card-header">
                  <span class="card-title">Token 用量趋势</span>
                  <el-segmented v-model="tokenRange" :options="trendRangeOptions" @change="fetchTokenTrend" />
                </div>
              </template>
              <div ref="tokenChartRef" class="chart-container"></div>
            </el-card>
          </div>

          <div v-show="dashboardContentView === 'rank'" class="rank-panel">
            <div class="rank-panel-header">
              <div class="content-card-header rank-card-header">
                <span class="card-title">用户实扣排行</span>
                <div class="rank-actions">
                  <el-segmented
                    v-model="rankRange"
                    :options="rankRangeOptions"
                    @change="handleRankRangeChange"
                  />
                </div>
              </div>
            </div>
            <div class="rank-panel-body">
              <div class="rank-table-scroll">
                <el-table
                  :data="userRankData"
                  v-loading="userRankLoading"
                  stripe
                  style="width: 100%"
                  size="default"
                >
                  <el-table-column type="index" :index="rankIndex" label="#" width="70" />
                  <el-table-column prop="username" label="用户名" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span class="user-link" @click="handleUserSummaryClick(row)">
                        {{ row.username || row.userId }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="requestCount" label="调用次数" min-width="140" align="right">
                    <template #default="{ row }">
                      {{ row.requestCount.toLocaleString() }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="totalTokens" label="Token 消耗" min-width="160" align="right">
                    <template #default="{ row }">
                      {{ formatTokenCount(row.totalTokens) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="totalChargedAmount" label="累计实扣" min-width="140" align="right">
                    <template #default="{ row }">
                      {{ formatCurrency(row.totalChargedAmount, 4, true) }}
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div v-if="userRankTotal > 0" class="rank-pagination">
                <el-pagination
                  v-model:current-page="userRankPageNum"
                  v-model:page-size="userRankPageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="userRankTotal"
                  background
                  layout="total, sizes, prev, pager, next"
                  @current-change="fetchUserRank"
                  @size-change="handleUserRankPageSizeChange"
                />
              </div>
            </div>
          </div>

          <div v-if="dashboardContentView === 'online'" class="online-panel">
            <OnlineUserPage class="online-page" />
          </div>
        </div>
      </div>
    </div>
    <AiUserSummaryDialog ref="userSummaryDialogRef" />
  </div>
</template>

<script setup lang="ts" name="AdminDashboard">
import { nextTick, type Component } from 'vue'
import { Coin, Money, Timer, TrendCharts, User, WarningFilled } from '@element-plus/icons-vue'
import type { DashboardRankRange, DashboardUserRank } from '@/types'
import AiUserSummaryDialog from '../aigate/components/AiUserSummaryDialog.vue'
import OnlineUserPage from '../monitor/online/index.vue'
import CheckinCard from './components/CheckinCard.vue'
import { useAdminDashboard } from './useAdminDashboard'
import { formatCurrency, formatMilliseconds, formatTokenCount } from '../aigate/common'

interface OverviewStatisticItem {
  key: string
  title: string
  value: number
  formatter: (value: number) => string
  icon: Component
  color: string
}

type DashboardContentView = 'trend' | 'rank' | 'online'

const checkinEnabled = ref(false)
const dashboardContentView = ref<DashboardContentView>('rank')
const dashboardContentOptions: Array<{ label: string; value: DashboardContentView }> = [
  { label: '排行榜单', value: 'rank' },
  { label: '趋势统计', value: 'trend' },
  { label: '在线用户', value: 'online' }
]

const userSummaryDialogRef = ref<InstanceType<typeof AiUserSummaryDialog> | null>(null)
const rankRangeOptions: Array<{ label: string; value: DashboardRankRange }> = [
  { label: '今日', value: 'today' },
  { label: '近7天', value: '7' },
  { label: '近30天', value: '30' },
  { label: '近90天', value: '90' },
  { label: '全部', value: 'all' }
]

function formatNumberStatistic(value: number): string {
  return Number(value || 0).toLocaleString()
}

function formatTokenStatistic(value: number): string {
  return formatTokenCount(Number(value || 0))
}

function formatCostStatistic(value: number): string {
  return formatCurrency(Number(value || 0), 2, true)
}

function formatMillisecondStatistic(value: number): string {
  return formatMilliseconds(Number(value || 0))
}

const {
  overviewLoading,
  trendLoading,
  tokenTrendLoading,
  trendRange,
  tokenRange,
  rankRange,
  trendRangeOptions,
  overviewData,
  userRankData,
  trendLoaded,
  tokenTrendLoaded,
  userRankLoading,
  userRankTotal,
  userRankPageNum,
  userRankPageSize,
  trendChartRef,
  tokenChartRef,
  fetchTrend,
  fetchTokenTrend,
  fetchUserRank,
  resizeCharts,
  init
} = useAdminDashboard()

const overviewStats = computed<OverviewStatisticItem[]>(() => [
  {
    key: 'userCount',
    title: '用户总数',
    value: overviewData.value.userCount,
    formatter: formatNumberStatistic,
    icon: User,
    color: '#8b5cf6'
  },
  {
    key: 'todayNewUsers',
    title: '今日新增用户',
    value: overviewData.value.todayNewUsers,
    formatter: formatNumberStatistic,
    icon: User,
    color: '#a78bfa'
  },
  {
    key: 'todayRequests',
    title: '今日请求',
    value: overviewData.value.todayRequests,
    formatter: formatNumberStatistic,
    icon: TrendCharts,
    color: '#e6a23c'
  },
  {
    key: 'requestsPerMinute',
    title: 'RPM',
    value: overviewData.value.requestsPerMinute,
    formatter: formatNumberStatistic,
    icon: TrendCharts,
    color: '#f59e0b'
  },
  {
    key: 'todayTokens',
    title: '今日 Token',
    value: overviewData.value.todayTokens,
    formatter: formatTokenStatistic,
    icon: Coin,
    color: '#06b6d4'
  },
  {
    key: 'totalTokens',
    title: '累计 Token',
    value: overviewData.value.totalTokens,
    formatter: formatTokenStatistic,
    icon: Coin,
    color: '#0891b2'
  },
  {
    key: 'todayCost',
    title: '今日计算费用',
    value: overviewData.value.todayCost,
    formatter: formatCostStatistic,
    icon: Money,
    color: '#f56c6c'
  },
  {
    key: 'todayChargedAmount',
    title: '今日实扣',
    value: overviewData.value.todayChargedAmount,
    formatter: formatCostStatistic,
    icon: Money,
    color: '#dc2626'
  },
  {
    key: 'failedSettlementCount',
    title: '结算异常',
    value: overviewData.value.failedSettlementCount,
    formatter: formatNumberStatistic,
    icon: WarningFilled,
    color: '#e6a23c'
  },
  {
    key: 'averageFirstTokenTime',
    title: '首 Token 响应',
    value: overviewData.value.averageFirstTokenTime,
    formatter: formatMillisecondStatistic,
    icon: Timer,
    color: '#14b8a6'
  },
  {
    key: 'averageResponseTime',
    title: '平均请求响应',
    value: overviewData.value.averageResponseTime,
    formatter: formatMillisecondStatistic,
    icon: Timer,
    color: '#0f766e'
  }
])

async function handleDashboardContentChange(value: DashboardContentView): Promise<void> {
  if (value === 'trend') {
    await nextTick()
    const requests: Promise<void>[] = []
    if (!trendLoaded.value) requests.push(fetchTrend())
    if (!tokenTrendLoaded.value) requests.push(fetchTokenTrend())
    await Promise.all(requests)
    await nextTick()
    resizeCharts()
  }
}

async function handleRankRangeChange(): Promise<void> {
  userRankPageNum.value = 1
  userRankData.value = []
  userRankTotal.value = 0
  await fetchUserRank()
}

function rankIndex(index: number): number {
  return (userRankPageNum.value - 1) * userRankPageSize.value + index + 1
}

function handleUserRankPageSizeChange(): void {
  userRankPageNum.value = 1
  fetchUserRank()
}

function handleUserSummaryClick(row: DashboardUserRank): void {
  const selectedText = window.getSelection()?.toString().trim()
  if (selectedText) {
    return
  }
  userSummaryDialogRef.value?.open(row.userId, row.username)
}

onMounted(() => {
  init()
})
</script>

<style scoped lang="scss">
:deep(.el-card__body) {
  padding: 14px !important;
}

:deep(.el-card__header) {
  padding: 7px 14px !important;
}

:deep(.el-table__inner-wrapper:before) {
  height: 0;
}

.dashboard-container {
  min-height: 100%;
  padding: 20px 40px;
  box-sizing: border-box;
}

.dashboard-layout {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-card-list {
  min-width: 0;
  row-gap: 16px;
}

.overview-stat-col {
  min-width: 0;
}

.overview-stat-item {
  height: 100%;
  min-height: 96px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.overview-stat-title {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  .el-icon {
    color: var(--overview-stat-color);
    flex-shrink: 0;
  }
}

.overview-statistic {
  min-width: 0;
  width: 100%;
}

:deep(.overview-statistic .el-statistic__head) {
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.2;
}

:deep(.overview-statistic .el-statistic__content) {
  justify-content: center;
  min-width: 0;
  font-size: 24px;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

:deep(.overview-statistic .el-statistic__number) {
  min-width: 0;
  max-width: 100%;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dashboard-content-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  overflow: hidden;
  border-radius: 8px;
}

.dashboard-content-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.dashboard-content-header :deep(.el-segmented) {
  max-width: 100%;
  overflow-x: auto;
}

.dashboard-content-body {
  min-width: 0;
}

.trend-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card {
  min-width: 0;
  height: 420px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.chart-card .el-card__body) {
  flex: 1;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.chart-container {
  width: 100%;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

.content-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.rank-panel {
  width: 100%;
  min-width: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
  overflow: hidden;
}

.online-panel {
  width: 100%;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}

.online-page {
  min-width: 0;
  padding: 14px;
}

.rank-panel-header {
  padding: 7px 14px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.rank-panel-body {
  min-width: 0;
}

.rank-panel :deep(.el-table) {
  --el-table-border: none;
}

.rank-table-scroll {
  min-width: 0;
  overflow-x: auto;
}

.rank-table-scroll :deep(.el-table) {
  min-width: 690px;
}

.rank-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 14px;
  overflow-x: auto;
}

.rank-card-header {
  flex-wrap: wrap;
}

.rank-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.user-link {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: var(--el-color-primary);
  cursor: pointer;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.user-link:hover {
  text-decoration: underline;
}

@media (max-width: 1199px) {
  .trend-grid {
    grid-template-columns: 1fr;
  }

  .chart-card {
    height: 360px;
  }
}

@media (max-width: 767px) {
  .dashboard-container {
    padding: 12px 12px 0;
  }

  .dashboard-content-header :deep(.el-segmented) {
    width: 100%;
  }

  .chart-card {
    height: 340px;
  }

  .rank-card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .rank-actions {
    width: 100%;
    align-items: stretch;
    justify-content: flex-start;
    flex-direction: column;
  }

  .rank-actions :deep(.el-segmented) {
    width: 100%;
    overflow-x: auto;
  }
}
</style>
