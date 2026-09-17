<template>
  <div class="app-container billing-settlement-page">
    <el-alert
      title="failed 任务不会自动退款或核销，必须由管理员重试结算，或明确放弃追收并释放冻结。"
      type="warning"
      :closable="false"
      show-icon
      class="mb16"
    />

    <el-form ref="queryRef" :model="queryParams" inline v-show="showSearch">
      <el-form-item label="请求 ID" prop="requestId">
        <el-input
          v-model="queryParams.requestId"
          placeholder="请输入请求 ID"
          clearable
          style="width: 280px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-tag type="danger" effect="dark" round>待处置 {{ failedCount }}</el-tag>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="taskList" stripe>
      <el-table-column label="请求 ID" prop="requestId" min-width="230" show-overflow-tooltip />
      <el-table-column label="用户" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          <div>{{ scope.row.username || `用户#${scope.row.userId}` }}</div>
          <small class="secondary-text">ID: {{ scope.row.userId }}</small>
        </template>
      </el-table-column>
      <el-table-column label="API Key" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          <div>{{ scope.row.keyName || '-' }}</div>
          <small v-if="scope.row.keyId" class="secondary-text">ID: {{ scope.row.keyId }}</small>
        </template>
      </el-table-column>
      <el-table-column label="应计金额" align="right" width="130">
        <template #default="scope">{{ formatCurrency(scope.row.amount, 6, true) }}</template>
      </el-table-column>
      <el-table-column label="钱包冻结" align="right" width="130">
        <template #default="scope">{{ formatCurrency(scope.row.walletReservedAmount, 6, true) }}</template>
      </el-table-column>
      <el-table-column label="订阅冻结" align="right" width="130">
        <template #default="scope">{{ formatCurrency(scope.row.subscriptionReservedAmount, 6, true) }}</template>
      </el-table-column>
      <el-table-column label="Key 冻结" align="right" width="130">
        <template #default="scope">{{ formatCurrency(scope.row.keyReservedAmount, 6, true) }}</template>
      </el-table-column>
      <el-table-column label="重试次数" prop="retryCount" align="center" width="100" />
      <el-table-column label="失败原因" prop="errorMessage" min-width="260" show-overflow-tooltip />
      <el-table-column label="失败时间" prop="failedTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.failedTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right" width="170">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="handleRetry(scope.row)"
            v-hasPermi="['aigate:billingSettlement:retry']"
          >
            重试
          </el-button>
          <el-button
            link
            type="danger"
            @click="openWriteOff(scope.row)"
            v-hasPermi="['aigate:billingSettlement:writeOff']"
          >
            免费核销
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="writeOffOpen" title="人工免费核销" width="min(560px, 92vw)" append-to-body>
      <el-alert
        title="核销后将释放全部冻结额度，本次调用将不再扣款，且不能再次自动结算。"
        type="error"
        :closable="false"
        show-icon
        class="mb16"
      />
      <el-descriptions :column="1" border class="mb16">
        <el-descriptions-item label="请求 ID">{{ selectedTask?.requestId }}</el-descriptions-item>
        <el-descriptions-item label="应计金额">
          {{ formatCurrency(selectedTask?.amount, 6, true) }}
        </el-descriptions-item>
      </el-descriptions>
      <el-form ref="writeOffFormRef" :model="writeOffForm" :rules="writeOffRules" label-width="88px">
        <el-form-item label="核销原因" prop="reason">
          <el-input
            v-model="writeOffForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写放弃追收的明确原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="writeOffOpen = false">取消</el-button>
        <el-button type="danger" :loading="writeOffLoading" @click="submitWriteOff">确认核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AiBillingSettlement">
import type { FormInstance, FormRules } from 'element-plus'
import {
  countFailedBillingSettlements,
  listFailedBillingSettlements,
  retryFailedBillingSettlement,
  writeOffFailedBillingSettlement
} from '@/api/aigate/billingSettlement'
import type { AiBillingSettlementFailed, AiBillingSettlementFailedQuery } from '@/types'
import { parseTime } from '@/utils/ruoyi'
import { formatCurrency } from '../common'

const { proxy } = getCurrentInstance() as any
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const failedCount = ref(0)
const taskList = ref<AiBillingSettlementFailed[]>([])
const writeOffOpen = ref(false)
const writeOffLoading = ref(false)
const selectedTask = ref<AiBillingSettlementFailed | null>(null)
const writeOffFormRef = ref<FormInstance>()

const queryParams = reactive<AiBillingSettlementFailedQuery>({
  pageNum: 1,
  pageSize: 10,
  requestId: undefined,
  username: undefined
})

const writeOffForm = reactive({ reason: '' })
const writeOffRules: FormRules = {
  reason: [
    { required: true, message: '核销原因不能为空', trigger: 'blur' },
    { max: 500, message: '核销原因不能超过500个字符', trigger: 'blur' }
  ]
}

async function getList(): Promise<void> {
  loading.value = true
  try {
    const [listResponse, countResponse] = await Promise.all([
      listFailedBillingSettlements(queryParams),
      countFailedBillingSettlements()
    ])
    taskList.value = listResponse.rows || []
    total.value = Number(listResponse.total || 0)
    failedCount.value = Number(countResponse.data || 0)
  } finally {
    loading.value = false
  }
}

function handleQuery(): void {
  queryParams.pageNum = 1
  getList()
}

function resetQuery(): void {
  proxy.resetForm('queryRef')
  handleQuery()
}

async function handleRetry(row: AiBillingSettlementFailed): Promise<void> {
  try {
    await proxy.$modal.confirm(`确认重新结算请求「${row.requestId}」？系统将按原应计金额正常扣款。`)
  } catch {
    return
  }
  await retryFailedBillingSettlement(row.requestId)
  proxy.$modal.msgSuccess('结算任务已重新进入队列')
  await getList()
}

function openWriteOff(row: AiBillingSettlementFailed): void {
  selectedTask.value = row
  writeOffForm.reason = ''
  writeOffOpen.value = true
  nextTick(() => writeOffFormRef.value?.clearValidate())
}

async function submitWriteOff(): Promise<void> {
  if (!selectedTask.value || !writeOffFormRef.value) {
    return
  }
  const valid = await writeOffFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  try {
    await proxy.$modal.confirm('二次确认：本次调用将不再扣款，并立即释放全部冻结额度。是否继续？')
  } catch {
    return
  }

  writeOffLoading.value = true
  try {
    await writeOffFailedBillingSettlement(selectedTask.value.requestId, writeOffForm.reason.trim())
    proxy.$modal.msgSuccess('核销成功，冻结额度已释放')
    writeOffOpen.value = false
    await getList()
  } finally {
    writeOffLoading.value = false
  }
}

getList()
</script>

<style scoped>
.billing-settlement-page {
  min-width: 0;
}

.secondary-text {
  color: var(--el-text-color-secondary);
}
</style>
