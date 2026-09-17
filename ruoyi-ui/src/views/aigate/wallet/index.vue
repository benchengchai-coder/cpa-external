<template>
  <div class="app-container home wallet-page">
    <div class="wallet-summary-grid" v-loading="walletInfoLoading">
      <div class="wallet-summary-item">
        <span>可用余额</span>
        <strong>{{ formatCurrency(walletInfo.availableBalance, 4, true) }}</strong>
      </div>
      <div class="wallet-summary-item">
        <span>累计应计金额</span>
        <strong>{{ formatCurrency(walletInfo.usedBalance, 4, true) }}</strong>
      </div>
      <div class="wallet-summary-item wallet-summary-item--charged">
        <span>累计实扣</span>
        <strong>{{ formatCurrency(walletInfo.totalChargedAmount, 4, true) }}</strong>
      </div>
      <div class="wallet-summary-item wallet-summary-item--uncovered">
        <span>累计未覆盖</span>
        <strong>{{ formatCurrency(walletInfo.totalUncoveredAmount, 4, true) }}</strong>
      </div>
    </div>
    <div class="wallet-main-grid">
      <div class="wallet-column wallet-left-column">
        <el-card class="surface-card account-card" shadow="never">
          <template #header>
            <div class="panel-heading">
              <div>
                <span class="panel-kicker">TOP UP</span>
                <h2>{{ pendingPayOrder ? '完成当前支付' : onlineRechargeEnabled ? '选择充值金额' : '在线充值' }}</h2>
                <p>
                  {{
                    pendingPayOrder
                      ? '支付结果会自动同步，请保留当前页面。'
                      : !onlineRechargeEnabled
                        ? '当前暂未开放在线充值服务。'
                      : '选择常用金额或输入自定义金额，然后前往支付宝完成付款。'
                  }}
                </p>
              </div>
              <div class="panel-heading-actions">
                <span class="secure-badge">安全支付</span>
                <el-button class="history-button" @click="handleOpenBillDrawer">
                  <el-icon><Tickets /></el-icon>
                  充值记录
                </el-button>
              </div>
            </div>
          </template>

          <template v-if="onlineRechargeStatusLoading">
            <div class="recharge-status-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在获取在线充值状态...</span>
            </div>
          </template>

          <template v-else-if="!onlineRechargeEnabled">
            <div class="recharge-disabled-state">
              <div class="recharge-disabled-icon">
                <el-icon><Lock /></el-icon>
              </div>
              <h3>管理员未开启在线充值业务</h3>
              <p>当前无法使用支付宝在线充值。如需充值，可以使用下方的兑换充值码。</p>
            </div>
          </template>

          <template v-else-if="!onlineRechargeAvailable">
            <div class="recharge-disabled-state">
              <div class="recharge-disabled-icon is-warning">
                <el-icon><WarningFilled /></el-icon>
              </div>
              <h3>在线充值暂不可用</h3>
              <p>支付服务当前未就绪，请联系管理员检查支付配置。</p>
            </div>
          </template>

          <template v-else-if="pendingPayOrder">
            <div class="pending-payment-panel">
              <div class="pending-order-main">
                <div class="pending-status-icon">
                  <el-icon><Loading /></el-icon>
                </div>
                <div class="pending-order-copy">
                  <span class="pending-order-label">支付宝订单已创建</span>
                  <h3>等待支付完成</h3>
                  <p>支付页面已在新窗口打开。完成付款后，本页面会自动更新订单状态。</p>
                </div>
              </div>

              <div class="pending-order-summary">
                <div class="pending-summary-item">
                  <span>充值金额</span>
                  <strong>{{ formatCurrency(pendingPayOrder.amount, 2, true) }}</strong>
                </div>
                <div class="pending-summary-item pending-summary-order">
                  <span>订单编号</span>
                  <strong>{{ pendingPayOrder.outTradeNo }}</strong>
                </div>
                <div class="pending-summary-item pending-summary-countdown">
                  <span>剩余支付时间</span>
                  <strong>{{ pendingCountdownText }}</strong>
                </div>
              </div>

              <div class="pending-actions">
                <el-button type="primary" :loading="reopenPayLoading" @click="handleReopenPendingPay">
                  重新打开支付页面
                </el-button>
                <el-button :loading="cancelOrderLoading" @click="handleCancelPendingOrder">取消订单</el-button>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="recharge-step">
              <div class="step-label">
                <span>1</span>
                选择金额
              </div>
              <div class="recharge-amount-grid">
                <button
                  v-for="amount in rechargeAmountOptions"
                  :key="amount"
                  type="button"
                  class="recharge-amount-card"
                  :class="{ 'is-active': selectedRechargeAmount === amount }"
                  @click="handleSelectRechargeAmount(amount)"
                >
                  <span class="recharge-amount-currency">$</span>
                  <strong>{{ amount }}</strong>
                  <small>USD</small>
                  <span class="amount-check">✓</span>
                </button>
              </div>
            </div>

            <div class="recharge-options-grid">
              <div class="recharge-step recharge-custom-step">
                <div class="step-label">
                  <span>2</span>
                  自定义金额
                </div>
                <div class="custom-amount-box">
                  <span class="custom-amount-symbol">$</span>
                  <el-input-number
                    v-model="rechargeQuantity"
                    :min="0.1"
                    :step="0.1"
                    :precision="1"
                    :controls="false"
                    class="recharge-quantity-input"
                    @input="handleRechargeQuantityInput"
                  />
                  <span class="custom-amount-unit">USD</span>
                </div>
                <p class="field-help">最低充值 0.1 美元</p>
              </div>

              <div class="recharge-step">
                <div class="step-label">
                  <span>3</span>
                  支付方式
                </div>
                <button
                  type="button"
                  class="pay-method-card is-active"
                  @click="selectedPayMethod = 'alipay'"
                >
                  <span class="pay-method-icon alipay-icon">支</span>
                  <span class="pay-method-copy">
                    <strong>支付宝</strong>
                    <small>跳转至支付宝安全收银台</small>
                  </span>
                  <span class="pay-method-check">✓</span>
                </button>
              </div>
            </div>

            <div class="recharge-checkout">
              <div class="checkout-amount">
                <span>本次充值</span>
                <strong>{{ formatCurrency(rechargeQuantity, 2, true) }}</strong>
              </div>
              <el-button
                type="primary"
                class="confirm-pay-button"
                :disabled="!rechargeQuantity || rechargeQuantity <= 0"
                :loading="rechargeLoading"
                @click="handleConfirmRecharge"
              >
                前往支付宝支付
                <el-icon class="checkout-arrow"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
        </el-card>

        <div class="preference-redeem-grid">
          <el-card class="surface-card utility-card preference-card" shadow="never">
            <div class="utility-card-icon preference-icon">
              <el-icon><Switch /></el-icon>
            </div>
            <div class="utility-card-heading">
              <span>扣费偏好</span>
              <p>设置订阅与钱包的使用顺序</p>
            </div>
            <el-select v-model="billingPreference" class="preference-select" @change="handlePreferenceChange">
              <el-option
                v-for="option in billingPreferenceOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <div class="preference-description">{{ currentBillingPreference.description }}</div>
          </el-card>

          <el-card class="surface-card utility-card redeem-card" shadow="never">
            <a
              class="redeem-shop-link"
              href="https://pay.ldxp.cn/shop/35UDLZ7R"
              target="_blank"
              rel="noopener noreferrer"
            >
              小铺购买
              <el-icon><TopRight /></el-icon>
            </a>
            <div class="utility-card-icon redeem-icon">
              <el-icon><Ticket /></el-icon>
            </div>
            <div class="utility-card-heading">
              <span>兑换充值码</span>
              <p>输入兑换码，权益将立即生效</p>
            </div>
            <div class="redeem-actions">
              <el-input
                v-model="redeemCodeInput"
                placeholder="请输入兑换码"
                clearable
                @keyup.enter="handleRedeem"
              />
              <el-button type="primary" :loading="redeemLoading" @click="handleRedeem">立即兑换</el-button>
            </div>
          </el-card>
        </div>
      </div>

    </div>

    <el-drawer
      v-model="billDrawerVisible"
      title="充值记录"
      direction="rtl"
      size="min(760px, 94vw)"
      class="bill-drawer"
    >
      <div class="bill-drawer-content">
        <div class="drawer-intro">
          <span>交易流水</span>
          <p>查看兑换、在线支付与其他充值记录。</p>
        </div>

        <el-form
          :model="queryParams"
          ref="queryRef"
          :inline="true"
          v-show="showSearch"
          label-width="58px"
          class="record-search"
        >
          <el-form-item label="来源" prop="sourceName">
            <el-input
              v-model="queryParams.sourceName"
              placeholder="搜索来源名称"
              clearable
              class="source-input"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="recordList" class="bill-drawer-table">
          <el-table-column label="时间" align="left" prop="createTime" width="172">
            <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="类型" align="center" prop="type" width="110">
            <template #default="scope">
              <dict-tag :options="ai_recharge_type" :value="scope.row.type" />
            </template>
          </el-table-column>
          <el-table-column
            label="来源"
            align="left"
            prop="sourceName"
            min-width="160"
            :show-overflow-tooltip="true"
          />
          <el-table-column label="金额" align="right" prop="amount" width="118">
            <template #default="scope">
              <span class="record-amount">{{ formatCurrency(scope.row.amount, 4) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="82">
            <template #default="scope">
              <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" effect="light" round>
                {{ scope.row.status === '0' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getRecordList"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="AiWallet">
import {
  getWalletInfo,
  redeemCode,
  listRechargeRecord,
  getOnlineRechargeStatus,
  createAlipayOrder,
  getPendingAlipayOrder,
  reopenAlipayOrder,
  cancelAlipayOrder,
  queryOrderStatus,
  type PendingAlipayOrder
} from '@/api/aigate/wallet'
import { getSelfSubscription, updateBillingPreference } from '@/api/aigate/subscription'
import type {
  AiBillingPreference,
  AiRechargeRecord,
  AiWalletInfo
} from '@/types'
import { formatCurrency } from '../common'

const { proxy } = getCurrentInstance() as any
const { ai_recharge_type } = useDict('ai_recharge_type')
const emit = defineEmits<{ (event: 'balance-changed'): void }>()

const walletInfoLoading = ref(false)
const walletInfo = reactive<AiWalletInfo>({
  balance: 0,
  frozenBalance: 0,
  availableBalance: 0,
  usedBalance: 0,
  totalChargedAmount: 0,
  totalUncoveredAmount: 0,
  requestCount: 0
})

const redeemCodeInput = ref('')
const redeemLoading = ref(false)
const recordList = ref<AiRechargeRecord[]>([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const billDrawerVisible = ref(false)
const billingPreference = ref<AiBillingPreference>('subscription_first')
const billingPreferenceOptions = [
  {
    value: 'subscription_first',
    label: '订阅优先',
    description: '优先消耗订阅额度，额度不足时自动使用钱包。'
  },
  {
    value: 'wallet_first',
    label: '钱包优先',
    description: '优先从钱包扣费，钱包不可用时再使用订阅额度。'
  },
  {
    value: 'subscription_only',
    label: '仅订阅',
    description: '只允许使用订阅额度，不会从钱包自动扣费。'
  },
  {
    value: 'wallet_only',
    label: '仅钱包',
    description: '只允许使用钱包，不消耗任何订阅额度。'
  }
] satisfies Array<{ value: AiBillingPreference; label: string; description: string }>
type RechargePayMethod = 'alipay'
const rechargeAmountOptions = [10, 20, 50, 100]
const selectedRechargeAmount = ref<number | undefined>(rechargeAmountOptions[0])
const rechargeQuantity = ref(rechargeAmountOptions[0])
const selectedPayMethod = ref<RechargePayMethod>('alipay')
const rechargeLoading = ref(false)
const onlineRechargeStatusLoading = ref(true)
const onlineRechargeEnabled = ref(false)
const onlineRechargeAvailable = ref(false)
const pendingPayOrder = ref<PendingAlipayOrder | null>(null)
const pendingRemainingSeconds = ref(0)
const reopenPayLoading = ref(false)
const cancelOrderLoading = ref(false)
let pendingCountdownTimer: ReturnType<typeof setInterval> | null = null
let pendingStatusTimer: ReturnType<typeof setInterval> | null = null

const currentBillingPreference = computed(
  () => billingPreferenceOptions.find((item) => item.value === billingPreference.value) || billingPreferenceOptions[0]
)

const pendingCountdownText = computed(() => {
  const seconds = Math.max(0, pendingRemainingSeconds.value)
  const minutesText = String(Math.floor(seconds / 60)).padStart(2, '0')
  const secondsText = String(seconds % 60).padStart(2, '0')
  return `${minutesText}:${secondsText}`
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    sourceName: undefined
  } as Record<string, any>
})

const { queryParams } = toRefs(data)

function loadWalletInfo() {
  walletInfoLoading.value = true
  getWalletInfo()
    .then((response) => {
      Object.assign(walletInfo, response.data || {})
    })
    .catch(() => {})
    .finally(() => {
      walletInfoLoading.value = false
    })
}

function loadBillingPreference() {
  getSelfSubscription()
    .then((response) => {
      billingPreference.value = response.data?.billingPreference || 'subscription_first'
    })
    .catch(() => {})
}

function loadOnlineRechargeStatus() {
  onlineRechargeStatusLoading.value = true
  getOnlineRechargeStatus()
    .then((response) => {
      onlineRechargeEnabled.value = response.data?.enabled === true
      onlineRechargeAvailable.value = response.data?.available === true
      if (onlineRechargeAvailable.value) {
        loadPendingPayOrder()
      } else {
        setPendingOrder(null)
      }
    })
    .catch(() => {
      onlineRechargeEnabled.value = false
      onlineRechargeAvailable.value = false
      setPendingOrder(null)
    })
    .finally(() => {
      onlineRechargeStatusLoading.value = false
    })
}

function getRecordList() {
  loading.value = true
  listRechargeRecord(queryParams.value)
    .then((response) => {
      recordList.value = response.rows
      total.value = response.total
    })
    .catch(() => {
      recordList.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleOpenBillDrawer() {
  billDrawerVisible.value = true
  getRecordList()
}

function handleRedeem() {
  if (!redeemCodeInput.value) {
    proxy.$modal.msgError('请输入兑换码')
    return
  }
  redeemLoading.value = true
  redeemCode(redeemCodeInput.value)
    .then(() => {
      proxy.$modal.msgSuccess('兑换成功')
      redeemCodeInput.value = ''
      if (billDrawerVisible.value) {
        getRecordList()
      }
      loadWalletInfo()
      emit('balance-changed')
    })
    .catch(() => {})
    .finally(() => {
      redeemLoading.value = false
    })
}

function handleSelectRechargeAmount(amount: number) {
  selectedRechargeAmount.value = amount
  rechargeQuantity.value = amount
}

function handleRechargeQuantityInput() {
  selectedRechargeAmount.value = undefined
}

function normalizePendingOrder(order?: PendingAlipayOrder | null): PendingAlipayOrder | null {
  if (!order || !order.outTradeNo) {
    return null
  }
  return {
    ...order,
    amount: Number(order.amount || 0),
    remainingSeconds: Math.max(0, Number(order.remainingSeconds || 0))
  }
}

function setPendingOrder(order?: PendingAlipayOrder | null) {
  const normalized = normalizePendingOrder(order)
  pendingPayOrder.value = normalized
  pendingRemainingSeconds.value = normalized?.remainingSeconds || 0
  startPendingCountdown()
  startPendingStatusPolling()
}

function clearPendingCountdown() {
  if (pendingCountdownTimer) {
    clearInterval(pendingCountdownTimer)
    pendingCountdownTimer = null
  }
}

function clearPendingStatusPolling() {
  if (pendingStatusTimer) {
    clearInterval(pendingStatusTimer)
    pendingStatusTimer = null
  }
}

function startPendingCountdown() {
  clearPendingCountdown()
  if (!pendingPayOrder.value || pendingRemainingSeconds.value <= 0) {
    return
  }
  pendingCountdownTimer = setInterval(() => {
    pendingRemainingSeconds.value = Math.max(0, pendingRemainingSeconds.value - 1)
    if (pendingRemainingSeconds.value === 0) {
      clearPendingCountdown()
      loadPendingPayOrder()
    }
  }, 1000)
}

function startPendingStatusPolling() {
  clearPendingStatusPolling()
  const order = pendingPayOrder.value
  if (!order?.outTradeNo) {
    return
  }
  pendingStatusTimer = setInterval(() => {
    queryOrderStatus(order.outTradeNo)
      .then((response) => {
        const status = response.data?.status
        if (status === '1') {
          proxy.$modal.msgSuccess('充值成功')
          setPendingOrder(null)
          if (billDrawerVisible.value) {
            getRecordList()
          }
          loadWalletInfo()
          emit('balance-changed')
          return
        }
        if (status === '2' || status === '3' || status === 'not_found') {
          setPendingOrder(null)
        }
      })
      .catch(() => {})
  }, 3000)
}

function loadPendingPayOrder() {
  getPendingAlipayOrder()
    .then((response) => {
      setPendingOrder(response.data || null)
    })
    .catch(() => {
      setPendingOrder(null)
    })
}

function getAlipayPopupFeatures() {
  const availableWidth = window.screen.availWidth || 1100
  const availableHeight = window.screen.availHeight || 760
  const width = Math.min(1280, Math.max(960, availableWidth - 32))
  const height = Math.min(900, Math.max(720, availableHeight - 32))
  const left = Math.max(window.screenX + (window.outerWidth - width) / 2, 0)
  const top = Math.max(window.screenY + (window.outerHeight - height) / 2, 0)
  const needsScrollbar = width < 1180 || height < 860

  return [
    'popup=yes',
    `width=${Math.round(width)}`,
    `height=${Math.round(height)}`,
    `left=${Math.round(left)}`,
    `top=${Math.round(top)}`,
    'toolbar=no',
    'menubar=no',
    'location=no',
    'status=no',
    'directories=no',
    'resizable=yes',
    `scrollbars=${needsScrollbar ? 'yes' : 'no'}`
  ].join(',')
}

function openBlankPayWindow() {
  const payWindow = window.open('', 'alipayPayWindow', getAlipayPopupFeatures())
  if (!payWindow) {
    proxy.$modal.msgError('浏览器拦截了支付窗口，请允许弹出窗口后重试')
    return null
  }

  payWindow.document.open()
  payWindow.document.write('<!doctype html><html><head><title>支付宝支付</title></head><body>正在跳转支付宝付款页面...</body></html>')
  payWindow.document.close()
  payWindow.focus()
  return payWindow
}

function writePayFormToWindow(payWindow: Window, payForm?: string) {
  if (!payForm) {
    proxy.$modal.msgError('获取支付表单失败')
    payWindow.close()
    return false
  }
  payWindow.document.open()
  payWindow.document.write(payForm)
  payWindow.document.close()
  payWindow.focus()
  return true
}

function handleConfirmRecharge() {
  if (!onlineRechargeEnabled.value) {
    proxy.$modal.msgError('管理员未开启在线充值业务')
    return
  }
  if (!onlineRechargeAvailable.value) {
    proxy.$modal.msgError('在线充值暂不可用，请联系管理员检查支付配置')
    return
  }

  const amount = rechargeQuantity.value
  if (!amount || amount <= 0) {
    proxy.$modal.msgError('请输入有效的充值金额')
    return
  }

  const payWindow = openBlankPayWindow()
  if (!payWindow) {
    return
  }

  rechargeLoading.value = true
  createAlipayOrder(amount)
    .then((response) => {
      setPendingOrder(response.data || null)
      writePayFormToWindow(payWindow, response.data?.payForm)
    })
    .catch(() => {
      payWindow.close()
    })
    .finally(() => {
      rechargeLoading.value = false
    })
}

function handleReopenPendingPay() {
  const outTradeNo = pendingPayOrder.value?.outTradeNo
  if (!outTradeNo) {
    return
  }
  const payWindow = openBlankPayWindow()
  if (!payWindow) {
    return
  }

  reopenPayLoading.value = true
  reopenAlipayOrder(outTradeNo)
    .then((response) => {
      setPendingOrder(response.data || null)
      writePayFormToWindow(payWindow, response.data?.payForm)
    })
    .catch(() => {
      payWindow.close()
      loadPendingPayOrder()
    })
    .finally(() => {
      reopenPayLoading.value = false
    })
}

function handleCancelPendingOrder() {
  const outTradeNo = pendingPayOrder.value?.outTradeNo
  if (!outTradeNo) {
    return
  }
  cancelOrderLoading.value = true
  cancelAlipayOrder(outTradeNo)
    .then(() => {
      proxy.$modal.msgSuccess('订单已取消')
      setPendingOrder(null)
    })
    .catch(() => {
      loadPendingPayOrder()
    })
    .finally(() => {
      cancelOrderLoading.value = false
    })
}

function handlePreferenceChange(value: AiBillingPreference) {
  updateBillingPreference(value).then(() => {
    proxy.$modal.msgSuccess('扣费偏好已更新')
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getRecordList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

loadBillingPreference()
loadOnlineRechargeStatus()
loadWalletInfo()

onBeforeUnmount(() => {
  clearPendingCountdown()
  clearPendingStatusPolling()
})
</script>

<style scoped lang="scss">
.wallet-page {
  --wallet-radius: 18px;
  --wallet-border: color-mix(in srgb, var(--el-border-color-lighter) 82%, transparent);
  --wallet-shadow: 0 14px 36px rgba(24, 34, 64, 0.13);
  width: min(100%, 1480px);
  margin: 0 auto;
  padding: 24px;
  overflow-x: hidden;
}

.wallet-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  width: min(100%, 960px);
  margin: 0 auto 22px;
  gap: 14px;
}

.wallet-summary-item {
  display: flex;
  min-width: 0;
  padding: 18px;
  flex-direction: column;
  gap: 8px;
  background: var(--el-bg-color);
  border: 1px solid var(--wallet-border);
  border-radius: 14px;
  box-shadow: var(--el-box-shadow-light);

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  strong {
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-family: 'SF Mono', 'Cascadia Code', Consolas, monospace;
    font-size: 20px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.wallet-summary-item--charged strong {
  color: var(--el-color-danger);
}

.wallet-summary-item--uncovered strong {
  color: var(--el-color-warning);
}

.panel-kicker {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.wallet-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 960px);
  gap: 22px;
  align-items: start;
  justify-content: center;
}

.wallet-column {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 22px;
}

.surface-card {
  box-sizing: border-box;
  overflow: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--wallet-border);
  border-radius: var(--wallet-radius);
  box-shadow: var(--wallet-shadow);
}

:deep(.surface-card.el-card) {
  --el-card-border-color: transparent;
  border: 1px solid var(--wallet-border);
  border-radius: var(--wallet-radius);
}

:deep(.surface-card > .el-card__header) {
  padding: 24px 26px 18px;
  border-bottom: 0;
}

:deep(.surface-card > .el-card__body) {
  padding: 8px 26px 26px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;

  h2 {
    margin: 5px 0 5px;
    color: var(--el-text-color-primary);
    font-size: 21px;
    font-weight: 800;
    line-height: 1.3;
    letter-spacing: -0.3px;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
    line-height: 1.6;
  }
}

.panel-kicker {
  color: var(--el-color-primary);
}

.panel-heading-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: stretch;
  flex-direction: column;
  gap: 8px;
}

.secure-badge {
  padding: 7px 10px;
  color: var(--el-color-success);
  font-size: 11px;
  font-weight: 700;
  text-align: center;
  background: var(--el-color-success-light-9);
  border: 1px solid var(--el-color-success-light-7);
  border-radius: 999px;
}

.history-button {
  height: 32px;
  padding: 0 11px;
  margin: 0;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
  border-radius: 9px;

  &:hover,
  &:focus {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-8);
    border-color: var(--el-color-primary-light-5);
  }
}

.recharge-step + .recharge-step {
  margin-top: 20px;
}

.step-label {
  display: flex;
  margin-bottom: 12px;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 750;

  > span {
    display: grid;
    width: 22px;
    height: 22px;
    place-items: center;
    color: var(--el-color-primary);
    font-size: 11px;
    background: var(--el-color-primary-light-9);
    border-radius: 7px;
  }
}

.recharge-amount-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.recharge-amount-card {
  position: relative;
  display: flex;
  min-height: 116px;
  padding: 17px 16px;
  overflow: hidden;
  align-items: baseline;
  justify-content: center;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;

  strong {
    font-family: 'SF Mono', 'Cascadia Code', Consolas, monospace;
    font-size: 28px;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
  }

  small {
    position: absolute;
    right: 14px;
    bottom: 12px;
    color: var(--el-text-color-placeholder);
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.1em;
  }

  &:hover {
    transform: translateY(-2px);
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 12px 24px rgba(64, 112, 244, 0.1);
  }

  &.is-active {
    color: var(--el-color-primary);
    background:
      linear-gradient(145deg, var(--el-color-primary-light-9), var(--el-fill-color-blank));
    border-color: var(--el-color-primary);
    box-shadow: 0 12px 26px rgba(64, 112, 244, 0.14);
  }
}

.recharge-amount-currency {
  margin-right: 3px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  font-weight: 800;
}

.amount-check {
  position: absolute;
  top: 10px;
  right: 10px;
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  background: var(--el-color-primary);
  border-radius: 50%;
  opacity: 0;
  transform: scale(0.6);
  transition: 0.2s ease;
}

.recharge-amount-card.is-active .amount-check {
  opacity: 1;
  transform: scale(1);
}

.recharge-options-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.82fr) minmax(0, 1.18fr);
  gap: 18px;
  margin-top: 22px;
}

.recharge-options-grid .recharge-step + .recharge-step {
  margin-top: 0;
}

.custom-amount-box {
  display: flex;
  height: 68px;
  padding: 0 16px;
  align-items: center;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  transition: border-color 0.2s ease;

  &:focus-within {
    border-color: var(--el-color-primary);
  }
}

.custom-amount-symbol {
  color: var(--el-text-color-secondary);
  font-size: 17px;
  font-weight: 800;
}

.custom-amount-unit {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
}

.recharge-quantity-input {
  flex: 1;
  min-width: 0;

  :deep(.el-input__wrapper) {
    padding: 0 8px;
    background: transparent;
    box-shadow: none !important;
  }

  :deep(.el-input__inner) {
    color: var(--el-text-color-primary);
    font-family: 'SF Mono', 'Cascadia Code', Consolas, monospace;
    font-size: 24px;
    font-weight: 800;
    text-align: left;
  }
}

.field-help {
  margin: 7px 0 0;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.pay-method-card {
  display: flex;
  width: 100%;
  min-height: 68px;
  padding: 11px 14px;
  align-items: center;
  gap: 12px;
  color: var(--el-text-color-primary);
  text-align: left;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 14px;
  cursor: pointer;
}

.pay-method-icon {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  color: #ffffff;
  font-size: 21px;
  font-weight: 800;
  background: #1677ff;
  border-radius: 11px;
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.2);
}

.pay-method-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;

  strong {
    font-size: 14px;
  }

  small {
    overflow: hidden;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.pay-method-check {
  display: grid;
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  place-items: center;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  background: var(--el-color-primary);
  border-radius: 50%;
}

.recharge-checkout {
  display: flex;
  padding: 18px;
  margin-top: 24px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 15px;
}

.checkout-amount {
  display: flex;
  flex-direction: column;
  gap: 4px;

  span {
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }

  strong {
    color: var(--el-text-color-primary);
    font-family: 'SF Mono', 'Cascadia Code', Consolas, monospace;
    font-size: 22px;
    font-weight: 800;
  }
}

.confirm-pay-button {
  min-width: 190px;
  height: 44px;
  border-radius: 11px;
  box-shadow: 0 10px 22px rgba(64, 112, 244, 0.2);
}

.checkout-arrow {
  margin-left: 6px;
}

.preference-redeem-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

:deep(.utility-card > .el-card__body) {
  position: relative;
  display: flex;
  height: 100%;
  padding: 22px;
  flex-direction: column;
}

.utility-card {
  min-height: 236px;
}

.utility-card-icon {
  display: grid;
  width: 42px;
  height: 42px;
  margin-bottom: 18px;
  place-items: center;
  font-size: 20px;
  border-radius: 13px;
}

.preference-icon {
  color: #6f55e8;
  background: rgba(111, 85, 232, 0.11);
}

.redeem-icon {
  color: #d5850b;
  background: rgba(245, 168, 37, 0.13);
}

.redeem-shop-link {
  position: absolute;
  top: 22px;
  right: 22px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
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

.utility-card-heading {
  margin-bottom: 16px;

  span {
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 800;
  }

  p {
    margin: 5px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.55;
  }
}

.preference-select {
  width: 100%;
}

.preference-description {
  padding: 10px 12px;
  margin-top: 12px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 1.6;
  background: var(--el-fill-color-lighter);
  border-radius: 10px;
}

.redeem-actions {
  display: flex;
  margin-top: auto;
  flex-direction: column;
  gap: 10px;

  .el-button {
    width: 100%;
    height: 38px;
    margin-left: 0;
    border-radius: 9px;
  }
}

.recharge-status-loading,
.recharge-disabled-state {
  display: flex;
  min-height: 250px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;
}

.recharge-status-loading {
  gap: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;

  .el-icon {
    color: var(--el-color-primary);
    font-size: 28px;
  }
}

.recharge-disabled-state {
  padding: 28px 16px;

  h3 {
    margin: 16px 0 8px;
    color: var(--el-text-color-primary);
    font-size: 18px;
    font-weight: 800;
  }

  p {
    max-width: 460px;
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
    line-height: 1.7;
  }
}

.recharge-disabled-icon {
  display: grid;
  width: 58px;
  height: 58px;
  color: var(--el-color-info);
  font-size: 27px;
  background: var(--el-fill-color-light);
  border-radius: 18px;
  place-items: center;

  &.is-warning {
    color: var(--el-color-warning);
    background: var(--el-color-warning-light-9);
  }
}

.pending-payment-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pending-order-main {
  display: flex;
  padding: 22px;
  align-items: center;
  gap: 18px;
  background:
    linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-fill-color-lighter));
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 16px;
}

.pending-status-icon {
  display: grid;
  width: 58px;
  height: 58px;
  flex: 0 0 auto;
  place-items: center;
  color: var(--el-color-primary);
  font-size: 28px;
  background: var(--el-bg-color);
  border-radius: 50%;

  .el-icon {
    animation: pending-payment-rotate 1.2s linear infinite;
  }
}

.pending-order-copy {
  min-width: 0;

  h3 {
    margin: 4px 0 6px;
    color: var(--el-text-color-primary);
    font-size: 20px;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.65;
  }
}

.pending-order-label {
  color: var(--el-color-primary);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.pending-order-summary {
  display: grid;
  grid-template-columns: 0.7fr 1.3fr 0.8fr;
  gap: 10px;
}

.pending-summary-item {
  display: flex;
  min-width: 0;
  padding: 14px;
  flex-direction: column;
  gap: 6px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;

  span {
    color: var(--el-text-color-secondary);
    font-size: 10px;
  }

  strong {
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-family: 'SF Mono', 'Cascadia Code', Consolas, monospace;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.pending-summary-countdown strong {
  color: var(--el-color-primary);
  font-size: 22px;
}

.pending-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;

  .el-button {
    min-width: 142px;
    margin-left: 0;
    border-radius: 10px;
  }
}

@keyframes pending-payment-rotate {
  to {
    transform: rotate(360deg);
  }
}

.drawer-intro {
  padding: 14px 16px;
  margin-bottom: 18px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;

  span {
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 800;
  }

  p {
    margin: 4px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.bill-drawer-content {
  display: flex;
  min-height: 100%;
  flex-direction: column;
}

.record-search {
  margin-bottom: 8px;
}

.source-input {
  width: 210px;
}

.bill-drawer-table {
  flex: 1;
}

.record-amount {
  color: var(--el-text-color-primary);
  font-family: 'SF Mono', 'Cascadia Code', Consolas, monospace;
  font-weight: 750;
}

:deep(.bill-drawer .el-drawer__body) {
  padding: 16px 20px 20px;
  overflow: auto;
}

@media (max-width: 860px) {
  .wallet-page {
    padding: 16px;
  }

  .recharge-amount-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .wallet-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .preference-redeem-grid,
  .recharge-options-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .pending-order-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pending-summary-order {
    grid-column: 1 / -1;
    grid-row: 2;
  }
}

@media (max-width: 640px) {
  .wallet-page {
    padding: 10px;
  }

  .wallet-main-grid,
  .wallet-column,
  .preference-redeem-grid {
    gap: 14px;
  }

  .wallet-summary-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
  }

  :deep(.surface-card > .el-card__header) {
    padding: 20px 18px 14px;
  }

  :deep(.surface-card > .el-card__body) {
    padding: 8px 18px 20px;
  }

  .panel-heading {
    flex-direction: column;
  }

  .panel-heading-actions {
    align-items: flex-start;
  }

  .recharge-amount-card {
    min-height: 94px;
  }

  .recharge-checkout {
    align-items: stretch;
    flex-direction: column;
  }

  .confirm-pay-button {
    width: 100%;
    min-width: 0;
  }

  .pending-order-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .pending-order-summary {
    grid-template-columns: minmax(0, 1fr);
  }

  .pending-summary-order {
    grid-column: auto;
    grid-row: auto;
  }

  .pending-actions {
    flex-direction: column;

    .el-button {
      width: 100%;
      margin-left: 0;
    }
  }

  .record-search :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }

  .record-search :deep(.el-form-item__content) {
    flex: 1;
  }

  .source-input {
    width: 100%;
  }
}
</style>
