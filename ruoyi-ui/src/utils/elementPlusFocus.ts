const MODAL_FOCUS_TRAP_SELECTOR = [
  '.el-dialog',
  '.el-drawer',
  '.el-message-box',
  '.el-image-viewer__wrapper',
  '.el-tooltip'
].join(', ')

/**
 * 阻止 Element Plus 模态层关闭后把焦点恢复到触发元素。
 *
 * Element Plus 的 Dialog/Drawer 会在按 Esc 关闭后恢复焦点，导致触发按钮
 * 长时间保留 focus-visible 样式。底层事件不可通过组件回调取消，因此在捕获阶段统一处理。
 */
export function installElementPlusModalFocusGuard(): void {
  if (typeof document === 'undefined') {
    return
  }

  document.addEventListener(
    'focus-trap.focus-after-released',
    (event: Event) => {
      const target = event.target
      if (target instanceof HTMLElement && target.matches(MODAL_FOCUS_TRAP_SELECTOR)) {
        event.preventDefault()
      }
    },
    true
  )
}
