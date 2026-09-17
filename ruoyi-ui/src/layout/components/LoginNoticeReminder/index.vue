<template>
  <Teleport to="body">
    <Transition name="notice-reminder-modal">
      <div v-if="visible" class="login-notice-reminder-overlay">
        <div
          ref="noticePanelRef"
          class="login-notice-reminder-panel"
          role="dialog"
          aria-modal="true"
          aria-labelledby="login-notice-reminder-title"
          tabindex="-1"
          @keydown.esc.prevent.stop
        >
          <header class="notice-reminder-header">
            <div class="notice-reminder-header-top">
              <div class="notice-reminder-header-identity">
                <div class="notice-reminder-icon">
                  <el-icon><InfoFilled /></el-icon>
                </div>
                <span
                  v-if="currentNotice"
                  class="notice-reminder-type"
                  :class="currentNotice.noticeType === '1' ? 'is-notice' : 'is-announcement'"
                >
                  {{ currentNotice.noticeType === '1' ? '通知' : '公告' }}
                </span>
              </div>
              <div
                class="notice-reminder-count"
                :aria-label="`第 ${currentNoticeNumber} 条，共 ${noticeList.length} 条`"
              >
                <strong>{{ currentNoticeNumber }}</strong>
                <span>/ {{ noticeList.length }}</span>
              </div>
            </div>

            <h2 id="login-notice-reminder-title">
              {{ currentNotice?.noticeTitle || '未命名通知' }}
            </h2>

            <div v-if="currentNotice" class="notice-reminder-header-meta">
              <el-icon aria-hidden="true"><Clock /></el-icon>
              <span>发布时间</span>
              <time>{{ currentNotice.createTime || '时间未知' }}</time>
            </div>
          </header>

          <div ref="noticeBodyRef" class="notice-reminder-body">
            <Transition name="notice-reminder-switch" mode="out-in">
              <article v-if="currentNotice" :key="currentNotice.noticeId" class="notice-reminder-item">
                <div v-if="currentNotice.noticeContent" class="notice-reminder-content" v-html="currentNotice.noticeContent"></div>
                <div v-else class="notice-reminder-content is-empty">暂无正文内容</div>
              </article>
            </Transition>
          </div>

          <footer class="notice-reminder-footer">
            <button
              type="button"
              class="notice-reminder-confirm"
              :disabled="confirming"
              :aria-busy="confirming"
              @click="confirmRead"
            >
              <span v-if="confirming" class="notice-reminder-spinner" aria-hidden="true"></span>
              <el-icon v-else aria-hidden="true"><Check /></el-icon>
              <span>我知道了</span>
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { Check, Clock, InfoFilled } from '@element-plus/icons-vue'
import { listUnreadNotice, markNoticeRead } from '@/api/system/notice'
import type { SysNotice } from '@/types/api/system/notice'
import useUserStore from '@/store/modules/user'
import {
  clearNoticeReminderCheckPending,
  hasNoticeReminderCheckPending,
  notifyNoticeReadStatusChanged
} from '@/utils/noticeReminder'

const userStore = useUserStore()
const visible = ref<boolean>(false)
const confirming = ref<boolean>(false)
const noticeList = ref<SysNotice[]>([])
const currentNoticeIndex = ref<number>(0)
const noticeBodyRef = ref<HTMLElement | null>(null)
const noticePanelRef = ref<HTMLElement | null>(null)
const currentNotice = computed<SysNotice | null>(() => noticeList.value[currentNoticeIndex.value] || null)
const currentNoticeNumber = computed<number>(() => currentNotice.value ? currentNoticeIndex.value + 1 : 0)
let previousBodyOverflow = ''

async function checkUnreadNotice(): Promise<void> {
  if (!hasNoticeReminderCheckPending() || userStore.id === '') {
    return
  }

  try {
    const response = await listUnreadNotice()
    noticeList.value = response.data || []
    currentNoticeIndex.value = 0
    if (noticeList.value.length === 0) {
      clearNoticeReminderCheckPending()
      return
    }
    visible.value = true
  } catch {
    // 查询失败时保留待检查标记，刷新后台后可再次尝试。
  }
}

async function confirmRead(): Promise<void> {
  const noticeId = currentNotice.value?.noticeId
  if (noticeId === undefined) {
    clearNoticeReminderCheckPending()
    visible.value = false
    return
  }

  confirming.value = true
  try {
    await markNoticeRead(noticeId)
    notifyNoticeReadStatusChanged()
    if (currentNoticeIndex.value < noticeList.value.length - 1) {
      currentNoticeIndex.value += 1
      await nextTick()
      if (noticeBodyRef.value) {
        noticeBodyRef.value.scrollTop = 0
      }
    } else {
      clearNoticeReminderCheckPending()
      visible.value = false
    }
  } finally {
    confirming.value = false
  }
}

