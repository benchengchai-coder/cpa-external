<template>
  <div class="user-home-container" v-loading="dashboardLoading">
    <div class="user-home-layout">
      <el-row :gutter="16" class="overview-card-list">
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
          <div
            class="overview-stat-item"
            :class="{ 'has-low-amount-reminder': Boolean(item.reminderText) }"
            :style="{ '--overview-stat-color': item.color }"
          >
            <el-tooltip v-if="item.reminderText" :content="item.reminderText" placement="top">
              <span
                class="low-amount-indicator"
                role="status"
                tabindex="0"
                :aria-label="item.reminderText"
              >
                <el-icon><WarningFilled /></el-icon>
              </span>
            </el-tooltip>
            <a
              v-if="item.key === 'totalTokens'"
              class="overview-detail-link"
              href="#"
              @click.prevent="handleTokenDetailClick"
            >
              详情
              <el-icon><ArrowRight /></el-icon>
            </a>
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
          <CheckinCard
            class="overview-stat-item"
            @signed="handleCheckinSigned"
            @enabled-change="checkinEnabled = $event"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4" class="overview-stat-col">
          <div
            class="overview-stat-item subscription-overview-item"
            :class="{ 'has-low-amount-reminder': Boolean(subscriptionReminderText) }"
            v-loading="subscriptionLoading"
          >
            <el-tooltip v-if="subscriptionReminderText" :content="subscriptionReminderText" placement="top">
              <span
                class="low-amount-indicator"
                role="status"
                tabindex="0"
                :aria-label="subscriptionReminderText"
              >
                <el-icon><WarningFilled /></el-icon>
              </span>
            </el-tooltip>
            <div class="subscription-overview-content">
              <div class="overview-stat-title subscription-overview-title">
                <el-icon :size="14"><Tickets /></el-icon>
                <span>当前订阅</span>
              </div>
              <template v-if="primarySubscription">
                <div class="subscription-summary-value">{{ subscriptionUsageText(primarySubscription) }}</div>
                <div v-if="activeSubscriptions.length > 1" class="subscription-summary-count">
                  共 {{ activeSubscriptions.length }} 个生效订阅
                </div>
                <div class="subscription-summary-period">{{ subscriptionPeriodText(primarySubscription) }}</div>
              </template>
              <template v-else>
                <div class="subscription-summary-value is-empty">暂无订阅</div>
                <el-button link type="primary" size="small" @click="dashboardContentView = 'subscription'">
                  去订阅
                </el-button>
              </template>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="dashboard-content-card">
        <div class="dashboard-content-header">
          <el-tooltip content="点击复制 Base URL" placement="top">
            <button
              type="button"
              class="base-url-hint"
              :aria-label="`复制 Base URL：${apiBaseUrl}`"
              @click="handleCopyBaseUrl"
            >
              <span class="base-url-label">Base URL</span>
              <code class="base-url-value">{{ apiBaseUrl }}</code>
              <el-icon><CopyDocument /></el-icon>
            </button>
          </el-tooltip>
          <el-segmented v-model="dashboardContentView" :options="dashboardContentOptions" />
        </div>
        <div class="dashboard-content-body">
          <KeepAlive>
            <component
              :is="dashboardContentComponent"
              class="embedded-dashboard-page"
              @subscription-changed="handleSubscriptionChanged"
              @balance-changed="fetchData"
            />
          </KeepAlive>
        </div>
      </div>
    </div>
    <TokenUsageDetailDialog ref="tokenUsageDetailDialogRef" />
  </div>
</template>

