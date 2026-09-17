<template>
  <UserApiKeyCard v-if="!isAdmin" />
  <div v-else class="app-container api-key-page">
    <div class="toolbar">
      <div class="mb8 toolbar-row">
        <el-form
          :model="queryParams"
          ref="queryRef"
          :inline="true"
          v-show="showSearch"
          label-width="80px"
          class="api-key-search-form"
        >
          <el-form-item v-if="isAdmin" label="用户" prop="userId">
            <el-select
              v-model="queryParams.userId"
              placeholder="请选择用户"
              clearable
              filterable
              class="search-control"
            >
              <el-option v-for="item in userOptions" :key="item.userId" :label="userLabel(item)" :value="item.userId" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="search-control">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item class="search-button-item">
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <div class="toolbar-actions">
          <el-button type="warning" plain icon="Connection" @click="handleOpenSyncStatus">CPA同步状态</el-button>
          <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete()"
            >删除</el-button
          >
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </div>
      </div>
    </div>

    <el-table v-loading="loading" :data="apiKeyList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="keyId" width="80" />
      <el-table-column label="用户" align="center" prop="userId" min-width="150">
        <template #default="scope">{{ userNameById(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="CPA同步" align="center" prop="cpaSyncStatus" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.cpaSyncStatus" :type="cpaSyncTagType(scope.row.cpaSyncStatus)">
            {{ cpaSyncTagLabel(scope.row.cpaSyncStatus) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        :width="isCompactOperation ? 72 : 320"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-dropdown
            v-if="isCompactOperation"
            trigger="click"
            @command="(command: string) => handleOperationCommand(command, scope.row)"
          >
            <el-button
              class="operation-more-button"
              link
              type="primary"
              icon="MoreFilled"
              aria-label="更多操作"
            ></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="view">
                  <el-icon><View /></el-icon>
                  <span>查看</span>
                </el-dropdown-item>
                <el-dropdown-item command="custom">
                  <el-icon><Key /></el-icon>
                  <span>自定义密钥</span>
                </el-dropdown-item>
                <el-dropdown-item command="sync">
                  <el-icon><Position /></el-icon>
                  <span>同步CPA生效</span>
                </el-dropdown-item>
                <el-dropdown-item command="delete">
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <template v-else>
            <el-button
              link
              type="primary"
              icon="View"
              @click="handleViewSecret(scope.row)"
              >查看</el-button
            >
            <el-button
              link
              type="primary"
              icon="Key"
              @click="handleCustomize(scope.row)"
              >自定义</el-button
            >
            <el-button
              link
              type="success"
              icon="Position"
              @click="handleSyncCpa(scope.row)"
              >同步CPA</el-button
            >
            <el-button
              link
              type="primary"
              icon="Delete"
              @click="handleDelete(scope.row)"
              >删除</el-button
            >
          </template>
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

    <el-dialog :title="secretTitle" v-model="secretOpen" width="560px" append-to-body>
      <div v-loading="secretLoading">
        <el-table :data="secretRows" border>
          <el-table-column label="API Key" min-width="380">
            <template #default="scope">
              <el-input v-model="scope.row.apiKey" readonly class="api-key-secret-input" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="scope">
              <el-button
                link
                type="primary"
                icon="DocumentCopy"
                :disabled="!scope.row.apiKey"
                @click="handleCopySecret"
                >复制</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="secretOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="自定义密钥" v-model="customOpen" width="520px" append-to-body>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="设置后旧密钥将立即失效，所有使用旧密钥的应用都会停止调用。"
        class="mb12"
      />
      <el-form label-width="80px" @submit.prevent>
        <el-form-item label="新密钥" required>
          <el-input
            v-model="customKeyValue"
            placeholder="sk- 开头，8~128位，仅含字母、数字、下划线或短横线"
            clearable
            show-password
            @keyup.enter="submitCustomize"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="customLoading" @click="submitCustomize">确 定</el-button>
          <el-button @click="customOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="CPA同步状态" v-model="syncStatusOpen" width="860px" append-to-body>
      <div v-loading="syncStatusLoading">
        <el-alert
          v-if="syncStatus && !syncStatus.pushEnabled"
          type="warning"
          :closable="false"
          show-icon
          title="API Key 推送同步开关未开启，实时推送与对账补偿均不会执行，只能在此手动同步。"
          class="mb12"
        />
        <div v-if="syncStatus" class="sync-status-summary">
          <el-tag type="success" effect="light">已同步 {{ syncStatus.syncedCount ?? 0 }}</el-tag>
          <el-tag type="danger" effect="light">CPA缺失 {{ missingDiffs.length }}</el-tag>
          <el-tag type="warning" effect="light">停用残留 {{ residualDiffs.length }}</el-tag>
          <el-tag type="info" effect="plain">CPA未登记 {{ orphanKeys.length }}</el-tag>
          <span class="sync-status-counts">
            平台 {{ syncStatus.platformKeyCount ?? 0 }} 个 / CPA {{ syncStatus.cpaKeyCount ?? 0 }} 个
          </span>
        </div>
        <el-empty
          v-if="syncStatus && !missingDiffs.length && !residualDiffs.length && !orphanKeys.length"
          description="两端一致，全部已同步"
          :image-size="60"
        />
        <template v-if="missingDiffs.length">
          <div class="sync-section-title">平台启用但 CPA 缺失（这些 Key 当前无法调用 CLIProxyAPI）</div>
          <el-table :data="missingDiffs" size="small" border>
            <el-table-column label="用户" align="center" min-width="140">
              <template #default="scope">{{ userNameById(scope.row.userId) }}</template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="120">
              <template #default="scope">
                <el-button link type="success" icon="Position" @click="handleFixDiff(scope.row)">同步生效</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <template v-if="residualDiffs.length">
          <div class="sync-section-title">平台已停用但 CPA 仍残留（对账任务会自动移除，也可立即处理）</div>
          <el-table :data="residualDiffs" size="small" border>
            <el-table-column label="用户" align="center" min-width="140">
              <template #default="scope">{{ userNameById(scope.row.userId) }}</template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="130">
              <template #default="scope">
                <el-button link type="danger" icon="Delete" @click="handleFixDiff(scope.row)">从CPA移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <template v-if="orphanKeys.length">
          <div class="sync-section-title">CPA 存在但平台未登记（可能为手工配置，系统不自动删除）</div>
          <div class="orphan-keys">
            <el-tag v-for="maskedKey in orphanKeys" :key="maskedKey" type="info" effect="plain" class="orphan-key-tag">
              {{ maskedKey }}
            </el-tag>
          </div>
        </template>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button icon="Refresh" :loading="syncStatusLoading" @click="loadSyncStatus">刷新</el-button>
          <el-button @click="syncStatusOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer :title="title" v-model="open" direction="rtl" :size="drawerSize" append-to-body>
      <el-form ref="apiKeyRef" :model="form" :rules="rules" label-width="108px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="自定义密钥" prop="apiKey">
              <el-input
                v-model="form.apiKey"
                placeholder="留空则自动生成；sk- 开头，8~128位"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="AiApiKey">
import {
  addAiApiKey,
  customizeAiApiKeySecret,
  delAiApiKey,
  getAiApiKeySecret,
  getCpaApiKeySyncStatus,
  listAiApiKey,
  syncAiApiKeyToCpa,
  updateAiApiKey
} from '@/api/aigate/apiKey'
import { listUser } from '@/api/system/user'
import useUserStore from '@/store/modules/user'
import type {
  AiApiKey,
  AiApiKeyQueryParams,
  AiApiKeySyncDiffItem,
  AiApiKeySyncStatus,
  AjaxResult,
  SysUser
} from '@/types'
import { useWindowSize } from '@vueuse/core'
import { statusOptions, useAiDrawerSize } from '../common'
import UserApiKeyCard from './UserApiKeyCard.vue'

const { proxy } = getCurrentInstance() as any
const userStore = useUserStore()
const { width: windowWidth } = useWindowSize()
const isAdmin = computed(() => Number(userStore.id) === 1)
const currentUserId = computed(() => Number(userStore.id))
const drawerSize = useAiDrawerSize()
const API_KEY_OPERATION_COMPACT_WIDTH = 1200
const isCompactOperation = computed(() => windowWidth.value < API_KEY_OPERATION_COMPACT_WIDTH)

const userOptions = ref<SysUser[]>([])
const apiKeyList = ref<AiApiKey[]>([])
const open = ref(false)
const secretOpen = ref(false)
const secretLoading = ref(false)
const secretTitle = ref('查看API密钥')
const secretValue = ref('')
const secretRows = computed(() => [{ apiKey: secretValue.value }])
const customOpen = ref(false)
const customLoading = ref(false)
const customKeyId = ref<number>()
const customKeyValue = ref('')
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const title = ref('')

/** 自定义密钥格式：sk- 前缀 + 5~125 位字母/数字/下划线/短横线（与后端校验一致） */
const CUSTOM_KEY_PATTERN = /^sk-[A-Za-z0-9_-]{5,125}$/
const CUSTOM_KEY_TIP = '以 sk- 开头，总长8~128位，仅含字母、数字、下划线或短横线'

function validateCustomKey(_rule: any, value: string | undefined, callback: (error?: Error) => void) {
  if (!value) {
    callback()
    return
  }
  if (!CUSTOM_KEY_PATTERN.test(value.trim())) {
    callback(new Error(CUSTOM_KEY_TIP))
    return
  }
  callback()
}

const data = reactive({
  form: {} as AiApiKey,
  queryParams: {
    pageNum: 1,
    pageSize: isCompactOperation.value ? 10 : 20,
    userId: undefined,
    status: undefined
  } as AiApiKeyQueryParams,
  rules: {
    apiKey: [{ validator: validateCustomKey, trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

watch(isCompactOperation, (compact: boolean) => {
  if (!isAdmin.value) return
  const pageSize = compact ? 10 : 20
  if (queryParams.value.pageSize === pageSize) return
  queryParams.value.pageNum = 1
  queryParams.value.pageSize = pageSize
  getList()
})

function userLabel(user: SysUser) {
  return user.nickName ? `${user.userName}（${user.nickName}）` : user.userName || String(user.userId)
}

function userNameById(userId?: number) {
  const user = userOptions.value.find((item: SysUser) => item.userId === userId)
  return user ? user.userName || String(user.userId) : '-'
}

function loadOptions() {
  if (isAdmin.value) {
    listUser({ pageNum: 1, pageSize: 1000, status: '0' }).then((response) => {
      userOptions.value = response.rows
    })
  } else {
    userOptions.value = [
      {
        userId: currentUserId.value,
        userName: userStore.name,
        nickName: userStore.nickName
      }
    ]
    queryParams.value.userId = undefined
  }
}

function getList() {
  loading.value = true
  listAiApiKey(queryParams.value).then((response) => {
    apiKeyList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    keyId: undefined,
    userId: currentUserId.value,
    apiKey: undefined,
    status: '0',
    remark: undefined
  }
  proxy.resetForm('apiKeyRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection: AiApiKey[]) {
  ids.value = selection.map((item) => item.keyId!)
  multiple.value = !selection.length
}

/** 表格内切换启停：复用编辑接口的状态变更语义，停用从 CLIProxyAPI 移除，启用重新推送 */
function handleStatusChange(row: AiApiKey) {
  const text = row.status === '0' ? '启用' : '停用'
  proxy.$modal
    .confirm('确认要"' + text + '"该密钥吗？')
    .then(() => {
      return updateAiApiKey({ keyId: row.keyId, status: row.status })
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + '成功')
      getList()
    })
    .catch(() => {
      row.status = row.status === '0' ? '1' : '0'
    })
}

function handleOperationCommand(command: string, row: AiApiKey) {
  switch (command) {
    case 'view':
      handleViewSecret(row)
      break
    case 'custom':
      handleCustomize(row)
      break
    case 'sync':
      handleSyncCpa(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

/** CPA 同步状态列的标签与颜色（数据来自后端短 TTL 缓存对比） */
const CPA_SYNC_LABELS: Record<string, string> = {
  synced: '已同步',
  missing: 'CPA缺失',
  residual: '停用残留',
  unknown: '未知'
}
const CPA_SYNC_TAG_TYPES: Record<string, string> = {
  synced: 'success',
  missing: 'danger',
  residual: 'warning',
  unknown: 'info'
}

function cpaSyncTagLabel(status?: string) {
  return CPA_SYNC_LABELS[status || ''] || '-'
}

function cpaSyncTagType(status?: string) {
  return CPA_SYNC_TAG_TYPES[status || ''] || 'info'
}

/** 手动同步该 Key 到 CLIProxyAPI：启用状态推送生效，停用状态从 CPA 移除 */
function handleSyncCpa(row: AiApiKey) {
  if (!row.keyId) return
  syncAiApiKeyToCpa(row.keyId)
    .then((response: AjaxResult) => {
      proxy.$modal.msgSuccess(response.msg || '已同步到CLIProxyAPI')
      getList()
    })
    .catch(() => {})
}

const syncStatusOpen = ref(false)
const syncStatusLoading = ref(false)
const syncStatus = ref<AiApiKeySyncStatus>()
const missingDiffs = computed(() => syncStatus.value?.missingInCpa ?? [])
const residualDiffs = computed(() => syncStatus.value?.disabledResidual ?? [])
const orphanKeys = computed(() => syncStatus.value?.orphanInCpa ?? [])

function handleOpenSyncStatus() {
  syncStatusOpen.value = true
  loadSyncStatus()
}

function loadSyncStatus() {
  syncStatusLoading.value = true
  getCpaApiKeySyncStatus()
    .then((response) => {
      syncStatus.value = response.data
    })
    .catch(() => {})
    .finally(() => {
      syncStatusLoading.value = false
    })
}

/** 修复差异：启用 Key 推送生效，停用 Key 从 CPA 移除（同一接口的状态语义） */
function handleFixDiff(row: AiApiKeySyncDiffItem) {
  if (!row.keyId) return
  syncAiApiKeyToCpa(row.keyId)
    .then((response: AjaxResult) => {
      proxy.$modal.msgSuccess(response.msg || '已同步到CLIProxyAPI')
      loadSyncStatus()
      getList()
    })
    .catch(() => {})
}

/** 打开自定义密钥弹窗 */
function handleCustomize(row: AiApiKey) {
  if (!row.keyId) return
  customKeyId.value = row.keyId
  customKeyValue.value = ''
  customLoading.value = false
  customOpen.value = true
}

/** 提交自定义密钥，成功后展示新密钥 */
function submitCustomize() {
  const value = (customKeyValue.value || '').trim()
  if (!CUSTOM_KEY_PATTERN.test(value)) {
    proxy.$modal.msgWarning(CUSTOM_KEY_TIP)
    return
  }
  proxy.$modal
    .confirm('设置后旧密钥将立即失效，确认自定义该密钥吗？', '确认自定义密钥', {
      confirmButtonText: '确认设置',
      cancelButtonText: '取消',
      type: 'warning'
    })
    .then(() => {
      customLoading.value = true
      return customizeAiApiKeySecret(customKeyId.value!, value)
    })
    .then((response: AjaxResult<AiApiKey>) => {
      if (!response) return
      customOpen.value = false
      proxy.$modal.msgSuccess('密钥自定义成功，旧密钥已失效')
      secretTitle.value = '自定义后的API密钥'
      secretValue.value = response.data?.apiKey || ''
      secretLoading.value = false
      secretOpen.value = true
    })
    .catch(() => {})
    .finally(() => {
      customLoading.value = false
    })
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加API Key'
}

function handleViewSecret(row: AiApiKey) {
  if (!row.keyId) return
  secretOpen.value = true
  secretLoading.value = true
  secretValue.value = ''
  secretTitle.value = '查看API密钥'
  getAiApiKeySecret(row.keyId)
    .then((response) => {
      secretValue.value = response.data?.apiKey || ''
    })
    .finally(() => {
      secretLoading.value = false
    })
}

function copyText(text: string) {
  if (navigator.clipboard) {
    return navigator.clipboard
      .writeText(text)
      .then(() => true)
      .catch(() => fallbackCopyText(text))
  }
  return Promise.resolve(fallbackCopyText(text))
}

function fallbackCopyText(text: string) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  let copied = false
  try {
    copied = document.execCommand('copy')
  } catch {
    copied = false
  }
  document.body.removeChild(textarea)
  return copied
}

function handleCopySecret() {
  if (!secretValue.value) return
  copyText(secretValue.value).then((copied) => {
    if (copied) {
      proxy.$modal.msgSuccess('复制成功')
    } else {
      proxy.$modal.msgError('复制失败')
    }
  })
}

function submitForm() {
  proxy.$refs['apiKeyRef'].validate((valid: boolean) => {
    if (valid) {
      form.value.userId = currentUserId.value
      const submitData = { ...form.value }
      if (submitData.apiKey) {
        submitData.apiKey = submitData.apiKey.trim()
      }
      addAiApiKey(submitData).then((response) => {
        open.value = false
        getList()
        if (response.data?.apiKey) {
          secretTitle.value = '查看API密钥'
          secretValue.value = response.data.apiKey
          secretLoading.value = false
          secretOpen.value = true
        } else {
          proxy.$modal.msgSuccess('新增成功')
        }
      })
    }
  })
}

function handleDelete(row?: AiApiKey) {
  const keyIds = row?.keyId || ids.value
  proxy.$modal
    .confirm('是否确认删除API Key编号为"' + keyIds + '"的数据项？')
    .then(() => {
      return delAiApiKey(keyIds)
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

if (isAdmin.value) {
  loadOptions()
  getList()
}
</script>

<style scoped>

.operation-more-button {
  width: 32px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.operation-more-button :deep(.el-icon) {
  margin: 0;
}

.toolbar {
  --api-key-filter-width: 180px;
  padding: 0;
}

.toolbar-row {
  display: flex;
  align-items: flex-start;
  width: 100%;
  column-gap: 10px;
}

.api-key-search-form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  row-gap: 8px;
}

.api-key-search-form :deep(.el-form-item) {
  margin-right: 10px;
  margin-bottom: 0;
}

.api-key-search-form :deep(.el-form-item:last-child) {
  margin-right: 0;
}

.api-key-search-form :deep(.search-control) {
  width: var(--api-key-filter-width);
}

.search-button-item :deep(.el-form-item__content) {
  display: flex;
  width: var(--api-key-filter-width);
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
  /* min-width: 340px; */
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

  .api-key-search-form {
    flex: 1 0 100%;
    width: 100%;
  }

  .api-key-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    margin-right: 0;
  }

  .api-key-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .api-key-search-form :deep(.search-control) {
    width: 100%;
  }

  .search-button-item :deep(.el-form-item__content) {
    width: 100%;
  }

  .toolbar-actions {
    flex: 1 0 100%;
    width: 100%;
    margin-left: 0;
  }

  .toolbar :deep(.top-right-btn) {
    justify-content: flex-end;
    margin-left: 0;
  }
}

.api-key-secret-input :deep(.el-input__inner) {
  font-family: Consolas, Monaco, monospace;
}

.sync-status-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.sync-status-counts {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.sync-section-title {
  margin: 12px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.orphan-keys {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.orphan-key-tag {
  font-family: Consolas, Monaco, monospace;
}
</style>
