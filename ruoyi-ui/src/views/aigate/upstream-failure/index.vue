<template>
  <div class="app-container failure-page">
    <div class="toolbar">
      <div class="mb8 toolbar-row">
        <el-form
          :model="queryParams"
          ref="queryRef"
          :inline="true"
          v-show="showSearch"
          label-width="80px"
          class="failure-search-form"
        >
          <el-form-item label="供应商" prop="provider">
            <el-input
              v-model="queryParams.provider"
              placeholder="请输入供应商"
              clearable
              class="search-control"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="模型" prop="model">
            <el-input
              v-model="queryParams.model"
              placeholder="请输入模型"
              clearable
              class="search-control"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态码" prop="statusCode">
            <el-input-number
              v-model="queryParams.statusCode"
              :min="100"
              :max="599"
              :controls="false"
              placeholder="如 429"
              class="search-control"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="可重试" prop="retryable">
            <el-select v-model="queryParams.retryable" placeholder="请选择" clearable class="search-control">
              <el-option v-for="item in booleanOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="凭证冷却" prop="authUnavailable">
            <el-select v-model="queryParams.authUnavailable" placeholder="请选择" clearable class="search-control">
              <el-option v-for="item in booleanOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="配额超限" prop="quotaExceeded">
            <el-select v-model="queryParams.quotaExceeded" placeholder="请选择" clearable class="search-control">
              <el-option v-for="item in booleanOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="事件时间">
            <el-date-picker
              v-model="dateRange"
              value-format="YYYY-MM-DD"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              class="search-control search-control--date"
            ></el-date-picker>
          </el-form-item>
          <el-form-item class="search-button-item">
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList">
          <el-tooltip :content="autoRefresh ? '关闭自动刷新' : '开启自动刷新，每5秒刷新一次'" placement="top">
            <el-button
              circle
              icon="SwitchButton"
              class="auto-refresh-button"
              :class="{ 'is-auto-refresh-active': autoRefresh }"
              :type="autoRefresh ? 'primary' : 'info'"
              :plain="!autoRefresh"
              :aria-label="autoRefresh ? '关闭自动刷新' : '开启自动刷新'"
              :aria-pressed="autoRefresh"
              @click="toggleAutoRefresh"
            />
          </el-tooltip>
        </right-toolbar>
      </div>
    </div>

    <el-table stripe v-loading="loading" :data="failureList">
      <el-table-column label="事件时间" align="center" prop="eventTime" width="180">
        <template #default="scope">{{ formatEventTime(scope.row.eventTime) }}</template>
      </el-table-column>
      <el-table-column label="供应商" align="center" prop="provider" width="110">
        <template #default="scope">{{ scope.row.provider || '-' }}</template>
      </el-table-column>
      <el-table-column label="模型" align="center" prop="model" min-width="100" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.model || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态码" align="center" prop="statusCode" width="80">
        <template #default="scope">
          <el-tag :type="statusCodeTagType(scope.row.statusCode)" effect="plain">{{ scope.row.statusCode ?? '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="错误码" align="center" prop="code" min-width="140" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.code || '-' }}</template>
      </el-table-column>
<!--      <el-table-column label="可重试" align="center" prop="retryable" width="80">-->
<!--        <template #default="scope">-->
<!--          <el-tag :type="scope.row.retryable ? 'warning' : 'danger'" effect="plain" size="small">-->
<!--            {{ scope.row.retryable ? '是' : '否' }}-->
<!--          </el-tag>-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column label="凭证状态" align="center" min-width="100">
        <template #default="scope">
          <div v-if="hasAuthStateTags(scope.row)" class="state-tags">
            <el-tag v-if="scope.row.authDisabled" type="danger" effect="plain" size="small">已禁用</el-tag>
            <el-tag v-if="scope.row.authUnavailable" type="warning" effect="plain" size="small">冷却中</el-tag>
            <el-tag v-if="scope.row.quotaExceeded" type="danger" effect="plain" size="small">
              配额超限{{ scope.row.quotaReason ? `：${scope.row.quotaReason}` : '' }}
            </el-tag>
          </div>
          <span v-else class="state-tags__normal">{{ scope.row.authStatus || '正常' }}</span>
        </template>
      </el-table-column>
<!--      <el-table-column label="预计恢复" align="center" prop="authNextRetryAt" width="170">-->
<!--        <template #default="scope">{{ formatEventTime(scope.row.authNextRetryAt) }}</template>-->
<!--      </el-table-column>-->
      <el-table-column label="错误消息" align="center" prop="body" min-width="320" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.body || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="130" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['aigate:upstreamFailure:query']">详情</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aigate:upstreamFailure:remove']">删除</el-button>
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

    <el-drawer title="上游失败事件详情" v-model="detailOpen" direction="rtl" :size="drawerSize" append-to-body>
      <el-divider content-position="left">事件信息</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="事件时间">{{ formatEventTime(detail.eventTime) }}</el-descriptions-item>
        <el-descriptions-item label="状态码">{{ detail.statusCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.provider || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ detail.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="凭证ID">{{ detail.authId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认证索引">{{ detail.authIndex || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误码">{{ detail.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否可重试">{{ detail.retryable ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="凭证状态">{{ detail.authStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计恢复时间">{{ formatEventTime(detail.authNextRetryAt) }}</el-descriptions-item>
        <el-descriptions-item label="配额超限">{{ detail.quotaExceeded ? `是${detail.quotaReason ? `：${detail.quotaReason}` : ''}` : '否' }}</el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ formatEventTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">上游错误消息</el-divider>
      <pre class="failure-pre">{{ detail.body || '-' }}</pre>
      <el-divider content-position="left">凭证状态快照</el-divider>
      <pre class="failure-pre">{{ formatSnapshot(detail.authStatusSnapshot) }}</pre>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="UpstreamFailure">
import { listUpstreamFailure, getUpstreamFailure, delUpstreamFailure } from '@/api/aigate/upstreamFailure'
import { usePersistentAutoRefresh } from '@/composables/usePersistentAutoRefresh'
import { parseTime } from '@/utils/ruoyi'
import type { UpstreamFailureQueryParams, UpstreamFailureRecord, UpstreamFailureAuthStatusSnapshot } from '@/types'
import { formatRequestTime, useAiDrawerSize } from '../common'

const { proxy } = getCurrentInstance() as any

const booleanOptions = [
  { label: '是', value: true },
  { label: '否', value: false }
]
const AUTO_REFRESH_INTERVAL = 5000

const drawerSize = useAiDrawerSize()
const failureList = ref<UpstreamFailureRecord[]>([])
const detail = ref<UpstreamFailureRecord>({})
const detailOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
// 进入页面默认查询今天
const dateRange = ref<string[]>(defaultTodayRange())

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    provider: undefined,
    model: undefined,
    authIndex: undefined,
    statusCode: undefined,
    retryable: undefined,
    authUnavailable: undefined,
    quotaExceeded: undefined
  } as UpstreamFailureQueryParams
})

const { queryParams } = toRefs(data)

/** 默认时间范围：今天到今天（YYYY-MM-DD）。 */
function defaultTodayRange(): string[] {
  const today = parseTime(new Date(), '{y}-{m}-{d}')
  return today ? [today, today] : []
}

function formatEventTime(value: string | undefined) {
  return value ? formatRequestTime(value) : '-'
}

function statusCodeTagType(statusCode: number | undefined): 'danger' | 'warning' | 'info' {
  if (statusCode == null) {
    return 'info'
  }
  if (statusCode >= 500) {
    return 'danger'
  }
  if (statusCode >= 400) {
    return 'warning'
  }
  return 'info'
}

function hasAuthStateTags(row: UpstreamFailureRecord) {
  return row.authDisabled === true || row.authUnavailable === true || row.quotaExceeded === true
}

function formatSnapshot(snapshot: UpstreamFailureAuthStatusSnapshot | undefined) {
  if (!snapshot) {
    return '-'
  }
  return JSON.stringify(snapshot, null, 2)
}

function getList(options: { silent?: boolean } = {}) {
  const silent = options.silent === true
  if (!silent) {
    loading.value = true
  }
  return listUpstreamFailure(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    failureList.value = response.rows
    total.value = response.total
  }).finally(() => {
    if (!silent) {
      loading.value = false
    }
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = defaultTodayRange()
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleDetail(row: UpstreamFailureRecord) {
  const failureId = row.failureId!
  getUpstreamFailure(failureId).then((response) => {
    detail.value = response.data || {}
    detailOpen.value = true
  })
}

function handleDelete(row: UpstreamFailureRecord) {
  const failureIds = row.failureId!
  proxy.$modal.confirm('是否确认删除该上游失败事件？').then(() => {
    return delUpstreamFailure(failureIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

const { autoRefresh, toggleAutoRefresh } = usePersistentAutoRefresh({
  storageKeyPrefix: 'aigate:upstream-failure:auto-refresh',
  intervalMs: AUTO_REFRESH_INTERVAL,
  refresh: () => getList({ silent: true })
})

getList()
</script>

<style scoped>
.toolbar {
  --aigate-filter-width: 180px;
  padding: 0;
}

.toolbar-row {
  display: flex;
  width: 100%;
  align-items: flex-start;
}

.failure-search-form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  row-gap: 8px;
}

.failure-search-form :deep(.el-form-item) {
  margin-right: 10px;
  margin-bottom: 0;
}

.search-control {
  width: var(--aigate-filter-width);
}

.search-control--date {
  width: calc(var(--aigate-filter-width) + 60px);
}

.state-tags {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}

.state-tags__normal {
  color: var(--el-text-color-secondary);
}

.failure-pre {
  margin: 0;
  padding: 8px 12px;
  max-height: 280px;
  overflow: auto;
  font-family: inherit;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
