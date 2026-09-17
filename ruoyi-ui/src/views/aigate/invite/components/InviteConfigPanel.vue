<template>
  <div class="invite-config-panel" v-loading="loading">
    <el-alert type="info" :closable="false" show-icon class="config-alert">
      <template #title>配置保存后立即生效，无需重启服务</template>
      <p>调整仅影响后续邀请绑定与返利计算，不会追溯重算历史流水；用户专属返利比例始终优先于全局比例。</p>
      <p>关闭总开关不会删除已有关系和流水，也不影响领取已经产生的待领返利。</p>
    </el-alert>

    <el-result v-if="loadError" icon="error" title="邀请参数加载失败" :sub-title="loadError">
      <template #extra>
        <el-button type="primary" :loading="loading" @click="loadConfig">重新加载</el-button>
      </template>
    </el-result>

    <div v-else>
      <el-form
        ref="configFormRef"
        :model="configForm"
        :rules="configRules"
        class="config-form"
        :disabled="!loaded || saving"
      >
        <section class="master-switch" :class="{ 'is-enabled': configForm.enabled }">
          <div class="master-switch-main">
            <div class="master-switch-icon">
              <el-icon><Promotion /></el-icon>
            </div>
            <div class="master-switch-copy">
              <div class="master-switch-title">
                <span>邀请返利总开关</span>
                <span class="core-badge">核心功能</span>
              </div>
              <p>
                {{ configForm.enabled
                  ? '正在建立新的邀请关系，并按下方规则计算充值返利。'
                  : '关闭后不再建立新的邀请关系，也不再产生新的充值返利。' }}
              </p>
            </div>
          </div>
          <el-form-item prop="enabled" class="master-switch-control">
            <span class="switch-state">{{ configForm.enabled ? '已开启' : '已关闭' }}</span>
            <el-switch v-model="configForm.enabled" aria-label="邀请返利总开关" />
          </el-form-item>
        </section>

        <div class="section-heading">
          <div>
            <h4>返利参数</h4>
            <p>设置邀请奖励的计算方式、等待时间与累计上限</p>
          </div>
          <span>0 值规则已在各项中标注</span>
        </div>

        <div class="settings-grid">
          <section class="setting-item setting-item-rate">
            <div class="setting-item-header">
              <div class="setting-icon">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div>
                <h5>全局返利比例</h5>
                <p>用户专属比例始终优先于此设置</p>
              </div>
            </div>
            <el-form-item prop="rebateRate" class="setting-form-item">
              <div class="number-field">
                <el-input-number
                  v-model="configForm.rebateRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :step="0.1"
                  aria-label="全局返利比例"
                />
                <span class="field-unit">%</span>
              </div>
              <div class="field-meta">
                <span>可设置范围</span>
                <strong>0 — 100</strong>
              </div>
            </el-form-item>
          </section>

          <section class="setting-item setting-item-freeze">
            <div class="setting-item-header">
              <div class="setting-icon">
                <el-icon><Lock /></el-icon>
              </div>
              <div>
                <h5>返利冻结期</h5>
                <p>返利产生后需要等待多久才能领取</p>
              </div>
            </div>
            <el-form-item prop="freezeHours" class="setting-form-item">
              <div class="number-field">
                <el-input-number
                  v-model="configForm.freezeHours"
                  :min="0"
                  :max="720"
                  :precision="0"
                  :step="1"
                  aria-label="返利冻结期"
                />
                <span class="field-unit">小时</span>
              </div>
              <div class="field-meta">
                <span>0 表示无需冻结</span>
                <strong>最长 720 小时</strong>
              </div>
            </el-form-item>
          </section>

          <section class="setting-item setting-item-duration">
            <div class="setting-item-header">
              <div class="setting-icon">
                <el-icon><Calendar /></el-icon>
              </div>
              <div>
                <h5>返利有效期</h5>
                <p>从被邀请人注册时间开始计算</p>
              </div>
            </div>
            <el-form-item prop="durationDays" class="setting-form-item">
              <div class="number-field">
                <el-input-number
                  v-model="configForm.durationDays"
                  :min="0"
                  :max="3650"
                  :precision="0"
                  :step="1"
                  aria-label="返利有效期"
                />
                <span class="field-unit">天</span>
              </div>
              <div class="field-meta">
                <span>0 表示永久有效</span>
                <strong>最长 3650 天</strong>
              </div>
            </el-form-item>
          </section>

          <section class="setting-item setting-item-cap">
            <div class="setting-item-header">
              <div class="setting-icon">
                <el-icon><Money /></el-icon>
              </div>
              <div>
                <h5>单个被邀请人返利上限</h5>
                <p>限制单个用户累计贡献的返利金额</p>
              </div>
            </div>
            <el-form-item prop="perInviteeCap" class="setting-form-item">
              <div class="number-field">
                <el-input-number
                  v-model="configForm.perInviteeCap"
                  :min="0"
                  :step="1"
                  aria-label="单个被邀请人返利上限"
                />
                <span class="field-unit">美元</span>
              </div>
              <div class="field-meta">
                <span>0 表示不设上限</span>
                <strong>按累计返利计算</strong>
              </div>
            </el-form-item>
          </section>
        </div>

        <div class="config-actions">
          <div class="change-state" :class="{ 'is-dirty': dirty }">
            <div class="change-state-icon">
              <el-icon><component :is="dirty ? EditPen : CircleCheckFilled" /></el-icon>
            </div>
            <div>
              <strong>{{ dirty ? '有未保存的修改' : '所有配置已同步' }}</strong>
              <span>{{ dirty ? '保存后新规则将立即生效' : '当前显示的是已加载配置' }}</span>
            </div>
          </div>
          <div class="action-buttons">
            <el-button :disabled="!loaded || !dirty || saving" @click="restoreLoadedConfig">
              <el-icon><RefreshLeft /></el-icon>
              恢复已加载值
            </el-button>
            <el-button type="primary" :loading="saving" :disabled="!loaded || !dirty" @click="saveConfig">
              保存配置
            </el-button>
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts" name="InviteConfigPanel">
import {
  Calendar,
  CircleCheckFilled,
  EditPen,
  Lock,
  Money,
  Promotion,
  RefreshLeft,
  TrendCharts
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getInviteConfig, updateInviteConfig } from '@/api/aigate/invite'
import type { InviteAdminConfig } from '@/types'

