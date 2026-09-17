import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

export const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' }
]

export const channelStatusOptions = [
  { label: '正常', value: 'active' },
  { label: '限流', value: 'rate_limited' },
  { label: '降级', value: 'degraded' },
  { label: '禁用', value: 'disabled' }
]

export const enabledOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

export const yesNoOptions = [
  { label: '是', value: 1 },
  { label: '否', value: 0 }
]

export const streamOptions = [
  { label: '流式', value: 1 },
  { label: '非流式', value: 0 }
]

export const redemptionCodeStatusOptions = [
  { label: '未使用', value: '0' },
  { label: '已使用', value: '1' },
  { label: '已禁用', value: '2' }
]

export const channelTypeOptions = [
  { label: 'API Key', value: 'apikey' },
  { label: 'OAuth', value: 'oauth' }
]

export function optionLabel<T extends Record<string, any>>(
  list: T[],
  value: number | string | undefined,
  valueKey: keyof T,
  labelKey: keyof T
) {
  const item = list.find((entry) => entry[valueKey] === value)
  return item ? String(item[labelKey]) : '-'
}

export function fixedNumber(value: number | string | undefined, digits = 6) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  return Number(value).toFixed(digits)
}

export function formatCurrency(value: number | string | undefined | null, digits = 6, trimTrailingZeros = false) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  const amount = Number(value)
  if (!Number.isFinite(amount)) {
    return '-'
  }
  const formatted = amount.toFixed(digits)
  return `$${trimTrailingZeros ? formatted.replace(/\.?0+$/, '') : formatted}`
}

/** 用户计费倍率格式化：保留两位小数并去掉末尾多余的 0，未归属用户时显示 - */
export function formatMultiplier(value: number | string | undefined | null) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  const multiplier = Number(value)
  if (!Number.isFinite(multiplier)) {
    return '-'
  }
  return `${parseFloat(multiplier.toFixed(2))}x`
}

const REASONING_EFFORT_LABELS: Record<string, string> = {
  minimal: 'Minimal',
  low: 'Low',
  medium: 'Medium',
  high: 'High'
}

const REASONING_EFFORT_TAG_TYPES: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
  minimal: 'info',
  low: 'primary',
  medium: 'success',
  high: 'warning',
  xhigh: 'danger'
}

/** 推理等级展示文案，未上报时显示 - */
export function reasoningEffortLabel(effort: string | undefined | null) {
  if (!effort) {
    return '-'
  }
  return REASONING_EFFORT_LABELS[effort] || effort
}

/** 推理等级标签颜色，未知等级使用默认色 */
export function reasoningEffortTagType(effort: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  return REASONING_EFFORT_TAG_TYPES[effort] || ''
}

export function billingModeLabel(value: string | undefined | null) {
  if (!value) {
    return '-'
  }
  const labels: Record<string, string> = {
    wallet: '按量',
    subscription: '订阅',
    mixed: '混合',
    no_charge: '未计费',
    write_off: '人工核销'
  }
  return labels[value] || value
}

export function billingModeTagType(
  value: string | undefined | null
): 'primary' | 'success' | 'info' | 'warning' {
  const types: Record<string, 'primary' | 'success' | 'info' | 'warning'> = {
    wallet: 'primary',
    subscription: 'success',
    mixed: 'warning',
    no_charge: 'info',
    write_off: 'info'
  }
  return (value && types[value]) || 'info'
}

export function billingStatusLabel(value: string | undefined | null) {
  if (!value) {
    return '-'
  }
  const labels: Record<string, string> = {
    processing: '处理中',
    reserved: '已预占',
    pending_settlement: '结算处理中',
    success: '结算成功',
    partial: '部分结算',
    released: '已释放',
    expired: '已过期',
    failed: '结算失败',
    written_off: '已核销'
  }
  return labels[value] || value
}

export function billingStatusTagType(
  value: string | undefined | null
): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  const types: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
    processing: 'info',
    reserved: 'primary',
    pending_settlement: 'warning',
    success: 'success',
    partial: 'warning',
    released: 'info',
    expired: 'info',
    failed: 'danger',
    written_off: 'info'
  }
  return (value && types[value]) || 'info'
}

export function formatMilliseconds(value: number | string | undefined | null, zeroAsEmpty = false) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  const milliseconds = Number(value)
  if (!Number.isFinite(milliseconds) || (zeroAsEmpty && milliseconds === 0)) {
    return '-'
  }
  return milliseconds > 1000 ? `${(milliseconds / 1000).toFixed(2)}s` : `${value}ms`
}

/**
 * 请求时间格式化。
 *
 * ai_log.timestamp 是 datetime，接口返回 “yyyy-MM-dd HH:mm:ss”；
 * 兼容场景下仍可能是 CLIProxyAPI 上报的 ISO-8601（秒小数位可能超过 3 位，纳秒），
 * 若直接交给 parseTime 会被截成非法字符串，因此这里自行解析。
 * 解析失败时原样返回，避免页面出现 NaN。
 */
export function formatRequestTime(value: string | undefined | null) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  const date = parseRequestDate(String(value))
  if (!date) {
    return String(value)
  }
  const pad = (input: number) => (input < 10 ? `0${input}` : String(input))
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

/** 把“日期 时间”补成 ISO 形式，并把秒的小数位截断到毫秒，保证各浏览器都能解析。 */
function parseRequestDate(value: string): Date | null {
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }
  if (/^\d+$/.test(trimmed)) {
    const numeric = Number(trimmed)
    return new Date(trimmed.length === 10 ? numeric * 1000 : numeric)
  }
  const normalized = trimmed.replace(' ', 'T').replace(/(\.\d{3})\d+/, '$1')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatTokenCount(value: number | string | undefined | null) {
  const tokens = Number(value || 0)
  if (!Number.isFinite(tokens)) {
    return '0'
  }
  if (tokens > 1000 * 1000) {
    return `${(tokens / 1000 / 1000).toFixed(2)}M`
  }
  if (tokens > 1000) {
    return `${(tokens / 1000).toFixed(2)}K`
  }
  return String(tokens)
}

export function formatCacheHitRate(
  cacheReadTokens: number | string | undefined | null,
  promptTokens: number | string | undefined | null
) {
  const cacheRead = Number(cacheReadTokens || 0)
  const prompt = Number(promptTokens || 0)
  if (!Number.isFinite(cacheRead) || !Number.isFinite(prompt) || cacheRead <= 0 || prompt <= 0) {
    return '0%'
  }
  return `${Number(((cacheRead / prompt) * 100).toFixed(2))}%`
}

export function useAiDrawerSize() {
  const width = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)

  const updateWidth = () => {
    width.value = window.innerWidth
  }

  onMounted(() => {
    updateWidth()
    window.addEventListener('resize', updateWidth)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', updateWidth)
  })

  return computed(() => {
    if (width.value < 768) {
      return '100%'
    }
    if (width.value < 1200) {
      return '70%'
    }
    if (width.value < 1600) {
      return '52%'
    }
    return '720px'
  })
}
