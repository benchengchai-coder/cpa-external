<template>
  <div class="pricing-page">
    <div class="page-inner">
      <!-- 标题区 -->
      <div class="hero-section">
        <h1 class="page-title">选择适合你的订阅</h1>
        <p class="page-desc">灵活的定价，按需选择。所有方案均包含核心功能，无隐藏费用。</p>
      </div>

      <!-- 套餐卡片 -->
      <div v-loading="loading">
        <div v-if="plans.length === 0 && !loading" class="empty-plans">暂无可用套餐</div>
        <div v-else class="plan-card-grid">
          <div v-for="plan in plans" :key="plan.planId" class="plan-card">
            <div class="plan-card-title">{{ plan.title }}</div>
            <div v-if="getSubtitle(plan)" class="plan-card-subtitle">{{ getSubtitle(plan) }}</div>
            <div class="plan-card-price">¥{{ formatPrice(plan.priceAmount) }}</div>
            <div class="plan-card-meta">
              <span
                ><el-icon><Timer /></el-icon> {{ durationText(plan) }}</span
              >
              <span
                ><el-icon><Coin /></el-icon>
                {{ !plan.amountTotal ? '不限' : '¥' + formatPrice(plan.amountTotal) }}</span
              >
            </div>
            <el-button type="primary" size="small" class="plan-card-btn" @click="handlePurchase">
              {{ isLoggedIn ? '立即购买' : '登录购买' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 底部提示 -->
      <div class="bottom-note">
        <p>所有方案均支持多 Key 管理 · 实时用量看板 · 数据加密传输</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getToken } from '@/utils/auth'
import { useRouter } from 'vue-router'
import { publicPlans } from '@/api/aigate/public'

const router = useRouter()

interface PlanItem {
  planId: number
  title: string
  subTitle: string
  priceAmount: number
  durationUnit: string
  durationValue: number
  amountTotal: number
  remark: string
  customSeconds: number | null
  allowBalancePurchase: number
}

const plans = ref<PlanItem[]>([])
const loading = ref(false)
const isLoggedIn = !!getToken()

const formatPrice = (val: number | string): string => {
  const n = Number(val)
  if (isNaN(n) || n === 0) return '0'
  // 去除末尾多余零
  return n % 1 === 0 ? String(Math.floor(n)) : n.toFixed(2).replace(/\.?0+$/, '')
}

const getSubtitle = (plan: PlanItem): string => {
  if (plan.subTitle && plan.subTitle !== plan.title) return plan.subTitle
  return ''
}

const durationText = (row: PlanItem): string => {
  const unitMap: Record<string, string> = { day: '天', week: '周', month: '月', year: '年', custom: '秒' }
  return `${row.durationUnit === 'custom' ? row.customSeconds || row.durationValue : row.durationValue}${unitMap[row.durationUnit || 'month']}`
}

const handlePurchase = (): void => {
  if (getToken()) {
    router.push('/general-service/wallet')
  } else {
    router.push('/login?redirect=/general-service/wallet')
  }
}

const loadPlans = async () => {
  loading.value = true
  try {
    const res = await publicPlans()
    if (res.code === 200 && res.data?.length) {
      plans.value = res.data
    }
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped lang="scss">
.pricing-page {
  width: 100%;
  min-height: calc(100vh - 60px);
}

.page-inner {
  max-width: 800px;
  margin: 0 auto;
  padding: 48px 24px 64px;
}

/* ── Hero ── */
.hero-section {
  text-align: center;
  margin-bottom: 36px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary, #303133);
  margin: 0 0 10px;
}

.page-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* ── 可购买套餐卡片（复用钱包页样式） ── */
.plan-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 32px;
  box-shadow: var(--el-box-shadow-light);
  border-radius: 8px;
}

.empty-plans {
  grid-column: 1 / -1;
  padding: 40px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.plan-card {
  background: var(--el-bg-color, #ffffff);
  border: 1px solid var(--el-border-color-light, #ebeef5);
  border-radius: 8px;
  padding: 16px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-shadow: var(--el-box-shadow-light);
}

html.dark .plan-card {
  background: var(--el-bg-color, #1d1e1f);
  border-color: var(--el-border-color-light, #414243);
}

.plan-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
}

.plan-card-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--el-text-color-primary, #303133);
  margin-bottom: 4px;
}

.plan-card-subtitle {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.plan-card-price {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', 'Courier New', monospace;
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 10px;
}

.plan-card-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 14px;
  flex: 1;

  .plan-card-upgrade {
    color: #e6a23c;
    font-weight: 500;
  }

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.plan-card-btn {
  width: 100%;
}

/* ── Bottom ── */
.bottom-note {
  text-align: center;

  p {
    font-size: 13px;
    color: #909399;
    margin: 0;
  }
}

/* ── Responsive ── */
@media (max-width: 640px) {
  .plan-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