<script setup lang="ts" name="UserHome">
import type { Component } from 'vue'
import { ArrowRight, Coin, CopyDocument, DataLine, Money, Tickets, Wallet, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getInviteInfo } from '@/api/aigate/invite'
import { getSelfSubscription } from '@/api/aigate/subscription'
import type { AiUserSubscription } from '@/types'
import { parseTime } from '@/utils/ruoyi'
import UserApiKeyCard from '@/views/aigate/api-key/UserApiKeyCard.vue'
import UserInvitePage from '@/views/aigate/user-invite/index.vue'
import UsageLog from '@/views/aigate/usage-log/index.vue'
import UserSubscriptionPage from '@/views/aigate/user-subscription/index.vue'
import WalletPage from '@/views/aigate/wallet/index.vue'
import { fixedNumber, formatCurrency, formatTokenCount } from '../aigate/common'
import CheckinCard from './components/CheckinCard.vue'
import TokenUsageDetailDialog from './components/TokenUsageDetailDialog.vue'
import { useUserHome } from './useUserHome'

interface OverviewStatisticItem {
  key: string
  title: string
  value: number
  formatter: (value: number) => string
  icon: Component
  color: string
  reminderText?: string
}

const LOW_AMOUNT_THRESHOLD = 5
const SUBSCRIPTION_EXPIRY_REMINDER_MS = 3 * 24 * 60 * 60 * 1000
const apiBaseUrl = `${window.location.origin}/v1`

const {
  loading: dashboardLoading,
  loaded: dashboardLoaded,
  availableBalance,
  totalChargedAmount,
  requestCount,
  totalTokens,
  fetchData
} = useUserHome()

const checkinEnabled = ref(false)
const subscriptionLoading = ref(false)
const subscriptionLoaded = ref(false)
const activeSubscriptions = ref<AiUserSubscription[]>([])
const primarySubscription = computed(() => activeSubscriptions.value[0])
const tokenUsageDetailDialogRef = ref<InstanceType<typeof TokenUsageDetailDialog> | null>(null)

const balanceReminderText = computed(() => {
  if (!dashboardLoaded.value || Number(availableBalance.value) >= LOW_AMOUNT_THRESHOLD) {
    return ''
  }
  return '可用余额低于 $5，请及时充值'
})

const subscriptionReminderText = computed(() => {
  if (!subscriptionLoaded.value) {
    return ''
  }
  if (!primarySubscription.value) {
    return ''
  }

  const reminderMessages: string[] = []
  if (
    primarySubscription.value.availableAmount != null
    && Number(primarySubscription.value.availableAmount) < LOW_AMOUNT_THRESHOLD
  ) {
    reminderMessages.push('当前订阅可用额度低于 $5')
  }

  if (isSubscriptionExpiringSoon(primarySubscription.value)) {
    reminderMessages.push('距到期不足 3 天')
  }

  return reminderMessages.length > 0 ? `${reminderMessages.join('，')}，请及时续订` : ''
})

function isSubscriptionExpiringSoon(row: AiUserSubscription): boolean {
  if (!row.endTime) {
    return false
  }

  const endTime = new Date(row.endTime.replace(/-/g, '/').replace('T', ' ')).getTime()
  if (Number.isNaN(endTime)) {
    return false
  }

  const remainingTime = endTime - Date.now()
  return remainingTime > 0 && remainingTime < SUBSCRIPTION_EXPIRY_REMINDER_MS
}

function formatNumberStatistic(value: number): string {
  return Number(value || 0).toLocaleString()
}

function formatTokenStatistic(value: number): string {
  return formatTokenCount(Number(value || 0))
}

function formatCostStatistic(value: number): string {
  return formatCurrency(Number(value || 0), 2, true)
}

const overviewStats = computed<OverviewStatisticItem[]>(() => [
  {
    key: 'balance',
    title: '可用余额',
    value: availableBalance.value,
    formatter: formatCostStatistic,
    icon: Wallet,
    color: '#67c23a',
    reminderText: balanceReminderText.value
  },
  {
    key: 'totalChargedAmount',
    title: '累计实扣',
    value: totalChargedAmount.value,
    formatter: formatCostStatistic,
    icon: Money,
    color: '#f56c6c'
  },
  {
    key: 'requestCount',
    title: '累计请求',
    value: requestCount.value,
    formatter: formatNumberStatistic,
    icon: DataLine,
    color: '#6366f1'
  },
  {
    key: 'totalTokens',
    title: '累计 Token',
    value: totalTokens.value,
    formatter: formatTokenStatistic,
    icon: Coin,
    color: '#06b6d4'
  }
])

