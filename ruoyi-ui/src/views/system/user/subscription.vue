<template>
  <el-drawer
    title="订阅管理"
    v-model="visible"
    direction="rtl"
    :size="drawerSize"
    append-to-body
    :before-close="handleClose"
  >
    <div v-loading="loading" class="subscription-drawer-content">
      <h4 class="section-header">用户信息</h4>
      <el-descriptions :column="1" border class="mb20">
        <el-descriptions-item label="用户名称">{{ currentUser }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="section-header">订阅操作</h4>
      <div class="action-panel mb20">
        <el-button type="primary" icon="Plus" @click="openGrant" v-hasPermi="['aigate:subscription:grant']"
          >授予订阅</el-button
        >
      </div>

      <h4 class="section-header">订阅列表</h4>
      <el-table :data="subscriptions" size="small" :max-height="420">
        <el-table-column label="套餐" prop="planTitle" min-width="100" :show-overflow-tooltip="true" />
        <el-table-column label="来源" width="96" align="center">
          <template #default="scope">
            <el-tag :type="sourceTagType(scope.row.sourceType)" effect="plain">
              {{ sourceLabel(scope.row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="100">
          <template #default="scope">
            <div class="validity-period">
              <div>{{ parseTime(scope.row.startTime) }}</div>
              <div>{{ parseTime(scope.row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用量" min-width="100">
          <template #default="scope">{{ usageText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'active'"
              type="warning"
              link
              @click="handleCancel(scope.row)"
              v-hasPermi="['aigate:subscription:cancel']"
              >取消</el-button
            >
            <el-button
              v-else
              type="danger"
              link
              @click="handleDelete(scope.row)"
              v-hasPermi="['aigate:subscription:remove']"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        @pagination="getList"
        :pageSizes="[5, 10, 20]"
        :small="true"
      />
    </div>

    <el-dialog
      title="授予订阅"
      v-model="grantOpen"
      width="min(520px, calc(100vw - 24px))"
      append-to-body
      class="grant-subscription-dialog"
    >
      <el-alert title="授予新订阅将自动替换用户当前生效的订阅" type="info" :closable="false" show-icon class="mb20" />
      <el-form
        :model="grantForm"
        :label-width="isSmallScreen ? '76px' : '96px'"
        class="grant-form"
      >
        <el-form-item label="套餐" required>
          <el-select v-model="grantForm.planId" placeholder="请选择套餐" filterable style="width: 100%">
            <el-option
              v-for="item in planOptions"
              :key="item.planId"
              :label="planOptionLabel(item)"
              :value="item.planId!"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedPlan" label="套餐详情" class="plan-detail-form-item">
          <div class="plan-detail-card">
            <div class="plan-detail-header">
              <div class="plan-detail-title">
                <strong>{{ selectedPlan.title }}</strong>
                <span>{{ selectedPlan.subTitle || '暂无套餐副标题' }}</span>
              </div>
              <div class="plan-detail-tags">
                <el-tag :type="selectedPlan.status === '0' ? 'success' : 'info'" effect="plain" size="small">
                  {{ selectedPlan.status === '0' ? '正常' : '停用' }}
                </el-tag>
                <el-tag type="primary" effect="plain" size="small">{{ durationText(selectedPlan) }}</el-tag>
              </div>
            </div>
            <div class="plan-detail-grid">
              <div class="plan-detail-item">
                <span>套餐价格</span>
                <strong>{{ formatCurrency(selectedPlan.priceAmount, 4, true) }}</strong>
              </div>
              <div class="plan-detail-item">
                <span>套餐额度</span>
                <strong>{{ amountText(selectedPlan.amountTotal) }}</strong>
              </div>
              <div class="plan-detail-item">
                <span>额度重置</span>
                <strong>{{ quotaResetText(selectedPlan) }}</strong>
              </div>
            </div>
            <div v-if="selectedPlan.remark" class="plan-detail-remark">
              <span>套餐说明</span>
              <p>{{ selectedPlan.remark }}</p>
            </div>
            <div class="plan-detail-tip">下方手动设置有效期后，以手动设置的时间为准。</div>
          </div>
        </el-form-item>
        <el-form-item label="快捷有效期">
          <el-space wrap class="duration-shortcuts">
            <el-button
              v-for="item in durationShortcuts"
              :key="item.label"
              size="small"
              @click="applyDurationShortcut(item)"
            >
              {{ item.label }}
            </el-button>
          </el-space>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="grantForm.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="grantForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="grantForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitGrant" :loading="grantLoading">确定</el-button>
        <el-button @click="grantOpen = false">取消</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts" name="UserSubscriptionDrawer">
import {
  cancelSubscription,
  delUserSubscriptions,
  grantSubscription,
  listAdminUserSubscriptions,
  listSubscriptionPlan
} from '@/api/aigate/subscription'
import { fixedNumber, formatCurrency, useAiDrawerSize } from '@/views/aigate/common'
import { useWindowSize } from '@vueuse/core'
import type { AiSubscriptionPlan, AiUserSubscription } from '@/types'

const { proxy } = getCurrentInstance() as any
const emit = defineEmits<{
  change: []
}>()
const drawerSize = useAiDrawerSize()
const { width: windowWidth } = useWindowSize()
const isSmallScreen = computed(() => windowWidth.value < 576)

const visible = ref(false)
const loading = ref(false)
const grantOpen = ref(false)
const grantLoading = ref(false)
const currentUserId = ref<number>(0)
const currentUser = ref('')
const subscriptions = ref<AiUserSubscription[]>([])
const planOptions = ref<AiSubscriptionPlan[]>([])
const total = ref(0)
const selectedPlan = computed(() => planOptions.value.find((item) => item.planId === grantForm.planId))

const query = reactive({
  pageNum: 1,
  pageSize: 10
})

const grantForm = reactive({
  planId: undefined as number | undefined,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  remark: ''
})

const durationShortcuts = [
  { label: '1小时', amount: 1, unit: 'hour' },
  { label: '3小时', amount: 3, unit: 'hour' },
  { label: '1天', amount: 1, unit: 'day' },
  { label: '1个月', amount: 1, unit: 'month' }
] as const

async function getList() {
  const res = await listAdminUserSubscriptions(currentUserId.value, query)
  subscriptions.value = res.rows || []
  total.value = res.total || 0
}

async function loadPlans() {
  const res = await listSubscriptionPlan({ pageNum: 1, pageSize: 100 })
  planOptions.value = res.rows || []
}

function openGrant() {
  grantForm.planId = undefined
  grantForm.startTime = undefined
  grantForm.endTime = undefined
  grantForm.remark = ''
  grantOpen.value = true
}

function planOptionLabel(row: AiSubscriptionPlan) {
  return row.status === '0' ? row.title || '-' : `${row.title || '-'}（停用）`
}

function durationText(row: AiSubscriptionPlan) {
  const unitMap: Record<string, string> = { day: '天', week: '周', month: '个月', year: '年', custom: '秒' }
  const duration = row.durationUnit === 'custom' ? row.customSeconds || row.durationValue : row.durationValue
  return duration ? `${duration}${unitMap[row.durationUnit || 'month']}` : '-'
}

function amountText(value?: number) {
  return !value || Number(value) === 0 ? '不限' : formatCurrency(value, 4, true)
}

function quotaResetText(row: AiSubscriptionPlan) {
  const resetMap: Record<string, string> = {
    none: '不重置',
    day: '每天',
    week: '每周',
    month: '每月',
    custom: `每 ${row.quotaResetCustomSeconds || 0} 秒`
  }
  return resetMap[row.quotaResetPeriod || 'none']
}

function applyDurationShortcut(shortcut: (typeof durationShortcuts)[number]) {
  const start = new Date()
  const end = addDuration(start, shortcut.amount, shortcut.unit)

  grantForm.startTime = formatDateTime(start)
  grantForm.endTime = formatDateTime(end)
}

function addDuration(date: Date, amount: number, unit: 'hour' | 'day' | 'month') {
  const result = new Date(date)
  if (unit === 'hour') {
    result.setHours(result.getHours() + amount)
  } else if (unit === 'day') {
    result.setDate(result.getDate() + amount)
  } else {
    const day = result.getDate()
    result.setDate(1)
    result.setMonth(result.getMonth() + amount)
    result.setDate(Math.min(day, getLastDateOfMonth(result)))
  }
  return result
}

function getLastDateOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
}

function formatDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function submitGrant() {
  if (!grantForm.planId) {
    proxy.$modal.msgError('请选择套餐')
    return
  }
  grantLoading.value = true
  try {
    await grantSubscription(currentUserId.value, grantForm)
    proxy.$modal.msgSuccess('授予成功')
    grantOpen.value = false
    query.pageNum = 1
    await getList()
    emit('change')
  } finally {
    grantLoading.value = false
  }
}

function handleCancel(row: AiUserSubscription) {
  proxy.$modal
    .confirm(`确认取消「${row.planTitle}」订阅？`)
    .then(() => {
      return cancelSubscription(row.subscriptionId!)
    })
    .then(() => {
      proxy.$modal.msgSuccess('取消成功')
      getList()
      emit('change')
    })
    .catch(() => {})
}

function handleDelete(row: AiUserSubscription) {
  proxy.$modal
    .confirm(`确认删除订阅编号「${row.subscriptionId}」？`)
    .then(() => {
      return delUserSubscriptions(row.subscriptionId!)
    })
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    })
    .catch(() => {})
}

function usageText(row: AiUserSubscription) {
  if (!row.amountTotal || Number(row.amountTotal) === 0) {
    return `${fixedNumber(row.amountUsed, 4)} / 不限`
  }
  return `${fixedNumber(row.amountUsed, 4)} / ${fixedNumber(row.amountTotal, 4)}`
}

function statusLabel(status?: string) {
  const map: Record<string, string> = { active: '生效中', cancelled: '已取消', expired: '已过期' }
  return map[status || ''] || status || '-'
}

function statusType(status?: string) {
  if (status === 'active') return 'success'
  if (status === 'cancelled') return 'warning'
  return 'info'
}

function sourceLabel(sourceType?: AiUserSubscription['sourceType']) {
  const map: Record<string, string> = {
    grant: '后台授予',
    balance_purchase: '余额购买',
    register_trial: '新人试用'
  }
  return map[sourceType || ''] || sourceType || '-'
}

function sourceTagType(sourceType?: AiUserSubscription['sourceType']) {
  if (sourceType === 'register_trial') return 'success'
  if (sourceType === 'balance_purchase') return 'primary'
  return 'info'
}

async function open(userId: number, userName?: string, nickName?: string): Promise<void> {
  currentUserId.value = userId
  currentUser.value = nickName || userName || ''
  visible.value = true
  loading.value = true
  query.pageNum = 1
  try {
    await Promise.all([getList(), loadPlans()])
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
.subscription-drawer-content {
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

.mb20 {
  margin-bottom: 20px;
}

.action-panel {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  padding: 16px;
  border-radius: 4px;
}

.duration-shortcuts {
  width: 100%;
}

.grant-form :deep(.el-form-item__content) {
  min-width: 0;
}

.plan-detail-form-item :deep(.el-form-item__content) {
  display: block;
}

.plan-detail-card {
  width: 100%;
  padding: 14px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
}

.plan-detail-header {
  display: flex;
  padding-bottom: 12px;
  margin-bottom: 12px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.plan-detail-title {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.plan-detail-tags {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
}

.plan-detail-title strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
  line-height: 1.4;
}

.plan-detail-title span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.plan-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.plan-detail-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.plan-detail-item span,
.plan-detail-remark span {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.plan-detail-item strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-detail-remark {
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px dashed var(--el-border-color);
}

.plan-detail-remark p {
  margin: 3px 0 0;
  color: var(--el-text-color-primary);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.plan-detail-tip {
  margin-top: 10px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  line-height: 1.5;
}

@media (max-width: 575px) {
  :global(.grant-subscription-dialog) {
    display: flex;
    flex-direction: column;
    max-height: calc(100vh - 24px);
    margin: 12px auto !important;
    overflow: hidden;
  }

  :global(.grant-subscription-dialog .el-dialog__header),
  :global(.grant-subscription-dialog .el-dialog__footer) {
    flex-shrink: 0;
  }

  :global(.grant-subscription-dialog .el-dialog__body) {
    min-height: 0;
    padding-right: 16px;
    padding-left: 16px;
    overflow-y: auto;
  }

  .grant-form :deep(.el-form-item__label) {
    padding-right: 8px;
  }

  .plan-detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.validity-period {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.4;
}

.validity-period div {
  white-space: nowrap;
}
</style>
