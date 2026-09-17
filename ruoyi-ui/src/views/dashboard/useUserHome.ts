import { onMounted, ref } from 'vue'
import { getUserDashboard } from '@/api/aigate/dashboard'

export function useUserHome()
{
  const loading = ref(false)
  const loaded = ref(false)
  const availableBalance = ref(0)
  const usedBalance = ref(0)
  const totalChargedAmount = ref(0)
  const totalUncoveredAmount = ref(0)
  const requestCount = ref(0)
  const totalTokens = ref(0)

  async function fetchData()
  {
    loading.value = true
    try
    {
      const dashboard = await getUserDashboard()

      if (dashboard.data)
      {
        availableBalance.value = dashboard.data.availableBalance
        usedBalance.value = dashboard.data.usedBalance
        totalChargedAmount.value = dashboard.data.totalChargedAmount
        totalUncoveredAmount.value = dashboard.data.totalUncoveredAmount
        requestCount.value = dashboard.data.requestCount
        totalTokens.value = dashboard.data.totalTokens
        loaded.value = true
      }
    }
    finally
    {
      loading.value = false
    }
  }

  onMounted(() =>
  {
    fetchData()
  })

  return {
    loading,
    loaded,
    availableBalance,
    usedBalance,
    totalChargedAmount,
    totalUncoveredAmount,
    requestCount,
    totalTokens,
    fetchData
  }
}
