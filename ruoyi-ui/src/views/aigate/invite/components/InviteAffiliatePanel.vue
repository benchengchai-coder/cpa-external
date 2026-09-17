<template>
  <div class="invite-panel">
    <el-form :model="affQuery" ref="affQueryRef" :inline="true" label-width="68px" class="affiliate-search-form">
      <el-form-item label="邀请码" prop="inviteCode">
        <el-input
          v-model="affQuery.inviteCode"
          placeholder="请输入邀请码"
          clearable
          class="affiliate-search-control"
          @keyup.enter="handleAffQuery"
        />
      </el-form-item>
      <el-form-item class="affiliate-search-button-item">
        <el-button type="primary" icon="Search" @click="handleAffQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetAffQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="affLoading" :data="affList">
      <el-table-column label="用户ID" prop="userId" width="90" align="center" />
      <el-table-column label="用户名" prop="userName" min-width="120" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.userName || '-' }}</template>
      </el-table-column>
      <el-table-column label="邀请码" prop="inviteCode" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="邀请人ID" prop="inviterId" width="100" align="center">
        <template #default="scope">{{ scope.row.inviterId ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="邀请人用户名" prop="inviterUserName" min-width="140" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.inviterUserName || '-' }}</template>
      </el-table-column>
      <el-table-column label="邀请人数" prop="inviteCount" width="90" align="center" />
      <el-table-column label="待领取" prop="pendingRebate" width="110" align="right">
        <template #default="scope">{{ formatCurrency(scope.row.pendingRebate, 4) }}</template>
      </el-table-column>
      <el-table-column label="冻结中" prop="frozenRebate" width="110" align="right">
        <template #default="scope">{{ formatCurrency(scope.row.frozenRebate, 4) }}</template>
      </el-table-column>
      <el-table-column label="历史返利" prop="historyRebate" width="110" align="right">
        <template #default="scope">{{ formatCurrency(scope.row.historyRebate, 4) }}</template>
      </el-table-column>
      <el-table-column label="专属比例" prop="rebateRate" width="90" align="center">
        <template #default="scope">{{ scope.row.rebateRate != null ? scope.row.rebateRate + '%' : '全局' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleSetRate(scope.row)"
            v-hasPermi="['aigate:invite:rate']"
          >设置比例</el-button>
          <el-button
            link
            type="warning"
            icon="Refresh"
            @click="handleResetCode(scope.row)"
            v-hasPermi="['aigate:invite:resetCode']"
          >重置码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="affTotal > 0"
      :total="affTotal"
      v-model:page="affQuery.pageNum"
      v-model:limit="affQuery.pageSize"
      @pagination="getAffList"
    />

    <el-dialog v-model="rateDialogVisible" title="设置专属返利比例" width="420px">
      <el-form label-width="100px">
        <el-form-item label="用户ID">
          <span>{{ currentRow?.userId }}</span>
        </el-form-item>
        <el-form-item label="当前邀请码">
          <span>{{ currentRow?.inviteCode }}</span>
        </el-form-item>
        <el-form-item label="专属比例(%)">
          <el-input-number v-model="rateForm.rebateRate" :min="0" :max="100" :precision="2" :step="1" style="width: 200px" />
          <div class="rate-tip">留空/清除表示沿用全局比例。范围 0-100。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearRate">清除(用全局)</el-button>
        <el-button type="primary" @click="submitRate">确定</el-button>
        <el-button @click="rateDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="InviteAffiliatePanel">
import {
  listInviteAffiliate,
  resetInviteCode,
  setInviteRebateRate
} from '@/api/aigate/invite'
import type { AiInviteAffiliate } from '@/types'
import { formatCurrency } from '../../common'

const { proxy } = getCurrentInstance() as any
const affLoading = ref(false)
const affList = ref<AiInviteAffiliate[]>([])
const affTotal = ref(0)
const affQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  inviteCode: undefined as string | undefined
})

function getAffList() {
  affLoading.value = true
  listInviteAffiliate(affQuery)
    .then((response) => {
      affList.value = response.rows
      affTotal.value = response.total
    })
    .catch(() => {
      affList.value = []
      affTotal.value = 0
    })
    .finally(() => {
      affLoading.value = false
    })
}

function handleAffQuery() {
  affQuery.pageNum = 1
  getAffList()
}

function resetAffQuery() {
  proxy.resetForm('affQueryRef')
  handleAffQuery()
}

const rateDialogVisible = ref(false)
const currentRow = ref<AiInviteAffiliate | null>(null)
const rateForm = reactive({ rebateRate: 0 as number | null })

function handleSetRate(row: AiInviteAffiliate) {
  currentRow.value = row
  rateForm.rebateRate = row.rebateRate ?? 0
  rateDialogVisible.value = true
}

function clearRate() {
  rateForm.rebateRate = null
}

function submitRate() {
  if (!currentRow.value?.userId) {
    return
  }
  setInviteRebateRate(currentRow.value.userId, rateForm.rebateRate)
    .then(() => {
      proxy.$modal.msgSuccess('设置成功')
      rateDialogVisible.value = false
      getAffList()
    })
    .catch(() => {})
}

function handleResetCode(row: AiInviteAffiliate) {
  proxy.$modal
    .confirm(`确认重置用户 ${row.userId} 的邀请码？重置后原邀请码失效。`)
    .then(() => resetInviteCode(row.userId as number))
    .then((response: any) => {
      proxy.$modal.msgSuccess('已重置为新邀请码：' + response.data?.inviteCode)
      getAffList()
    })
    .catch(() => {})
}

onMounted(getAffList)
</script>

<style scoped lang="scss">
.invite-panel {
  min-width: 0;
}

.affiliate-search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.affiliate-search-form :deep(.affiliate-search-control) {
  width: 200px;
}

.rate-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .affiliate-search-form {
    width: 100%;
  }

  .affiliate-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    max-width: 100%;
    margin-right: 0;
    margin-bottom: 8px;
  }

  .affiliate-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .affiliate-search-form :deep(.affiliate-search-control) {
    width: 100%;
  }

  .affiliate-search-button-item :deep(.el-form-item__content) {
    display: flex;
    width: 100%;
    gap: 10px;
  }

  .affiliate-search-button-item :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }

  .affiliate-search-button-item :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
</style>
