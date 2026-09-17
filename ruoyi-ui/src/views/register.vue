<template>
  <div class="auth-page register">
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
            <h2 class="auth-heading">创建账户</h2>
            <p class="auth-eyebrow">新用户注册</p>
          </div>

          <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="auth-form register-form">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" type="text" size="large" auto-complete="off" placeholder="账号">
                <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
              </el-input>
            </el-form-item>
            
            <el-form-item prop="password" :rules="registerPwdValidator">
              <el-input
                v-model="registerForm.password"
                type="password"
                size="large"
                auto-complete="off"
                placeholder="密码"
                @keyup.enter="handleRegister"
              >
                <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                size="large"
                auto-complete="off"
                placeholder="确认密码"
                @keyup.enter="handleRegister"
              >
                <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
              </el-input>
            </el-form-item>
            <el-form-item v-if="emailVerifyEnabled" prop="email">
              <el-input v-model="registerForm.email" type="text" size="large" auto-complete="off" placeholder="邮箱">
                <template #prefix><svg-icon icon-class="email" class="el-input__icon input-icon" /></template>
              </el-input>
            </el-form-item>
            <el-form-item v-if="emailVerifyEnabled" prop="emailCode">
              <div class="email-code-row">
                <el-input
                  v-model="registerForm.emailCode"
                  size="large"
                  auto-complete="off"
                  placeholder="邮箱验证码"
                  @keyup.enter="handleRegister"
                >
                  <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
                </el-input>
                <el-button
                  :loading="emailCodeLoading"
                  :disabled="emailCodeCountdown > 0 || !registerEnabled"
                  size="large"
                  @click.prevent="handleSendEmailCode"
                  >{{ emailCodeCountdown > 0 ? emailCodeCountdown + 's' : '发送验证码' }}</el-button
                >
              </div>
            </el-form-item>
            <el-form-item prop="code" v-if="captchaEnabled">
              <div class="auth-code-row">
                <el-input
                  size="large"
                  v-model="registerForm.code"
                  auto-complete="off"
                  placeholder="验证码"
                  @keyup.enter="handleRegister"
                >
                  <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
                </el-input>
                <button class="auth-code-image" type="button" aria-label="刷新验证码" @click="getCode">
                  <img :src="codeUrl" alt="验证码" />
                </button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button :loading="loading" size="large" type="primary" class="auth-submit" @click.prevent="handleRegister">
                <span v-if="!loading">注册</span>
                <span v-else>注册中...</span>
              </el-button>
            </el-form-item>
            <div class="auth-switch">
              <span>已有账户？</span>
              <router-link class="link-type" :to="'/login'">立即登录</router-link>
            </div>
          </el-form>
        </div>
      </section>
    </main>

    <!-- <div class="auth-footer">
      <span>{{ footerContent }}</span>
    </div> -->
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCodeImg, getRegisterStatus as fetchRegisterStatus, register, sendRegisterEmailCode } from '@/api/login'
import defaultSettings from '@/settings'
import logo from '@/assets/logo/logo.png'
import { usePasswordRule } from '@/utils/passwordRule'
import type { RegisterForm, RegisterPayload } from '@/types/api/login'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const { registerPwdValidator } = usePasswordRule()

/** 邀请返利邀请码（来自注册链接 ?invite=xxx，优先 url，其次 localStorage 跨页保留） */
const inviteCodeFromUrl = computed(() => {
  const fromQuery = (route.query.invite || route.query.aff) as string | undefined
  if (fromQuery) {
    return fromQuery
  }
  return localStorage.getItem('invite_referral_code') || undefined
})

const registerForm = ref<RegisterForm>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  emailCode: '',
  code: '',
  uuid: ''
})

