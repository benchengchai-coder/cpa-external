<template>
  <div class="app-container invite-management-page">
    <div class="invite-content-card">
      <div class="invite-content-header">
        <el-segmented v-model="activeView" :options="availableOptions" />
      </div>

      <div class="invite-content-body">
        <KeepAlive>
          <component :is="activeComponent" />
        </KeepAlive>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="AiInvite">
import type { Component } from 'vue'
import { checkPermi } from '@/utils/permission'
import InviteAffiliatePanel from './components/InviteAffiliatePanel.vue'
import InviteConfigPanel from './components/InviteConfigPanel.vue'
import InviteRebatePanel from './components/InviteRebatePanel.vue'

type InviteView = 'affiliate' | 'rebate' | 'config'

const activeView = ref<InviteView>('affiliate')
const baseOptions: Array<{ label: string; value: InviteView; permission?: string }> = [
  { label: '邀请关系', value: 'affiliate' },
  { label: '返利流水', value: 'rebate' },
  { label: '邀请参数配置', value: 'config', permission: 'aigate:invite:config' }
]

const availableOptions = computed(() =>
  baseOptions.filter((item) => !item.permission || checkPermi([item.permission]))
)

const componentMap: Record<InviteView, Component> = {
  affiliate: InviteAffiliatePanel,
  rebate: InviteRebatePanel,
  config: InviteConfigPanel
}

const activeComponent = computed<Component>(() => componentMap[activeView.value])
</script>

<style scoped lang="scss">
.invite-management-page {
  min-width: 0;
}

.invite-content-card {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
  border-radius: 8px;
}

.invite-content-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.invite-content-header :deep(.el-segmented) {
  max-width: 100%;
  overflow-x: auto;
}

.invite-content-body {
  min-width: 0;
}

.invite-content-body :deep(.el-table) {
  --el-table-border: none;
}

@media (max-width: 767px) {
  .invite-content-header :deep(.el-segmented) {
    width: 100%;
  }
}
</style>