onMounted(() => {
  void checkUnreadNotice()
})

watch(visible, async (isVisible: boolean) => {
  if (isVisible) {
    previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    await nextTick()
    noticePanelRef.value?.focus({ preventScroll: true })
    return
  }

  document.body.style.overflow = previousBodyOverflow
})

onBeforeUnmount(() => {
  if (visible.value) {
    document.body.style.overflow = previousBodyOverflow
  }
})
</script>

<style lang="scss">
.login-notice-reminder-overlay {
  position: fixed;
  z-index: 2100;
  inset: 0;
  display: grid;
  padding: 24px 16px;
  overflow-y: auto;
  place-items: center;
  background: rgb(15 23 42 / 48%);
  backdrop-filter: blur(4px);
  overscroll-behavior: contain;
}

.login-notice-reminder-panel {
  position: relative;
  display: flex;
  width: min(680px, calc(100vw - 32px));
  max-height: min(760px, calc(100vh - 48px));
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: var(--el-bg-color-overlay);
  box-shadow: 0 18px 48px rgb(0 0 0 / 16%);
  outline: none;
}

.notice-reminder-modal-enter-active,
.notice-reminder-modal-leave-active {
  transition: opacity 0.2s ease;

  .login-notice-reminder-panel {
    transition: transform 0.22s ease, opacity 0.2s ease;
  }
}

.notice-reminder-modal-enter-from,
.notice-reminder-modal-leave-to {
  opacity: 0;

  .login-notice-reminder-panel {
    opacity: 0;
    transform: translateY(14px) scale(0.98);
  }
}

.notice-reminder-header {
  position: relative;
  flex: 0 0 auto;
  padding: 30px 32px 28px;
  overflow: hidden;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background:
    radial-gradient(circle at 88% 16%, rgb(92 112 255 / 18%), transparent 34%),
    linear-gradient(135deg, var(--el-color-primary-light-9) 0%, var(--el-bg-color-overlay) 55%, rgb(139 92 246 / 8%) 100%);
}

.notice-reminder-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.notice-reminder-header-identity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notice-reminder-header h2 {
  margin: 22px 0 13px;
  color: var(--el-text-color-primary);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.notice-reminder-icon {
  display: grid;
  flex: 0 0 46px;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6 0%, #5146e5 100%);
  box-shadow: 0 10px 22px rgb(79 70 229 / 25%);
  font-size: 22px;
}

.notice-reminder-header-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--el-text-color-secondary);
  font-size: 13px;

  .el-icon {
    font-size: 16px;
  }

  span::after {
    margin-left: 7px;
    color: var(--el-border-color-darker);
    content: '·';
  }

  time {
    font-variant-numeric: tabular-nums;
  }
}

