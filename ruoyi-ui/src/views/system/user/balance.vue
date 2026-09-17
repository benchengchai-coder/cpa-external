<template>
  <el-drawer
    title="余额管理"
    v-model="visible"
    direction="rtl"
    :size="drawerSize"
    append-to-body
    :before-close="handleClose"
  >
    <div v-loading="loading" class="balance-drawer-content">
      <!-- 余额信息 -->
      <h4 class="section-header">余额信息</h4>
      <el-descriptions :column="descColumn" border class="mb20">
        <el-descriptions-item label="用户名称">{{ currentUser }}</el-descriptions-item>
        <el-descriptions-item label="请求次数">{{ walletInfo.requestCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="当前余额">
          <span class="balance-amount">${{ formatAmount(walletInfo.balance) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计应计金额">
          <span class="used-amount">${{ formatAmount(walletInfo.usedBalance) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计实扣">
          <span class="used-amount">${{ formatAmount(walletInfo.totalChargedAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计未覆盖">
          <span>${{ formatAmount(walletInfo.totalUncoveredAmount) }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 并发限制 -->
      <h4 class="section-header">并发限制</h4>
      <div class="adjust-form mb20">
        <el-form :model="concurrencyForm" :label-width="isSmallScreen ? '68px' : '80px'" :label-position="isSmallScreen ? 'top' : 'right'">
          <el-form-item label="当前配置">
            <el-tag v-if="walletInfo.aiConcurrencyLimit === 0" type="danger">已禁用</el-tag>
            <el-tag v-else-if="walletInfo.aiConcurrencyLimit != null" type="primary">{{ walletInfo.aiConcurrencyLimit }} 个并发</el-tag>
            <el-tag v-else type="info">未加载</el-tag>
          </el-form-item>
          <el-form-item label="当前占用">
            <el-tag :type="activeConcurrencyType">{{ activeConcurrencyText }}</el-tag>
            <div class="form-tip">在途 AI 请求占用数（含已完成待结算的请求），随刷新更新。</div>
          </el-form-item>
          <el-form-item label="并发上限">
            <div class="adjust-row">
              <el-input-number
                v-model="concurrencyForm.aiConcurrencyLimit"
                :min="0"
                :max="1000"
                :precision="0"
                :step="1"
                controls-position="right"
                class="adjust-input"
              />
              <el-button type="primary" @click="handleSaveConcurrency" :loading="concurrencySaveLoading" class="adjust-btn">保存</el-button>
            </div>
            <div class="form-tip">允许设置 0～1000，设置为 0 将禁止该用户调用 AI 中继接口。</div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 余额调整 -->
      <h4 class="section-header">余额调整</h4>
      <div class="adjust-form mb20">
        <el-form :model="adjustForm" :label-width="isSmallScreen ? '68px' : '80px'" :label-position="isSmallScreen ? 'top' : 'right'">
          <el-form-item label="调整方式">
            <el-radio-group v-model="adjustForm.type">
              <el-radio value="add">添加</el-radio>
              <el-radio value="sub">减少</el-radio>
              <el-radio value="set">覆盖</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="金额(USD)">
            <div class="adjust-row">
              <el-input-number
                v-model="adjustForm.amount"
                :min="0"
                :precision="4"
                :step="1"
                controls-position="right"
                class="adjust-input"
              />
              <el-button type="primary" @click="handleAdjust" :loading="adjustLoading" class="adjust-btn">确认调整</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 用户计费倍率 -->
      <h4 class="section-header">用户计费倍率</h4>
      <div class="adjust-form mb20">
        <el-form :model="multiplierForm" :label-width="isSmallScreen ? '68px' : '80px'" :label-position="isSmallScreen ? 'top' : 'right'">
          <el-form-item label="当前倍率">
            <el-tag type="primary">{{ Number(multiplierForm.billingMultiplier ?? 1).toFixed(2) }}x</el-tag>
          </el-form-item>
          <el-form-item label="计费倍率">
            <div class="adjust-row">
              <el-input-number
                v-model="multiplierForm.billingMultiplier"
                :min="0"
                :precision="2"
                :step="0.01"
                controls-position="right"
                class="adjust-input"
              />
              <el-button type="primary" @click="handleSaveMultiplier" :loading="multiplierSaveLoading" class="adjust-btn">保存</el-button>
            </div>
            <div class="form-tip">按用户实际计费金额乘以该倍率，支持两位小数。</div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 充值记录 -->
      <h4 class="section-header">充值记录</h4>
      <el-table :data="rechargeList" size="small" :max-height="300" class="recharge-table">
        <el-table-column label="时间" align="center" min-width="140">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" align="center" min-width="80">
          <template #default="scope">
            <dict-tag :options="ai_recharge_type" :value="scope.row.type" />
          </template>
        </el-table-column>
        <el-table-column label="来源" align="center" prop="sourceName" :show-overflow-tooltip="true" min-width="80" />
        <el-table-column label="金额" align="center" min-width="80">
          <template #default="scope">
            <span>${{ formatAmount(scope.row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" min-width="60">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="rechargeTotal > 0"
        :total="rechargeTotal"
        v-model:page="rechargeQuery.pageNum"
        v-model:limit="rechargeQuery.pageSize"
        @pagination="getRechargeList"
        :pageSizes="[5, 10, 20]"
        :small="true"
      />
    </div>
  </el-drawer>
</template>

<script setup lang="ts" name="UserBalanceDrawer">
import { adminGetWalletInfo, adminAdjustBalance, adminListRechargeRecord, adminUpdateConcurrency } from '@/api/aigate/wallet'
import { updateUserBillingMultiplier } from '@/api/system/user'
import { useAiDrawerSize } from '@/views/aigate/common'

const { proxy } = getCurrentInstance()
const emit = defineEmits<{
  change: []
}>()
const { ai_recharge_type } = useDict('ai_recharge_type')
const drawerSize = useAiDrawerSize()
const screenWidth = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)

const isSmallScreen = computed(() => screenWidth.value < 768)
const descColumn = computed(() => isSmallScreen.value ? 1 : 2)

const updateScreenWidth = () => { screenWidth.value = window.innerWidth }
onMounted(() => window.addEventListener('resize', updateScreenWidth))
onBeforeUnmount(() => window.removeEventListener('resize', updateScreenWidth))

const visible = ref<boolean>(false)
const loading = ref<boolean>(false)
const adjustLoading = ref<boolean>(false)
const concurrencySaveLoading = ref<boolean>(false)

const currentUserId = ref<number>(0)
const currentUser = ref<string>('')
const walletInfo = reactive<Record<string, any>>({
  balance: 0,
  usedBalance: 0,
  totalChargedAmount: 0,
  totalUncoveredAmount: 0,
  requestCount: 0,
  aiConcurrencyLimit: undefined,
  activeRequestCount: 0,
  billingMultiplier: 1
})

/** 当前并发占用文案与样式（占满时给出警示色） */
const activeConcurrencyText = computed(() => {
  if (walletInfo.aiConcurrencyLimit === 0) return '已禁用'
  const active = walletInfo.activeRequestCount ?? 0
  const limit = walletInfo.aiConcurrencyLimit
  return limit != null ? `${active} / ${limit}` : `${active}`
})
const activeConcurrencyType = computed(() => {
  const active = walletInfo.activeRequestCount ?? 0
  const limit = walletInfo.aiConcurrencyLimit
  if (limit === 0) return 'danger'
  return limit != null && active >= limit ? 'warning' : 'success'
})

const concurrencyForm = reactive({
  aiConcurrencyLimit: undefined as number | undefined
})

const adjustForm = reactive({
  type: 'add',
  amount: 0
})

const rechargeList = ref<any[]>([])
const rechargeTotal = ref<number>(0)
const rechargeQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

// 用户计费倍率相关
const multiplierSaveLoading = ref<boolean>(false)
const multiplierForm = reactive({
  billingMultiplier: 1 as number
})

/** 格式化金额 */
function formatAmount(value: any): string {
  if (value == null) return '0.0000'
  return Number(value).toFixed(4)
}

/** 加载钱包信息 */
async function getWalletInfo() {
  const res = await adminGetWalletInfo(currentUserId.value)
  const data = res.data || res
  walletInfo.balance = data.balance
  walletInfo.usedBalance = data.usedBalance
  walletInfo.totalChargedAmount = data.totalChargedAmount
  walletInfo.totalUncoveredAmount = data.totalUncoveredAmount
  walletInfo.requestCount = data.requestCount
  walletInfo.aiConcurrencyLimit = Number(data.aiConcurrencyLimit)
  walletInfo.activeRequestCount = Number(data.activeRequestCount ?? 0)
  walletInfo.billingMultiplier = Number(data.billingMultiplier ?? 1)
  concurrencyForm.aiConcurrencyLimit = walletInfo.aiConcurrencyLimit
  multiplierForm.billingMultiplier = walletInfo.billingMultiplier
}

/** 保存用户AI并发上限 */
async function handleSaveConcurrency() {
  const concurrencyLimit = concurrencyForm.aiConcurrencyLimit
  if (concurrencyLimit == null || !Number.isInteger(concurrencyLimit) || concurrencyLimit < 0 || concurrencyLimit > 1000) {
    proxy.$modal.msgWarning('请输入0～1000之间的整数')
    return
  }
  if (concurrencyLimit === 0) {
    try {
      await proxy.$modal.confirm('设置后该用户将无法调用 AI 中继接口')
    } catch {
      return
    }
  }

  concurrencySaveLoading.value = true
  try {
    const res = await adminUpdateConcurrency({
      userId: currentUserId.value,
      aiConcurrencyLimit: concurrencyLimit
    })
    const updatedLimit = Number(res.data?.aiConcurrencyLimit ?? concurrencyLimit)
    walletInfo.aiConcurrencyLimit = updatedLimit
    concurrencyForm.aiConcurrencyLimit = updatedLimit
    proxy.$modal.msgSuccess('并发上限修改成功')
  } catch (error) {
    console.error('修改用户AI并发上限失败:', error)
  } finally {
    concurrencySaveLoading.value = false
  }
}

/** 加载充值记录 */
async function getRechargeList() {
  const res = await adminListRechargeRecord(currentUserId.value, rechargeQuery)
  rechargeList.value = res.rows || []
  rechargeTotal.value = res.total || 0
}

/** 调整余额 */
async function handleAdjust() {
  const invalidAmount = adjustForm.type === 'set' ? adjustForm.amount < 0 : adjustForm.amount <= 0
  if (invalidAmount) {
    proxy.$modal.msgWarning(adjustForm.type === 'set' ? '金额不能为负数' : '请输入大于0的金额')
    return
  }
  const actionLabel = adjustForm.type === 'set' ? '覆盖为' : adjustForm.type === 'add' ? '添加' : '减少'
  try {
    await proxy.$modal.confirm(`确认将用户「${currentUser.value}」的余额${actionLabel} $${adjustForm.amount.toFixed(4)} ？`)
  } catch {
    return
  }
  adjustLoading.value = true
  try {
    await adminAdjustBalance({
      userId: currentUserId.value,
      type: adjustForm.type,
      amount: adjustForm.amount
    })
    proxy.$modal.msgSuccess('操作成功')
    adjustForm.amount = 0
    await getWalletInfo()
    rechargeQuery.pageNum = 1
    await getRechargeList()
    emit('change')
  } catch (error) {
    console.error('调整余额失败:', error)
  } finally {
    adjustLoading.value = false
  }
}

/** 保存用户计费倍率 */
async function handleSaveMultiplier() {
  const multiplier = multiplierForm.billingMultiplier
  if (!Number.isFinite(multiplier) || multiplier < 0 || Math.round(multiplier * 100) !== multiplier * 100) {
    proxy.$modal.msgWarning('请输入不小于0且最多两位小数的计费倍率')
    return
  }
  multiplierSaveLoading.value = true
  try {
    await updateUserBillingMultiplier(currentUserId.value, Number(multiplier.toFixed(2)))
    multiplierForm.billingMultiplier = Number(multiplier.toFixed(2))
    proxy.$modal.msgSuccess('计费倍率修改成功')
  } catch (error) {
    console.error('保存计费倍率失败:', error)
  } finally {
    multiplierSaveLoading.value = false
  }
}

/** 打开抽屉 */
async function open(userId: number, userName?: string, nickName?: string): Promise<void> {
  currentUserId.value = userId
  currentUser.value = nickName || userName || ''
  visible.value = true
  loading.value = true
  adjustForm.type = 'add'
  adjustForm.amount = 0
  concurrencyForm.aiConcurrencyLimit = undefined
  rechargeQuery.pageNum = 1
  try {
    await Promise.all([getWalletInfo(), getRechargeList()])
  } catch (error) {
    console.error('加载余额信息失败:', error)
  } finally {
    loading.value = false
  }
}

function handleClose(): void {
  visible.value = false
}

defineExpose({ open })
</script>

<style scoped>
.balance-drawer-content {
  padding: 0 10px;
}

.section-header {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 16px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.section-header:first-child {
  margin-top: 0;
}

.balance-amount {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.used-amount {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-warning);
}

.mb20 {
  margin-bottom: 20px;
}

.adjust-form {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  padding: 16px;
  border-radius: 4px;
}

.adjust-row {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 12px;
}

.adjust-input {
  flex: 1 1 auto;
  width: 100%;
  min-width: 0;
}

.adjust-btn {
  flex-shrink: 0;
}

.form-tip {
  width: 100%;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.recharge-table {
  width: 100%;
}
</style>