const equalToPassword = (rule: any, value: string, callback: (error?: Error) => void): void => {
  if (registerForm.value.password !== value) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = computed(() => ({
  username: [
    { required: true, trigger: 'blur', message: '请输入您的账号' },
    { min: 5, max: 12, message: '用户账号长度必须介于 5 和 12 之间', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]+$/, message: '用户账号只能包含字母和数字', trigger: 'blur' }
  ],
  email: emailVerifyEnabled.value
    ? [
        { required: true, trigger: 'blur', message: '请输入您的邮箱' },
        { type: 'email' as const, message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] },
        {
          pattern: /^[A-Za-z0-9+_.-]+@(qq\.com|163\.com|gmail\.com)$/i,
          message: '仅支持QQ、163、Gmail邮箱',
          trigger: ['blur', 'change']
        }
      ]
    : [],
  confirmPassword: [
    { required: true, trigger: 'blur', message: '请再次输入您的密码' },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ],
  emailCode: emailVerifyEnabled.value
    ? [{ required: true, trigger: 'change', message: '请输入邮箱验证码' }]
    : [],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}))

const codeUrl = ref<string>('')
const loading = ref<boolean>(false)
const captchaEnabled = ref<boolean>(false)
const registerEnabled = ref<boolean>(false)
const emailVerifyEnabled = ref<boolean>(true)
const emailCodeLoading = ref<boolean>(false)
const emailCodeCountdown = ref<number>(0)
let emailCodeTimer: ReturnType<typeof setInterval> | undefined

function handleRegister(): void {
  proxy.$refs.registerRef.validate((valid: boolean) => {
    if (valid) {
      loading.value = true
      const { confirmPassword, ...payload } = registerForm.value
      // 邮箱验证关闭时清空邮箱相关字段
      if (!emailVerifyEnabled.value) {
        payload.email = ''
        payload.emailCode = ''
      }
      // 附带邀请码（邀请返利）
      if (inviteCodeFromUrl.value) {
        payload.inviteCode = inviteCodeFromUrl.value
        localStorage.setItem('invite_referral_code', inviteCodeFromUrl.value)
      }
      register(payload as RegisterPayload)
        .then(() => {
          const username = registerForm.value.username
          ElMessageBox.alert('恭喜你，您的账号 ' + username + ' 注册成功！', '系统提示', {
            type: 'success'
          })
            .then(() => {
              router.push('/login')
            })
            .catch(() => {})
        })
        .catch(() => {
          loading.value = false
          if (captchaEnabled.value) {
            getCode()
          }
        })
    }
  })
}

function handleSendEmailCode(): void {
  if (!registerEnabled.value || !emailVerifyEnabled.value || emailCodeLoading.value || emailCodeCountdown.value > 0) {
    return
  }
  proxy.$refs.registerRef.validateField('email', (valid: boolean) => {
    if (!valid) {
      return
    }
    emailCodeLoading.value = true
    sendRegisterEmailCode({ email: registerForm.value.email })
      .then(() => {
        ElMessage.success('邮箱验证码已发送')
        startEmailCodeCountdown()
      })
      .finally(() => {
        emailCodeLoading.value = false
      })
  })
}

function startEmailCodeCountdown(): void {
  emailCodeCountdown.value = 60
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer)
  }
  emailCodeTimer = setInterval(() => {
    emailCodeCountdown.value -= 1
    if (emailCodeCountdown.value <= 0 && emailCodeTimer) {
      clearInterval(emailCodeTimer)
      emailCodeTimer = undefined
    }
  }, 1000)
}

function checkRegisterStatus(): void {
  fetchRegisterStatus()
    .then((res) => {
      registerEnabled.value = res.registerEnabled === true
      emailVerifyEnabled.value = res.emailVerifyEnabled === true
      if (!registerEnabled.value) {
        ElMessage.error('当前系统没有开启注册功能')
        router.push('/login')
        return
      }
      getCode()
    })
    .catch(() => {
      router.push('/login')
    })
}

function getCode(): void {
  getCodeImg().then((res) => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      registerForm.value.uuid = res.uuid
    }
  })
}

onUnmounted(() => {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer)
  }
})

checkRegisterStatus()
</script>

<style lang="scss" scoped>
@use '../assets/styles/auth-page.scss' as auth;

@include auth.auth-page;
</style>