.notice-reminder-count {
  display: flex;
  flex: 0 0 auto;
  height: 36px;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0 13px;
  border: 1px solid rgb(64 158 255 / 16%);
  border-radius: 999px;
  background: rgb(255 255 255 / 52%);
  box-shadow: 0 5px 16px rgb(51 65 85 / 6%);
  backdrop-filter: blur(10px);
  white-space: nowrap;

  strong {
    color: var(--el-color-primary);
    font-size: 16px;
    font-weight: 700;
    line-height: 1;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
}

.notice-reminder-body {
  min-height: 0;
  flex: 1 1 auto;
  padding: 32px;
  overflow-y: auto;
  scrollbar-color: var(--el-border-color) transparent;
  scrollbar-width: thin;
}

.notice-reminder-item {
  position: relative;
  min-width: 0;
  padding-left: 26px;

  &::before {
    position: absolute;
    top: 2px;
    bottom: 2px;
    left: 0;
    width: 4px;
    border-radius: 4px;
    background: linear-gradient(180deg, #3b82f6 0%, #8b5cf6 100%);
    content: '';
  }
}

.notice-reminder-type {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 11px;
  border: 1px solid;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
}

.notice-reminder-type.is-notice {
  border-color: var(--el-color-primary-light-7);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.notice-reminder-type.is-announcement {
  border-color: rgb(99 102 241 / 18%);
  color: #4f46e5;
  background: rgb(99 102 241 / 10%);
}

.notice-reminder-content {
  color: var(--el-text-color-regular);
  font-size: 15px;
  line-height: 1.9;
  overflow-wrap: anywhere;

  &.is-empty {
    color: var(--el-text-color-placeholder);
    font-size: 13px;
  }

  p:first-child,
  h1:first-child,
  h2:first-child,
  h3:first-child {
    margin-top: 0;
  }

  p:last-child {
    margin-bottom: 0;
  }

  img {
    max-width: 100%;
    height: auto;
    border-radius: 6px;
  }

  table {
    display: block;
    max-width: 100%;
    overflow-x: auto;
    border-collapse: collapse;
  }

  th,
  td {
    padding: 6px 9px;
    border: 1px solid var(--el-border-color);
  }
}

.notice-reminder-switch-enter-active,
.notice-reminder-switch-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.notice-reminder-switch-enter-from {
  opacity: 0;
  transform: translateX(8px);
}

.notice-reminder-switch-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

.notice-reminder-footer {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  padding: 16px 26px 18px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-extra-light);
}

.notice-reminder-confirm {
  display: inline-flex;
  min-width: 132px;
  height: 42px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 22px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: var(--el-color-primary);
  box-shadow: 0 8px 18px rgb(64 158 255 / 24%);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;

  &:hover:not(:disabled) {
    background: var(--el-color-primary-light-3);
    box-shadow: 0 10px 22px rgb(64 158 255 / 30%);
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    box-shadow: 0 4px 10px rgb(64 158 255 / 22%);
    transform: translateY(0);
  }

  &:focus-visible {
    outline: 2px solid var(--el-color-primary-light-5);
    outline-offset: 2px;
  }

  &:disabled {
    cursor: wait;
    opacity: 0.72;
  }

  .el-icon {
    font-size: 16px;
  }
}

.notice-reminder-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgb(255 255 255 / 42%);
  border-top-color: #fff;
  border-radius: 50%;
  animation: notice-reminder-spin 0.7s linear infinite;
}

@keyframes notice-reminder-spin {
  to {
    transform: rotate(360deg);
  }
}

html.dark .login-notice-reminder-overlay {
  background: rgb(0 0 0 / 62%);
}

html.dark .login-notice-reminder-panel {
  box-shadow: 0 18px 48px rgb(0 0 0 / 48%);

  .notice-reminder-header {
    background:
      radial-gradient(circle at 88% 16%, rgb(92 112 255 / 18%), transparent 36%),
      linear-gradient(135deg, rgb(64 158 255 / 10%) 0%, var(--el-bg-color-overlay) 58%, rgb(139 92 246 / 10%) 100%);
  }

  .notice-reminder-icon {
    background: linear-gradient(135deg, #3b82f6 0%, #5146e5 100%);
  }

  .notice-reminder-count {
    border-color: rgb(255 255 255 / 8%);
    background: rgb(255 255 255 / 5%);
    box-shadow: 0 5px 16px rgb(0 0 0 / 14%);
  }

  .notice-reminder-type.is-notice {
    border-color: rgb(64 158 255 / 34%);
    background: rgb(64 158 255 / 12%);
  }

  .notice-reminder-type.is-announcement {
    border-color: rgb(129 140 248 / 30%);
    color: #a5b4fc;
    background: rgb(99 102 241 / 14%);
  }

  .notice-reminder-content {
    color: #d7dae0;

    :where(p, div, span, li, td, th, label, font) {
      color: inherit !important;
      background-color: transparent !important;
    }

    :where(h1, h2, h3, h4, h5, h6, strong, b) {
      color: #f1f3f6 !important;
    }

    a {
      color: #8ec5ff !important;
    }
  }
}

@media (max-width: 640px) {
  .login-notice-reminder-overlay {
    padding: 16px;
  }

  .login-notice-reminder-panel {
    width: 100%;
    max-height: calc(100vh - 32px);
    border-radius: 16px;
  }

  .notice-reminder-header {
    padding: 22px 20px 21px;
  }

  .notice-reminder-header h2 {
    margin: 18px 0 11px;
    font-size: 22px;
  }

  .notice-reminder-header-meta {
    flex-wrap: wrap;
    font-size: 12px;
  }

  .notice-reminder-icon {
    flex-basis: 42px;
    width: 42px;
    height: 42px;
    border-radius: 12px;
    font-size: 20px;
  }

  .notice-reminder-type {
    height: 26px;
    padding: 0 9px;
    font-size: 12px;
  }

  .notice-reminder-count {
    height: 34px;
    padding: 0 11px;

    strong {
      font-size: 15px;
    }
  }

  .notice-reminder-body {
    padding: 25px 20px 27px;
  }

  .notice-reminder-item {
    padding-left: 20px;
  }

  .notice-reminder-content {
    font-size: 14px;
  }

  .notice-reminder-footer {
    padding: 14px 16px 16px;
  }

  .notice-reminder-confirm {
    width: 100%;
  }
}
</style>
