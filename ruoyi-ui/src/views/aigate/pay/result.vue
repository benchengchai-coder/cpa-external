<template>
  <div class="pay-result-page">
    <el-card class="result-card">
      <div class="result-content">
        <!-- 处理中 -->
        <div v-if="state === 'pending'" class="result-block">
          <el-icon class="result-icon pending-icon"><Loading /></el-icon>
          <div class="result-title">支付结果确认中</div>
          <div class="result-desc">正在确认支付结果，请稍候...</div>
        </div>
        <!-- 成功 -->
        <div v-else-if="state === 'success'" class="result-block">
          <el-icon class="result-icon success-icon"><CircleCheck /></el-icon>
          <div class="result-title">充值成功</div>
          <div class="result-desc">
            已到账金额：<span class="highlight">{{ formatCurrency(amount, 4, true) }}</span>
          </div>
          <div v-if="balance !== null" class="result-desc">当前余额：{{ formatCurrency(balance, 4, true) }}</div>
        </div>
        <!-- 失败/未找到 -->
        <div v-else class="result-block">
          <el-icon class="result-icon fail-icon"><CircleClose /></el-icon>
          <div class="result-title">未查询到支付结果</div>
          <div class="result-desc">{{ failMsg }}</div>
        </div>

        <div class="result-actions">
          <el-button type="primary" @click="closeWindow">关闭窗口</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="PayResult">
import { useRoute } from 'vue-router'
import { queryOrderStatus } from '@/api/aigate/wallet'
import { formatCurrency } from '../common'

const route = useRoute()

type ResultState = 'pending' | 'success' | 'fail'
interface OrderStatusData {
  status: string
  balance?: number
  amount?: number
}
const state = ref<ResultState>('pending')
const balance = ref<number | null>(null)
const amount = ref<number | undefined>(undefined)
const failMsg = ref('订单可能尚未支付，或支付结果仍在处理中，请稍后在钱包页查看余额。')

// 轮询参数
const POLL_INTERVAL = 2500
const MAX_POLL_TIMES = 24 // 最多轮询约 1 分钟
let pollTimer: ReturnType<typeof setTimeout> | null = null
let pollTimes = 0

function clearTimer() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

function poll() {
  const outTradeNo = route.query.out_trade_no as string | undefined
  if (!outTradeNo) {
    state.value = 'fail'
    failMsg.value = '缺少订单号参数。'
    return
  }
  pollTimes++
  queryOrderStatus(outTradeNo)
    .then((response) => {
      const data = (response.data || {}) as OrderStatusData
      if (data.status === '1') {
        // 已支付
        state.value = 'success'
        balance.value = data.balance ?? null
        amount.value = data.amount
        clearTimer()
        return
      }
      if (data.status === 'not_found') {
        state.value = 'fail'
        clearTimer()
        return
      }
      // 待支付或已关闭，继续轮询直到超时
      if (pollTimes >= MAX_POLL_TIMES) {
        state.value = 'fail'
        clearTimer()
      } else {
        pollTimer = setTimeout(poll, POLL_INTERVAL)
      }
    })
    .catch(() => {
      // 接口异常（如未登录），继续轮询几次，超时则提示
      if (pollTimes >= MAX_POLL_TIMES) {
        state.value = 'fail'
        failMsg.value = '查询支付结果失败，请稍后在钱包页查看余额。'
        clearTimer()
      } else {
        pollTimer = setTimeout(poll, POLL_INTERVAL)
      }
    })
}

function closeWindow() {
  window.close()
}

onMounted(() => {
  poll()
})

onBeforeUnmount(() => {
  clearTimer()
})
</script>

<style scoped lang="scss">
.pay-result-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 84px);
  padding: 20px;
}

.result-card {
  width: 100%;
  max-width: 460px;
}

.result-content {
  text-align: center;
  padding: 24px 0;
}

.result-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 8px;
}

.pending-icon {
  color: var(--el-color-primary);
  animation: rotate 1.5s linear infinite;
}

.success-icon {
  color: var(--el-color-success);
}

.fail-icon {
  color: var(--el-color-info);
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.result-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.result-desc {
  font-size: 14px;
  color: var(--el-text-color-regular);

  .highlight {
    color: var(--el-color-primary);
    font-weight: 700;
    font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', 'Courier New', monospace;
  }
}

.result-actions {
  margin-top: 28px;
}
</style>