async function fetchSubscriptions() {
  subscriptionLoading.value = true
  try {
    const response = await getSelfSubscription()
    activeSubscriptions.value = (response.data?.subscriptions || []).filter((item) => item.status === 'active')
  } catch {
    activeSubscriptions.value = []
  } finally {
    subscriptionLoading.value = false
    subscriptionLoaded.value = true
  }
}

function trimFixedNumber(value: number | string | undefined, digits = 4): string {
  return fixedNumber(value, digits).replace(/\.?0+$/, '')
}

function subscriptionUsageText(row: AiUserSubscription): string {
  if (!row.amountTotal || Number(row.amountTotal) === 0) {
    return `${trimFixedNumber(row.amountUsed, 4)} / 不限`
  }
  return `${formatCurrency(row.amountUsed, 4, true)} / ${formatCurrency(row.amountTotal, 4, true)}`
}

type DashboardContentView = 'usageLog' | 'apiKey' | 'wallet' | 'subscription' | 'invite'

const inviteEnabled = ref(false)
const dashboardContentOptions = computed<Array<{ label: string; value: DashboardContentView }>>(() => {
  const options: Array<{ label: string; value: DashboardContentView }> = [
    { label: '使用记录', value: 'usageLog' },
    { label: '密钥管理', value: 'apiKey' },
    { label: '钱包充值', value: 'wallet' },
    { label: '订阅计划', value: 'subscription' }
  ]
  if (inviteEnabled.value) {
    options.push({ label: '推荐计划', value: 'invite' })
  }
  return options
})

const dashboardContentView = ref<DashboardContentView>('usageLog')
const dashboardContentComponentMap: Record<DashboardContentView, Component> = {
  usageLog: UsageLog,
  apiKey: UserApiKeyCard,
  wallet: WalletPage,
  subscription: UserSubscriptionPage,
  invite: UserInvitePage
}
const dashboardContentComponent = computed<Component>(() => dashboardContentComponentMap[dashboardContentView.value])

function subscriptionPeriodText(row: AiUserSubscription): string {
  const startTime = parseTime(row.startTime, '{y}-{m}-{d}') || '-'
  const endTime = parseTime(row.endTime, '{y}-{m}-{d}') || '-'
  return `${startTime} 至 ${endTime}`
}

function handleCheckinSigned() {
  fetchData()
}

function handleTokenDetailClick(): void {
  tokenUsageDetailDialogRef.value?.open()
}

function handleSubscriptionChanged() {
  fetchSubscriptions()
  fetchData()
}

async function fetchInviteAvailability() {
  try {
    const response = await getInviteInfo()
    inviteEnabled.value = response.data != null
  } catch {
    inviteEnabled.value = false
  }

  if (!inviteEnabled.value && dashboardContentView.value === 'invite') {
    dashboardContentView.value = 'usageLog'
  }
}

async function handleCopyBaseUrl() {
  let copied = false
  try {
    await navigator.clipboard.writeText(apiBaseUrl)
    copied = true
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = apiBaseUrl
    textarea.setAttribute('readonly', '')
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      copied = document.execCommand('copy')
    } catch {
      copied = false
    }
    document.body.removeChild(textarea)
  }

  copied ? ElMessage.success('Base URL 已复制') : ElMessage.error('复制失败，请手动复制')
}

onMounted(() => {
  fetchSubscriptions()
  fetchInviteAvailability()
})
</script>

<style scoped lang="scss">
:deep(.el-card__body) {
  padding: 14px !important;
}

:deep(.el-card__header) {
  padding: 7px 14px !important;
}

