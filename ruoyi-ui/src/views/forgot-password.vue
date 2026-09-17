<template>
  <div class="auth-page forgot-password">
    <main class="auth-shell">
      <section class="auth-brand">
        <div class="brand-lockup">
          <div class="brand-mark">
            <img :src="logo" class="brand-logo" alt="" />
          </div>
          <h1 class="brand-title">{{ title }}</h1>
        </div>
      </section>

      <section class="auth-panel">
        <div class="auth-card">
          <div class="auth-header">
            <h2 class="auth-heading">找回密码</h2>
            <p class="auth-eyebrow">邮箱验证</p>
          </div>

          <div v-if="statusLoading" class="status-loading">
            <el-skeleton :rows="4" animated />
          </div>

          <div v-else-if="!resetEnabled" class="reset-unavailable">
            <el-empty description="密码找回功能未启用，请联系管理员">
              <el-button type="primary" @click="returnToLogin">返回登录</el-button>
            </el-empty>
          </div>

          <template v-else>
            <el-steps :active="step - 1" simple finish-status="success" class="reset-steps">
              <el-step title="验证账号" />
              <el-step title="设置密码" />
            </el-steps>

            <el-form
              v-if="step === 1"
              ref="identityRef"
              :model="form"
              :rules="identityRules"
              class="auth-form"
            >
              <el-form-item prop="username">
                <el-input v-model.trim="form.username" size="large" autocomplete="username" placeholder="账号">
                  <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="email">
                <el-input v-model.trim="form.email" size="large" autocomplete="email" placeholder="绑定邮箱">
                  <template #prefix><svg-icon icon-class="email" class="el-input__icon input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item v-if="captchaEnabled" prop="code">
                <div class="auth-code-row">
                  <el-input
                    v-model="form.code"
                    size="large"
                    autocomplete="off"
                    placeholder="图形验证码"
                    @keyup.enter="handleSendEmailCode"
                  >
                    <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
                  </el-input>
                  <button class="auth-code-image" type="button" aria-label="刷新验证码" @click="getCode">
                    <img :src="codeUrl" alt="验证码" />
                  </button>
                </div>
              </el-form-item>
              <el-form-item>
                <el-button
                  :loading="emailCodeLoading"
                  size="large"
                  type="primary"
                  class="auth-submit"
                  @click.prevent="handleSendEmailCode"
                >
                  <span v-if="!emailCodeLoading">发送邮箱验证码</span>
                  <span v-else>发送中...</span>
                </el-button>
              </el-form-item>
              <div class="auth-switch">
                <span>想起密码了？</span>
                <router-link class="link-type" :to="'/login'">返回登录</router-link>
              </div>
            </el-form>

            <el-form
              v-else
              ref="resetRef"
              :model="form"
              :rules="resetRules"
              class="auth-form"
            >
              <div class="account-summary">
                <div><span>账号</span><strong>{{ form.username }}</strong></div>
                <div><span>邮箱</span><strong>{{ form.email }}</strong></div>
              </div>
              <el-form-item prop="emailCode">
                <el-input v-model.trim="form.emailCode" size="large" autocomplete="one-time-code" placeholder="邮箱验证码">
                  <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="newPassword" :rules="infoPwdValidator">
                <el-input
                  v-model="form.newPassword"
                  type="password"
                  show-password
                  size="large"
                  autocomplete="new-password"
                  placeholder="新密码"
                >
                  <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  show-password
                  size="large"
                  autocomplete="new-password"
                  placeholder="确认新密码"
                  @keyup.enter="handleResetPassword"
                >
                  <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
                </el-input>
              </el-form-item>
              <div class="reset-actions">
                <el-button link type="primary" @click="editIdentity">修改账号邮箱</el-button>
                <el-button link type="primary" :disabled="emailCodeCountdown > 0" @click="editIdentity">
                  {{ emailCodeCountdown > 0 ? emailCodeCountdown + 's后可重新获取' : '重新获取验证码' }}
                </el-button>
              </div>
              <el-form-item>
                <el-button
                  :loading="resetLoading"
                  size="large"
                  type="primary"
                  class="auth-submit"
                  @click.prevent="handleResetPassword"
                >
                  <span v-if="!resetLoading">重置密码</span>
                  <span v-else>重置中...</span>
                </el-button>
              </el-form-item>
            </el-form>
          </template>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import Cookies from 'js-cookie'
import { ElMessage } from 'element-plus'
import { getCodeImg, getPasswordResetStatus, resetPassword, sendPasswordResetEmailCode } from '@/api/login'
import { removeToken } from '@/utils/auth'
import { usePasswordRule } from '@/utils/passwordRule'
import useLockStore from '@/store/modules/lock'
import useUserStore from '@/store/modules/user'
import logo from '@/assets/logo/logo.png'
import type { ForgotPasswordForm } from '@/types'

const title = import.meta.env.VITE_APP_TITLE
const router = useRouter()
const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const lockStore = useLockStore()
const { pwdChrType, infoPwdValidator } = usePasswordRule()

const form = reactive<ForgotPasswordForm>({
  username: '',
  email: '',
  code: '',
  uuid: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: ''
})

