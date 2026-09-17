<template>
  <div class="home-navbar">
    <div class="navbar-inner">
      <!-- 左侧：Logo -->
      <div class="navbar-left">
        <router-link to="/home" class="navbar-logo">
          <img :src="logo" class="logo-img" />
          <span class="logo-title">{{ title }}</span>
        </router-link>

        <!-- 桌面端：水平导航菜单 -->
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          :ellipsis="false"
          class="navbar-menu desktop-menu"
          router
        >
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/home/models">模型广场</el-menu-item>
          <el-menu-item index="/home/pricing">套餐定价</el-menu-item>
          <el-menu-item index="/home/docs">使用文档</el-menu-item>
          <el-menu-item index="/home/about">关于</el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧：用户操作 + 汉堡按钮 -->
      <div class="navbar-right">
        <router-link
          v-if="isLoggedIn"
          :to="{ name: 'Index' }"
          class="console-entry"
        >
          控制台
        </router-link>

        <!-- 用户区域（桌面端和移动端均显示） -->
        <div class="user-area">
          <template v-if="!isLoggedIn">
            <el-button type="primary" size="small" @click="handleLogin">登录</el-button>
          </template>
          <template v-else>
            <el-dropdown @command="handleCommand" class="avatar-container" trigger="hover">
              <div class="avatar-wrapper">
                <img :src="userStore.avatar" class="user-avatar" />
                <span class="user-nickname">{{ userStore.nickName }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <router-link to="/user/profile">
                    <el-dropdown-item>个人中心</el-dropdown-item>
                  </router-link>
                  <el-dropdown-item divided command="logout">
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>

        <!-- 移动端汉堡按钮 -->
        <div class="hamburger-btn" @click="toggleMobileMenu">
          <div class="hamburger-icon" :class="{ 'is-active': mobileMenuOpen }">
            <span class="line"></span>
            <span class="line"></span>
            <span class="line"></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端导航覆盖层 -->
    <transition name="overlay-fade">
      <div v-if="mobileMenuOpen" class="mobile-overlay" @click.self="closeMobileMenu">
        <nav class="mobile-nav">
          <router-link
            v-for="(item, index) in navLinks"
            :key="item.path"
            :to="item.path"
            class="mobile-nav-item"
            :class="{ active: activeMenu === item.path }"
            :style="{ transitionDelay: mobileMenuOpen ? `${index * 50}ms` : '0ms' }"
            @click="closeMobileMenu"
          >
            {{ item.label }}
          </router-link>
          <div class="mobile-nav-divider"></div>
          <!-- 未登录时在覆盖层中也显示登录/注册 -->
          <template v-if="!isLoggedIn">
            <div class="mobile-nav-actions">
              <el-button type="primary" class="mobile-login-btn" @click="handleLoginAndClose">登录</el-button>
            </div>
          </template>
        </nav>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import logo from '@/assets/logo/logo.png'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const title = import.meta.env.VITE_APP_TITLE

const isLoggedIn = computed(() => !!userStore.token)
const activeMenu = computed(() => route.path)
const mobileMenuOpen = ref(false)

const navLinks = [
  { path: '/home', label: '首页' },
  { path: '/home/models', label: '模型广场' },
  { path: '/home/pricing', label: '套餐定价' },
  { path: '/home/docs', label: '使用文档' },
  { path: '/home/about', label: '关于' },
]

// 路由变化时关闭移动菜单
watch(() => route.path, () => {
  closeMobileMenu()
})

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
  document.body.style.overflow = mobileMenuOpen.value ? 'hidden' : ''
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
  document.body.style.overflow = ''
}

onBeforeUnmount(() => {
  document.body.style.overflow = ''
})

function handleLogin(): void {
  router.push('/login')
}

function handleLoginAndClose(): void {
  closeMobileMenu()
  router.push('/login')
}

function handleCommand(command: string): void {
  if (command === 'logout') {
    ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logOut().then(() => {
         location.href = '/home'
      })
    }).catch(() => {})
  }
}
</script>

<style lang="scss" scoped>
$navbar-height: 50px;
$breakpoint: 768px;

/* ── Theme tokens ── */
.home-navbar {
  /* navbar */
  --nb-bg: rgba(255, 255, 255, 0.82);
  --nb-shadow: 0 1px 0 rgba(0, 0, 0, 0.05);
  /* text */
  --nb-text: #303133;
  --nb-text-secondary: #606266;
  --nb-text-active: #6366f1;
  --nb-menu-hover: rgba(0, 0, 0, 0.03);
  --nb-border-active: #6366f1;
  /* misc */
  --nb-overlay: rgba(255, 255, 255, 0.97);
  --nb-divider: #ebeef5;
  --nb-hamburger-hover: #f5f7fa;
  --nb-hamburger-line: #303133;
}