.user-home-container {
  min-height: 100%;
  padding: 20px 40px;
  box-sizing: border-box;
}

.user-home-layout {
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
  position: relative;
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

.overview-stat-item.has-low-amount-reminder {
  border-color: var(--el-color-danger-light-5);
}

.low-amount-indicator {
  position: absolute;
  top: 9px;
  right: 10px;
  z-index: 1;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-white);
  background: var(--el-color-danger);
  box-shadow: 0 4px 10px rgba(245, 108, 108, 0.35);
  cursor: help;
  animation: low-amount-indicator-bounce 1.5s ease-in-out infinite;
}

.low-amount-indicator:focus-visible {
  outline: 2px solid var(--el-color-danger-light-3);
  outline-offset: 2px;
}

@keyframes low-amount-indicator-bounce {
  0%,
  100% {
    transform: translateY(0);
  }

  35% {
    transform: translateY(-7px);
  }

  50% {
    transform: translateY(0);
  }

  65% {
    transform: translateY(-3px);
  }

  78% {
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .low-amount-indicator {
    animation: none;
  }
}

.overview-stat-title {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  line-height: 1.2;
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  gap: 12px;
  flex-wrap: wrap;
}

.overview-detail-link {
  position: absolute;
  bottom: 10px;
  right: 10px;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: color 0.2s;

  &:hover,
  &:focus-visible {
    color: var(--el-color-primary-light-3);
  }

  &:focus-visible {
    outline: 2px solid var(--el-color-primary-light-5);
    outline-offset: 3px;
    border-radius: 3px;
  }
}

.base-url-hint {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 30%;
  padding: 6px 10px;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 6px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font: inherit;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background-color 0.2s;
}

.base-url-hint:hover,
.base-url-hint:focus-visible {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-3);
  background: var(--el-color-primary-light-8);
}

.base-url-hint:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: 2px;
}

.base-url-label,
.base-url-hint .el-icon {
  flex-shrink: 0;
}

.base-url-label {
  font-size: 12px;
  font-weight: 600;
}

.base-url-value {
  min-width: 0;
  overflow: hidden;
  color: inherit;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-content-header :deep(.el-segmented) {
  max-width: 100%;
  overflow-x: auto;
}

.dashboard-content-body {
  min-width: 0;
}

.dashboard-content-body :deep(.el-table) {
  --el-table-border: none;
}

.dashboard-content-body :deep(.el-table__body tr:hover > td.el-table__cell),
.dashboard-content-body :deep(.el-table__body tr.hover-row > td.el-table__cell) {
  background-color: var(--el-table-tr-bg-color) !important;
}

.dashboard-content-body :deep(.el-table--striped .el-table__body tr.el-table__row--striped:hover > td.el-table__cell),
.dashboard-content-body :deep(.el-table--striped .el-table__body tr.el-table__row--striped.hover-row > td.el-table__cell) {
  background-color: var(--el-fill-color-lighter) !important;
}

.subscription-overview-item {
  --overview-stat-color: var(--el-color-primary);
}

.subscription-overview-content {
  width: 100%;
  min-width: 0;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.subscription-overview-title {
  margin-bottom: 8px;
}

.subscription-summary-value {
  max-width: 100%;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.subscription-summary-value.is-empty {
  color: var(--el-text-color-secondary);
}

.subscription-summary-count {
  margin-top: 5px;
  font-size: 12px;
  line-height: 1.2;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.subscription-summary-period {
  width: 100%;
  margin-top: auto;
  padding-top: 6px;
  font-size: 12px;
  line-height: 1.2;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.embedded-dashboard-page.app-container) {
  padding: 0;
}

@media (max-width: 767px) {
  .user-home-container {
    padding: 12px 12px 0;
  }

  .dashboard-content-header :deep(.el-segmented) {
    width: 100%;
  }
}

@media (max-width: 1199px) {
  .base-url-hint {
    display: none;
  }
}
</style>
