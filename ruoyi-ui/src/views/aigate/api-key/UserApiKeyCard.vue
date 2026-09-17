<template>
  <div class="app-container user-api-key-page">
    <el-card class="api-key-card" shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <div class="card-title-wrap">
            <span class="card-title-icon">
              <el-icon><Key /></el-icon>
            </span>
            <div>
              <h2 class="card-title">API 密钥</h2>
              <p class="card-subtitle">每个账号仅有一个密钥，可随时更换</p>
            </div>
          </div>
          <el-tag v-if="apiKey" :type="apiKey.status === '0' ? 'success' : 'danger'" effect="light">
            {{ apiKey.status === '0' ? '正常' : '已停用' }}
          </el-tag>
        </div>
      </template>

      <template v-if="apiKey">
        <el-alert
          class="security-alert"
          title="请勿在客户端代码、公开仓库或聊天记录中暴露密钥"
          type="warning"
          :closable="false"
          show-icon
        />

        <section class="secret-section" aria-labelledby="api-key-label">
          <div class="section-heading">
            <div>
              <div id="api-key-label" class="field-label">当前密钥</div>
              <div class="field-description">调用 API 时，请在 Authorization 请求头中使用此密钥</div>
            </div>
            <el-tag type="info" effect="plain">Bearer Token</el-tag>
          </div>

          <div class="secret-control">
            <el-input
              class="secret-input"
              :model-value="apiKey.apiKey || ''"
              :type="secretVisible ? 'text' : 'password'"
              readonly
              autocomplete="off"
              aria-label="当前 API 密钥"
            />
            <div class="secret-actions">
              <el-tooltip :content="secretVisible ? '隐藏密钥' : '显示密钥'" placement="top">
                <el-button
                  :icon="secretVisible ? Hide : View"
                  :aria-label="secretVisible ? '隐藏密钥' : '显示密钥'"
                  @click="secretVisible = !secretVisible"
                />
              </el-tooltip>
              <el-tooltip content="复制密钥" placement="top">
                <el-button type="primary" :icon="CopyDocument" aria-label="复制密钥" @click="handleCopy" />
              </el-tooltip>
            </div>
          </div>
        </section>

        <div class="key-meta-grid">
          <div class="key-meta-item">
            <span class="key-meta-label">密钥名称</span>
            <strong>{{ apiKey.keyName || '默认密钥' }}</strong>
          </div>
          <div class="key-meta-item">
            <span class="key-meta-label">最近使用</span>
            <strong>{{ apiKey.accessedTime ? formatDateTime(apiKey.accessedTime) : '从未使用' }}</strong>
          </div>
        </div>

        <div class="rotate-panel">
          <div class="rotate-copy">
            <div class="rotate-title">定期更换密钥</div>
            <div class="rotate-description">更换后旧密钥会立即失效，请及时更新所有正在使用它的应用。</div>
          </div>
          <el-button type="danger" plain :icon="RefreshRight" :loading="rotating" @click="handleRotate">
            更换密钥
          </el-button>
        </div>
      </template>

      <el-result
        v-else-if="!loading"
        icon="warning"
        title="暂时无法获取密钥"
        :sub-title="loadError || '请稍后重试'"
      >
        <template #extra>
          <el-button type="primary" @click="loadApiKey">重新加载</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="UserApiKeyCard">
import { CopyDocument, Hide, Key, RefreshRight, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSelfAiApiKey, rotateSelfAiApiKey } from '@/api/aigate/apiKey'
import type { AiApiKey } from '@/types'
import { parseTime } from '@/utils/ruoyi'

const apiKey = ref<AiApiKey>()
const loading = ref(false)
const rotating = ref(false)
const secretVisible = ref(false)
const loadError = ref('')

function formatDateTime(value?: string | null): string {
  return value ? parseTime(value) || '-' : '-'
}

function fallbackCopyText(text: string): boolean {
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

async function copyText(text: string): Promise<boolean> {
  if (!navigator.clipboard) {
    return fallbackCopyText(text)
  }

  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return fallbackCopyText(text)
  }
}

async function handleCopy(): Promise<void> {
  const secret = apiKey.value?.apiKey
  if (!secret) {
    ElMessage.warning('当前没有可复制的密钥')
    return
  }

  const copied = await copyText(secret)
  copied ? ElMessage.success('密钥已复制') : ElMessage.error('复制失败，请手动复制')
}

async function loadApiKey(): Promise<void> {
  loading.value = true
  secretVisible.value = false
  loadError.value = ''
  try {
    const response = await getSelfAiApiKey()
    if (!response.data?.apiKey) {
      apiKey.value = undefined
      loadError.value = '接口未返回有效密钥，请联系管理员'
      return
    }
    apiKey.value = response.data
  } catch {
    apiKey.value = undefined
    loadError.value = '密钥加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

async function handleRotate(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '更换后，旧密钥将立即失效，所有使用旧密钥的应用都会停止调用。确认继续吗？',
      '确认更换密钥',
      {
        type: 'warning',
        confirmButtonText: '确认更换',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }

  rotating.value = true
  try {
    const response = await rotateSelfAiApiKey()
    if (!response.data?.apiKey) {
      ElMessage.error('接口未返回新密钥，请刷新后重试')
      return
    }
    apiKey.value = response.data
    secretVisible.value = true
    ElMessage.success('密钥已更换，旧密钥已失效')
  } catch {
    // 请求层已统一展示错误信息。
  } finally {
    rotating.value = false
  }
}

onMounted(loadApiKey)
</script>

<style scoped lang="scss">
.user-api-key-page {
  min-width: 0;
}

.api-key-card {
  width: min(100%, 960px);
  margin: 0 auto;
  border-radius: 10px;
}

.card-header,
.card-title-wrap,
.section-heading,
.secret-control,
.secret-actions,
.rotate-panel {
  display: flex;
  align-items: center;
}

.card-header,
.section-heading,
.rotate-panel {
  justify-content: space-between;
}

.card-header {
  gap: 16px;
}

.card-title-wrap {
  min-width: 0;
  gap: 12px;
}

.card-title-icon {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 22px;
}

.card-title,
.card-subtitle {
  margin: 0;
}

.card-title {
  color: var(--el-text-color-primary);
  font-size: 18px;
  line-height: 1.45;
}

.card-subtitle {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.45;
}

.security-alert {
  margin-bottom: 22px;
}

.secret-section {
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.section-heading {
  gap: 16px;
  margin-bottom: 12px;
}

.field-label {
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
}

.field-description {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.secret-control {
  gap: 10px;
}

.secret-input {
  min-width: 0;
  flex: 1;
}

.secret-input :deep(.el-input__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  letter-spacing: 0.02em;
}

.secret-actions {
  flex: 0 0 auto;
  gap: 8px;
}

.secret-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.key-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.key-meta-item {
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.key-meta-item strong {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.key-meta-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.rotate-panel {
  gap: 24px;
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.rotate-copy {
  min-width: 0;
}

.rotate-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.rotate-description {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 767px) {
  .secret-section {
    padding: 16px;
  }

  .secret-control,
  .rotate-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .secret-actions :deep(.el-button) {
    flex: 1;
  }

  .key-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .rotate-panel :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 479px) {
  .card-header {
    align-items: flex-start;
  }

  .key-meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
