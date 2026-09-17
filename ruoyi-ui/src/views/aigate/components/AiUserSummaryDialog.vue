<template>
  <el-dialog
    title="用户关键信息"
    v-model="visible"
    width="min(820px, 92vw)"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="user-summary">
      <h4 class="user-summary__section-title">基本信息</h4>
      <el-descriptions
        :column="descriptionColumns"
        border
        :label-width="descriptionLabelWidth"
        class="user-summary__descriptions"
      >
        <el-descriptions-item label="登录账号">{{ userSummary.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户昵称">{{ userSummary.nickName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag v-if="userSummary.status != null" :type="userSummary.status === '0' ? 'success' : 'danger'">
            {{ userSummary.status === '0' ? '正常' : '停用' }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ userSummary.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userSummary.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注信息">{{ userSummary.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ parseTime(userSummary.createTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ parseTime(userSummary.loginDate) || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="user-summary__section-title">余额信息</h4>
      <el-descriptions
        :column="descriptionColumns"
        border
        :label-width="descriptionLabelWidth"
        class="user-summary__descriptions"
      >
        <el-descriptions-item label="当前余额">
          <span class="user-summary__amount">{{ formatCurrency(userSummary.balance, 10, true) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="可用余额">
          <span class="user-summary__amount user-summary__amount--available">
            {{ formatCurrency(userSummary.availableBalance, 10, true) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="冻结余额">{{ formatCurrency(userSummary.frozenBalance, 4) }}</el-descriptions-item>
        <el-descriptions-item label="累计应计金额">
          {{ formatCurrency(userSummary.usedBalance, 10, true) }}
        </el-descriptions-item>
        <el-descriptions-item label="累计实扣">
          {{ formatCurrency(userSummary.totalChargedAmount, 10, true) }}
        </el-descriptions-item>
        <el-descriptions-item label="累计未覆盖">
          {{ formatCurrency(userSummary.totalUncoveredAmount, 10, true) }}
        </el-descriptions-item>
        <el-descriptions-item label="请求次数">{{ formatRequestCount(userSummary.requestCount) }}</el-descriptions-item>
        <el-descriptions-item label="并发上限">{{ concurrencyLimitText(userSummary.aiConcurrencyLimit) }}</el-descriptions-item>
        <el-descriptions-item label="扣费偏好" :span="2">
          {{ billingPreferenceLabel(userSummary.billingPreference) }}
        </el-descriptions-item>
      </el-descriptions>

      <h4 class="user-summary__section-title">生效订阅</h4>
      <el-table
        v-if="userSummary.subscriptions.length > 0"
        :data="userSummary.subscriptions"
        border
        size="small"
      >
        <el-table-column label="套餐" prop="planTitle" min-width="130" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="状态" width="88" align="center">
          <template #default="scope">
            <el-tag type="success" size="small">{{ subscriptionStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="180" align="center">
          <template #default="scope">
            <div>起始：{{ parseTime(scope.row.startTime) || '-' }}</div>
            <div class="user-summary__secondary">截止：{{ parseTime(scope.row.endTime) || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="额度用量" min-width="150" align="center">
          <template #default="scope">
            <div>已用：{{ formatCurrency(scope.row.amountUsed, 10, true) }}</div>
            <div class="user-summary__secondary">总额：{{ subscriptionTotalText(scope.row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="冻结额度" width="110" align="center">
          <template #default="scope">
            {{ formatCurrency(scope.row.frozenBalance, 10, true) }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无生效订阅" :image-size="72" />
    </div>
    <template #footer>
      <el-button @click="visible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { getAiLogUserSummary } from '@/api/aigate/log'
import type { AiBillingPreference, AiLogUserSummary, AiUserSubscription } from '@/types'
import { parseTime } from '@/utils/ruoyi'
import { formatCurrency } from '../common'

const visible = ref(false)
const loading = ref(false)
const screenWidth = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)
const descriptionColumns = computed(() => (screenWidth.value < 768 ? 1 : 2))
const descriptionLabelWidth = computed(() => (screenWidth.value < 768 ? '96px' : '134px'))
const userSummary = ref<AiLogUserSummary>({ subscriptions: [] })
let requestSequence = 0

function updateScreenWidth(): void {
  screenWidth.value = window.innerWidth
}

onMounted(() => {
  updateScreenWidth()
  window.addEventListener('resize', updateScreenWidth)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateScreenWidth)
})

async function open(userId: number, username?: string): Promise<void> {
  const currentRequest = ++requestSequence
  userSummary.value = {
    userId,
    username,
    subscriptions: []
  }
  visible.value = true
  loading.value = true
  try {
    const response = await getAiLogUserSummary(userId)
    if (currentRequest !== requestSequence) {
      return
    }
    userSummary.value = {
      ...response.data,
      subscriptions: response.data?.subscriptions || []
    }
  } catch (error) {
    if (currentRequest === requestSequence) {
      visible.value = false
    }
    console.error('加载用户关键信息失败:', error)
  } finally {
    if (currentRequest === requestSequence) {
      loading.value = false
    }
  }
}

function formatRequestCount(value?: number): string {
  const count = Number(value || 0)
  return Number.isFinite(count) ? count.toLocaleString('zh-CN') : '0'
}

function concurrencyLimitText(value?: number): string {
  if (value == null) {
    return '-'
  }
  return value === 0 ? '已禁用' : `${value} 个并发`
}

function billingPreferenceLabel(value?: AiBillingPreference): string {
  const labels: Record<AiBillingPreference, string> = {
    subscription_first: '订阅优先',
    wallet_first: '钱包优先',
    subscription_only: '仅订阅',
    wallet_only: '仅钱包'
  }
  return value ? labels[value] : '订阅优先（默认）'
}

function subscriptionStatusLabel(status?: string): string {
  const labels: Record<string, string> = {
    active: '生效中',
    cancelled: '已取消',
    expired: '已过期'
  }
  return labels[status || ''] || status || '-'
}

function subscriptionTotalText(subscription: AiUserSubscription): string {
  const total = Number(subscription.amountTotal || 0)
  return total === 0 ? '不限' : formatCurrency(subscription.amountTotal, 10, true)
}

defineExpose({ open })
</script>

<style scoped>
.user-summary__section-title {
  padding-bottom: 8px;
  margin: 20px 0 12px;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color-light);
}

.user-summary__section-title:first-child {
  margin-top: 0;
}

.user-summary__amount {
  color: var(--el-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.user-summary__amount--available {
  color: var(--el-color-success);
}

.user-summary__descriptions :deep(.el-descriptions__table) {
  table-layout: fixed;
}

.user-summary__secondary {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