const { proxy } = getCurrentInstance() as any
const configFormRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const loadError = ref('')
const loadedConfig = ref<InviteAdminConfig | null>(null)
const configForm = reactive<InviteAdminConfig>({
  enabled: false,
  rebateRate: 0,
  freezeHours: 0,
  durationDays: 0,
  perInviteeCap: 0
})

const configRules: FormRules<InviteAdminConfig> = {
  enabled: [{ required: true, message: '请选择邀请返利总开关', trigger: 'change' }],
  rebateRate: [
    { required: true, message: '请输入全局返利比例', trigger: 'change' },
    { type: 'number', min: 0, max: 100, message: '全局返利比例必须在 0-100 之间', trigger: 'change' }
  ],
  freezeHours: [
    { required: true, message: '请输入返利冻结期', trigger: 'change' },
    { type: 'number', min: 0, max: 720, message: '返利冻结期必须在 0-720 小时之间', trigger: 'change' }
  ],
  durationDays: [
    { required: true, message: '请输入返利有效期', trigger: 'change' },
    { type: 'number', min: 0, max: 3650, message: '返利有效期必须在 0-3650 天之间', trigger: 'change' }
  ],
  perInviteeCap: [
    { required: true, message: '请输入单人返利上限', trigger: 'change' },
    { type: 'number', min: 0, message: '单人返利上限不能小于 0', trigger: 'change' }
  ]
}

const dirty = computed(() => {
  const original = loadedConfig.value
  return original != null && (
    original.enabled !== configForm.enabled
    || original.rebateRate !== configForm.rebateRate
    || original.freezeHours !== configForm.freezeHours
    || original.durationDays !== configForm.durationDays
    || original.perInviteeCap !== configForm.perInviteeCap
  )
})

