import { onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch } from 'vue'
import useUserStore from '@/store/modules/user'

interface PersistentAutoRefreshOptions {
  storageKeyPrefix: string
  intervalMs: number
  refresh: () => Promise<unknown>
}

export function usePersistentAutoRefresh(options: PersistentAutoRefreshOptions) {
  const userStore = useUserStore()
  const autoRefresh = ref(false)
  let autoRefreshTimer: ReturnType<typeof setTimeout> | undefined
  let storageKey: string | undefined
  let active = false
  let refreshing = false
  let restoring = false

  function readStoredState(key: string): boolean {
    if (typeof window === 'undefined') {
      return false
    }
    try {
      return window.localStorage.getItem(key) === 'true'
    } catch {
      return false
    }
  }

  function writeStoredState(key: string, enabled: boolean): void {
    if (typeof window === 'undefined') {
      return
    }
    try {
      window.localStorage.setItem(key, String(enabled))
    } catch {
      // 本地存储不可用时，仅保留当前会话内的开关状态。
    }
  }

  function stopAutoRefresh(): void {
    if (!autoRefreshTimer) {
      return
    }
    clearTimeout(autoRefreshTimer)
    autoRefreshTimer = undefined
  }

  function scheduleAutoRefresh(): void {
    stopAutoRefresh()
    if (!active || !autoRefresh.value) {
      return
    }
    autoRefreshTimer = setTimeout(runAutoRefresh, options.intervalMs)
  }

  async function runAutoRefresh(): Promise<void> {
    autoRefreshTimer = undefined
    if (!active || !autoRefresh.value) {
      return
    }
    if (refreshing) {
      scheduleAutoRefresh()
      return
    }
    refreshing = true
    try {
      await options.refresh()
    } catch {
      // 请求错误由统一请求层处理，失败后仍继续下一轮刷新。
    } finally {
      refreshing = false
      scheduleAutoRefresh()
    }
  }

  function activate(): void {
    active = true
    scheduleAutoRefresh()
  }

  function deactivate(): void {
    active = false
    stopAutoRefresh()
  }

  function toggleAutoRefresh(): void {
    autoRefresh.value = !autoRefresh.value
  }

  watch(autoRefresh, (enabled: boolean) => {
    if (!restoring && storageKey) {
      writeStoredState(storageKey, enabled)
    }
    if (enabled) {
      scheduleAutoRefresh()
      return
    }
    stopAutoRefresh()
  }, { flush: 'sync' })

  watch(() => userStore.id, (userId: string | number) => {
    stopAutoRefresh()
    storageKey = userId === '' || userId === undefined || userId === null
      ? undefined
      : `${options.storageKeyPrefix}:${String(userId)}`
    restoring = true
    autoRefresh.value = storageKey ? readStoredState(storageKey) : false
    restoring = false
    scheduleAutoRefresh()
  }, { immediate: true, flush: 'sync' })

  onMounted(activate)
  onActivated(activate)
  onDeactivated(deactivate)
  onBeforeUnmount(deactivate)

  return {
    autoRefresh,
    toggleAutoRefresh
  }
}
