import { nextTick, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'
import {
  getAdminOverview,
  getAdminTrend,
  getAdminUserRank
} from '@/api/aigate/dashboard'
import type {
  DashboardOverview,
  DashboardTrend,
  DashboardUserRank,
  DashboardTrendRange,
  DashboardRankRange
} from '@/types'
import { formatCurrency, formatTokenCount } from '../aigate/common'

const trendRangeOptions: Array<{ label: string; value: DashboardTrendRange }> = [
  { label: '今日', value: 'today' },
  { label: '近7天', value: '7' },
  { label: '近30天', value: '30' }
]

function getCssVar(name: string, fallback: string): string
{
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

function getChartColors()
{
  return {
    primary: getCssVar('--el-color-primary', '#409EFF'),
    textPrimary: getCssVar('--el-text-color-primary', '#303133'),
    textRegular: getCssVar('--el-text-color-regular', '#606266'),
    textSecondary: getCssVar('--el-text-color-secondary', '#909399'),
    border: getCssVar('--el-border-color-light', '#ebeef5'),
    surface: getCssVar('--el-bg-color-overlay', '#ffffff')
  }
}

function padNumber(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

function formatLocalDate(date: Date): string {
  return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())}`
}

function fillTrendData(data: DashboardTrend[], range: DashboardTrendRange): DashboardTrend[] {
  const result: DashboardTrend[] = []
  const dataMap = new Map(data.map(item => [item.date, item]))
  const now = new Date()

  if (range === 'today')
  {
    for (let i = 0; i <= now.getHours(); i++)
    {
      const hour = `${padNumber(i)}:00`
      const existing = dataMap.get(hour)
      result.push(existing || { date: hour, requestCount: 0, totalTokens: 0, cost: 0 })
    }
    return result
  }

  const days = Number(range)
  for (let i = days - 1; i >= 0; i--)
  {
    const date = new Date(now)
    date.setDate(date.getDate() - i)
    const dateStr = formatLocalDate(date)
    const existing = dataMap.get(dateStr)
    result.push(existing || { date: dateStr, requestCount: 0, totalTokens: 0, cost: 0 })
  }

  return result
}

function trendAxisLabel(value: string, range: DashboardTrendRange): string {
  return range === 'today' ? value : value.slice(5)
}

function trendTooltipTitle(value: string, range: DashboardTrendRange): string {
  return range === 'today' ? `今日 ${value}` : value
}

export function useAdminDashboard()
{
  const overviewLoading = ref(false)
  const trendLoading = ref(false)
  const tokenTrendLoading = ref(false)
  const trendRange = ref<DashboardTrendRange>('7')
  const tokenRange = ref<DashboardTrendRange>('7')
  const rankRange = ref<DashboardRankRange>('all')
  const overviewData = ref<DashboardOverview>({
    channelCount: 0,
    modelCount: 0,
    userCount: 0,
    todayNewUsers: 0,
    apiKeyCount: 0,
    todayRequests: 0,
    requestsPerMinute: 0,
    todayTokens: 0,
    totalTokens: 0,
    todayCost: 0,
    todayChargedAmount: 0,
    failedSettlementCount: 0,
    averageResponseTime: 0,
    averageFirstTokenTime: 0
  })
  const userRankData = ref<DashboardUserRank[]>([])
  const trendLoaded = ref(false)
  const tokenTrendLoaded = ref(false)
  const userRankLoading = ref(false)
  const userRankTotal = ref(0)
  const userRankPageNum = ref(1)
  const userRankPageSize = ref(10)

  // ECharts DOM refs
  const trendChartRef = ref<HTMLElement | null>(null)
  const tokenChartRef = ref<HTMLElement | null>(null)

  // ECharts 实例
  let trendChart: echarts.ECharts | null = null
  let tokenChart: echarts.ECharts | null = null
  let latestTrendData: DashboardTrend[] = []
  let latestTrendRange: DashboardTrendRange = '7'
  let latestTokenTrendData: DashboardTrend[] = []
  let latestTokenTrendRange: DashboardTrendRange = '7'
  let themeObserver: MutationObserver | null = null
  let themeRenderFrame: number | null = null
  let chartResizeObserver: ResizeObserver | null = null
  let chartResizeFrame: number | null = null
  let trendRequestId = 0
  let tokenTrendRequestId = 0
  let userRankRequestId = 0

  async function fetchOverview()
  {
    overviewLoading.value = true
    try
    {
      const overview = await getAdminOverview()
      if (overview.data) {
        overviewData.value = overview.data
      }
    }
    finally
    {
      overviewLoading.value = false
    }
  }

  async function fetchTrend()
  {
    const requestId = ++trendRequestId
    const requestedRange = trendRange.value
    trendLoading.value = true
    try
    {
      const trend = await getAdminTrend(requestedRange)
      if (requestId !== trendRequestId || requestedRange !== trendRange.value) return

      const filledTrend = fillTrendData(trend.data || [], requestedRange)
      trendLoaded.value = true
      await nextTick()
      if (requestId !== trendRequestId || requestedRange !== trendRange.value) return
      renderTrendChart(filledTrend, requestedRange)
    }
    finally
    {
      if (requestId === trendRequestId) {
        trendLoading.value = false
      }
    }
  }

  async function fetchUserRank(pageNum?: number)
  {
    if (pageNum !== undefined) {
      userRankPageNum.value = pageNum
    }

    const requestId = ++userRankRequestId
    const requestedRange = rankRange.value
    const requestedPageNum = userRankPageNum.value
    const requestedPageSize = userRankPageSize.value
    userRankLoading.value = true
    try
    {
      const response = await getAdminUserRank({
        range: requestedRange,
        pageNum: requestedPageNum,
        pageSize: requestedPageSize
      })
      if (requestId !== userRankRequestId
        || requestedRange !== rankRange.value
        || requestedPageNum !== userRankPageNum.value
        || requestedPageSize !== userRankPageSize.value) return

      userRankData.value = response.rows || []
      userRankTotal.value = Number(response.total || 0)
    }
    catch
    {
      if (requestId === userRankRequestId
        && requestedRange === rankRange.value
        && requestedPageNum === userRankPageNum.value
        && requestedPageSize === userRankPageSize.value) {
        userRankData.value = []
        userRankTotal.value = 0
      }
    }
    finally
    {
      if (requestId === userRankRequestId) {
        userRankLoading.value = false
      }
    }
  }

  async function fetchTokenTrend()
  {
    const requestId = ++tokenTrendRequestId
    const requestedRange = tokenRange.value
    tokenTrendLoading.value = true
    try
    {
      const tokenTrend = await getAdminTrend(requestedRange)
      if (requestId !== tokenTrendRequestId || requestedRange !== tokenRange.value) return

      const filledTokenTrend = fillTrendData(tokenTrend.data || [], requestedRange)
      tokenTrendLoaded.value = true
      await nextTick()
      if (requestId !== tokenTrendRequestId || requestedRange !== tokenRange.value) return
      renderTokenChart(filledTokenTrend, requestedRange)
    }
    finally
    {
      if (requestId === tokenTrendRequestId) {
        tokenTrendLoading.value = false
      }
    }
  }
  function renderTrendChart(data: DashboardTrend[], range: DashboardTrendRange)
  {
    latestTrendData = data
    latestTrendRange = range
    if (!trendChartRef.value) return
    const colors = getChartColors()
    if (!trendChart) {
      trendChart = echarts.init(trendChartRef.value)
    }
    trendChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'line',
          lineStyle: {
            color: colors.primary,
            width: 1,
            type: 'dashed'
          }
        },
        backgroundColor: colors.surface,
        borderColor: colors.border,
        textStyle: {
          color: colors.textPrimary
        },
        formatter(params: any) {
          const list = Array.isArray(params) ? params : [params]
          const first = list[0] || {}
          const item = data[first.dataIndex] || { date: '-', requestCount: 0, totalTokens: 0, cost: 0 }
          const marker = first.marker || ''
          return [
            `<div style="font-weight:600;margin-bottom:4px">${trendTooltipTitle(item.date, range)}</div>`,
            `<div>${marker}请求数：${Number(item.requestCount || 0).toLocaleString()}</div>`,
            `<div>计算费用：${formatCurrency(item.cost, 4, true)}</div>`
          ].join('')
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: data.map(item => trendAxisLabel(item.date, range)),
        boundaryGap: false,
        axisLabel: {
          color: colors.textSecondary
        },
        axisLine: {
          lineStyle: {
            color: colors.border
          }
        },
        axisTick: {
          lineStyle: {
            color: colors.border
          }
        }
      },
      yAxis: {
        type: 'value',
        name: '请求数',
        minInterval: 1,
        nameTextStyle: {
          color: colors.textSecondary
        },
        axisLabel: {
          color: colors.textSecondary
        },
        splitLine: {
          lineStyle: {
            color: colors.border
          }
        }
      },
      series: [
        {
          name: '请求数',
          type: 'line',
          smooth: true,
          data: data.map(item => item.requestCount),
          areaStyle: {
            opacity: 0.15,
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: colors.primary },
              { offset: 1, color: colors.surface }
            ])
          },
          itemStyle: { color: colors.primary },
          lineStyle: { width: 2 }
        }
      ]
    }, true)
  }

  function renderTokenChart(data: DashboardTrend[], range: DashboardTrendRange)
  {
    latestTokenTrendData = data
    latestTokenTrendRange = range
    if (!tokenChartRef.value) return
    const colors = getChartColors()
    if (!tokenChart) {
      tokenChart = echarts.init(tokenChartRef.value)
    }

    tokenChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'line',
          lineStyle: {
            color: colors.primary,
            width: 1,
            type: 'dashed'
          }
        },
        formatter(params: any) {
          const list = Array.isArray(params) ? params : [params]
          const first = list[0] || {}
          const item = data[first.dataIndex] || { date: '-', requestCount: 0, totalTokens: 0, cost: 0 }
          const marker = first.marker || ''
          return [
            `<div style="font-weight:600;margin-bottom:4px">${trendTooltipTitle(item.date, range)}</div>`,
            `<div>${marker}Token 数：${formatTokenCount(item.totalTokens)}</div>`,
            `<div>请求数：${Number(item.requestCount || 0).toLocaleString()}</div>`
          ].join('')
        },
        backgroundColor: colors.surface,
        borderColor: colors.border,
        textStyle: {
          color: colors.textPrimary
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: data.map(item => trendAxisLabel(item.date, range)),
        boundaryGap: false,
        axisLabel: {
          color: colors.textSecondary
        },
        axisLine: {
          lineStyle: {
            color: colors.border
          }
        },
        axisTick: {
          lineStyle: {
            color: colors.border
          }
        }
      },
      yAxis: {
        type: 'value',
        name: 'Token 数',
        minInterval: 1,
        nameTextStyle: {
          color: colors.textSecondary
        },
        axisLabel: {
          color: colors.textSecondary,
          formatter(value: number) {
            return formatTokenCount(value)
          }
        },
        splitLine: {
          lineStyle: {
            color: colors.border
          }
        }
      },
      series: [
        {
          name: 'Token 数',
          type: 'line',
          smooth: true,
          data: data.map(item => item.totalTokens),
          areaStyle: {
            opacity: 0.15,
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: colors.primary },
              { offset: 1, color: colors.surface }
            ])
          },
          itemStyle: { color: colors.primary },
          lineStyle: { width: 2 },
          emphasis: {
            focus: 'series'
          },
          animationEasing: 'cubicInOut',
          animationDuration: 1000
        }
      ]
    }, true)
  }

  function refreshChartsForTheme()
  {
    if (themeRenderFrame !== null) {
      cancelAnimationFrame(themeRenderFrame)
    }

    themeRenderFrame = requestAnimationFrame(() => {
      themeRenderFrame = null
      renderTrendChart(latestTrendData, latestTrendRange)
      renderTokenChart(latestTokenTrendData, latestTokenTrendRange)
    })
  }

  function setupThemeObserver()
  {
    if (themeObserver) return

    themeObserver = new MutationObserver(refreshChartsForTheme)
    themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class', 'style']
    })
  }

  function resizeCharts()
  {
    trendChart?.resize()
    tokenChart?.resize()
  }

  function scheduleChartResize()
  {
    if (chartResizeFrame !== null) {
      cancelAnimationFrame(chartResizeFrame)
    }

    chartResizeFrame = requestAnimationFrame(() => {
      chartResizeFrame = null
      resizeCharts()
    })
  }

  function setupChartResizeObserver()
  {
    if (chartResizeObserver || typeof ResizeObserver === 'undefined') return

    chartResizeObserver = new ResizeObserver(scheduleChartResize)
    if (trendChartRef.value) {
      chartResizeObserver.observe(trendChartRef.value)
    }
    if (tokenChartRef.value) {
      chartResizeObserver.observe(tokenChartRef.value)
    }
  }

  function handleResize()
  {
    scheduleChartResize()
  }

  async function init()
  {
    setupThemeObserver()
    setupChartResizeObserver()
    window.addEventListener('resize', handleResize)
    await Promise.allSettled([fetchOverview(), fetchUserRank()])
  }

  onBeforeUnmount(() =>
  {
    window.removeEventListener('resize', handleResize)
    themeObserver?.disconnect()
    if (themeRenderFrame !== null) {
      cancelAnimationFrame(themeRenderFrame)
    }
    if (chartResizeFrame !== null) {
      cancelAnimationFrame(chartResizeFrame)
    }
    chartResizeObserver?.disconnect()
    trendChart?.dispose()
    tokenChart?.dispose()
  })

  return {
    overviewLoading,
    trendLoading,
    tokenTrendLoading,
    trendRange,
    tokenRange,
    rankRange,
    trendRangeOptions,
    overviewData,
    userRankData,
    trendLoaded,
    tokenTrendLoaded,
    userRankLoading,
    userRankTotal,
    userRankPageNum,
    userRankPageSize,
    trendChartRef,
    tokenChartRef,
    fetchOverview,
    fetchTrend,
    fetchTokenTrend,
    fetchUserRank,
    resizeCharts,
    init
  }
}