const step = ref<number>(1)
const statusLoading = ref<boolean>(true)
const resetEnabled = ref<boolean>(false)
const captchaEnabled = ref<boolean>(false)
const codeUrl = ref<string>('')
const emailCodeLoading = ref<boolean>(false)
const resetLoading = ref<boolean>(false)
const emailCodeCountdown = ref<number>(0)
let emailCodeTimer: ReturnType<typeof setInterval> | undefined

const equalToPassword = (rule: any, value: string, callback: (error?: Error) => void): void => {
  if (form.newPassword !== value) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const identityRules = computed(() => ({
  username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
  email: [
    { required: true, trigger: 'blur', message: '请输入绑定邮箱' },
    { type: 'email' as const, trigger: ['blur', 'change'], message: '请输入正确的邮箱地址' },
    { max: 50, trigger: 'blur', message: '邮箱长度不能超过50个字符' }
  ],
  code: captchaEnabled.value ? [{ required: true, trigger: 'change', message: '请输入图形验证码' }] : []
}))

const resetRules = {
  emailCode: [{ required: true, trigger: 'change', message: '请输入邮箱验证码' }],
  confirmPassword: [
    { required: true, trigger: 'blur', message: '请再次输入新密码' },
    { validator: equalToPassword, trigger: 'blur' }
  ]
}

function loadStatus(): void {
  statusLoading.value = true
  getPasswordResetStatus()
    .then((res) => {
      resetEnabled.value = res.resetEnabled === true
      captchaEnabled.value = res.captchaEnabled === true
      pwdChrType.value = res.pwdChrtype || '0'
      if (resetEnabled.value && captchaEnabled.value) {
        getCode()
      }
    })
    .catch(() => {
      resetEnabled.value = false
    })
    .finally(() => {
      statusLoading.value = false
    })
}

function getCode(): void {
  getCodeImg().then((res) => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      form.uuid = res.uuid
      form.code = ''
    }
  })
}

function handleSendEmailCode(): void {
  proxy.$refs.identityRef.validate((valid: boolean) => {
    if (!valid) {
      return
    }
    emailCodeLoading.value = true
    sendPasswordResetEmailCode({
      username: form.username,
      email: form.email,
      code: form.code,
      uuid: form.uuid
    })
      .then(() => {
        ElMessage.success('邮箱验证码已发送')
        step.value = 2
        form.emailCode = ''
        startEmailCodeCountdown()
      })
      .catch(() => {
        if (captchaEnabled.value) {
          getCode()
        }
      })
      .finally(() => {
        emailCodeLoading.value = false
      })
  })
}

function handleResetPassword(): void {
  proxy.$refs.resetRef.validate((valid: boolean) => {
    if (!valid) {
      return
    }
    resetLoading.value = true
    resetPassword({
      username: form.username,
      email: form.email,
      emailCode: form.emailCode,
      newPassword: form.newPassword
    })
      .then(() => {
        clearEmailCodeTimer()
        Cookies.remove('password')
        Cookies.remove('rememberMe')
        removeToken()
        userStore.$reset()
        lockStore.unlockScreen()
        ElMessage.success('密码重置成功，请使用新密码登录')
        router.replace({ path: '/login', query: { username: form.username } })
      })
      .finally(() => {
        resetLoading.value = false
      })
  })
}

function editIdentity(): void {
  step.value = 1
  form.emailCode = ''
  form.newPassword = ''
  form.confirmPassword = ''
  clearEmailCodeTimer()
  if (captchaEnabled.value) {
    getCode()
  }
}

function startEmailCodeCountdown(): void {
  clearEmailCodeTimer()
  emailCodeCountdown.value = 60
  emailCodeTimer = setInterval(() => {
    emailCodeCountdown.value -= 1
    if (emailCodeCountdown.value <= 0) {
      clearEmailCodeTimer()
    }
  }, 1000)
}

function clearEmailCodeTimer(): void {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer)
    emailCodeTimer = undefined
  }
  emailCodeCountdown.value = 0
}

function returnToLogin(): void {
  router.push('/login')
}

onUnmounted(() => {
  clearEmailCodeTimer()
})

loadStatus()
</script>

<style lang="scss" scoped>
@use '../assets/styles/auth-page.scss' as auth;

@include auth.auth-page;

.reset-steps {
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
}

.status-loading {
  padding: 6px 0;
}

.reset-unavailable :deep(.el-empty) {
  padding: 12px 0 4px;
}

.account-summary {
  margin-bottom: 18px;
  padding: 12px 14px;
  border: 1px solid var(--auth-border);
  border-radius: 8px;
  background: var(--auth-soft);
  color: var(--auth-muted);
  font-size: 13px;
}

.account-summary div {
  display: flex;
  min-width: 0;
  justify-content: space-between;
  gap: 12px;
}

.account-summary div + div {
  margin-top: 7px;
}

.account-summary strong {
  min-width: 0;
  color: var(--auth-text);
  font-weight: 600;
  overflow-wrap: anywhere;
  text-align: right;
}

.reset-actions {
  display: flex;
  margin: -2px 0 14px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.reset-actions :deep(.el-button) {
  height: auto;
  margin: 0;
  padding: 2px 0;
}

@media (max-width: 480px) {
  .reset-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
