<template>
  <div class="auth-page login">
    <main class="auth-shell">
      <section class="auth-brand">
        <div class="brand-lockup">
          <div class="brand-mark">
            <img :src="logo" class="brand-logo" alt="" />
          </div>
          <h1 class="brand-title">{{ title }}</h1>
        </div>
        <el-tag class="brand-tag" type="primary" effect="light" round>个人自用</el-tag>
      </section>

      <section class="auth-panel">
        <div class="auth-card">

          <el-segmented v-model="loginMode" :options="loginModeOptions" class="auth-mode-switch" />

          <Transition
            mode="out-in"
            @before-enter="beforeAuthFormEnter"
            @enter="enterAuthForm"
            @after-enter="resetAuthFormTransition"
            @enter-cancelled="resetAuthFormTransition"
            @before-leave="beforeAuthFormLeave"
            @leave="leaveAuthForm"
            @after-leave="resetAuthFormTransition"
            @leave-cancelled="resetAuthFormTransition"
          >
            <el-form v-if="loginMode === 'password'" key="password" ref="loginRef" :model="loginForm" :rules="loginRules" class="auth-form login-form">
              <el-form-item prop="username">
                <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号">
                  <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  size="large"
                  auto-complete="off"
                  placeholder="密码"
                  @keyup.enter="handleLogin"
                >
                  <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="code" v-if="captchaEnabled">
                <div class="auth-code-row">
                  <el-input
                    v-model="loginForm.code"
                    size="large"
                    auto-complete="off"
                    placeholder="验证码"
                    @keyup.enter="handleLogin"
                  >
                    <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
                  </el-input>
                  <button class="auth-code-image" type="button" aria-label="刷新验证码" @click="getCode">
                    <img :src="codeUrl" alt="验证码" />
                  </button>
                </div>
              </el-form-item>
              <div class="auth-options">
                <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
              </div>
              <el-form-item>
                <el-button :loading="loading" size="large" type="primary" class="auth-submit" @click.prevent="handleLogin">
                  <span v-if="!loading">登录</span>
                  <span v-else>登录中...</span>
                </el-button>
              </el-form-item>
            </el-form>

            <el-form v-else key="apikey" ref="apiKeyRef" :model="apiKeyForm" :rules="apiKeyRules" class="auth-form login-form">
              <el-form-item prop="apiKey">
                <el-input
                  v-model="apiKeyForm.apiKey"
                  type="text"
                  size="large"
                  auto-complete="off"
                  placeholder="请输入 API Key（sk-xxx）"
                  @keyup.enter="handleApiKeyLogin"
                >
                  <template #prefix><svg-icon icon-class="key" class="el-input__icon input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button :loading="loading" size="large" type="primary" class="auth-submit" @click.prevent="handleApiKeyLogin">
                  <span v-if="!loading">登录</span>
                  <span v-else>登录中...</span>
                </el-button>
              </el-form-item>
            </el-form>
          </Transition>

          <el-dropdown
            v-if="loginMode === 'password' && (passwordResetEnabled || register)"
            class="auth-switch auth-dropdown"
            placement="bottom-end"
            trigger="click"
            @command="handleAuthSwitchCommand"
          >
            <button class="auth-dropdown-trigger" type="button">
              <span>更多操作</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="passwordResetEnabled" command="forgot-password">忘记密码</el-dropdown-item>
                <el-dropdown-item v-if="register" command="register" :divided="passwordResetEnabled">立即注册</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </section>
    </main>

    <div class="auth-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getCodeImg, getPasswordResetStatus as fetchPasswordResetStatus, getRegisterStatus as fetchRegisterStatus } from '@/api/login'
import { ArrowDown } from '@element-plus/icons-vue'
import type { LoginMode } from '@/types/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import logo from '@/assets/logo/logo.png'
import type { CaptchaInfoResult } from '@/types/api/login'
import type { LoginForm } from '@/types/api/login'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref<LoginForm>({
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
})

// 登录模式：password=账密登录，apikey=密钥登录
const loginMode = ref<LoginMode>('password')
const loginModeOptions: Array<{ label: string; value: LoginMode }> = [
  { label: '账号', value: 'password' },
  { label: 'API Key', value: 'apikey' }
]

// 密钥登录表单
const apiKeyForm = ref({ apiKey: '' })

// 密钥登录校验规则
const apiKeyRules = {
  apiKey: [{ required: true, trigger: 'blur', message: '请输入 API Key' }]
}

