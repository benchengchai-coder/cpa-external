<template>
  <el-drawer v-model="visible" title="公告详情" direction="rtl" size="50%" append-to-body :before-close="handleClose" class="notice-detail-drawer">
    <div v-loading="loading" class="notice-detail-drawer__body">
      <div v-if="!detail" class="notice-empty">
        <el-icon><Document /></el-icon>
        <span>暂无数据</span>
      </div>
      <div v-else class="notice-page">
        <div class="notice-type-wrap">
          <span v-if="detail.noticeType === '1'" class="notice-type-tag type-notify">
            <el-icon><Bell /></el-icon> 通知
          </span>
          <span v-else-if="detail.noticeType === '2'" class="notice-type-tag type-announce">
            <el-icon><Message /></el-icon> 公告
          </span>
          <span v-else class="notice-type-tag type-notify">
            <el-icon><Document /></el-icon> 消息
          </span>
        </div>

        <h1 class="notice-title">{{ detail.noticeTitle }}</h1>

        <div class="notice-meta">
          <span class="meta-item">
            <el-icon><User /></el-icon>
            <span>{{ detail.createBy || '—' }}</span>
          </span>
          <span class="meta-item">
            <el-icon><Clock /></el-icon>
            <span>{{ detail.createTime || '—' }}</span>
          </span>
          <span class="meta-item">
            <span :class="['status-dot', isStatusNormal ? 'status-ok' : 'status-off']"></span>
            <span>{{ isStatusNormal ? '正常' : '已关闭' }}</span>
          </span>
        </div>

        <div class="notice-divider">
          <span class="notice-divider-dot"></span>
          <span class="notice-divider-dot"></span>
          <span class="notice-divider-dot"></span>
        </div>

        <div class="notice-body">
          <div v-if="hasContent" class="notice-content" v-html="detail.noticeContent" />
          <div v-else class="notice-empty notice-empty--inner">
            <el-icon><Document /></el-icon> 暂无内容
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { getNotice } from '@/api/system/notice'
import type { SysNotice } from '@/types/api/system/notice'

const visible = ref<boolean>(false)
const loading = ref<boolean>(false)
const detail: Ref<SysNotice | null> = ref<SysNotice | null>(null)

const isStatusNormal = computed<boolean>(() => {
  const status = detail.value && detail.value.status
  return status === '0' || status === 0
})

const hasContent = computed<boolean>(() => {
  const content = detail.value && detail.value.noticeContent
  return content != null && String(content).trim() !== ''
})

function open(payload: any) {
  let id = null
  let preset = null
  if (payload != null && typeof payload === 'object') {
    id = payload.noticeId
    if (payload.noticeContent != null) {
      preset = payload
    }
  } else {
    id = payload
  }
  visible.value = true
  if (preset) {
    detail.value = preset
    return
  }
  if (id == null || id === '') {
    detail.value = null
    return
  }
  loading.value = true
  detail.value = null
  getNotice(id).then(res => {
    detail.value = res.data
  }).catch(() => {
    detail.value = null
  }).finally(() => {
    loading.value = false
  })
}

function handleClose() {
  visible.value = false
  detail.value = null
  loading.value = false
}

defineExpose({
  open
})
</script>

<style lang="scss" scoped>
.notice-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 8px 8px 20px;
  animation: notice-fade-up 0.28s ease both;
}

@keyframes notice-fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.notice-type-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 12px;
  border-radius: 2px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
  margin-bottom: 14px;
}

.type-notify {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning-dark-2);
  border-left: 3px solid var(--el-color-warning);
}

.type-announce {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success-dark-2);
  border-left: 3px solid var(--el-color-success);
}

.notice-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.45;
  margin: 0 0 16px;
  letter-spacing: 0;
}

.notice-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 28px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.meta-item .el-icon {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 4px;
}

.status-ok {
  background: #38a169;
}

.status-off {
  background: #e53e3e;
}

.notice-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.notice-divider::before,
.notice-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--el-border-color), transparent);
}

.notice-divider-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-border-color);
}

.notice-body {
  background: var(--el-bg-color-overlay);
  border-radius: 6px;
  padding: 28px 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06), 0 0 0 1px var(--el-border-color-light);
  min-height: 120px;
}

.notice-content {
  font-size: 14px;
  line-height: 1.85;
  color: var(--el-text-color-regular);
  word-break: break-word;
}

.notice-content :deep(p) {
  margin: 0 0 1em;
}

.notice-content :deep(h1),
.notice-content :deep(h2),
.notice-content :deep(h3) {
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 1.4em 0 0.6em;
}

.notice-content :deep(h1) {
  font-size: 18px;
}

.notice-content :deep(h2) {
  font-size: 16px;
}

.notice-content :deep(h3) {
  font-size: 14px;
}

.notice-content :deep(a) {
  color: var(--el-color-primary);
  text-decoration: underline;
}

.notice-content :deep(a:hover) {
  color: var(--el-color-primary-light-3);
}

.notice-content :deep(img) {
  max-width: 100%;
  border-radius: 4px;
  margin: 8px 0;
}

.notice-content :deep(ul),
.notice-content :deep(ol) {
  padding-left: 20px;
  margin: 0 0 1em;
}

.notice-content :deep(li) {
  margin-bottom: 4px;
}

.notice-content :deep(blockquote) {
  border-left: 3px solid var(--el-border-color);
  margin: 1em 0;
  padding: 6px 16px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-light);
}

.notice-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  font-size: 13px;
}

.notice-content :deep(table th),
.notice-content :deep(table td) {
  border: 1px solid var(--el-border-color);
  padding: 7px 12px;
}

.notice-content :deep(table th) {
  background: var(--el-fill-color-light);
  font-weight: 600;
}

.notice-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.notice-empty .el-icon {
  font-size: 28px;
  display: inline-flex;
  margin-bottom: 10px;
}

.notice-empty--inner {
  padding: 32px 0;
}

.notice-detail-drawer__body {
  height: 100%;
  overflow: auto;
  padding: 10px 16px 22px;
}

html.dark .notice-body {
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.35), 0 0 0 1px var(--el-border-color);
}

html.dark .notice-content,
html.dark .notice-content :deep(*) {
  color: var(--el-text-color-regular) !important;
}

html.dark .notice-content :deep(h1),
html.dark .notice-content :deep(h2),
html.dark .notice-content :deep(h3),
html.dark .notice-content :deep(strong),
html.dark .notice-content :deep(b) {
  color: var(--el-text-color-primary) !important;
}

html.dark .notice-content :deep(a),
html.dark .notice-content :deep(a *) {
  color: var(--el-color-primary-dark-2) !important;
}

html.dark .notice-content :deep(a:hover),
html.dark .notice-content :deep(a:hover *) {
  color: var(--el-color-primary) !important;
}
</style>

<style lang="scss">
.notice-detail-drawer {
  background: var(--el-bg-color) !important;
  color: var(--el-text-color-regular);

  .el-drawer__header {
    margin-bottom: 0;
    padding: 16px 20px;
    border-bottom: 1px solid var(--el-border-color-light);
    background: var(--el-bg-color-overlay);
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .el-drawer__title {
    color: inherit;
  }

  .el-drawer__close-btn {
    color: var(--el-text-color-secondary);

    &:hover {
      color: var(--el-text-color-primary);
    }
  }
  
  .el-drawer__body {
    background: var(--el-bg-color);
    padding: 0;
  }
}

html.dark .notice-detail-drawer {
  box-shadow: -6px 0 18px rgba(0, 0, 0, 0.45);
}
</style>
