<template>
  <div
    v-show="checkinEnabled"
    class="checkin-card"
    :class="{ 'is-done': checkinTodaySigned }"
    v-loading="checkinLoading || checkinSubmitting"
  >
    <span v-if="canCheckin" class="checkin-pending-indicator" role="status" aria-label="今日未签到"></span>
    <div class="checkin-summary">
      <div class="checkin-summary-text">
        <div class="checkin-summary-title">
          <el-icon><Calendar /></el-icon>
          <el-tag
            :type="checkinTodaySigned ? 'success' : 'warning'"
            effect="light"
            class="checkin-status-tag"
            :class="{ 'is-clickable': canCheckin }"
            :aria-disabled="!canCheckin"
            :tabindex="canCheckin ? 0 : -1"
            role="button"
            @click="handleCheckinClick"
            @keydown.enter.prevent="handleCheckinClick"
            @keydown.space.prevent="handleCheckinClick"
          >
            {{ checkinTodaySigned ? '已签到' : '签到' }}
          </el-tag>
        </div>
        <div class="checkin-summary-value">{{ checkinSignedDates.length }}天</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="CheckinCard">
import { Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { dailyCheckin, getCheckinCalendar } from '@/api/aigate/checkin'
import { formatCurrency } from '@/views/aigate/common'

interface CheckinSignedPayload {
  rewardAmount: number
  balance?: number
}

const emit = defineEmits<{
  (event: 'signed', payload: CheckinSignedPayload): void
  (event: 'enabled-change', enabled: boolean): void
}>()

function padNumber(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

function formatLocalDate(date: Date): string {
  return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())}`
}

function formatLocalMonth(date: Date): string {
  return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}`
}

const checkinEnabled = ref(false)
const checkinToday = ref(formatLocalDate(new Date()))
const checkinSignedDates = ref<string[]>([])
const checkinTodaySigned = ref(false)
const checkinLoading = ref(false)
const checkinSubmitting = ref(false)
const canCheckin = computed(() => checkinEnabled.value && !checkinLoading.value && !checkinSubmitting.value && !checkinTodaySigned.value)

watch(checkinEnabled, enabled => emit('enabled-change', enabled))

async function fetchCheckinCalendar() {
  if (checkinLoading.value) {
    return
  }

  checkinLoading.value = true
  try {
    const response = await getCheckinCalendar(formatLocalMonth(new Date()))
    const data = response.data
    checkinEnabled.value = data?.enabled === true
    checkinToday.value = data?.today || formatLocalDate(new Date())
    checkinSignedDates.value = data?.signedDates || []
    checkinTodaySigned.value = Boolean(data?.todaySigned)
  } catch {
    checkinEnabled.value = false
  } finally {
    checkinLoading.value = false
  }
}

async function handleCheckinClick() {
  if (!canCheckin.value) {
    return
  }

  const checkinDate = checkinToday.value
  checkinSubmitting.value = true
  try {
    const response = await dailyCheckin(checkinDate)
    const data = response.data
    const reward = Number(data?.rewardAmount || 0)
    checkinTodaySigned.value = Boolean(data?.todaySigned ?? true)
    checkinSignedDates.value = data?.signedDates || Array.from(new Set([...checkinSignedDates.value, checkinDate]))
    ElMessage.success(`签到成功，获得 ${formatCurrency(reward, 4, true)}`)
    emit('signed', {
      rewardAmount: reward,
      balance: data?.balance == null ? undefined : Number(data.balance)
    })
  } catch {
    // 页面打开后开关可能已关闭，失败时重新同步状态并隐藏卡片。
    await fetchCheckinCalendar()
  } finally {
    checkinSubmitting.value = false
  }
}

onMounted(fetchCheckinCalendar)
onActivated(fetchCheckinCalendar)
</script>

<style scoped lang="scss">
.checkin-card {
  position: relative;
  height: 100%;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.checkin-pending-indicator {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--el-color-primary);
  box-shadow: 0 0 0 3px var(--el-color-primary-light-8);
}

.checkin-summary {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.checkin-summary-text {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.checkin-summary-title {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.checkin-summary-title .el-icon {
  color: var(--el-color-warning);
  flex-shrink: 0;
}

.checkin-card.is-done .checkin-summary-title .el-icon {
  color: var(--el-color-success);
}

.checkin-summary-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.checkin-status-tag {
  cursor: default;
  user-select: none;
}

.checkin-status-tag.is-clickable {
  cursor: pointer;
}

.checkin-status-tag.is-clickable:hover {
  filter: brightness(0.96);
}

.checkin-status-tag.is-clickable:focus-visible {
  outline: 2px solid var(--el-color-warning-light-5);
  outline-offset: 2px;
}
</style>
