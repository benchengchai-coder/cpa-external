<template>
  <div class="app-container personal-subscription-page">
    <section class="subscription-section current-section">
      <div class="section-header">
        <div>
          <span class="section-kicker">我的订阅</span>
          <h2>订阅管理</h2>
          <p>查看正在生效的套餐、有效期与额度使用情况。</p>
        </div>
      </div>

      <div class="current-subscription-content" v-loading="subscriptionLoading">
        <div v-if="activeSubscriptions.length === 0" class="empty-state">
          <div class="empty-state-icon">
            <el-icon><Tickets /></el-icon>
          </div>
          <div>
            <strong>暂时没有生效中的订阅</strong>
            <p>可以继续使用钱包余额，或从下方选择一个适合你的套餐。</p>
          </div>
        </div>

        <div v-else class="current-subscription-list">
          <article v-for="sub in activeSubscriptions" :key="sub.subscriptionId" class="current-subscription-item">
            <div class="subscription-item-header">
              <div class="subscription-title-row">
                <span class="subscription-title-label">当前订阅</span>
                <el-tag type="primary" effect="plain" size="small">{{ sub.planTitle }}</el-tag>
              </div>
              <el-tag type="success" size="small" effect="light" round>生效中</el-tag>
            </div>

            <div class="subscription-stat-grid">
              <div class="subscription-stat">
                <span>开始时间</span>
                <strong>{{ parseTime(sub.startTime) || '-' }}</strong>
              </div>
              <div class="subscription-stat">
                <span>到期时间</span>
                <strong>{{ parseTime(sub.endTime) || '-' }}</strong>
              </div>
              <div class="subscription-stat">
                <span>已用 / 总额度</span>
                <strong>{{ subscriptionUsageText(sub) }}</strong>
              </div>
              <div class="subscription-stat">
                <span>可用 / 冻结</span>
                <strong>{{ subscriptionAvailableText(sub) }}</strong>
              </div>
            </div>

            <div v-if="sub.amountTotal && Number(sub.amountTotal) > 0" class="subscription-progress">
              <div class="subscription-progress-copy">
                <span>额度使用进度</span>
                <strong>{{ subscriptionUsagePercent(sub) }}%</strong>
              </div>
              <el-progress :percentage="subscriptionUsagePercent(sub)" :stroke-width="8" :show-text="false" />
            </div>

          </article>
        </div>
      </div>
    </section>

    <section class="subscription-section plans-section">
      <div class="section-header">
        <div>
          <span class="section-kicker">订阅计划</span>
          <h2>选择适合你的套餐</h2>
          <p>使用钱包余额购买套餐，套餐生效后即可使用对应额度与权益。</p>
        </div>
         <el-tag v-if="hasActiveSubscription" type="info" effect="plain" round>已有套餐生效中</el-tag>
      </div>

      <div v-loading="plansLoading">
        <div v-if="purchasePlans.length === 0" class="empty-state compact-empty-state">
          <div class="empty-state-icon">
            <el-icon><Promotion /></el-icon>
          </div>
          <div>
            <strong>暂无可购买套餐</strong>
            <p>管理员暂未发布支持钱包余额购买的套餐。</p>
          </div>
        </div>

        <div v-else class="plan-grid">
          <article v-for="plan in purchasePlans" :key="plan.planId" class="plan-item">
            <div class="plan-header">
              <div>
                <h3>{{ plan.title }}</h3>
                <p>{{ plan.subTitle || '灵活套餐，即买即用' }}</p>
              </div>
              <el-tag type="primary" effect="plain" size="small">{{ durationText(plan) }}</el-tag>
            </div>

            <div class="plan-price">
              {{ formatCurrency(plan.priceAmount, 4, true) }}
              <small>/ 每月</small>
            </div>

            <div class="plan-meta">
              <div>
                <span>套餐额度</span>
                <strong>{{ !plan.amountTotal ? '不限' : formatCurrency(plan.amountTotal, 4, true) }}</strong>
              </div>
            </div>

            <el-button
              type="primary"
              class="purchase-button"
              :disabled="hasActiveSubscription"
              :loading="!hasActiveSubscription && purchaseLoading === plan.planId"
              @click="handlePurchase(plan)"
            >
              {{ hasActiveSubscription ? '当前已有生效套餐' : '使用余额购买' }}
            </el-button>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts" name="AiUserSubscriptionPage">
