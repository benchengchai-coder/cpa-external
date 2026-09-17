<template>
  <div class="home-layout">
    <home-navbar />
    <div ref="contentRef" class="home-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import HomeNavbar from './components/HomeNavbar.vue'

const route = useRoute()
const contentRef = ref<HTMLElement>()

watch(() => route.path, () => {
  contentRef.value?.scrollTo({ top: 0 })
})
</script>

<style lang="scss" scoped>
.home-layout {
  --hl-bg: #f7f8fa;
}

html.dark .home-layout {
  --hl-bg: #0a0a0f;
}

.home-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--hl-bg);
}

.home-content {
  flex: 1;
  overflow-y: auto;
}
</style>
