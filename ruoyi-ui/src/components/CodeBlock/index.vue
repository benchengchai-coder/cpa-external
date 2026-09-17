<template>
  <div class="code-block" :class="{ 'is-copied': copied }">
    <div class="code-block-bar">
      <span class="code-block-lang">{{ langLabel }}</span>
      <button class="code-block-copy" type="button" @click="copy">
        <el-icon v-if="!copied"><CopyDocument /></el-icon>
        <el-icon v-else><Select /></el-icon>
        <span>{{ copied ? '已复制' : '复制' }}</span>
      </button>
    </div>
    <pre class="code-block-pre"><code>{{ code }}</code></pre>
  </div>
</template>

<script setup lang="ts">
import { CopyDocument, Select } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    code: string
    language?: string
  }>(),
  { language: 'bash' }
)

const copied = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

const langLabel = computed(() => {
  const map: Record<string, string> = {
    http: 'HTTP',
    bash: 'Shell',
    json: 'JSON',
    javascript: 'JavaScript',
    python: 'Python',
    text: 'Text'
  }
  return map[props.language] || props.language.toUpperCase()
})

async function copy() {
  try {
    await navigator.clipboard.writeText(props.code)
  } catch {
    // Fallback for non-secure contexts
    const ta = document.createElement('textarea')
    ta.value = props.code
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }
  copied.value = true
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    copied.value = false
  }, 2000)
}

onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})
</script>

<style lang="scss" scoped>
.code-block {
  margin: 16px 0;
  border: 1px solid #2d2d2d;
  border-radius: 10px;
  overflow: hidden;
  background: #1e1e1e;
}

.code-block-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: #2a2a2a;
  border-bottom: 1px solid #333;
}

.code-block-lang {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #8f959e;
}

.code-block-copy {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.06);
  color: #c0c4cc;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.code-block-copy:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.code-block.is-copied .code-block-copy {
  background: rgba(16, 185, 129, 0.18);
  color: #34d399;
}

.code-block-pre {
  margin: 0;
  padding: 18px 16px;
  overflow-x: auto;
}

.code-block-pre code {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 13.5px;
  line-height: 1.7;
  color: #d4d4d4;
  white-space: pre;
  tab-size: 2;
}
</style>
