<template>
  <div class="models-page">
    <div class="models-layout">
      <!-- 主内容区 -->
      <main class="models-main">
        <!-- 顶部标题 -->
        <div class="models-header">
          <h2 class="header-title">模型可用状态与费率明细（计价单位：1M Tokens）</h2>

          <!-- 平台筛选 + 搜索 -->
          <div class="filter-row">
            <div class="platform-buttons">
              <button
                class="platform-btn"
                :class="{ active: activePlatform === undefined }"
                @click="onPlatformChange(undefined)"
              >全部平台</button>
              <button
                v-for="platform in platformOptions"
                :key="platform.value"
                class="platform-btn"
                :class="{ active: activePlatform === platform.value }"
                @click="onPlatformChange(platform.value)"
              >{{ platform.label }}</button>
            </div>

            <el-input
              v-model="searchKeyword"
              class="search-input"
              placeholder="搜索模型名称..."
              :prefix-icon="Search"
              clearable
            />
          </div>
        </div>

        <!-- 表格 -->
        <el-table
          v-loading="loading"
          :data="filteredModels"
          class="models-table"
          :header-cell-style="headerCellStyle"
        >
          <el-table-column label="模型名称" min-width="260">
            <template #default="{ row }">
              <div class="model-name-cell">
                <span class="status-dot"></span>
                <span class="model-name">{{ row.modelName }}</span>
                <span class="platform-tag">{{ getPlatformName(row.platform) }}</span>
                <span
                  v-for="tag in parseTags(row.tags)"
                  :key="tag"
                  class="badge"
                  :class="tag.toLowerCase()"
                >{{ tag.toUpperCase() }}</span>
                <el-icon class="copy-btn" @click="copyModelName(row.modelName!)">
                  <CopyDocument />
                </el-icon>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="输入" align="center" width="140">
            <template #default="{ row }">
              <span>{{ formatPrice(row.officialInputPrice) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="输出" align="center" width="140">
            <template #default="{ row }">
              <span>{{ formatPrice(row.officialOutputPrice) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="缓存读取" align="center" width="140">
            <template #default="{ row }">
              <span>{{ formatPrice(row.officialCacheReadPrice) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="缓存写入" align="center" width="140">
            <template #default="{ row }">
              <span>{{ formatPrice(row.officialCacheWritePrice) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { publicModels } from '@/api/aigate/public'
import { fixedNumber } from '@/views/aigate/common'
import type { AiModel, AiPlatform } from '@/types'

const PLATFORM_LABELS: Record<AiPlatform, string> = {
  openai: 'OpenAI',
  azure: 'Azure OpenAI',
  anthropic: 'Anthropic',
  gemini: 'Gemini',
  deepseek: 'DeepSeek',
  ollama: 'Ollama',
  custom: '自定义',
  zhipu: '智谱',
  aliyun: '阿里云',
}

// ── 状态变量 ──
const allModels = ref<AiModel[]>([])
const loading = ref(false)

const activePlatform = ref<AiPlatform | undefined>(undefined)
const searchKeyword = ref('')

const platformOptions = computed(() => {
  const seen = new Set<AiPlatform>()
  return allModels.value
    .map((model) => model.platform)
    .filter((platform): platform is AiPlatform => platform !== undefined)
    .filter((platform) => {
      if (seen.has(platform)) return false
      seen.add(platform)
      return true
    })
    .map((platform) => ({
      value: platform,
      label: PLATFORM_LABELS[platform] ?? platform,
    }))
})

// ── 表头样式（响应主题切换） ──
const headerCellStyle = computed(() => {
  const isDark = document.documentElement.classList.contains('dark')
  return {
    background: isDark ? '#1d1e1f' : '#f5f7fa',
    color: isDark ? '#d0d0d0' : '#606266',
    fontWeight: 600,
    fontSize: '13px',
  }
})

// ── 数据加载 ──
async function loadData() {
  loading.value = true
  try {
    const modelRes = await publicModels()
    allModels.value = modelRes.data
  } finally {
    loading.value = false
  }
}

// ── 平台切换 ──
function onPlatformChange(platform: AiPlatform | undefined) {
  activePlatform.value = platform
}

// ── 计算属性：筛选后的模型列表 ──
const filteredModels = computed(() => {
  let list = [...allModels.value]

  // 平台筛选
  if (activePlatform.value !== undefined) {
    list = list.filter(m => m.platform === activePlatform.value)
  }

  // 搜索筛选
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(m => m.modelName?.toLowerCase().includes(kw))
  }

  return list
})

// ── 辅助函数 ──

/** 平台名称映射 */
function getPlatformName(platform: AiPlatform | undefined): string {
  return platform ? PLATFORM_LABELS[platform] ?? platform : '-'
}

/** 价格格式化（官方价，$），去除末尾多余的0 */
function formatPrice(value: number | undefined): string {
  if (value === undefined || value === null) return '-'
  return '$' + fixedNumber(value).replace(/\.?0+$/, '')
}

/** 标签解析 */
function parseTags(tags: string | undefined): string[] {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

/** 复制模型名称 */
async function copyModelName(name: string) {
  try {
    await navigator.clipboard.writeText(name)
    ElMessage.success('已复制: ' + name)
  } catch {
    ElMessage.error('复制失败')
  }
}

// ── 初始化 ──
onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
/* ── Theme tokens ── */
.models-page {
  /* backgrounds */
  --mp-bg: #ffffff;
  --mp-bg-hover: #f5f7fa;
  --mp-bg-active: #303133;
  --mp-bg-platform-tag: rgba(99, 102, 241, 0.08);

  /* text */
  --mp-text: #303133;
  --mp-text-secondary: #606266;
  --mp-text-muted: #909399;
  --mp-text-placeholder: #c0c4cc;
  --mp-text-active: #ffffff;
  --mp-text-price: #303133;
  --mp-text-platform-tag: #6366f1;
  --mp-text-copy: #c0c4cc;
  --mp-text-copy-hover: #6366f1;

  /* border */
  --mp-border: #dcdfe6;
  --mp-border-hover: #303133;
  --mp-border-active: #303133;

  /* shadow / misc */
  --mp-shadow-card: none;
}

html.dark .models-page {
  --mp-bg: #0a0a0f;
  --mp-bg-hover: rgba(255, 255, 255, 0.04);
  --mp-bg-active: #6366f1;
  --mp-bg-platform-tag: rgba(99, 102, 241, 0.12);

  --mp-text: #f0f0f5;
  --mp-text-secondary: #9ca3b0;
  --mp-text-muted: #6b7280;
  --mp-text-placeholder: #4b5563;
  --mp-text-active: #ffffff;
  --mp-text-price: #f0f0f5;
  --mp-text-platform-tag: #a5b4fc;
  --mp-text-copy: #4b5563;
  --mp-text-copy-hover: #a5b4fc;

  --mp-border: rgba(255, 255, 255, 0.08);
  --mp-border-hover: rgba(255, 255, 255, 0.15);
  --mp-border-active: #6366f1;

  --mp-shadow-card: 0 1px 3px rgba(0, 0, 0, 0.2);
}

/* ── Page ── */
.models-page {
  width: 100%;
  background: var(--mp-bg);
  color: var(--mp-text);
  min-height: calc(100vh - 60px);
}

.models-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* ── 主区域 ── */
.models-main {
  flex: 1;
  min-width: 0;
}

/* ── 顶部筛选区 ── */
.models-header {
  margin-bottom: 20px;

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--mp-text);
    margin: 0 0 16px;
  }
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

/* ── 平台筛选按钮 ── */
.platform-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.platform-btn {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid var(--mp-border);
  background: var(--mp-bg);
  color: var(--mp-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    border-color: var(--mp-border-hover);
    color: var(--mp-text);
  }

  &.active {
    background: var(--mp-bg-active);
    color: var(--mp-text-active);
    border-color: var(--mp-border-active);
  }
}

.search-input {
  width: 240px;
  flex-shrink: 0;
}

/* ── 表格 ── */
.models-table {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

/* ── 模型名称列 ── */
.model-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #67c23a;
    flex-shrink: 0;
  }

  .model-name {
    font-weight: 500;
    color: var(--mp-text);
    font-size: 14px;
  }

  .platform-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 10px;
    background: var(--mp-bg-platform-tag);
    color: var(--mp-text-platform-tag);
    white-space: nowrap;
  }

  .badge {
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 4px;
    color: #fff;
    font-weight: 600;
    white-space: nowrap;

    &.hot {
      background: #e6a23c;
    }

    &.new {
      background: #667eea;
    }
  }

  .copy-btn {
    cursor: pointer;
    color: var(--mp-text-copy);
    transition: color 0.2s;
    font-size: 14px;
    flex-shrink: 0;

    &:hover {
      color: var(--mp-text-copy-hover);
    }
  }
}

/* ── 价格列 ── */
.price-value {
  font-size: 14px;
  color: var(--mp-text-price);
  font-weight: 500;
}

/* ── 响应式适配 ── */
@media (max-width: 768px) {
  .models-layout {
    padding: 16px;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .platform-buttons {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 4px;
  }

  .search-input {
    width: 100% !important;
  }
}
</style>