function normalizeConfig(config: InviteAdminConfig): InviteAdminConfig {
  return {
    enabled: Boolean(config.enabled),
    rebateRate: Number(config.rebateRate),
    freezeHours: Number(config.freezeHours),
    durationDays: Number(config.durationDays),
    perInviteeCap: Number(config.perInviteeCap)
  }
}

function applyLoadedConfig(config: InviteAdminConfig) {
  const normalized = normalizeConfig(config)
  loadedConfig.value = { ...normalized }
  Object.assign(configForm, normalized)
  loaded.value = true
  nextTick(() => configFormRef.value?.clearValidate())
}

function loadConfig() {
  loading.value = true
  loadError.value = ''
  getInviteConfig()
    .then((response) => {
      applyLoadedConfig(response.data)
    })
    .catch(() => {
      loaded.value = false
      loadedConfig.value = null
      loadError.value = '请检查邀请参数是否完整，并确认已执行邀请参数升级 SQL。'
    })
    .finally(() => {
      loading.value = false
    })
}

async function saveConfig() {
  if (!configFormRef.value) {
    return
  }
  const valid = await configFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  saving.value = true
  const submitted = normalizeConfig(configForm)
  updateInviteConfig(submitted)
    .then((response) => {
      applyLoadedConfig(response.data || submitted)
      proxy.$modal.msgSuccess('邀请参数配置保存成功')
    })
    .catch(() => {})
    .finally(() => {
      saving.value = false
    })
}

function restoreLoadedConfig() {
  if (loadedConfig.value) {
    Object.assign(configForm, loadedConfig.value)
    nextTick(() => configFormRef.value?.clearValidate())
  }
}

onMounted(loadConfig)
</script>

<style scoped lang="scss">
.invite-config-panel {
  width: 100%;
  min-width: 0;
  max-width: 1080px;
  margin: 0 auto;
}

.config-alert {
  margin-bottom: 16px;
}

.config-alert p {
  margin: 4px 0 0;
  line-height: 1.6;
}

.master-switch {
  display: flex;
  min-height: 104px;
  padding: 20px;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.master-switch.is-enabled {
  background: linear-gradient(135deg, rgb(103 194 58 / 10%), var(--el-fill-color-lighter));
  border-color: var(--el-color-success-light-5);
  box-shadow: 0 8px 24px rgb(103 194 58 / 8%);
}

.master-switch-main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 15px;
}

.master-switch-icon {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 auto;
  place-items: center;
  color: var(--el-text-color-secondary);
  font-size: 23px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  transition: color 0.2s ease, border-color 0.2s ease;
}

.is-enabled .master-switch-icon {
  color: var(--el-color-success);
  border-color: var(--el-color-success-light-5);
}

.master-switch-copy {
  min-width: 0;

  p {
    margin: 7px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
    line-height: 1.6;
  }
}