import {
  balancePurchaseSubscription,
  getSelfSubscription,
  listUserPlans
} from '@/api/aigate/subscription'
import type { AiSubscriptionPlan, AiUserSubscription } from '@/types'
import { parseTime } from '@/utils/ruoyi'
import { fixedNumber, formatCurrency } from '../common'

const emit = defineEmits<{
  subscriptionChanged: []
}>()

const { proxy } = getCurrentInstance() as any
const subscriptionLoading = ref(false)
const plansLoading = ref(false)
const purchaseLoading = ref<number | undefined>()
const activeSubscriptions = ref<AiUserSubscription[]>([])
const purchasePlans = ref<AiSubscriptionPlan[]>([])
const hasActiveSubscription = computed(() => activeSubscriptions.value.length > 0)

function loadSubscriptions() {
  subscriptionLoading.value = true
  getSelfSubscription()
    .then((response) => {
      activeSubscriptions.value = (response.data?.subscriptions || []).filter((item) => item.status === 'active')
    })
    .catch(() => {
      activeSubscriptions.value = []
    })
    .finally(() => {
      subscriptionLoading.value = false
    })
}

function loadPurchasePlans() {
  plansLoading.value = true
  listUserPlans()
    .then((response) => {
      purchasePlans.value = (response.data || []).filter(
        (item) => item.status === '0' && item.allowBalancePurchase === 1
      )
    })
    .catch(() => {
      purchasePlans.value = []
    })
    .finally(() => {
      plansLoading.value = false
    })
}

function handlePurchase(row: AiSubscriptionPlan) {
  if (hasActiveSubscription.value || !row.planId) {
    return
  }
  proxy.$modal
    .confirm(`确认使用余额购买「${row.title}」？`)
    .then(() => {
      purchaseLoading.value = row.planId
      return balancePurchaseSubscription(row.planId!)
    })
    .then(() => {
      proxy.$modal.msgSuccess('购买成功')
      loadSubscriptions()
      loadPurchasePlans()
      emit('subscriptionChanged')
    })
    .catch(() => {})
    .finally(() => {
      purchaseLoading.value = undefined
    })
}

function trimFixedNumber(value: number | string | undefined, digits = 4) {
  return fixedNumber(value, digits).replace(/\.?0+$/, '')
}

function subscriptionUsageText(row: AiUserSubscription) {
  if (!row.amountTotal || Number(row.amountTotal) === 0) {
    return `${trimFixedNumber(row.amountUsed, 4)} / 不限`
  }
  return `${formatCurrency(row.amountUsed, 4, true)} / ${formatCurrency(row.amountTotal, 4, true)}`
}

function subscriptionUsagePercent(row: AiUserSubscription) {
  const total = Number(row.amountTotal || 0)
  if (total <= 0) {
    return 0
  }
  const used = Number(row.amountUsed || 0)
  return Math.min(100, Math.max(0, Math.round((used / total) * 100)))
}

function subscriptionAvailableText(row: AiUserSubscription) {
  const frozen = formatCurrency(row.frozenBalance, 4, true)
  if (!row.amountTotal || Number(row.amountTotal) === 0) {
    return `不限 / ${frozen}`
  }
  return `${formatCurrency(row.availableAmount, 4, true)} / ${frozen}`
}

function durationText(row: AiSubscriptionPlan) {
  const unitMap: Record<string, string> = { day: '天', week: '周', month: '月', year: '年', custom: '秒' }
  const duration = row.durationUnit === 'custom' ? row.customSeconds || row.durationValue : row.durationValue
  return `${duration}${unitMap[row.durationUnit || 'month']}`
}

loadSubscriptions()
loadPurchasePlans()
</script>

