<template>
  <div class="app-container subscription-page">
    <!-- 筛选操作栏 -->
    <div class="toolbar">
      <div class="mb8 toolbar-row">
        <el-form :model="planQuery" :inline="true" label-width="80px" class="subscription-search-form">
          <el-form-item label="状态" prop="status">
            <el-select
              v-model="planQuery.status"
              placeholder="请选择状态"
              clearable
              class="search-control"
              @change="handlePlanQuery"
            >
              <el-option label="正常" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item class="search-button-item">
            <el-button type="primary" icon="Search" @click="handlePlanQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetPlanQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <div class="toolbar-actions">
          <el-button type="primary" icon="Plus" @click="handleAddPlan" v-hasPermi="['aigate:subscription:add']"
            >新增套餐</el-button
          >
          <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDeletePlan()"
            v-hasPermi="['aigate:subscription:remove']"
            >删除</el-button
          >
          <right-toolbar :search="false" @queryTable="getPlanList">
            <div v-if="checkPermi(['aigate:subscription:config'])" class="balance-switch">
              <span>余额购买</span>
              <el-switch
                v-model="balancePurchaseEnabled"
                active-value="true"
                inactive-value="false"
                inline-prompt
                active-text="开"
                inactive-text="关"
                @change="handleConfigChange"
              />
            </div>
          </right-toolbar>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper">
      <el-table
        class="subscription-table"
        v-loading="planLoading"
        :data="planList"
        @selection-change="handleSelectionChange"
        :row-class-name="planRowClassName"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="套餐ID" prop="planId" width="100" align="center" />
        <el-table-column label="套餐名称" prop="title" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column label="价格" width="120" align="center">
          <template #default="scope">
            <span class="money-text">${{ parseFloat(String(scope.row.priceAmount)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="110" align="center">
          <template #default="scope">{{ durationText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="额度" width="120" align="center">
          <template #default="scope">
            <span :class="{ 'money-text': scope.row.amountTotal && Number(scope.row.amountTotal) > 0 }">
              {{ amountText(scope.row.amountTotal) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="重置周期" width="110" align="center">
          <template #default="scope">{{ resetText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="余额购买" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.allowBalancePurchase === 1 ? 'success' : 'info'" effect="plain">
              {{ scope.row.allowBalancePurchase === 1 ? '允许' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sortOrder" width="90" align="center" />
        <el-table-column label="状态" width="92" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(scope.row)"
              v-hasPermi="['aigate:subscription:edit']"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button
              type="primary"
              link
              icon="Edit"
              @click="handleEditPlan(scope.row)"
              v-hasPermi="['aigate:subscription:edit']"
              >修改</el-button
            >
            <el-button
              type="primary"
              link
              icon="Delete"
              @click="handleDeletePlan(scope.row)"
              v-hasPermi="['aigate:subscription:remove']"
              >删除</el-button
            >
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无套餐" />
        </template>
      </el-table>
      <pagination
        v-show="planTotal > 0"
        :total="planTotal"
        v-model:page="planQuery.pageNum"
        v-model:limit="planQuery.pageSize"
        @pagination="getPlanList"
      />
    </div>

    <!-- 编辑套餐弹窗 -->
    <el-dialog v-model="planOpen" width="760px" append-to-body destroy-on-close class="plan-dialog">
      <template #header>
        <div class="dialog-heading">
          <span>{{ planTitle }}</span>
          <small>套餐信息</small>
        </div>
      </template>
      <el-form :model="planForm" ref="planRef" label-width="104px" class="plan-form">
        <div class="form-section">
          <div class="form-section-title">基础信息</div>
          <el-row :gutter="14">
            <el-col :xs="24" :sm="12">
              <el-form-item label="套餐标题" prop="title" required>
                <el-input v-model="planForm.title" placeholder="请输入套餐标题" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="副标题">
                <el-input v-model="planForm.subTitle" placeholder="请输入副标题" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="价格(USD)">
                <el-input-number v-model="planForm.priceAmount" :min="0" :precision="4" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="form-section-title">周期与权益</div>
          <el-row :gutter="14">
            <el-col :xs="24" :sm="12">
              <el-form-item label="有效期单位">
                <el-select v-model="planForm.durationUnit" style="width: 100%">
                  <el-option label="天" value="day" />
                  <el-option label="周" value="week" />
                  <el-option label="月" value="month" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="有效期数值">
                <el-input-number v-model="planForm.durationValue" :min="1" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="重置周期">
                <el-select v-model="planForm.quotaResetPeriod" style="width: 100%">
                  <el-option label="不重置" value="none" />
                  <el-option label="天" value="day" />
                  <el-option label="周" value="week" />
                  <el-option label="月" value="month" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="总额度">
                <el-input-number v-model="planForm.amountTotal" :min="0" :precision="4" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section form-section-last">
          <div class="form-section-title">发布设置</div>
          <el-row :gutter="14">
            <el-col :xs="24" :sm="12">
              <el-form-item label="排序">
                <el-input-number v-model="planForm.sortOrder" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="余额购买">
                <el-switch v-model="planForm.allowBalancePurchase" :active-value="1" :inactive-value="0" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="状态">
                <el-radio-group v-model="planForm.status">
                  <el-radio value="0">正常</el-radio>
                  <el-radio value="1">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="planForm.remark" type="textarea" :rows="3" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitPlan">确定</el-button>
        <el-button @click="planOpen = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AiSubscription">
import {
  addSubscriptionPlan,
  changeSubscriptionPlanStatus,
  delSubscriptionPlan,
  getSubscriptionConfig,
  getSubscriptionPlan,
  listSubscriptionPlan,
  updateSubscriptionConfig,
  updateSubscriptionPlan
} from '@/api/aigate/subscription'
import { checkPermi } from '@/utils/permission'
import { fixedNumber } from '../common'
import type { AiSubscriptionPlan } from '@/types'

const { proxy } = getCurrentInstance() as any

const planLoading = ref(false)
const planList = ref<AiSubscriptionPlan[]>([])
const balancePurchaseEnabled = ref('false')
const planTotal = ref(0)
const ids = ref<number[]>([])
const multiple = ref(true)
const planOpen = ref(false)
const planTitle = ref('')

const planQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined as string | undefined
})

const planForm = reactive<AiSubscriptionPlan>({
  planId: undefined,
  title: '',
  subTitle: '',
  priceAmount: 0,
  durationUnit: 'month',
  durationValue: 1,
  customSeconds: undefined,
  amountTotal: 0,
  quotaResetPeriod: 'none',
  quotaResetCustomSeconds: undefined,
  status: '0',
  sortOrder: 0,
  allowBalancePurchase: 0,
  remark: ''
})

const canManagePlan = computed(() =>
  checkPermi([
    'aigate:subscription:list',
    'aigate:subscription:add',
    'aigate:subscription:edit',
    'aigate:subscription:remove'
  ])
)
/** 停用行样式 */
function planRowClassName({ row }: { row: AiSubscriptionPlan }) {
  return row.status === '1' ? 'row-disabled' : ''
}

function getPlanList() {
  planLoading.value = true
  listSubscriptionPlan(planQuery)
    .then((res) => {
      planList.value = res.rows || []
      planTotal.value = res.total || 0
    })
    .finally(() => {
      planLoading.value = false
    })
}

function loadConfig() {
  if (!checkPermi(['aigate:subscription:config'])) return
  getSubscriptionConfig().then((res) => {
    balancePurchaseEnabled.value = res.data?.balancePurchaseEnabled || 'false'
  })
}

function handlePlanQuery() {
  planQuery.pageNum = 1
  getPlanList()
}

function resetPlanQuery() {
  planQuery.status = undefined
  handlePlanQuery()
}

function resetPlanForm() {
  planForm.planId = undefined
  planForm.title = ''
  planForm.subTitle = ''
  planForm.priceAmount = 0
  planForm.durationUnit = 'month'
  planForm.durationValue = 1
  planForm.customSeconds = undefined
  planForm.amountTotal = 0
  planForm.quotaResetPeriod = 'none'
  planForm.quotaResetCustomSeconds = undefined
  planForm.status = '0'
  planForm.sortOrder = 0
  planForm.allowBalancePurchase = 0
  planForm.remark = ''
}

function handleAddPlan() {
  resetPlanForm()
  planTitle.value = '新增套餐'
  planOpen.value = true
}

function handleEditPlan(row?: AiSubscriptionPlan) {
  const planId = row?.planId || ids.value[0]
  getSubscriptionPlan(planId).then((res) => {
    resetPlanForm()
    Object.assign(planForm, res.data || {})
    planTitle.value = '修改套餐'
    planOpen.value = true
  })
}

function submitPlan() {
  if (!planForm.title) {
    proxy.$modal.msgError('请输入套餐标题')
    return
  }
  const request = planForm.planId ? updateSubscriptionPlan(planForm) : addSubscriptionPlan(planForm)
  request.then(() => {
    proxy.$modal.msgSuccess('保存成功')
    planOpen.value = false
    getPlanList()
  })
}

function handleDeletePlan(row?: AiSubscriptionPlan) {
  const planIds = row?.planId || ids.value
  proxy.$modal
    .confirm(`确认删除套餐编号为「${planIds}」的数据项？`)
    .then(() => {
      return delSubscriptionPlan(planIds)
    })
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getPlanList()
    })
    .catch(() => {})
}

function handleStatusChange(row: AiSubscriptionPlan) {
  changeSubscriptionPlanStatus(row.planId!, row.status!)
    .then(() => {
      proxy.$modal.msgSuccess('状态已更新')
    })
    .catch(() => {
      row.status = row.status === '0' ? '1' : '0'
    })
}

function handleConfigChange(value: string) {
  updateSubscriptionConfig(value).then(() => {
    proxy.$modal.msgSuccess('开关已更新')
  })
}

function handleSelectionChange(selection: AiSubscriptionPlan[]) {
  ids.value = selection.map((item) => item.planId!)
  multiple.value = !selection.length
}

function amountText(value?: number) {
  return !value || Number(value) === 0 ? '不限' : `$${fixedNumber(value, 0)}`
}

function durationText(row: AiSubscriptionPlan) {
  const unitMap: Record<string, string> = { day: '天', week: '周', month: '月', year: '年', custom: '秒' }
  return `${row.durationUnit === 'custom' ? row.customSeconds || row.durationValue : row.durationValue}${unitMap[row.durationUnit || 'month']}`
}

function resetText(row: AiSubscriptionPlan) {
  const map: Record<string, string> = {
    none: '不重置',
    day: '天',
    week: '周',
    month: '月',
    custom: `${row.quotaResetCustomSeconds || 0}秒`
  }
  return map[row.quotaResetPeriod || 'none']
}

onMounted(() => {
  if (canManagePlan.value) {
    getPlanList()
  }
  loadConfig()
})
</script>

<style scoped lang="scss">
/* ========== 页面头部 ========== */
.balance-switch {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  height: 36px;
  padding: 0 10px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;

  span {
    font-size: 12px;
    font-weight: 600;
  }
}

/* ========== 筛选操作栏 ========== */
.toolbar {
  --aigate-filter-width: 180px;
  padding: 0;
}

.toolbar-row {
  display: flex;
  width: 100%;
  align-items: flex-start;
}

.subscription-search-form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  row-gap: 8px;
}

.subscription-search-form :deep(.el-form-item) {
  margin-right: 10px;
  margin-bottom: 0;
}

.subscription-search-form :deep(.el-form-item:last-child) {
  margin-right: 0;
}

.subscription-search-form :deep(.search-control) {
  width: var(--aigate-filter-width);
}

.search-button-item :deep(.el-form-item__content) {
  display: flex;
  width: var(--aigate-filter-width);
  gap: 10px;
}

.search-button-item :deep(.el-button) {
  flex: 1;
  min-width: 0;
}

.search-button-item :deep(.el-button + .el-button) {
  margin-left: 0;
}

.toolbar-actions {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-left: auto;
}

.toolbar-actions > :deep(.el-button + .el-button) {
  margin-left: 0;
}

.toolbar :deep(.top-right-btn) {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .toolbar-row {
    flex-wrap: wrap;
    row-gap: 8px;
  }

  .subscription-search-form {
    flex: 1 0 100%;
    width: 100%;
  }

  .subscription-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    margin-right: 0;
  }

  .subscription-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .subscription-search-form :deep(.search-control) {
    width: 100%;
  }

  .search-button-item :deep(.el-form-item__content) {
    width: 100%;
  }

  .toolbar-actions {
    flex: 1 0 100%;
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }

  .toolbar :deep(.top-right-btn) {
    flex: 1 0 100%;
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }
}

/* ========== 表格容器 ========== */
.table-wrapper {
  background: var(--el-bg-color);
  // border: 1px solid var(--el-border-color-lighter);
}

.subscription-table {
  :deep(.el-table__header-wrapper),
  :deep(.el-table__fixed-header-wrapper) {
    th {
      // height: 40px !important;
      font-size: 13px;
      word-break: break-word;
      background-color: var(--el-fill-color-lighter) !important;
    }
  }
}

.money-text {
  font-family: Consolas, Monaco, monospace;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

:deep(.el-table .row-disabled) {
  opacity: 0.5;
}

/* ========== 弹窗 ========== */
.dialog-heading {
  display: flex;
  flex-direction: column;
  gap: 4px;

  span {
    font-size: 18px;
    font-weight: 700;
  }
  small {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.plan-form {
  .form-section {
    padding: 16px 16px 2px;
    margin-bottom: 14px;
    background: var(--el-fill-color-lighter);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
  }
  .form-section-last {
    margin-bottom: 0;
  }
  .form-section-title {
    margin-bottom: 14px;
    font-size: 13px;
    font-weight: 700;
  }
}

.plan-dialog {
  :deep(.el-dialog__body) {
    padding-top: 10px;
    padding-bottom: 8px;
  }
}

/* ========== 响应式 ========== */
@media (max-width: 1180px) {
  .table-wrapper {
    overflow-x: auto;
  }
}

@media (max-width: 720px) {
  .table-wrapper {
    padding: 12px 10px 14px;
    overflow-x: auto;
  }
}
</style>
