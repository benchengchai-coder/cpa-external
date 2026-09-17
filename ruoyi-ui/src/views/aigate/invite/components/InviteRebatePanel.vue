<template>
  <div class="invite-panel">
    <el-form :model="rebateQuery" ref="rebateQueryRef" :inline="true" label-width="68px" class="rebate-search-form">
      <el-form-item label="收益人ID" prop="inviterId">
        <el-input
          v-model="rebateQuery.inviterId"
          placeholder="请输入收益人ID"
          clearable
          class="rebate-search-control is-wide"
          @keyup.enter="handleRebateQuery"
        />
      </el-form-item>
      <el-form-item label="动作" prop="action">
        <el-select v-model="rebateQuery.action" placeholder="请选择" clearable class="rebate-search-control">
          <el-option label="产生返利" value="1" />
          <el-option label="领取返利" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源" prop="sourceType">
        <el-select v-model="rebateQuery.sourceType" placeholder="请选择" clearable class="rebate-search-control">
          <el-option label="在线支付" value="1" />
          <el-option label="兑换码" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item class="rebate-search-button-item">
        <el-button type="primary" icon="Search" @click="handleRebateQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetRebateQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="rebateLoading" :data="rebateList" style="width: 100%">
      <el-table-column label="流水ID" prop="recordId" width="90" align="center" />
      <el-table-column label="收益人ID" prop="inviterId" min-width="100" align="center" />
      <el-table-column label="收益人账号" prop="inviterUserName" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.inviterUserName || '-' }}</template>
      </el-table-column>
      <el-table-column label="被邀请人ID" prop="inviteeId" min-width="110" align="center">
        <template #default="scope">{{ scope.row.inviteeId ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="被邀请人账号" prop="inviteeUserName" min-width="150" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.inviteeUserName || '-' }}</template>
      </el-table-column>
      <el-table-column label="动作" prop="action" min-width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.action === '1' ? 'success' : 'primary'" size="small">
            {{ scope.row.action === '1' ? '产生返利' : '领取返利' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" prop="amount" min-width="120" align="right">
        <template #default="scope">{{ formatCurrency(scope.row.amount, 4) }}</template>
      </el-table-column>
      <el-table-column label="来源" prop="sourceType" min-width="100" align="center">
        <template #default="scope">
          <span v-if="scope.row.sourceType === '1'">在线支付</span>
          <span v-else-if="scope.row.sourceType === '2'">兑换码</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="来源ID" prop="sourceId" min-width="100" align="center">
        <template #default="scope">{{ scope.row.sourceId ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="领取后余额" prop="balanceAfter" min-width="140" align="right">
        <template #default="scope">{{ scope.row.balanceAfter != null ? formatCurrency(scope.row.balanceAfter, 4) : '-' }}</template>
      </el-table-column>
      <el-table-column label="冻结到期" prop="frozenUntil" min-width="160" align="center">
        <template #default="scope">{{ scope.row.frozenUntil ? parseTime(scope.row.frozenUntil) : '-' }}</template>
      </el-table-column>
      <el-table-column label="时间" prop="createTime" min-width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="rebateTotal > 0"
      :total="rebateTotal"
      v-model:page="rebateQuery.pageNum"
      v-model:limit="rebateQuery.pageSize"
      @pagination="getRebateList"
    />
  </div>
</template>

<script setup lang="ts" name="InviteRebatePanel">
import { listInviteRebate } from '@/api/aigate/invite'
import type { AiInviteRebateRecord } from '@/types'
import { formatCurrency } from '../../common'

const { proxy } = getCurrentInstance() as any
const rebateLoading = ref(false)
const rebateList = ref<AiInviteRebateRecord[]>([])
const rebateTotal = ref(0)
const rebateQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  inviterId: undefined as number | undefined,
  action: undefined as string | undefined,
  sourceType: undefined as string | undefined
})

function getRebateList() {
  rebateLoading.value = true
  listInviteRebate(rebateQuery)
    .then((response) => {
      rebateList.value = response.rows
      rebateTotal.value = response.total
    })
    .catch(() => {
      rebateList.value = []
      rebateTotal.value = 0
    })
    .finally(() => {
      rebateLoading.value = false
    })
}

function handleRebateQuery() {
  rebateQuery.pageNum = 1
  getRebateList()
}

function resetRebateQuery() {
  proxy.resetForm('rebateQueryRef')
  handleRebateQuery()
}

onMounted(getRebateList)
</script>

<style scoped lang="scss">
.invite-panel {
  min-width: 0;
}

.rebate-search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.rebate-search-form :deep(.rebate-search-control) {
  width: 160px;
}

.rebate-search-form :deep(.rebate-search-control.is-wide) {
  width: 200px;
}

@media (max-width: 768px) {
  .rebate-search-form {
    width: 100%;
  }

  .rebate-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    max-width: 100%;
    margin-right: 0;
    margin-bottom: 8px;
  }

  .rebate-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .rebate-search-form :deep(.rebate-search-control),
  .rebate-search-form :deep(.rebate-search-control.is-wide) {
    width: 100%;
  }

  .rebate-search-button-item :deep(.el-form-item__content) {
    display: flex;
    width: 100%;
    gap: 10px;
  }

  .rebate-search-button-item :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }

  .rebate-search-button-item :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
</style>
