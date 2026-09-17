const NOTICE_REMINDER_CHECK_PENDING_KEY = 'notice-reminder-check-pending'
export const NOTICE_READ_STATUS_CHANGED_EVENT = 'notice-read-status-changed'

export function markNoticeReminderCheckPending(): void {
  try {
    sessionStorage.setItem(NOTICE_REMINDER_CHECK_PENDING_KEY, 'true')
  } catch {
    // 浏览器禁用会话存储时不影响正常登录。
  }
}

export function hasNoticeReminderCheckPending(): boolean {
  try {
    return sessionStorage.getItem(NOTICE_REMINDER_CHECK_PENDING_KEY) === 'true'
  } catch {
    return false
  }
}

export function clearNoticeReminderCheckPending(): void {
  try {
    sessionStorage.removeItem(NOTICE_REMINDER_CHECK_PENDING_KEY)
  } catch {
    // 浏览器禁用会话存储时无需处理。
  }
}

export function notifyNoticeReadStatusChanged(): void {
  window.dispatchEvent(new CustomEvent(NOTICE_READ_STATUS_CHANGED_EVENT))
}