.master-switch-title {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.core-badge {
  padding: 3px 7px;
  color: var(--el-color-primary);
  font-size: 10px;
  font-weight: 600;
  line-height: 1;
  background: rgb(64 158 255 / 12%);
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 999px;
}

.master-switch-control {
  margin: 0;
  flex: 0 0 auto;
}

.master-switch-control :deep(.el-form-item__content) {
  flex-wrap: nowrap;
  gap: 10px;
}

.master-switch-control :deep(.el-form-item__error) {
  display: none;
}

.master-switch-control :deep(.el-switch) {
  --el-switch-on-color: var(--el-color-success);
}

.switch-state {
  min-width: 42px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 600;
  text-align: right;
}

.section-heading {
  display: flex;
  margin: 24px 2px 14px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;

  h4 {
    margin: 0 0 5px;
    color: var(--el-text-color-primary);
    font-size: 15px;
    font-weight: 700;
  }

  p,
  > span {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }

  > span {
    flex: 0 0 auto;
    color: var(--el-text-color-placeholder);
  }
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.setting-item {
  --setting-color: var(--el-color-primary);
  --setting-bg: rgb(64 158 255 / 12%);
  min-width: 0;
  padding: 19px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.setting-item:hover {
  border-color: var(--setting-color);
  box-shadow: 0 9px 24px rgb(15 23 42 / 7%);
  transform: translateY(-1px);
}

.setting-item-freeze {
  --setting-color: var(--el-color-warning);
  --setting-bg: rgb(230 162 60 / 12%);
}

.setting-item-duration {
  --setting-color: var(--el-color-success);
  --setting-bg: rgb(103 194 58 / 12%);
}

.setting-item-cap {
  --setting-color: var(--el-color-danger);
  --setting-bg: rgb(245 108 108 / 12%);
}

.setting-item-header {
  display: flex;
  min-height: 48px;
  align-items: center;
  gap: 12px;

  h5 {
    margin: 0 0 5px;
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 700;
    line-height: 1.35;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.45;
  }
}

.setting-icon {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  color: var(--setting-color);
  font-size: 19px;
  background: var(--setting-bg);
  border-radius: 11px;
}

.setting-form-item {
  margin: 17px 0 0;
}

.setting-form-item :deep(.el-form-item__content) {
  display: block;
  line-height: normal;
}

.setting-form-item :deep(.el-form-item__error) {
  position: static;
  padding-top: 7px;
}

.number-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: stretch;
  gap: 8px;
}

.number-field :deep(.el-input-number) {
  width: 100%;
  overflow: hidden;
  border-radius: 10px;
}

.number-field :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 10px;
}

.number-field :deep(.el-input-number__decrease) {
  border-radius: 10px 0 0 10px;
}

.number-field :deep(.el-input-number__increase) {
  border-radius: 0 10px 10px 0;
}

.number-field :deep(.el-input__inner) {
  font-size: 16px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.field-unit {
  display: grid;
  min-width: 58px;
  height: 44px;
  padding: 0 12px;
  place-items: center;
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
}

.field-meta {
  display: flex;
  margin-top: 9px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  line-height: 1.4;

  strong {
    color: var(--el-text-color-secondary);
    font-weight: 500;
    text-align: right;
  }
}

.config-actions {
  display: flex;
  margin-top: 24px;
  padding: 18px 20px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
}

.change-state {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;

  strong,
  span {
    display: block;
  }

  strong {
    margin-bottom: 3px;
    color: var(--el-text-color-primary);
    font-size: 13px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
}

.change-state-icon {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  color: var(--el-color-success);
  background: rgb(103 194 58 / 12%);
  border-radius: 10px;
}

.change-state.is-dirty .change-state-icon {
  color: var(--el-color-warning);
  background: rgb(230 162 60 / 12%);
}

.action-buttons {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;

  .el-button {
    min-width: 124px;
    margin-left: 0;
  }
}

@media (max-width: 767px) {

  .master-switch {
    padding: 17px;
    align-items: flex-start;
    flex-direction: column;
    gap: 15px;
  }

  .master-switch-control {
    width: 100%;
    padding-top: 13px;
    border-top: 1px solid var(--el-border-color-lighter);
  }

  .master-switch-control :deep(.el-form-item__content) {
    justify-content: space-between;
  }

  .switch-state {
    text-align: left;
  }

  .section-heading {
    align-items: flex-start;
  }

  .section-heading > span {
    display: none;
  }

  .settings-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .config-actions {
    padding: 16px;
    align-items: stretch;
    flex-direction: column;
  }

  .action-buttons {
    width: 100%;
  }

  .action-buttons .el-button {
    min-width: 0;
    flex: 1;
  }
}

@media (max-width: 420px) {
  .master-switch-main {
    align-items: flex-start;
  }

  .master-switch-icon {
    width: 42px;
    height: 42px;
  }

  .setting-item {
    padding: 16px;
  }

  .field-unit {
    min-width: 50px;
    padding: 0 9px;
  }

  .action-buttons {
    flex-direction: column-reverse;
  }
}
</style>
