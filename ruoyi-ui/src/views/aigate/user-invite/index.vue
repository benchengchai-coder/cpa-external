<template>
  <div class="app-container user-invite-page">
    <section class="invite-hero" v-loading="loading">
      <div class="invite-hero-copy">
        <span class="section-kicker">推荐计划</span>
        <h2>邀请好友，一起体验更高效的 AI 服务</h2>
        <p>分享你的专属链接，好友完成注册并充值后，你将获得对应比例的余额返利。</p>
        <div v-if="inviteInfo" class="hero-actions">
          <el-button type="primary" @click="handleCopy(fullInviteLink, '邀请链接')">
            <el-icon><Share /></el-icon>
            复制邀请链接
          </el-button>
          <el-button @click="openInviteeDialog">
            <el-icon><UserFilled /></el-icon>
            查看邀请记录
          </el-button>
        </div>
      </div>

      <div class="rebate-rate-panel">
        <div class="rebate-rate-icon">
          <el-icon><Present /></el-icon>
        </div>
        <span>当前充值返利比例</span>
        <strong>{{ inviteInfo?.effectiveRebateRate ?? 0 }}<small>%</small></strong>
        <p>好友每次符合条件的充值，都将返利。</p>
      </div>
    </section>

    <template v-if="inviteInfo">
      <div class="invite-main-grid">
        <section class="invite-section share-section">
          <div class="section-header">
            <div>
              <span class="section-kicker">专属邀请</span>
              <h3>把邀请方式分享给好友</h3>
              <p>好友通过专属链接注册后，将自动与你建立推荐关系。</p>
            </div>
            <el-button
              type="primary"
              class="claim-entry"
              :loading="claimLoading"
              :disabled="!Number(inviteInfo.pendingRebate || 0)"
              @click="handleClaimRebate"
            >
              <el-icon><WalletFilled /></el-icon>
              {{ Number(inviteInfo.pendingRebate || 0) > 0 ? '领取返利' : '暂无返利' }}
            </el-button>
          </div>

          <div class="invite-stat-grid">
            <div class="invite-stat-item">
              <span class="stat-icon stat-icon-warning"><el-icon><Money /></el-icon></span>
              <span class="stat-copy">
                <small>待领取返利</small>
                <strong>{{ formatCurrency(inviteInfo.pendingRebate, 4, true) }}</strong>
              </span>
            </div>

            <div class="invite-stat-item">
              <span class="stat-icon stat-icon-info"><el-icon><Lock /></el-icon></span>
              <span class="stat-copy">
                <small>冻结中返利</small>
                <strong>{{ formatCurrency(inviteInfo.frozenRebate, 4, true) }}</strong>
              </span>
            </div>

            <div class="invite-stat-item">
              <span class="stat-icon stat-icon-success"><el-icon><TrendCharts /></el-icon></span>
              <span class="stat-copy">
                <small>累计获得返利</small>
                <strong>{{ formatCurrency(inviteInfo.historyRebate, 4, true) }}</strong>
              </span>
            </div>
          </div>

          <div class="share-field">
            <span class="share-field-label">邀请码</span>
            <div class="share-field-value invite-code-value">
              <strong>{{ inviteInfo.inviteCode || '-' }}</strong>
              <el-button text type="primary" @click="handleCopy(inviteInfo.inviteCode, '邀请码')">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
          </div>

          <div class="share-field">
            <span class="share-field-label">邀请链接</span>
            <div class="share-field-value">
              <span class="invite-link-text">{{ fullInviteLink || '-' }}</span>
              <el-button text type="primary" @click="handleCopy(fullInviteLink, '邀请链接')">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
          </div>

          <div class="share-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>请提醒好友通过你的专属链接完成注册，注册后再补填邀请码将无法建立推荐关系。</span>
          </div>
        </section>

        <section class="invite-section guide-section">
          <div class="section-header guide-header">
            <div>
              <span class="section-kicker">参与方式</span>
              <h3>三步获得推荐返利</h3>
            </div>
          </div>
          <div class="guide-grid">
            <div class="guide-item">
              <span class="guide-index">01</span>
              <div class="guide-icon"><el-icon><Share /></el-icon></div>
              <strong>分享专属链接</strong>
              <p>复制上方链接或邀请码，发送给准备注册的好友。</p>
            </div>
            <div class="guide-item">
              <span class="guide-index">02</span>
              <div class="guide-icon"><el-icon><UserFilled /></el-icon></div>
              <strong>好友注册充值</strong>
              <p>好友通过链接完成注册，并进行符合条件的账户充值。</p>
            </div>
            <div class="guide-item">
              <span class="guide-index">03</span>
              <div class="guide-icon"><el-icon><CircleCheckFilled /></el-icon></div>
              <strong>返利到账领取</strong>
              <p>返利结束冻结后即可领取，并直接加入你的钱包余额。</p>
            </div>
          </div>
        </section>
      </div>

    </template>

    <section v-else-if="!loading" class="invite-section unavailable-section">
      <div class="unavailable-icon"><el-icon><Present /></el-icon></div>
      <h3>推荐计划暂未开放</h3>
      <p>计划开放后，你可以在这里生成专属链接并领取好友充值返利。</p>
    </section>

    <el-dialog
      v-model="inviteeDialogVisible"
      title="邀请记录"
      width="min(900px, calc(100vw - 32px))"
      class="invitee-dialog"
      destroy-on-close
    >
      <div class="invitee-dialog-summary">
        <div>
          <h3>我的邀请好友</h3>
          <p>共邀请 {{ inviteInfo?.inviteCount ?? 0 }} 人，可查看好友带来的累计返利。</p>
        </div>
        <el-tag type="primary" effect="plain" round>{{ inviteInfo?.inviteCount ?? 0 }} 位好友</el-tag>
      </div>

      <el-table
        v-if="inviteInfo?.invitees && inviteInfo.invitees.length > 0"
        :data="inviteInfo.invitees"
        max-height="480"
        class="invitee-table"
      >
        <el-table-column label="用户" prop="username" min-width="130" :show-overflow-tooltip="true">
          <template #default="scope">
            <div class="invitee-user-cell">
              <span class="invitee-avatar">{{ inviteeInitial(scope.row.username) }}</span>
              <span>{{ scope.row.username || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column label="贡献返利" prop="totalRebate" width="150" align="right">
          <template #default="scope">
            <strong class="rebate-value">{{ formatCurrency(scope.row.totalRebate, 4, true) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="加入时间" prop="createTime" width="180" align="center">
          <template #default="scope">{{ parseTime(scope.row.createTime) || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="还没有好友通过你的链接注册" :image-size="90" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AiUserInvitePage">
import {
  CircleCheckFilled,
  CopyDocument,
  InfoFilled,
  Lock,
  Money,
  Present,
  Share,
  TrendCharts,
  UserFilled,
  WalletFilled
} from '@element-plus/icons-vue'
import { claimInviteRebate, getInviteInfo } from '@/api/aigate/invite'
import type { AjaxResult, InviteClaimResult, InviteInfoVO } from '@/types'
import { parseTime } from '@/utils/ruoyi'
import { formatCurrency } from '../common'

const emit = defineEmits<{
  balanceChanged: []
}>()

const { proxy } = getCurrentInstance() as any
const loading = ref(false)
const claimLoading = ref(false)
const inviteInfo = ref<InviteInfoVO | null>(null)
const inviteeDialogVisible = ref(false)

const fullInviteLink = computed(() => {
  const link = inviteInfo.value?.inviteLink
  if (!link) {
    return ''
  }
  if (/^https?:\/\//i.test(link)) {
    return link
  }
  return new URL(link, window.location.origin).toString()
})

function loadInviteInfo() {
  loading.value = true
  getInviteInfo()
    .then((response: AjaxResult<InviteInfoVO>) => {
      inviteInfo.value = response.data ?? null
    })
    .catch(() => {
      inviteInfo.value = null
    })
    .finally(() => {
      loading.value = false
    })
}

function handleClaimRebate() {
  proxy.$modal
    .confirm('确认领取全部待领返利到账户余额？')
    .then(() => {
      claimLoading.value = true
      return claimInviteRebate()
    })
    .then((response: AjaxResult<InviteClaimResult>) => {
      const amount = response.data?.amount ?? 0
      proxy.$modal.msgSuccess(`成功领取返利 ${formatCurrency(amount, 4, true)}`)
      loadInviteInfo()
      emit('balanceChanged')
    })
    .catch(() => {})
    .finally(() => {
      claimLoading.value = false
    })
}

async function handleCopy(text: string | undefined, label: string) {
  if (!text) {
    proxy.$modal.msgError('暂无可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    proxy.$modal.msgSuccess(`${label}已复制`)
  } catch {
    const input = document.createElement('textarea')
    input.value = text
    input.style.position = 'fixed'
    input.style.opacity = '0'
    document.body.appendChild(input)
    input.select()
    const copied = document.execCommand('copy')
    document.body.removeChild(input)
    copied ? proxy.$modal.msgSuccess(`${label}已复制`) : proxy.$modal.msgError('复制失败，请手动复制')
  }
}

function openInviteeDialog() {
  inviteeDialogVisible.value = true
}

function inviteeInitial(username?: string) {
  return username?.trim().charAt(0).toUpperCase() || 'U'
}

loadInviteInfo()
</script>

<style scoped lang="scss">
.user-invite-page {
  --invite-card-shadow: 0 12px 30px rgba(24, 34, 64, 0.11);
  --invite-card-shadow-soft: 0 8px 20px rgba(24, 34, 64, 0.08);
  display: flex;
  width: min(100%, 1480px);
  padding: 0;
  margin: 0 auto;
  flex-direction: column;
  gap: 18px;
}

.invite-hero,
.invite-section {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  box-shadow: var(--invite-card-shadow);
}

.invite-hero {
  position: relative;
  display: flex;
  min-height: 220px;
  padding: 28px 32px;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  overflow: hidden;
}

.invite-hero::after {
  position: absolute;
  top: -90px;
  right: 20%;
  width: 220px;
  height: 220px;
  background: var(--el-color-primary-light-9);
  border-radius: 50%;
  content: '';
  opacity: 0.75;
  pointer-events: none;
}

.invite-hero-copy {
  position: relative;
  z-index: 1;
  max-width: 720px;

  h2 {
    margin: 7px 0 9px;
    color: var(--el-text-color-primary);
    font-size: clamp(24px, 2.4vw, 34px);
    font-weight: 750;
    line-height: 1.3;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 14px;
    line-height: 1.75;
  }
}

.section-kicker {
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  margin-top: 22px;
  gap: 10px;

  .el-button {
    margin-left: 0;
  }
}

.rebate-rate-panel {
  position: relative;
  z-index: 1;
  display: flex;
  width: 240px;
  min-height: 170px;
  padding: 20px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  box-shadow: var(--invite-card-shadow-soft);

  > span {
    margin-top: 10px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  > strong {
    margin-top: 3px;
    color: var(--el-color-primary);
    font-size: 42px;
    line-height: 1.15;
    font-variant-numeric: tabular-nums;

    small {
      margin-left: 2px;
      font-size: 17px;
    }
  }

  > p {
    margin: 7px 0 0;
    color: var(--el-text-color-placeholder);
    font-size: 11px;
    line-height: 1.5;
  }
}

.rebate-rate-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: var(--el-color-primary);
  font-size: 19px;
  background: var(--el-color-primary-light-9);
  border-radius: 11px;
}

.invite-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 14px 0;
  margin-bottom: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.invite-stat-item {
  display: flex;
  min-width: 0;
  min-height: 50px;
  padding: 0 18px;
  align-items: center;
  gap: 10px;
  color: inherit;
  text-align: left;
}

.invite-stat-item:first-child {
  padding-left: 0;
}

.invite-stat-item:last-child {
  padding-right: 0;
}

.invite-stat-item + .invite-stat-item {
  border-left: 1px solid var(--el-border-color-lighter);
}

.stat-icon {
  display: grid;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  place-items: center;
  font-size: 18px;
}

.stat-icon-warning {
  color: var(--el-color-warning);
}

.stat-icon-info {
  color: var(--el-color-info);
}

.stat-icon-success {
  color: var(--el-color-success);
}

.stat-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;

  small {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  strong {
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-size: 18px;
    font-weight: 700;
    line-height: 1.2;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-variant-numeric: tabular-nums;
  }
}

.invite-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.8fr);
  gap: 18px;
}

.invite-section {
  padding: 22px;
}

.section-header {
  display: flex;
  margin-bottom: 18px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h3 {
    margin: 4px 0 5px;
    color: var(--el-text-color-primary);
    font-size: 19px;
    font-weight: 700;
    line-height: 1.35;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.6;
  }
}

.claim-entry {
  flex: 0 0 auto;
}

.share-field + .share-field {
  margin-top: 12px;
}

.share-field-label {
  display: block;
  margin-bottom: 7px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.share-field-value {
  display: flex;
  min-width: 0;
  min-height: 45px;
  padding: 6px 8px 6px 13px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;

  strong,
  .invite-link-text {
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-family: 'SF Mono', 'Cascadia Code', Consolas, monospace;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .el-button {
    flex: 0 0 auto;
  }
}

.invite-code-value strong {
  color: var(--el-color-primary);
  font-size: 17px;
  letter-spacing: 0.08em;
}

.share-tip {
  display: flex;
  padding: 10px 12px;
  margin-top: 14px;
  align-items: flex-start;
  gap: 7px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 1.6;
  background: var(--el-fill-color-lighter);
  border-radius: 9px;

  .el-icon {
    margin-top: 2px;
    flex: 0 0 auto;
    color: var(--el-color-primary);
  }
}

.guide-header {
  margin-bottom: 15px;
}

.guide-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 14px;
}

.guide-item {
  position: relative;
  min-width: 0;
  padding: 18px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  box-shadow: var(--invite-card-shadow-soft);
  overflow: hidden;

  > strong {
    display: block;
    margin-top: 13px;
    color: var(--el-text-color-primary);
    font-size: 13px;
  }

  > p {
    margin: 6px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    line-height: 1.65;
  }
}

.guide-index {
  position: absolute;
  top: 11px;
  right: 14px;
  color: var(--el-border-color);
  font-size: 25px;
  font-weight: 800;
}

.guide-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: var(--el-color-primary);
  font-size: 18px;
  background: var(--el-color-primary-light-9);
  border-radius: 10px;
}

.invitee-dialog-summary {
  display: flex;
  margin-bottom: 18px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h3 {
    margin: 0 0 5px;
    color: var(--el-text-color-primary);
    font-size: 18px;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.6;
  }
}

.invitee-table {
  --el-table-border: none;
  width: 100%;
}

.invitee-user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.invitee-avatar {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  color: var(--el-color-primary);
  font-size: 12px;
  background: var(--el-color-primary-light-9);
  border-radius: 50%;
}

.rebate-value {
  color: var(--el-color-success);
  font-variant-numeric: tabular-nums;
}

.unavailable-section {
  display: flex;
  min-height: 260px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;

  h3 {
    margin: 14px 0 6px;
    color: var(--el-text-color-primary);
    font-size: 18px;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.unavailable-icon {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  color: var(--el-color-primary);
  font-size: 25px;
  background: var(--el-color-primary-light-9);
  border-radius: 15px;
}

@media (max-width: 1100px) {
  .invite-main-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .invite-hero {
    padding: 22px 18px;
    align-items: stretch;
    flex-direction: column;
  }

  .rebate-rate-panel {
    width: auto;
  }

  .hero-actions,
  .section-header,
  .invitee-dialog-summary {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-actions .el-button {
    width: 100%;
  }

  .invite-stat-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 0;
  }

  .invite-stat-item,
  .invite-stat-item:first-child,
  .invite-stat-item:last-child {
    min-height: 0;
    padding: 13px 0;
  }

  .invite-stat-item + .invite-stat-item {
    border-top: 1px solid var(--el-border-color-lighter);
    border-left: 0;
  }

  .invite-section {
    padding: 16px;
  }

  .share-field-value {
    align-items: stretch;
    flex-direction: column;

    .el-button {
      align-self: flex-end;
    }
  }

  .share-field-value strong,
  .share-field-value .invite-link-text {
    white-space: normal;
    word-break: break-all;
  }
}
</style>
