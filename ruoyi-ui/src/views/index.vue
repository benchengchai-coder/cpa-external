<template>
  <AdminDashboard v-if="isAdmin" />
  <UserHome v-else />
</template>

<script setup lang="ts" name="Index">
import AdminDashboard from '@/views/dashboard/AdminDashboard.vue'
import UserHome from '@/views/dashboard/UserHome.vue'
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const isAdmin = computed(() => {
  return userStore.roles.includes('admin')
})
</script>
