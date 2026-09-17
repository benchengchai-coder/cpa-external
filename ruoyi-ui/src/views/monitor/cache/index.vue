<template>
  <div class="app-container cache-page">
    <el-row :gutter="10">
      <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24" class="card-box">
        <el-card>
          <template #header><Monitor style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">基本信息</span></template>
          <div class="cache-info-grid">
            <div v-for="item in infoItems" :key="item.label" class="cache-info-item">
              <span class="cache-info-label">{{ item.label }}</span>
              <span class="cache-info-value">{{ item.value }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" class="card-box">
        <el-card>
          <template #header><PieChart style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">命令统计</span></template>
          <div class="cache-chart-wrap">
            <div ref="commandstats" class="cache-chart" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" class="card-box">
        <el-card>
          <template #header><Odometer style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">内存信息</span></template>
          <div class="cache-chart-wrap">
            <div ref="usedmemory" class="cache-chart" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="Cache">
import { getCache } from '@/api/monitor/cache'
import * as echarts from 'echarts'

const cache = ref<any>({})
const commandstats = ref<HTMLElement | null>(null)
const usedmemory = ref<HTMLElement | null>(null)
const { proxy } = getCurrentInstance()!

let commandstatsInstance: echarts.ECharts | null = null
let usedmemoryInstance: echarts.ECharts | null = null
let chartResizeObserver: ResizeObserver | null = null

const infoItems = computed(() => {
  const info = cache.value?.info
  return [
    { label: "Redis版本", value: displayValue(info?.redis_version) },
    { label: "运行模式", value: info ? (info.redis_mode == "standalone" ? "单机" : "集群") : "-" },
    { label: "端口", value: displayValue(info?.tcp_port) },
    { label: "客户端数", value: displayValue(info?.connected_clients) },
    { label: "运行时间(天)", value: displayValue(info?.uptime_in_days) },
    { label: "使用内存", value: displayValue(info?.used_memory_human) },
    { label: "使用CPU", value: formatCpu(info?.used_cpu_user_children) },
    { label: "内存配置", value: displayValue(info?.maxmemory_human) },
    { label: "AOF是否开启", value: info ? (info.aof_enabled == "0" ? "否" : "是") : "-" },
    { label: "RDB是否成功", value: displayValue(info?.rdb_last_bgsave_status) },
    { label: "Key数量", value: displayValue(cache.value?.dbSize) },
    {
      label: "网络入口/出口",
      value: info ? `${displayValue(info.instantaneous_input_kbps)}kps/${displayValue(info.instantaneous_output_kbps)}kps` : "-"
    }
  ]
})

function displayValue(value: unknown): string {
  if (value === undefined || value === null || value === '') return "-"
  return String(value)
}

function formatCpu(value: unknown): string {
  const cpu = Number.parseFloat(String(value ?? ""))
  return Number.isFinite(cpu) ? cpu.toFixed(2) : "-"
}

function getList(): void {
  proxy.$modal.loading("正在加载缓存监控数据，请稍候！")
  getCache().then(response => {
    proxy.$modal.closeLoading()
    cache.value = response.data || {}

    nextTick(() => {
      initCharts(response.data || {})
    })
  })
}

function initCharts(data: any): void {
  if (!commandstats.value || !usedmemory.value) return

  disposeCharts()

  commandstatsInstance = echarts.init(commandstats.value, "macarons")
  commandstatsInstance.setOption({
    tooltip: {
      trigger: "item",
      formatter: "{a} <br/>{b} : {c} ({d}%)"
    },
    series: [
      {
        name: "命令",
        type: "pie",
        roseType: "radius",
        radius: [15, 95],
        center: ["50%", "38%"],
        data: data.commandStats || [],
        animationEasing: "cubicInOut",
        animationDuration: 1000
      }
    ]
  })

  usedmemoryInstance = echarts.init(usedmemory.value, "macarons")
  const usedMemoryHuman = displayValue(cache.value?.info?.used_memory_human)
  const usedMemoryValue = Number.parseFloat(usedMemoryHuman)
  usedmemoryInstance.setOption({
    tooltip: {
      formatter: "{b} <br/>{a} : " + usedMemoryHuman
    },
    series: [
      {
        name: "峰值",
        type: "gauge",
        min: 0,
        max: 1000,
        detail: {
          formatter: usedMemoryHuman
        },
        data: [
          {
            value: Number.isFinite(usedMemoryValue) ? usedMemoryValue : 0,
            name: "内存消耗"
          }
        ]
      }
    ]
  })

  observeChartSize()
  resizeCharts()
}

function resizeCharts(): void {
  commandstatsInstance?.resize()
  usedmemoryInstance?.resize()
}

function observeChartSize(): void {
  chartResizeObserver?.disconnect()
  if (typeof ResizeObserver === "undefined") return

  chartResizeObserver = new ResizeObserver(() => {
    resizeCharts()
  })
  if (commandstats.value) chartResizeObserver.observe(commandstats.value)
  if (usedmemory.value) chartResizeObserver.observe(usedmemory.value)
}

function disposeCharts(): void {
  chartResizeObserver?.disconnect()
  chartResizeObserver = null
  commandstatsInstance?.dispose()
  usedmemoryInstance?.dispose()
  commandstatsInstance = null
  usedmemoryInstance = null
}

onMounted(() => {
  window.addEventListener("resize", resizeCharts)
  getList()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts)
  disposeCharts()
})
</script>

<style scoped lang="scss">
.cache-page {
  .card-box {
    margin-bottom: 10px;
  }
}

.cache-info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid var(--el-border-color-lighter);
  border-left: 1px solid var(--el-border-color-lighter);
}

.cache-info-item {
  display: flex;
  min-width: 0;
  min-height: 42px;
  border-right: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.cache-info-label,
.cache-info-value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 12px;
  line-height: 1.5;
}

.cache-info-label {
  flex: 0 0 140px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-lighter);
}

.cache-info-value {
  flex: 1;
  color: var(--el-text-color-primary);
  word-break: break-word;
}

.cache-chart-wrap {
  width: 100%;
  min-width: 0;
}

.cache-chart {
  width: 100%;
  height: 420px;
  min-width: 0;
}

@media (max-width: 1200px) {
  .cache-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .cache-page {
    padding: 10px;
  }

  .cache-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .cache-info-item {
    flex-direction: column;
    min-height: 72px;
  }

  .cache-info-label {
    flex: none;
    padding-bottom: 4px;
  }

  .cache-info-value {
    padding-top: 6px;
  }

  .cache-chart {
    height: 340px;
  }

  :deep(.el-card__body) {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .cache-info-label,
  .cache-info-value {
    padding: 9px 10px;
  }

  .cache-chart {
    height: 320px;
  }
}
</style>