const loginRules = {
  username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
  password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

const codeUrl = ref('')
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(false)
// 注册开关
const register = ref(false)
// 密码重置功能开关
const passwordResetEnabled = ref(false)
const redirect = ref<string | undefined>(undefined)
const authFormTransitionDuration = 260

watch(
  route,
  (newRoute: any) => {
    redirect.value = (newRoute.query && newRoute.query.redirect) as string | undefined
  },
  { immediate: true }
)

function handleLogin(): void {
  proxy.$refs.loginRef.validate((valid: boolean) => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set('username', loginForm.value.username, { expires: 30 })
        Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove('username')
        Cookies.remove('password')
        Cookies.remove('rememberMe')
      }
      // 调用action的登录方法
      userStore
        .login(loginForm.value)
        .then(() => {
          const query = route.query
          const otherQueryParams = Object.keys(query).reduce((acc: Record<string, any>, cur) => {
            if (cur !== 'redirect') {
              acc[cur] = query[cur]
            }
            return acc
          }, {})
          router.push({ path: redirect.value || '/index', query: otherQueryParams })
        })
        .catch(() => {
          loading.value = false
          // 重新获取验证码
          if (captchaEnabled.value) {
            getCode()
          }
        })
    }
  })
}

function handleApiKeyLogin(): void {
  proxy.$refs.apiKeyRef.validate((valid: boolean) => {
    if (valid) {
      loading.value = true
      userStore
        .apiKeyLogin(apiKeyForm.value.apiKey)
        .then(() => {
          const query = route.query
          const otherQueryParams = Object.keys(query).reduce((acc: Record<string, any>, cur) => {
            if (cur !== 'redirect') {
              acc[cur] = query[cur]
            }
            return acc
          }, {})
          router.push({ path: redirect.value || '/index', query: otherQueryParams })
        })
        .catch(() => {
          loading.value = false
        })
    }
  })
}

function handleAuthSwitchCommand(command: string): void {
  if (command === 'forgot-password') {
    router.push('/forgot-password')
    return
  }
  if (command === 'register') {
    router.push('/register')
  }
}

function beforeAuthFormEnter(el: Element): void {
  const element = el as HTMLElement
  element.style.height = '0'
  element.style.opacity = '0'
  element.style.overflow = 'hidden'
  element.style.transform = 'translateY(6px)'
}

function enterAuthForm(el: Element, done: () => void): void {
  const element = el as HTMLElement
  const targetHeight = element.scrollHeight
  element.style.transition = `height ${authFormTransitionDuration}ms ease, opacity 180ms ease, transform 180ms ease`

  requestAnimationFrame(() => {
    element.style.height = `${targetHeight}px`
    element.style.opacity = '1'
    element.style.transform = 'translateY(0)'
  })

  window.setTimeout(done, authFormTransitionDuration)
}

function beforeAuthFormLeave(el: Element): void {
  const element = el as HTMLElement
  element.style.height = `${element.scrollHeight}px`
  element.style.opacity = '1'
  element.style.overflow = 'hidden'
  element.style.transform = 'translateY(0)'
}

function leaveAuthForm(el: Element, done: () => void): void {
  const element = el as HTMLElement
  element.style.transition = `height ${authFormTransitionDuration}ms ease, opacity 160ms ease, transform 160ms ease`

  requestAnimationFrame(() => {
    element.style.height = '0'
    element.style.opacity = '0'
    element.style.transform = 'translateY(-6px)'
  })

  window.setTimeout(done, authFormTransitionDuration)
}

function resetAuthFormTransition(el: Element): void {
  const element = el as HTMLElement
  element.style.height = ''
  element.style.opacity = ''
  element.style.overflow = ''
  element.style.transform = ''
  element.style.transition = ''
}

function getCode(): void {
  getCodeImg().then((res) => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie(): void {
  const queryUsername = typeof route.query.username === 'string' ? route.query.username : undefined
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  loginForm.value = {
    username: queryUsername || (username === undefined ? loginForm.value.username : username),
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

function getPasswordResetStatus(): void {
  fetchPasswordResetStatus()
    .then((res) => {
      passwordResetEnabled.value = res.resetEnabled === true
    })
    .catch(() => {
      passwordResetEnabled.value = false
    })
}

function getRegisterStatus(): void {
  fetchRegisterStatus()
    .then((res) => {
      register.value = res.registerEnabled === true
    })
    .catch(() => {
      register.value = false
    })
}

getCode()
getCookie()
getRegisterStatus()
getPasswordResetStatus()
</script>

<style lang="scss" scoped>
@use '../assets/styles/auth-page.scss' as auth;

@include auth.auth-page;

.auth-card {
  position: relative;
  padding-bottom: 40px;
}

.auth-dropdown {
  position: absolute;
  right: 28px;
  bottom: 18px;
  margin: 0;
  line-height: 1;
  text-align: right;
}

.auth-dropdown-trigger {
  display: inline-flex;
  padding: 4px 0;
  align-items: center;
  gap: 4px;
  border: 0;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
  font: inherit;
  font-weight: 600;
}

.auth-dropdown-trigger:focus-visible {
  border-radius: 4px;
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}

@media (max-width: 480px) {
  .auth-card {
    padding-bottom: 54px;
  }

  .auth-dropdown {
    right: 18px;
    bottom: 16px;
  }
}
</style>