<style scoped lang="scss">
.personal-subscription-page {
  --subscription-card-shadow: 0 12px 30px rgba(24, 34, 64, 0.11);
  --subscription-card-shadow-soft: 0 8px 20px rgba(24, 34, 64, 0.08);
  display: grid;
  width: min(100%, 1480px);
  padding: 0;
  margin: 0 auto;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: stretch;
  gap: 18px;
}

.subscription-section {
  padding: 22px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  box-shadow: var(--subscription-card-shadow);
}

.section-header {
  display: flex;
  margin-bottom: 18px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h2 {
    margin: 4px 0 5px;
    color: var(--el-text-color-primary);
    font-size: 20px;
    font-weight: 700;
    line-height: 1.3;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
    line-height: 1.6;
  }
}

.section-kicker {
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 700;
}

.current-subscription-content {
  min-height: 148px;
}

.current-subscription-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
}

.current-subscription-item,
.plan-item {
  min-width: 0;
  padding: 18px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  box-shadow: var(--subscription-card-shadow-soft);
}

.subscription-item-header,
.subscription-title-row,
.subscription-benefit {
  display: flex;
  align-items: center;
}

.subscription-item-header {
  margin-bottom: 16px;
  justify-content: space-between;
  gap: 12px;
}

.subscription-title-row {
  min-width: 0;
  gap: 8px;
}

.subscription-title-label {
  flex-shrink: 0;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 700;
}

.subscription-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.subscription-stat {
  display: flex;
  min-width: 0;
  padding: 10px 12px;
  flex-direction: column;
  gap: 5px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-extra-light);
  border-radius: 9px;

  span {
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }

  strong {
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.subscription-progress {
  margin-top: 15px;
}

.subscription-progress-copy {
  display: flex;
  margin-bottom: 7px;
  justify-content: space-between;
  color: var(--el-text-color-secondary);
  font-size: 11px;

  strong {
    color: var(--el-text-color-primary);
  }
}

.subscription-benefit {
  margin-top: 14px;
  gap: 7px;
  color: var(--el-text-color-secondary);
  font-size: 12px;

  .el-icon {
    color: var(--el-color-warning);
  }
}

.empty-state {
  display: flex;
  min-height: 132px;
  padding: 22px;
  align-items: center;
  justify-content: center;
  gap: 14px;
  background: var(--el-fill-color-lighter);
  border: 1px dashed var(--el-border-color);
  border-radius: 12px;

  strong {
    color: var(--el-text-color-primary);
    font-size: 14px;
  }

  p {
    margin: 5px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.6;
  }
}

.empty-state-icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  place-items: center;
  color: var(--el-color-primary);
  font-size: 20px;
  background: var(--el-fill-color);
  border-radius: 12px;
}

.compact-empty-state {
  min-height: 110px;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.plan-item {
  display: flex;
  min-height: 280px;
  flex-direction: column;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: var(--el-color-primary);
    box-shadow: 0 14px 30px rgba(64, 112, 244, 0.14);
  }
}

.plan-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;

  h3 {
    margin: 0;
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 700;
  }

  p {
    margin: 5px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    line-height: 1.5;
  }
}

.plan-price {
  padding-bottom: 15px;
  margin-top: 20px;
  color: var(--el-color-primary);
  font-size: 26px;
  font-weight: 700;
  border-bottom: 1px solid var(--el-border-color-lighter);

  small {
    color: var(--el-text-color-placeholder);
    font-size: 11px;
    font-weight: 400;
  }
}

.plan-meta {
  display: flex;
  margin: 15px 0 18px;
  flex: 1;
  flex-direction: column;
  gap: 10px;

  > div {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  strong {
    max-width: 55%;
    overflow: hidden;
    color: var(--el-text-color-primary);
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.purchase-button {
  width: 100%;
  margin-top: auto;
}

@media (max-width: 1100px) {
  .personal-subscription-page {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .subscription-section {
    padding: 16px;
  }

  .section-header {
    flex-direction: column;
  }

  .current-subscription-list,
  .plan-grid,
  .subscription-stat-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .subscription-item-header {
    align-items: flex-start;
  }
}
</style>