html.dark .home-navbar {
  --nb-bg: rgba(10, 10, 15, 0.85);
  --nb-shadow: 0 1px 0 rgba(255, 255, 255, 0.04);
  --nb-text: #f0f0f5;
  --nb-text-secondary: #9ca3b0;
  --nb-text-active: #a5b4fc;
  --nb-menu-hover: rgba(255, 255, 255, 0.04);
  --nb-border-active: #6366f1;
  --nb-overlay: rgba(10, 10, 15, 0.97);
  --nb-divider: rgba(255, 255, 255, 0.06);
  --nb-hamburger-hover: rgba(255, 255, 255, 0.06);
  --nb-hamburger-line: #f0f0f5;
}

/* ── Navbar base ── */
.home-navbar {
  flex-shrink: 0;
  height: $navbar-height;
  z-index: 1000;
  background: var(--nb-bg);
  backdrop-filter: blur(12px);
  box-shadow: var(--nb-shadow);
  position: relative;
}

.navbar-inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-left {
  display: flex;
  align-items: center;
  height: 100%;
}

.navbar-logo {
  display: flex;
  align-items: center;
  text-decoration: none;
  margin-right: 32px;
  flex-shrink: 0;

  .logo-img {
    width: 28px;
    height: 28px;
    margin-right: 8px;
  }

  .logo-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--nb-text);
    white-space: nowrap;
  }
}

/* ── Desktop menu ── */
.desktop-menu {
  border-bottom: none !important;
  height: $navbar-height;
  background-color: transparent !important;

  :deep(.el-menu-item) {
    height: $navbar-height;
    line-height: $navbar-height;
    font-size: 14px;
    color: var(--nb-text-secondary) !important;
    border-bottom-color: transparent !important;

    &:hover,
    &:focus {
      color: var(--nb-text) !important;
      background-color: var(--nb-menu-hover) !important;
    }

    &.is-active {
      color: var(--nb-text-active) !important;
      border-bottom-color: var(--nb-border-active) !important;
    }
  }

  &.el-menu--horizontal {
    border-bottom: none;
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.user-area {
  display: flex;
  align-items: center;
}

.console-entry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: $navbar-height;
  padding: 0 20px;
  color: var(--nb-text-secondary);
  font-size: 14px;
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.2s, background-color 0.2s;

  &:hover {
    color: var(--nb-text);
    background-color: var(--nb-menu-hover);
  }
}

.avatar-container {
  cursor: pointer;

  .avatar-wrapper {
    display: flex;
    align-items: center;

    .user-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      margin-right: 6px;
    }

    .user-nickname {
      font-size: 13px;
      font-weight: 500;
      color: var(--nb-text);
    }
  }
}

/* ── Hamburger (hidden on desktop) ── */
.hamburger-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s;

  &:hover {
    background-color: var(--nb-hamburger-hover);
  }
}

.hamburger-icon {
  width: 20px;
  height: 14px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  .line {
    display: block;
    width: 100%;
    height: 2px;
    background-color: var(--nb-hamburger-line);
    border-radius: 1px;
    transition: all 0.3s ease;
    transform-origin: center;
  }

  &.is-active {
    .line:nth-child(1) {
      transform: translateY(6px) rotate(45deg);
    }
    .line:nth-child(2) {
      opacity: 0;
    }
    .line:nth-child(3) {
      transform: translateY(-6px) rotate(-45deg);
    }
  }
}

/* ── Mobile overlay ── */
.mobile-overlay {
  position: absolute;
  top: $navbar-height;
  left: 0;
  right: 0;
  height: calc(100vh - #{$navbar-height});
  background: var(--nb-overlay);
  z-index: 999;
  overflow-y: auto;
}

.mobile-nav {
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
}

.mobile-nav-item {
  display: block;
  padding: 14px 16px;
  font-size: 16px;
  font-weight: 500;
  color: var(--nb-text-secondary);
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(12px);

  &:hover {
    background-color: var(--nb-menu-hover);
    color: var(--nb-text-active);
  }

  &.active {
    color: var(--nb-text-active);
    background-color: rgba(99, 102, 241, 0.08);
  }
}

/* Overlay open → show items */
.mobile-overlay .mobile-nav-item {
  opacity: 1;
  transform: translateY(0);
}

.mobile-nav-divider {
  height: 1px;
  background-color: var(--nb-divider);
  margin: 12px 16px;
}

.mobile-nav-actions {
  padding: 8px 16px;

  .mobile-login-btn {
    width: 100%;
  }
}

/* ── Transition ── */
.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.25s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}

/* ── Responsive: mobile ── */
@media (max-width: #{$breakpoint - 1px}) {
  .navbar-logo {
    margin-right: 0;
  }

  .navbar-inner {
    padding: 0 16px;
  }

  .desktop-menu {
    display: none !important;
  }

  .hamburger-btn {
    display: flex;
  }

  .user-nickname {
    display: none;
  }

  .console-entry {
    padding: 0 12px;
  }
}
</style>
